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

import com.qyp.raft.data.ClusterRuntime;
import com.qyp.raft.data.RaftServerRuntime;
import com.qyp.raft.rpc.RaftRpcLaunchService;

/**
 * Leader跟跟随者之间的交互
 *
 * @author yupeng.qin
 * @since 2018-03-13
 */
public class CommunicateFollower {

    private RaftServerRuntime raftServerRuntime;
    private ClusterRuntime clusterRuntime;

    private RaftRpcLaunchService raftRpcLaunchService;

    public void heartBeat() {
        f:
        for (int i = 0; i < clusterRuntime.getClusterMachine().length; i++) {
            String clusterMachine = clusterRuntime.getClusterMachine()[i];
            if (!clusterMachine.equalsIgnoreCase(raftServerRuntime.getSelf())) {
                try {
                    raftRpcLaunchService
                            .notifyFollower(raftServerRuntime.getSelf(), clusterMachine, raftServerRuntime.getTerm());
                } catch (IOException e) {
                }
            }
        }
    }

}
