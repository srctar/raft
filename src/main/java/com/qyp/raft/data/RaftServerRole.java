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

package com.qyp.raft.data;

/**
 * RAFT 协议中, 单个节点的状态, 一定是在 {@link RaftServerRole} 其中之一
 * 初始状态定为 FOLLOWER.
 *
 * @author yupeng.qin
 * @since 2018-03-12
 */
public enum RaftServerRole {

    FOLLOWER,

    LEADER,

    CANDIDATE;
}
