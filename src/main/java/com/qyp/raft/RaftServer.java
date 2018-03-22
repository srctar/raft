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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.qyp.raft.hook.DestroyAdaptor;
import com.qyp.raft.hook.Destroyable;

/**
 * Leader 向客户机发心跳的定时任务
 *
 * @author yupeng.qin
 * @since 2018-03-19
 */
public class RaftServer {

    // 线程池只需要一个线程.
    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

    private CommunicateFollower communicateFollower;
    private volatile boolean run = false;

    public void setRun(boolean run) {
        this.run = run;
    }

    public RaftServer(CommunicateFollower communicateFollower) {
        this.communicateFollower = communicateFollower;
        executorService.schedule(new HeartBeat(), 100L, TimeUnit.MILLISECONDS);

        DestroyAdaptor.getInstance().add(new Destroyable() {
            @Override
            public void destroy() {
                executorService.shutdownNow();
            }
        });
    }

    private class HeartBeat implements Runnable {

        @Override
        public void run() {
            if (run) {
                communicateFollower.heartBeat();
            }
        }
    }

}
