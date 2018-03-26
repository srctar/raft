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

package com.qyp.raft.data;

/**
 * 集群状态描述, 包含整个集群的相关信息。
 * 注意, 这里的集群信息的可以变更的。
 *
 * @author yupeng.qin
 * @since 2018-03-13
 */
public class ClusterRuntime {

    /**
     * 集群的机器数目.
     * 储存的数据为集群中的机器的 ip:port
     */
    private String[] clusterMachine;

    private volatile ClusterRole clusterRole;

    public String[] getClusterMachine() {
        return clusterMachine;
    }

    public void setClusterMachine(String[] clusterMachine) {
        this.clusterMachine = clusterMachine;
    }

    public ClusterRole getClusterRole() {
        return clusterRole;
    }

    public void setClusterRole(ClusterRole clusterRole) {
        this.clusterRole = clusterRole;
    }
}
