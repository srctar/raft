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

import com.qyp.raft.cmd.RaftCommand;
import com.qyp.raft.cmd.StandardCommand;
import com.qyp.raft.util.SocketUtil;

/**
 * RaftRpcLaunchService 的 Socket 实现.
 *
 * @author yupeng.qin
 * @since 2018-03-13
 */
public class RaftRpcLaunch implements RaftRpcLaunchService {

    @Override
    public RaftCommand requestVote(String self, String other, int term) throws IOException {
        return simpleRequest(self, other, term, RaftCommand.REQUEST_VOTE);
    }

    @Override
    public RaftCommand notifyFollower(String self, String other, int term) throws IOException {
        return simpleRequest(self, other, term, RaftCommand.APPEND_ENTRIES);
    }

    private RaftCommand simpleRequest(String self, String other, int term, RaftCommand cd) throws IOException {
        StandardCommand cmd = new StandardCommand();
        cmd.setCommand(cd.name());
        cmd.setResource(self);
        cmd.setTarget(other);
        cmd.setTerm(String.valueOf(term));
        cmd.setTimestamp(Long.toString(System.currentTimeMillis()));
        String require = SocketUtil.notifyOfString(other, cmd.toByte());

        return RaftCommand.valueOf(require);
    }

    @Override
    public void heartCheck() {

    }
}
