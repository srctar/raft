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
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qyp.raft.cmd.RaftCommand;
import com.qyp.raft.data.t.TranslateData;
import com.qyp.raft.hook.DestroyAdaptor;
import com.qyp.raft.hook.Destroyable;
import com.qyp.raft.rpc.CommunicateFollower;

/**
 * Leader 向客户机发心跳的定时任务
 *
 * @author yupeng.qin
 * @since 2018-03-19
 */
public class RaftServer {

    private static final Logger logger = LoggerFactory.getLogger(RaftServer.class);

    public static final long HEART_TIME = 100L;

    // 线程池只需要一个线程.
    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

    private CommunicateFollower communicateFollower;

    private ReentrantLock lock = new ReentrantLock();

    private HeartBeat heartBeat = new HeartBeat();
    private volatile boolean run = false;
    // 防止子线程尚未进行完毕, 父线程又一次提交
    private volatile boolean ready = true;

    public void setRun(boolean run) {
        this.run = run;
    }

    public RaftServer(CommunicateFollower communicateFollower) {
        this.communicateFollower = communicateFollower;
        executorService.scheduleAtFixedRate(heartBeat, 100L, HEART_TIME, TimeUnit.MILLISECONDS);

        DestroyAdaptor.getInstance().add(new Destroyable() {
            @Override
            public void destroy() {
                executorService.shutdownNow();
            }
        });
    }

    /**
     * 2018年4月8日
     *
     * 当前的服务端对客户端的同步不支持队列, 即来一条同步命令处理一条.
     * 后续来到的消息直接拒绝.
     */
    public RaftCommand sync(Object data) {
        if (lock.tryLock()) {
            logger.info("Raft集群, 当前Raft角色:Leader, 正在处理Follower的同步请求...");
            try {
                return communicateFollower.sync(data) ? RaftCommand.APPEND_ENTRIES : RaftCommand.APPEND_ENTRIES_AGAIN;
            } catch (Exception e) {
                return RaftCommand.APPEND_ENTRIES_DENY;
            } finally {
                lock.unlock();
            }
        }
        return RaftCommand.APPEND_ENTRIES_AGAIN;
    }

    private class HeartBeat implements Runnable {

        @Override
        public void run() {
            if (run && ready) {
                ready = false;
                communicateFollower.heartBeat();
                ready = true;
            }
        }
    }
}
