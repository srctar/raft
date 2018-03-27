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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qyp.raft.cmd.RaftCommand;
import com.qyp.raft.cmd.StandardCommand;
import com.qyp.raft.data.ClusterRole;
import com.qyp.raft.data.ClusterRuntime;
import com.qyp.raft.data.RaftNodeRuntime;
import com.qyp.raft.data.RaftServerRole;
import com.qyp.raft.timer.HeartBeatTimer;

/**
 * 客户端的 Raft 服务
 *
 * @author yupeng.qin
 * @since 2018-03-13
 */
public class RaftClient {

    private static final Logger logger = LoggerFactory.getLogger(RaftClient.class);

    private LeaderElection leaderElection;
    private ClusterRuntime clusterRuntime;
    private HeartBeatTimer heartBeatTimer;
    private RaftNodeRuntime raftNodeRuntime;

    private static volatile Thread heartBeatThread = null;

    public RaftClient(LeaderElection leaderElection, ClusterRuntime clusterRuntime,
                      RaftNodeRuntime raftNodeRuntime, HeartBeatTimer heartBeatTimer) {
        this.leaderElection = leaderElection;
        this.clusterRuntime = clusterRuntime;
        this.heartBeatTimer = heartBeatTimer;
        this.raftNodeRuntime = raftNodeRuntime;

        heartBeatThread = new Thread(heartBeatTimer, heartBeatTimer.THREAD_NAME);
        heartBeatThread.start();
    }

    /**
     * 处理投票请求
     *
     * @param cmd 来自同级服务器的投票请求
     */
    public RaftCommand dealWithVote(StandardCommand cmd) {
        // 如果系统刚刚起步, 就接受到了别的服务器的投票申请, 则直接接受投票
        if (clusterRuntime.getClusterRole() == null) {
            clusterRuntime.setClusterRole(ClusterRole.ELECTION);
            return RaftCommand.ACCEPT;
        }
        if (raftNodeRuntime.getSelf().equalsIgnoreCase(cmd.getTarget())) {
            int idx = -1;
            f:
            for (int i = 0; i < clusterRuntime.getClusterMachine().length; i++) {
                String clusterMachine = clusterRuntime.getClusterMachine()[i];
                if (clusterMachine.equalsIgnoreCase(cmd.getResource())) {
                    idx = i;
                    break f;
                }
            }
            int term = Integer.valueOf(cmd.getTerm());
            if (idx > 0 && term >= raftNodeRuntime.getTerm()) {
                return leaderElection.dealWithVote(cmd.getResource());
            }
        }
        return RaftCommand.DENY;
    }

    /**
     * 处理心跳请求,  一般情况而言, 有心跳, 则一定是来自Leader的心跳.
     * 心跳的作用, 也是为了保持集群健康度使用.
     * <p>
     * 心跳是经过了Leader端检测了的, 同时, Follower 可以告诉 Leader
     *
     * @param cmd Leader的心跳
     */
    public RaftCommand dealWithHeartBeat(StandardCommand cmd) {

        if (logger.isDebugEnabled()) {
            logger.debug("当前节点:{}, 收到心跳:{}", raftNodeRuntime.getSelf(), cmd);
        }

        if (!raftNodeRuntime.getSelf().equalsIgnoreCase(cmd.getTarget())) {
            return RaftCommand.APPEND_ENTRIES_DENY;
        }
        int term = Integer.valueOf(cmd.getTerm());

        // 启动时期, 直接接受现存的服务器通知
        if (clusterRuntime.getClusterRole() == null) {
            buildCluster(term, cmd.getResource());
            clusterRuntime.setClusterRole(ClusterRole.ELECTION);
        }
        /**
         * 对于处于选举中的状态, 对所有的Leader声明请求表示赞许, 并立即转变为Follower
         * 老 Leader 在收到新 Leader 的心跳的时候, 也会自动转化为 Follower
         */
        if (clusterRuntime.getClusterRole() == ClusterRole.ELECTION) {
            if (term >= raftNodeRuntime.getTerm()) {

                buildCluster(term, cmd.getResource());

                logger.info("当前节点:{}, 既定Follower关系, Leader:{}, 准备心跳反射.",
                        raftNodeRuntime.getSelf(), raftNodeRuntime.getLeader());
                if (heartBeatThread.isAlive()) {
                    // 中断旧线程, 重新等待
                    heartBeatThread.interrupt();
                }
                // 在重建完选举线程之后,
                heartBeatThread = new Thread(heartBeatTimer, heartBeatTimer.THREAD_NAME);
                heartBeatThread.start();

                return RaftCommand.APPEND_ENTRIES;
            } else {
                return RaftCommand.APPEND_ENTRIES_DENY;
            }
        } else {
            // 如果集群依然处于工作中, 可能是
            // ① 当前的集群宕机, 其中一个机器发起选举, 但是接受的机器还没有超时.
            // ② 正常情况下的心跳检测.
            if (raftNodeRuntime.getLeader().equalsIgnoreCase(cmd.getResource())) {
                raftNodeRuntime.setLastHeartTime(System.currentTimeMillis());
                raftNodeRuntime.setTerm(term);
                raftNodeRuntime.setCurrentElectionTime(0);
                synchronized(heartBeatTimer) {
                    heartBeatTimer.notify();
                }
                return RaftCommand.APPEND_ENTRIES;
            }
            // 如果是自身是 老Leader
            if (raftNodeRuntime.getRole() == RaftServerRole.LEADER && term >= raftNodeRuntime.getTerm()) {
                buildCluster(term, cmd.getResource());
                raftNodeRuntime.setCurrentElectionTime(0);
                return RaftCommand.APPEND_ENTRIES;
            }
            return RaftCommand.APPEND_ENTRIES_AGAIN;
        }
    }

    // 成为 Follower
    private void buildCluster(int term, String leader) {
        raftNodeRuntime.setTerm(term);
        raftNodeRuntime.setLeader(leader);
        raftNodeRuntime.setRole(RaftServerRole.FOLLOWER);
        raftNodeRuntime.setVoteCount(-1);
        raftNodeRuntime.setVoteFor(null);

        clusterRuntime.setClusterRole(ClusterRole.PROCESSING);
    }

    private class ClientHeartDeamon implements Runnable {

        @Override
        public void run() {
            if (!heartBeatThread.isAlive()) {
                heartBeatThread = new Thread(heartBeatTimer, heartBeatTimer.THREAD_NAME);
                heartBeatThread.start();
            }
        }
    }
}
