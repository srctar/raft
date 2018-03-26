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
 * 当前服务器的节点信息.
 *
 * @author yupeng.qin
 * @since 2018-03-13
 */
public class RaftNodeRuntime {

    /**
     * 在集群选举的时候用, 只有在 voteCount > 集群节点数 的时候, 节点角色才会有改变.
     *
     * @see ClusterRuntime#clusterMachine
     */
    private int voteCount = 0;

    /**
     * 接受到任意服务器的选举请求, 并决定为之投票的机器 ip:port
     */
    private String voteFor;

    /**
     * 期序
     */
    private int term = 0;

    /**
     * Leader 节点的 IP:PORT
     */
    private String leader;

    /**
     * 当前节点的角色
     */
    private volatile RaftServerRole role;

    private String self;

    private long lastHeartTime;

    /**
     * 当前当此选举所花时间
     */
    private volatile int currentElectionTime;

    public int getVoteCount() {
        return voteCount;
    }

    public void setVoteCount(int voteCount) {
        this.voteCount = voteCount;
    }

    public synchronized void increaseVoteCount() {
        voteCount ++;
    }

    public String getVoteFor() {
        return voteFor;
    }

    public void setVoteFor(String voteFor) {
        this.voteFor = voteFor;
    }

    public int getTerm() {
        return term;
    }

    public void setTerm(int term) {
        this.term = term;
    }

    public String getLeader() {
        return leader;
    }

    public void setLeader(String leader) {
        this.leader = leader;
    }

    public synchronized RaftServerRole getRole() {
        return role;
    }

    public synchronized void setRole(RaftServerRole role) {
        this.role = role;
    }

    public String getSelf() {
        return self;
    }

    public void setSelf(String self) {
        this.self = self;
    }

    public long getLastHeartTime() {
        return lastHeartTime;
    }

    public void setLastHeartTime(long lastHeartTime) {
        this.lastHeartTime = lastHeartTime;
    }

    public int getCurrentElectionTime() {
        return currentElectionTime;
    }

    public void setCurrentElectionTime(int currentElectionTime) {
        this.currentElectionTime = currentElectionTime;
    }

    @Override
    public String toString() {
        return "RaftNodeRuntime{" +
                "voteCount=" + voteCount +
                ", voteFor='" + voteFor + '\'' +
                ", term=" + term +
                ", leader='" + leader + '\'' +
                ", role=" + role +
                ", self='" + self + '\'' +
                ", lastHeartTime=" + lastHeartTime +
                ", currentElectionTime=" + currentElectionTime +
                '}';
    }
}
