/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  The ASF licenses
 * this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qyp.raft;

import java.io.IOException;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qyp.raft.cmd.RaftCommand;
import com.qyp.raft.cmd.Singleton;
import com.qyp.raft.data.ClusterRole;
import com.qyp.raft.data.ClusterRuntime;
import com.qyp.raft.data.RaftNodeRuntime;
import com.qyp.raft.data.RaftServerRole;
import com.qyp.raft.rpc.RaftRpcLaunchService;

/**
 * Leader 选举服务, 提供
 * 给自己投票、接受并赞成投票、发起投票申请的功能
 * <p>
 * 线程在如下case被调度:
 * ① 心跳等待超时 (150ms ~ 300ms)
 * <p>
 * 在每次投票阶段, 每个节点都可能处于如下case:
 * ① 简单的Follower, 等待其它机器的请求
 * ② 简单的Follower, 正在其它机器的请求
 * ③ Candidate, 已经给自己投了一票.
 *
 * @author yupeng.qin
 * @since 2018-03-13
 */
@Singleton
public class LeaderElection {

    private static final Logger logger = LoggerFactory.getLogger(LeaderElection.class);

    private RaftNodeRuntime raftNode;
    private ClusterRuntime cluster;

    private RaftRpcLaunchService raftRpcLaunchService;
    private RaftServer raftServer;

    public LeaderElection(RaftNodeRuntime raftNodeRuntime, ClusterRuntime clusterRuntime,
                          RaftRpcLaunchService raftRpcLaunchService, RaftServer raftServer) {
        this.raftNode = raftNodeRuntime;
        this.cluster = clusterRuntime;
        this.raftRpcLaunchService = raftRpcLaunchService;
        this.raftServer = raftServer;
    }

    /**
     * 处理来自别的机器的投票请求:
     * 只要自己是Follower, 立马对请求机器发起应答, 同意投票.
     * <p>
     * 对于多次申请投票的, 对于第二次及以后的一律拒绝. 但是当上次投票请求到来, 但是心跳没有到来的, 同理重置
     *
     * @param node 发起申请投票的机器
     */
    public RaftCommand dealWithVote(String node) {

        // 自己正在参选则不参加投票. 以有候选人则直接拒绝后续的投票.
        if (!cluster.isInElection() && raftNode.getVoteFor() == null) {

            raftNode.setVoteFor(node);
            // 通过设置之后心跳时间可以避免心跳超时.
            raftNode.setLastHeartTime(System.currentTimeMillis());
            cluster.setClusterRole(ClusterRole.ELECTION);
            logger.info("当前节点:{}, 角色:{}, 接受了{} 的申请票, ", raftNode.getSelf(), raftNode.getRole(), node);

            return RaftCommand.ACCEPT;
        }

        logger.info("当前节点:{} 拒绝了{} 的申请票, 因为当前角色投票给了:{} 或者当前角色是: {}, 或者集群非选举态.",
                raftNode.getSelf(), node, raftNode.getVoteFor(), raftNode.getRole());
        return RaftCommand.DENY;
    }

    /**
     * 由 Follower 向 Candidate 的转变, 先给自己投票, 再请求其它机器给自己投票.
     * 一台机器直接
     */
    public synchronized void requestVote() {

        logger.info("当前节点:{}, 角色:{}, 申请Leader选举, 成员组成数据:{}, 有:{}",
                raftNode.getSelf(), raftNode.getRole(), cluster.getClusterMachine().length,
                Arrays.toString(cluster.getClusterMachine()));

        long lastHeart = System.currentTimeMillis() - raftNode.getLastHeartTime();

        // 如果在休眠时间完成了选举并有了心跳, 则不会再选举
        if (lastHeart < RaftServer.HEART_TIME * 2
                // 如果在休眠时间接受了其它节点的投票, 也不会再选举
                || (raftNode.getVoteFor() != null)) {
            return;
        }

        // 集群中如果只有一台机器, 直接选举为Leader。
        if (cluster.getClusterMachine().length == 1) {
            becomeLeader();
            return;
        }

        cluster.setInElection(true);
        vote();
        if (raftNode.getRole() == RaftServerRole.CANDIDATE) {
            raftNode.setRole(RaftServerRole.FOLLOWER);
            raftNode.setVoteFor(null);
        }
        cluster.setInElection(false);
    }

    private void vote() {
        /**
         * 集群状态变更为选举中
         */
        cluster.setClusterRole(ClusterRole.ELECTION);

        raftNode.setRole(RaftServerRole.CANDIDATE);
        raftNode.setVoteFor(raftNode.getSelf());
        raftNode.setVoteCount(1);
        raftNode.setLeader(null);
        // 只在当前第一次选举的时候加一
        if (raftNode.getCurrentElectionTime() == 0) {
            raftNode.setTerm(raftNode.getTerm() + 1);
        }
        raftNode.setCurrentElectionTime(raftNode.getCurrentElectionTime() + 1);

        f:
        for (int i = 0; i < cluster.getClusterMachine().length; i++) {
            String clusterMachine = cluster.getClusterMachine()[i];
            if (raftNode.getRole() != RaftServerRole.CANDIDATE) {
                break f;
            }
            if (clusterMachine.equalsIgnoreCase(raftNode.getSelf())) {
                continue f;
            }
            /*
              给集群中, 除了自身机器之外的其它机器发起投票请求, 投票请求会立即得到答复.
            */
            logger.info("当前节点:{} 申请Leader选举, 申请Leader选举, 申请:{}的票", raftNode.getSelf(), clusterMachine);
            try {
                RaftCommand cmd = raftRpcLaunchService
                        .requestVote(raftNode.getSelf(), clusterMachine, raftNode.getTerm());
                if (cmd == RaftCommand.ACCEPT) {
                    logger.info("当前节点:{} 申请Leader选举, 申请Leader选举, {}投递赞成票", raftNode.getSelf(), clusterMachine);
                    raftNode.increaseVoteCount();
                    // 得到多数派的赞成 => 成为 Leader
                    // 同时周知 Leader 的状态信息
                    if (raftNode.getVoteCount() > cluster.getClusterMachine().length / 2) {
                        becomeLeader();
                        break f;
                    }
                } else if (cmd == RaftCommand.DENY) {
                    // 如果被拒绝, 则累加term. 重新选举
                } else {
                    logger.info("当前节点:{} 申请Leader选举, 申请Leader选举, {}表态为: {}",
                            raftNode.getSelf(), clusterMachine, cmd);
                }
            } catch (IOException e) {
                logger.error("当前节点:{}申请客户机投票:{}, 网络异常.", raftNode.getSelf(), clusterMachine, e);
            }
        }
    }

    // 成为 Leader 之后, 就不再持续被心跳扫描了. 但是对心跳的处理依然继续.
    public void becomeLeader() {
        raftNode.setRole(RaftServerRole.LEADER);
        raftNode.setLeader(raftNode.getSelf());
        int time = raftNode.getCurrentElectionTime();
        raftNode.setCurrentElectionTime(0);

        cluster.setClusterRole(ClusterRole.PROCESSING);
        raftServer.setRun(true);

        logger.info("当前节点:{} 成为了新一届的Leader, 选举了{}次, 自身内容: {}", raftNode.getSelf(), time, raftNode);
    }

}
