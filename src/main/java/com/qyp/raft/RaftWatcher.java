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

import java.util.ArrayList;
import java.util.List;

/**
 * 集群间同步功能的对外服务对象, 需要集群中数据同步者请实现这个类。
 * 集群数据有收有发, 收用 {@link RaftSync}
 *
 * @author yupeng.qin
 * @since 2018-04-09
 */
public abstract class RaftWatcher {

    private static List<RaftWatcher> CHILD = new ArrayList<>();

    protected abstract void sync(Object obj);

    public RaftWatcher() {
        CHILD.add(this);
    }

    public static class RaftWatcherDispatcher {

        public static void syncAll(Object sync) {
            if (sync == null) {
                return;
            }
            for (RaftWatcher watcher: CHILD) {
                watcher.sync(sync);
            }
        }
    }

}
