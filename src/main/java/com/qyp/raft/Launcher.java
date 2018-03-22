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

import com.qyp.raft.data.ClusterRuntime;
import com.qyp.raft.data.RaftNodeRuntime;
import com.qyp.raft.rpc.RaftRpcLaunch;
import com.qyp.raft.rpc.RaftRpcLaunchService;
import com.qyp.raft.rpc.RaftRpcReceive;
import com.qyp.raft.rpc.RaftRpcReceiveService;

/**
 * Raft 的启动器.
 *
 * @author yupeng.qin
 * @since 2018-03-19
 */
public class Launcher {

    // 入口服务
    private RaftRpcReceiveService receiveService;
    private RaftRpcLaunchService launchService;
    private CommunicateFollower communicateFollower;

    private RaftServer raftServer;
    private RaftClient raftClient;

    private LeaderElection leaderElection;
    private ClusterRuntime clusterRuntime;
    private RaftNodeRuntime raftNodeRuntime;

    public Launcher(RaftRpcReceiveService receiveService, RaftRpcLaunchService launchService) {
        this.receiveService = receiveService;
        this.launchService = launchService;
    }

    public Launcher() {

        clusterRuntime = new ClusterRuntime();
        raftNodeRuntime = new RaftNodeRuntime();
        launchService = new RaftRpcLaunch();

        communicateFollower = new CommunicateFollower(raftNodeRuntime, clusterRuntime, launchService);

        raftServer = new RaftServer(communicateFollower);
        leaderElection = new LeaderElection(raftNodeRuntime, clusterRuntime, launchService, raftServer);

        raftClient = new RaftClient(leaderElection, clusterRuntime, raftNodeRuntime);

        receiveService = new RaftRpcReceive(raftClient, raftServer);

    }
}
