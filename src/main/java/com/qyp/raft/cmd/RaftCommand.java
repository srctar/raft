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

package com.qyp.raft.cmd;

/**
 * Raft 集群中的集群互相交互的命令.
 *
 * @author yupeng.qin
 * @since 2018-03-13
 */
public enum RaftCommand {

    /**
     * 请求被选举
     */
    REQUEST_VOTE,

    /**
     * 选举完毕之后, 周知 Follower。
     */
    APPEND_ENTRIES,

    /**
     * 来自其它服务器的响应, 表示接受
     */
    ACCEPT,
    /**
     * 来自其它服务器的响应, 表示拒绝
     */
    DENY;

}
