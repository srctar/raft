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
import com.qyp.raft.data.ClusterRuntime;
import com.qyp.raft.data.RaftNodeRuntime;
import com.qyp.raft.rpc.RaftRpcLaunchService;

/**
 * Leader跟跟随者之间的交互
 *
 * @author yupeng.qin
 * @since 2018-03-13
 */
public class CommunicateFollower {

    private RaftNodeRuntime raftNodeRuntime;
    private ClusterRuntime clusterRuntime;

    private RaftRpcLaunchService raftRpcLaunchService;

    public CommunicateFollower(RaftNodeRuntime raftNodeRuntime, ClusterRuntime clusterRuntime,
                               RaftRpcLaunchService raftRpcLaunchService) {
        this.raftNodeRuntime = raftNodeRuntime;
        this.clusterRuntime = clusterRuntime;
        this.raftRpcLaunchService = raftRpcLaunchService;
    }

    public void heartBeat() {
        f:
        for (int i = 0; i < clusterRuntime.getClusterMachine().length; i++) {
            String clusterMachine = clusterRuntime.getClusterMachine()[i];
            if (!clusterMachine.equalsIgnoreCase(raftNodeRuntime.getSelf())) {
                try {
                    RaftCommand cmd = raftRpcLaunchService
                            .notifyFollower(raftNodeRuntime.getSelf(), clusterMachine, raftNodeRuntime.getTerm());
                    // 收到仆从机器的心跳反应有: APPEND_ENTRIES、APPEND_ENTRIES_DENY、APPEND_ENTRIES_AGAIN
                    if (cmd == RaftCommand.APPEND_ENTRIES_DENY) {
                        break f;
                    }
                } catch (IOException e) {
                }
            }
        }
    }

}
