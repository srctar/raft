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

package com.qyp.raft.rpc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qyp.raft.RaftServer;
import com.qyp.raft.cmd.RaftCommand;
import com.qyp.raft.data.ClusterRuntime;
import com.qyp.raft.data.RaftNodeRuntime;
import com.qyp.raft.hook.DestroyAdaptor;
import com.qyp.raft.hook.Destroyable;

/**
 * Leader跟跟随者之间的交互
 *
 * @author yupeng.qin
 * @since 2018-03-13
 */
public class CommunicateFollower {

    private static final Logger logger = LoggerFactory.getLogger(CommunicateFollower.class);
    private final ExecutorService executor;

    private RaftNodeRuntime raftNodeRuntime;
    private ClusterRuntime clusterRuntime;

    private RaftRpcLaunchService raftRpcLaunchService;

    public CommunicateFollower(RaftNodeRuntime raftNodeRuntime, ClusterRuntime clusterRuntime,
                               RaftRpcLaunchService raftRpcLaunchService) {
        this.raftNodeRuntime = raftNodeRuntime;
        this.clusterRuntime = clusterRuntime;
        this.raftRpcLaunchService = raftRpcLaunchService;

        int size = clusterRuntime.getClusterMachine().length / 2;
        size = size > 0 ? size : 1;
        // 线程池保证有总机器数的一半提供服务.
        executor = Executors.newFixedThreadPool(size);

        DestroyAdaptor.getInstance().add(new Destroyable() {
            @Override
            public void destroy() {
                executor.shutdownNow();
            }
        });
    }

    /**
     * 2018年4月8日
     * <p>
     * 同步的关注点有两个:
     * Leader发给Follower的同步消息, 需要等到所有的Follower回应之后才会提交.因此Leader和Follower都不会有堆积.
     * Follower会等待Leader提交完毕上一次请求之后, 才可以做进一步的提交.
     */
    public boolean sync(Object sync) {
        boolean record = false;
        if (syncFollower(new Sync() {
            @Override
            public RaftCommand doSync(String clusterMachine) throws IOException {
                return raftRpcLaunchService.syncFollower(
                        raftNodeRuntime.getSelf(), clusterMachine, sync);
            }
        })) {
            int ty = 0;
            // 当给Follower发消息成功之后, 需要立即通知提交.
            // 目前尝试重试三次, 失败之后再停止提交.
            while (!(record = syncFollower(new Sync() {
                @Override
                public RaftCommand doSync(String clusterMachine) throws IOException {
                    return raftRpcLaunchService.notifyFollower(
                            raftNodeRuntime.getSelf(), clusterMachine,
                            raftNodeRuntime.getTerm(), RaftCommand.COMMIT);
                }
            })) && ty < 3) {
                ty ++;
            }
        }
        return record;
    }

    private boolean syncFollower(Sync command) {
        int len;
        if ((len = clusterRuntime.getClusterMachine().length) == 1) {
            return true;
        }
        List<Future<RaftCommand>> futures = new ArrayList<>(len - 1);
        f:
        for (int i = 0; i < len; i++) {
            String clusterMachine = clusterRuntime.getClusterMachine()[i];
            if (!clusterMachine.equalsIgnoreCase(raftNodeRuntime.getSelf())) {
                // 这个必须设置超时时间, 否则会导致其它节点的心跳接受时间超时. 进而重复选举.
                futures.add(executor.submit(new Callable<RaftCommand>() {
                    @Override
                    public RaftCommand call() throws Exception {
                        return command.doSync(clusterMachine);
                    }
                }));
            }
        }
        int count = 0;
        for (Future<RaftCommand> f: futures) {
            try {
                RaftCommand cmd = f.get(RaftServer.HEART_TIME, TimeUnit.MILLISECONDS);
                if (cmd == RaftCommand.APPEND_ENTRIES) {
                    count ++;
                }
            } catch (InterruptedException e) {
            } catch (ExecutionException e) {
            } catch (TimeoutException e) {
            }
        }
        return count == len - 1;
    }

    private interface Sync {
        RaftCommand doSync(String clusterMachine) throws IOException;
    }

    public void heartBeat() {
        if (logger.isDebugEnabled()) {
            logger.debug("当前节点(Leader):{}, 给Follower节点发心跳, Follower:{}",
                    raftNodeRuntime.getSelf(), Arrays.toString(clusterRuntime.getClusterMachine()));
        }
        f:
        for (int i = 0; i < clusterRuntime.getClusterMachine().length; i++) {
            String clusterMachine = clusterRuntime.getClusterMachine()[i];
            if (!clusterMachine.equalsIgnoreCase(raftNodeRuntime.getSelf())) {
                if (logger.isDebugEnabled()) {
                    logger.debug("当前节点(Leader):{}, 给Follower:{}节点发心跳", raftNodeRuntime.getSelf(), clusterMachine);
                }
                try {
                    // 这个必须设置超时时间, 否则会导致其它节点的心跳接受时间超时. 进而重复选举.
                    Future<RaftCommand> f = executor.submit(new Callable<RaftCommand>() {
                        @Override
                        public RaftCommand call() throws Exception {
                            return raftRpcLaunchService.notifyFollower(
                                    raftNodeRuntime.getSelf(), clusterMachine,
                                    raftNodeRuntime.getTerm(), RaftCommand.APPEND_ENTRIES);
                        }
                    });
                    RaftCommand cmd = f.get(RaftServer.HEART_TIME, TimeUnit.MILLISECONDS);
                    if (logger.isDebugEnabled()) {
                        logger.debug("当前节点(Leader):{}, 给Follower:{}节点发心跳, Follower的反应:{}",
                                raftNodeRuntime.getSelf(), clusterMachine, cmd);
                    }
                    // 收到仆从机器的心跳反应有: APPEND_ENTRIES、APPEND_ENTRIES_DENY、APPEND_ENTRIES_AGAIN
                    // 如果心跳被拒绝, 则可能自己是老机器, 需要直接重置主机状态
                    if (cmd == RaftCommand.APPEND_ENTRIES_DENY) {
                        break f;
                    }
                } catch (InterruptedException e) {
                    // 线程不会被中断
                } catch (ExecutionException e) {
                    // 对于windows而言, 一般都是 Connection refused: connect
                    // 对于mac而言, 一般都是 Operation timed out
                    if (logger.isDebugEnabled()) {
                        logger.debug("当前节点(Leader):{}, 给Follower:{}节点发心跳, 连接出现异常.",
                                raftNodeRuntime.getSelf(), clusterMachine, e);
                    }
                } catch (TimeoutException e) {
                    // 超时不能管
                    logger.debug("当前节点(Leader):{}, 给Follower:{}节点发心跳, 处理超时.",
                            raftNodeRuntime.getSelf(), clusterMachine, e);
                }
            }
        }
    }

}
