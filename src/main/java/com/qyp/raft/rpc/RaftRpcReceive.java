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

package com.qyp.raft.rpc;

import com.qyp.raft.LeaderElection;
import com.qyp.raft.RaftServerRole;
import com.qyp.raft.cmd.RaftCommand;
import com.qyp.raft.cmd.StandardCommand;
import com.qyp.raft.data.ClusterStatus;
import com.qyp.raft.data.NodeStatus;

/**
 * 出来来自其它服务器的RPC请求
 *
 * @author yupeng.qin
 * @since 2018-03-13
 */
public class RaftRpcReceive implements RaftRpcReceiveService {

    private LeaderElection leaderElection;
    private ClusterStatus clusterStatus;
    private NodeStatus nodeStatus;

    /**
     * 处理投票请求
     * @param cmd  来自同级服务器的投票请求
     */
    @Override
    public RaftCommand dealWithVote(StandardCommand cmd) {
        if (nodeStatus.getSelf().equalsIgnoreCase(cmd.getTarget())) {
            int idx = -1;
            f:
            for (int i = 0; i < clusterStatus.getClusterMachine().length; i++) {
                String clusterMachine = clusterStatus.getClusterMachine()[i];
                if (clusterMachine.equalsIgnoreCase(cmd.getResource())) {
                    idx = i;
                    break f;
                }
            }
            if (idx > 0) {
                return leaderElection.dealWithVote(cmd.getResource());
            }
        }
        return RaftCommand.DENY;
    }

    /**
     * 处理心跳请求,  一般情况而言, 有心跳, 则一定是来自Leader的心跳.
     * 心跳的作用, 也是为了保持集群健康度使用.
     *
     * 心跳是经过了Leader端检测了的
     *
     * @param cmd   Leader的心跳
     */
    @Override
    public RaftCommand dealWithHeartBeat(StandardCommand cmd) {

        if (nodeStatus.getSelf().equalsIgnoreCase(cmd.getTarget())) {
            int term = Integer.valueOf(cmd.getTerm());
            if (term >= nodeStatus.getTerm()) {
                nodeStatus.setTerm(term);
                nodeStatus.setLeader(cmd.getResource());
                nodeStatus.setRole(RaftServerRole.FOLLOWER);
                nodeStatus.setVoteCount(-1);
                nodeStatus.setVoteFor(null);
            }
        }
        return RaftCommand.APPEND_ENTRIES;
    }
}
