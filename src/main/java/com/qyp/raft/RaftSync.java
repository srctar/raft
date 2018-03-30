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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import com.qyp.raft.data.ClusterRole;
import com.qyp.raft.data.ClusterRuntime;
import com.qyp.raft.data.RaftNodeRuntime;

/**
 * Raft的同步模块, 提供集群的数据一致性的保障.
 * 数据写入分同步写入、同步等待写入、同步等待写入(带等待时间)
 *
 * 如果当前节点是Follower：
 * ① 将需要等待数据同步至Leader
 * ② Leader将数据分发给Follower
 * ③ Leader得到多数派的投票
 * ④ Leader通知所有Follower更新数据, 同时本Follower得知更新情况, 数据落地.
 *
 * 需要等待的情况是：
 * ① Leader还有尚未提交的数据。
 * ② Leader已经提交的数据但是没有让所有的子节点更新。
 *
 * 在当前情况下, 数据插入直接失败：
 * ① 当前集群处于选举状态
 * ② 集群同步中集群扭转成了选举状态
 *
 * @author yupeng.qin
 * @since 2018-03-29
 */
public class RaftSync {

    private ReentrantLock lock;
    private Condition notFull;

    private RaftNodeRuntime raftNodeRuntime;
    private ClusterRuntime clusterRuntime;

    public boolean sync(Object e, long timeout, TimeUnit unit)
            throws InterruptedException {

        if (clusterRuntime.getClusterRole() == ClusterRole.ELECTION) {
            return false;
        }
        long nanos = unit.toNanos(timeout);
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            while (count == items.length) {
                if (nanos <= 0)
                    return false;
                nanos = notFull.awaitNanos(nanos);
            }
            enqueue(e);
            return true;
        } finally {
            lock.unlock();
        }
    }



    public boolean sync(Object e)
            throws InterruptedException {
        if (clusterRuntime.getClusterRole() == ClusterRole.ELECTION) {
            return false;
        }
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            while (count == items.length)
                notFull.await();
        } finally {
            lock.unlock();
        }
    }


    public boolean put(Object e){
        if (clusterRuntime.getClusterRole() == ClusterRole.ELECTION) {
            return false;
        }
    }

    private void syncLeader() {

    }

}
