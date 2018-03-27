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

    private static final Object LOCK = new Object();

    private RaftNodeRuntime raftNodeRuntime;
    private ClusterRuntime clusterRuntime;

    private RaftRpcLaunchService raftRpcLaunchService;
    private RaftServer raftServer;

    public LeaderElection(RaftNodeRuntime raftNodeRuntime, ClusterRuntime clusterRuntime,
                          RaftRpcLaunchService raftRpcLaunchService, RaftServer raftServer) {
        this.raftNodeRuntime = raftNodeRuntime;
        this.clusterRuntime = clusterRuntime;
        this.raftRpcLaunchService = raftRpcLaunchService;
        this.raftServer = raftServer;
    }

    /**
     * 处理来自别的机器的投票请求:
     * 只要自己是Follower, 立马对请求机器发起应答, 同意投票.
     *
     * @param node 发起申请投票的机器
     */
    public RaftCommand dealWithVote(String node) {
        if (raftNodeRuntime.getRole() == RaftServerRole.FOLLOWER && clusterRuntime.getClusterRole() == ClusterRole.ELECTION) {
            if (raftNodeRuntime.getVoteFor() == null) {
                synchronized(LOCK) {
                    if (raftNodeRuntime.getVoteFor() == null
                            && raftNodeRuntime.getRole() == RaftServerRole.FOLLOWER
                            && clusterRuntime.getClusterRole() == ClusterRole.ELECTION) {
                        raftNodeRuntime.setVoteFor(node);
                        logger.info("当前节点:{} 接受了 {} 的申请票, 等待接受心跳成为 Follower!!!!!!!!!!!!!!!!!!!",
                                raftNodeRuntime.getSelf(), node, raftNodeRuntime.getRole(), raftNodeRuntime.getRole());
                        return RaftCommand.ACCEPT;
                    }
                    logger.info("当前节点:{} 拒绝了 {} 的申请票, 因为当前角色投票给了:{} 或者当前角色是: {}",
                            raftNodeRuntime.getSelf(), node, raftNodeRuntime.getRole(), raftNodeRuntime.getRole());
                }
            }
        }
        return RaftCommand.DENY;
    }

    /**
     * 由 Follower 向 Candidate 的转变, 先给自己投票, 再请求其它机器给自己投票.
     * 一台机器直接
     */
    public void requestVote() {

        logger.info("当前节点:{}, 角色:{}, 申请 Leader 选举, 成员组成数据:{}, 有:{}",
                raftNodeRuntime.getSelf(), raftNodeRuntime.getRole(),
                clusterRuntime.getClusterMachine().length, Arrays.toString(clusterRuntime.getClusterMachine()));
        if (raftNodeRuntime.getRole() != RaftServerRole.FOLLOWER
                || (System.currentTimeMillis() - raftNodeRuntime.getLastHeartTime()) < RaftServer.HEART_TIME * 2) {
            return;
        }

        // 集群中如果只有一台机器, 直接选举为Leader。
        if (clusterRuntime.getClusterMachine().length == 1) {
            becomeLeader();
            return;
        }

        /**
         * 集群状态变更为选举中
         */
        clusterRuntime.setClusterRole(ClusterRole.ELECTION);

        raftNodeRuntime.setRole(RaftServerRole.CANDIDATE);
        raftNodeRuntime.setVoteFor(raftNodeRuntime.getSelf());
        raftNodeRuntime.setVoteCount(1);
        // 只在当前第一次选举的时候加一
        if (raftNodeRuntime.getCurrentElectionTime() == 0) {
            raftNodeRuntime.setTerm(raftNodeRuntime.getTerm() + 1);
        }
        raftNodeRuntime.setCurrentElectionTime(raftNodeRuntime.getCurrentElectionTime() + 1);

        f:
        for (int i = 0; i < clusterRuntime.getClusterMachine().length; i++) {
            String clusterMachine = clusterRuntime.getClusterMachine()[i];
            if (raftNodeRuntime.getRole() != RaftServerRole.CANDIDATE) {
                break f;
            }
            if (clusterMachine.equalsIgnoreCase(raftNodeRuntime.getSelf())) {
                continue f;
            }
            /*
              给集群中, 除了自身机器之外的其它机器发起投票请求, 投票请求会立即得到答复.
            */
            logger.info("当前节点:{} 申请Leader选举, 申请 Leader选举, 申请:{}的票", raftNodeRuntime.getSelf(), clusterMachine);
            try {
                RaftCommand cmd = raftRpcLaunchService
                        .requestVote(raftNodeRuntime.getSelf(), clusterMachine, raftNodeRuntime.getTerm());
                if (cmd == RaftCommand.ACCEPT) {
                    logger.info("当前节点:{} 申请Leader选举, 申请Leader选举, {}投递赞成票", raftNodeRuntime.getSelf(), clusterMachine);
                    raftNodeRuntime.increaseVoteCount();
                    // 得到多数派的赞成 => 成为 Leader
                    // 同时周知 Leader 的状态信息
                    if (raftNodeRuntime.getVoteCount() > clusterRuntime.getClusterMachine().length / 2) {
                        becomeLeader();
                        break f;
                    }
                } else {
                    logger.info("当前节点:{} 申请Leader选举, 申请Leader选举, {}表态为: {}",
                            raftNodeRuntime.getSelf(), clusterMachine, cmd);
                }
            } catch (IOException e) {
                logger.error("当前节点:{}申请客户机投票:{}, 网络异常.", raftNodeRuntime.getSelf(), clusterMachine, e);
            }
        }
        if (raftNodeRuntime.getRole() == RaftServerRole.CANDIDATE) {
            raftNodeRuntime.setRole(RaftServerRole.FOLLOWER);
            raftNodeRuntime.setVoteFor(null);
        }
    }

    // 成为 Leader 之后, 就不再持续被心跳扫描了. 但是对心跳的处理依然继续.
    public void becomeLeader() {
        raftNodeRuntime.setRole(RaftServerRole.LEADER);
        raftNodeRuntime.setLeader(raftNodeRuntime.getSelf());
        int time = raftNodeRuntime.getCurrentElectionTime();
        raftNodeRuntime.setCurrentElectionTime(0);

        clusterRuntime.setClusterRole(ClusterRole.PROCESSING);
        raftServer.setRun(true);

        logger.info("当前节点:{} 成为了新一届的Leader, 选举了{}次, 自身内容: {}", raftNodeRuntime.getSelf(), time, raftNodeRuntime);
    }

}
