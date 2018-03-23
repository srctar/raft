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

import java.util.concurrent.TimeUnit;

import com.qyp.raft.LeaderElection;

/**
 * 选举的超时设定。 每次超时的时间不固定, 都随机在 150ms ~ 300ms
 *
 * @author yupeng.qin
 * @since 2018-03-19
 */
class ElectionTimer implements Runnable {

    private LeaderElection leaderElection;

    public ElectionTimer(LeaderElection leaderElection) {
        this.leaderElection = leaderElection;
    }

    @Override
    public void run() {
        try {
            // 选举的超时设定。 每次超时的时间不固定, 都随机在 150ms ~ 300ms
            TimeUnit.MILLISECONDS.sleep((long) (Math.random() * 150) + 150);
        } catch (InterruptedException e) {
            // 这个线程一般不会被中断
            e.printStackTrace();
        }
        leaderElection.requestVote();
    }
}
