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

import com.qyp.raft.cmd.RaftCommand;
import com.qyp.raft.data.ClusterStatus;
import com.qyp.raft.data.NodeStatus;
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
public class LeaderElection implements Runnable {

    private static final Object LOCK = new Object();

    private NodeStatus nodeStatus;
    private ClusterStatus clusterStatus;

    private RaftRpcLaunchService raftRpcLaunchService;
    private CommunicateFollower communicateFollower;

    /**
     * 是否接受来自其它服务器的选举申请
     */
    private boolean beginToAcceptOtherVote;

    @Override
    public void run() {

    }

    /**
     * 处理来自别的机器的投票请求:
     * 只要自己是Follower, 立马对请求机器发起应答, 同意投票.
     *
     * @param node 发起申请投票的机器
     */
    public RaftCommand dealWithVote(String node) {
        if (nodeStatus.getRole() == RaftServerRole.FOLLOWER) {
            if (nodeStatus.getVoteFor() == null) {
                synchronized(LOCK) {
                    if (nodeStatus.getVoteFor() == null
                            && nodeStatus.getRole() == RaftServerRole.FOLLOWER) {
                        nodeStatus.setVoteFor(node);
                        return RaftCommand.ACCEPT;
                    }
                }
            }
        }
        return RaftCommand.DENY;
    }

    /**
     * 由 Follower 向 Candidate 的转变, 先给自己投票, 再请求其它机器给自己投票.
     */
    public void requestVote() {

        if (nodeStatus.getRole() != RaftServerRole.FOLLOWER) {
            return;
        }

        nodeStatus.setRole(RaftServerRole.CANDIDATE);
        nodeStatus.setVoteFor(nodeStatus.getSelf());
        nodeStatus.setVoteCount(1);
        nodeStatus.setTerm(nodeStatus.getTerm() + 1);

        // 终止条件为: 自己被选举成功/别的机器被选举成功
        while (nodeStatus.getRole() == RaftServerRole.CANDIDATE) {
            f:
            for (int i = 0; i < clusterStatus.getClusterMachine().length; i++) {
                String clusterMachine = clusterStatus.getClusterMachine()[i];
                /*
                给集群中, 除了自身机器之外的其它机器发起投票请求, 投票请求会立即得到答复.
                 */
                if (!clusterMachine.equalsIgnoreCase(nodeStatus.getSelf())
                        && nodeStatus.getRole() == RaftServerRole.CANDIDATE) {
                    try {
                        RaftCommand cmd = raftRpcLaunchService
                                .requestVote(nodeStatus.getSelf(), clusterMachine, nodeStatus.getTerm());
                        if (cmd == RaftCommand.ACCEPT) {
                            nodeStatus.increaseVoteCount();
                            // 得到多数派的赞成 => 成为 Leader
                            // 同时周知 Leader 的状态信息
                            if (nodeStatus.getVoteCount() > clusterStatus.getClusterMachine().length / 2) {
                                nodeStatus.setRole(RaftServerRole.LEADER);
                                nodeStatus.setLeader(nodeStatus.getSelf());

                                communicateFollower.heartBeat();
                                break f;
                            }
                        }
                    } catch (IOException e) {
                    }
                }
            }
        }
    }

}
