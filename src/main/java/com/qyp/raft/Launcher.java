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

import java.util.HashSet;
import java.util.Set;

import com.qyp.raft.data.ClusterRuntime;
import com.qyp.raft.data.RaftNodeRuntime;
import com.qyp.raft.data.RaftServerRole;
import com.qyp.raft.data.t.ObjectDataTranslateService;
import com.qyp.raft.rpc.CommunicateFollower;
import com.qyp.raft.rpc.RaftRpcLaunch;
import com.qyp.raft.rpc.RaftRpcLaunchService;
import com.qyp.raft.rpc.RaftRpcReceive;
import com.qyp.raft.timer.HeartBeatTimer;

/**
 * Raft 的启动器.
 *
 * @author yupeng.qin
 * @since 2018-03-19
 */
public class Launcher {

    // 入口服务
    private RaftRpcReceive receiveService;
    private RaftRpcLaunchService launchService;
    private CommunicateFollower communicateFollower;

    private RaftServer raftServer;
    private RaftClient raftClient;

    private LeaderElection leaderElection;
    private volatile ClusterRuntime clusterRuntime;
    private volatile RaftNodeRuntime raftNodeRuntime;

    private HeartBeatTimer heartBeatTimer;

    // 使用前需要构造器初始化
    private static RaftSync sync;

    public Launcher(RaftRpcLaunchService launchService,
                    String ip, int port, Set<String> cluster, boolean userSystemPort) {
        this(ip, port, cluster, userSystemPort);
        this.launchService = launchService;
    }

    public Launcher(String ip, int port, Set<String> cluster, boolean userSystemPort) {

        String contract = ip + ":" + port;
        if (cluster == null) {
            cluster = new HashSet<>();
        }
        if (!cluster.contains(contract)) {
            cluster.add(contract);
        }

        new ObjectDataTranslateService();

        clusterRuntime = new ClusterRuntime();
        clusterRuntime.setClusterMachine(cluster.toArray(new String[]{}));
        raftNodeRuntime = new RaftNodeRuntime();
        raftNodeRuntime.setRole(RaftServerRole.FOLLOWER);
        raftNodeRuntime.setSelf(contract);
        launchService = new RaftRpcLaunch();

        communicateFollower = new CommunicateFollower(raftNodeRuntime, clusterRuntime, launchService);

        raftServer = new RaftServer(communicateFollower);
        leaderElection = new LeaderElection(raftNodeRuntime, clusterRuntime, launchService, raftServer);

        heartBeatTimer = new HeartBeatTimer(raftNodeRuntime, leaderElection);
        raftClient = new RaftClient(leaderElection, clusterRuntime, raftNodeRuntime, heartBeatTimer);

        if (userSystemPort) {
            receiveService = new RaftRpcReceive(raftClient, raftServer);
            receiveService.setConfigPort(port);
            receiveService.getStart();
        }

        sync = new RaftSync(raftNodeRuntime, clusterRuntime, launchService, raftServer);
    }

    public static RaftSync getSync() {
        return sync;
    }

}
