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
     * 集群节点之间同步数据用
     */
    SYNC,

    /**
     * Leader 请求被选举
     */
    REQUEST_VOTE,

    /**
     * 选举完毕之后, 周知 Follower。 || Follower 同意选举
     */
    APPEND_ENTRIES,

    /**
     * Leader 发起心跳(选举周知、正常心跳)的时候, 如果 Follower 的集群依然是在工作状态. 则告诉Leader再发一遍.
     */
    APPEND_ENTRIES_AGAIN,

    /**
     * Follower 拒绝选举(仅在于Leader宕机并恢复时.)
     *
     * 而一旦 Leader 收到了该消息, Leader 立马自动扭转为 Follower.
     */
    APPEND_ENTRIES_DENY,

    /**
     * 来自其它服务器的响应, 表示接受
     */
    ACCEPT,
    /**
     * 来自其它服务器的响应, 表示拒绝
     */
    DENY,

    /**
     * 专供外部接口访问, 用于查询当前的集群
     */
    INFORMATION

}
