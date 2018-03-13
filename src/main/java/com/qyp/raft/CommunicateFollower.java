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

import com.qyp.raft.data.ClusterStatus;
import com.qyp.raft.data.NodeStatus;
import com.qyp.raft.rpc.RaftRpcLaunchService;

/**
 * Leader跟跟随者之间的交互
 *
 * @author yupeng.qin
 * @since 2018-03-13
 */
public class CommunicateFollower {

    private NodeStatus nodeStatus;
    private ClusterStatus clusterStatus;

    private RaftRpcLaunchService raftRpcLaunchService;

    public void heartBeat() {
        f:
        for (int i = 0; i < clusterStatus.getClusterMachine().length; i++) {
            String clusterMachine = clusterStatus.getClusterMachine()[i];
            if (!clusterMachine.equalsIgnoreCase(nodeStatus.getSelf())) {
                try {
                    raftRpcLaunchService
                            .notifyFollower(nodeStatus.getSelf(), clusterMachine, nodeStatus.getTerm());
                } catch (IOException e) {
                }
            }
        }
    }

}
