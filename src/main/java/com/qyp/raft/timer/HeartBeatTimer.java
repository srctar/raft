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

package com.qyp.raft.timer;

import com.qyp.raft.Singleton;
import com.qyp.raft.data.RaftServerRole;
import com.qyp.raft.data.RaftServerRuntime;

/**
 * 心跳超时器. 非 Leader 使用. 对象须单例
 *
 * 在指定的超时时间内, 接受到了心跳活动, 程序继续执行.
 * 未接收到心跳:
 * 如果当前角色是Follower: 自动扭转为Candidate. 并发起投票.
 * 如果当前角色是Candidate: 超时情况一般是投票选举发生异常, 没有任何节点拿到多数票. 这时只需要重置自己就好了.
 *
 * @author yupeng.qin
 * @since 2018-03-14
 */
@Singleton
public class HeartBeatTimer implements Runnable {

    private RaftServerRuntime raftServerRuntime;

    /**
     * 接受心跳超时设置
     */
    public static long TIME_OUT = 1000;

    @Override
    public void run() {
        if (raftServerRuntime.getRole() == RaftServerRole.LEADER)
            return;
        while (raftServerRuntime.getRole() == RaftServerRole.FOLLOWER
                || raftServerRuntime.getRole() == RaftServerRole.CANDIDATE) {
            synchronized(this) {

            }
        }
    }
}
