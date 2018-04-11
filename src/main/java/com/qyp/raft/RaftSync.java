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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import com.qyp.raft.cmd.RaftCommand;
import com.qyp.raft.data.ClusterRole;
import com.qyp.raft.data.ClusterRuntime;
import com.qyp.raft.data.RaftNodeRuntime;
import com.qyp.raft.data.RaftServerRole;
import com.qyp.raft.data.t.DataTranslateAdaptor;
import com.qyp.raft.data.t.DataTranslateService;
import com.qyp.raft.data.t.TranslateData;
import com.qyp.raft.rpc.RaftRpcLaunchService;

/**
 * Raft的同步模块, 提供集群的数据一致性的保障.
 * 数据写入分同步写入、同步等待写入、同步等待写入(带等待时间)
 * <p>
 * 如果当前节点是Follower：
 * ① 将需要等待数据同步至Leader
 * ② Leader将数据分发给Follower
 * ③ Leader得到多数派的投票
 * ④ Leader通知所有Follower更新数据, 同时本Follower得知更新情况, 数据落地.  同步完毕.
 * <p>
 * 需要等待的情况是：
 * ① Leader还有尚未提交的数据。
 * ② Leader已经提交的数据但是没有让所有的子节点更新。
 * <p>
 * 在当前情况下, 数据插入直接失败：
 * ① 当前集群处于选举状态
 * ② 集群同步中集群扭转成了选举状态
 *
 * @author yupeng.qin
 * @since 2018-03-29
 */
public class RaftSync {

    private ReentrantLock lock;

    private RaftNodeRuntime raftNodeRuntime;
    private ClusterRuntime clusterRuntime;

    private RaftRpcLaunchService raftRpcLaunch;

    private RaftServer raftServer;

    RaftSync(RaftNodeRuntime raftNodeRuntime, ClusterRuntime clusterRuntime,
                    RaftRpcLaunchService raftRpcLaunch, RaftServer raftServer) {
        this.raftNodeRuntime = raftNodeRuntime;
        this.clusterRuntime = clusterRuntime;
        this.raftRpcLaunch = raftRpcLaunch;
        this.raftServer = raftServer;

        lock = new ReentrantLock();
    }

    /**
     * 向Raft集群发起同步.
     * 在一个指定的同步期间内, 如果同步不能成功, 则会多次同步. 直至同步成功, 或者时间完毕
     */
    public boolean syncMultile(Object e, long timeout, TimeUnit unit)
            throws InterruptedException {

        long nanos = unit.toNanos(timeout);
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            // 存在多种原因导致同步失败, 因此在等待时间内只要没有成功都可以一直重试
            long nano = System.nanoTime();
            while (!doSync(e)) {
                if (nanos >= System.nanoTime() - nano) {
                    return false;
                }
            }
            return true;
        } finally {
            lock.unlock();
        }
    }

    /*
    之所以加锁, 是因为单机可能存在多线程, 对集群的同步.
    而多线程的集群同步, 在Leader端本身就是被禁止的.
    因此加个锁, 能更高的保证同步成功率.
     */
    public boolean sync(Object e)
            throws InterruptedException {
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            return doSync(e);
        } finally {
            lock.unlock();
        }
    }

    private boolean doSync(Object o) {
        if (clusterRuntime.getClusterRole() == ClusterRole.ELECTION || o == null) {
            return false;
        }
        // follower 节点需要把信息告知Leader
        if (raftNodeRuntime.getRole() == RaftServerRole.FOLLOWER) {
            try {
                return syncLeader(o);
            } catch (Exception e) {
                return false;
            }
        } else {
            return RaftCommand.APPEND_ENTRIES == raftServer.sync(o);
        }
    }

    private boolean syncLeader(Object obj) {
        try {
            RaftCommand cmd = raftRpcLaunch.syncLeader(raftNodeRuntime.getSelf(), raftNodeRuntime.getLeader(), obj);
            switch (cmd) {
                // Leader存在待处理队列, 需要不断重试以便于插入成功
                case APPEND_ENTRIES_AGAIN:
                    return false;
                // Leader
                case APPEND_ENTRIES_DENY:
                    throw new IllegalStateException("暂时不能同步!");
                case APPEND_ENTRIES:
                    return true;
                default:
                    throw new IllegalStateException("同步出现异常, 请稍后重试!");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

}
