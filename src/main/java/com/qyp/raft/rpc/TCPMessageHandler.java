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
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import com.qyp.raft.RaftClient;
import com.qyp.raft.RaftServer;
import com.qyp.raft.cmd.RaftCommand;
import com.qyp.raft.cmd.StandardCommand;

/**
 * 用于把信息做区分, 交给TCP处理器或者是HTTP处理器去处理.
 *
 * @author yupeng.qin
 * @since 2017-12-06
 */
class TCPMessageHandler {

    // 缓冲区大小 1MB,
    private static final int TCP_BUFFER_SIZE = 1 << 20;

    private RaftClient raftClient;
    private RaftServer raftServer;

    public TCPMessageHandler(RaftClient raftClient, RaftServer raftServer) {
        this.raftClient = raftClient;
        this.raftServer = raftServer;
    }

    public void handleKey(SelectionKey key) throws IOException {

        if (key.isAcceptable()) {
            SocketChannel clientChannel = ((ServerSocketChannel) key.channel()).accept();
            clientChannel.configureBlocking(false);
            clientChannel.register(key.selector(),
                    SelectionKey.OP_READ, ByteBuffer.allocate(TCP_BUFFER_SIZE));
        }

        String answer = "";
        if (key.isReadable()) {
            SocketChannel client = (SocketChannel) key.channel();
            ByteBuffer buffer = (ByteBuffer) key.attachment();
            buffer.clear();

            // 读取信息获得读取的字节数
            long bytesRead = client.read(buffer);

            if (bytesRead == -1) {
                // 没有读取到内容的情况
                client.close();
            } else {
                // 将缓冲区准备为数据传出状态
                buffer.flip();
                StandardCommand query = null;
                try {
                    query = StandardCommand.toCommand(buffer.array());
                    answer = handleEachQuery(query);
                } catch (Exception e) {
                    answer = RaftCommand.DENY.name();
                }
                key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);

                buffer = ByteBuffer.wrap(answer.getBytes("UTF-8"));
                client.write(buffer);
                client.finishConnect();
                client.close();
            }
        }
    }

    /**
     * 被处理的请求, 需要处理 集群投票、心跳以及集群交互的相关情况
     * @param query
     * @return
     * @throws IOException
     */
    private String handleEachQuery(StandardCommand query) throws IOException {
        RaftCommand cmd = RaftCommand.valueOf(query.getCommand());
        switch (cmd) {
            // SYNC_LEADER 只是是 Follower 将消息 递交给服务端
            case SYNC_LEADER: return raftServer.sync(query.getDataNode()).name();
            case SYNC_FOLLOWER: return raftClient.dealWithSync(query).name();
            // COMMIT 便是服务端将周知的数据做提交
            case COMMIT: return raftClient.dealWithCommit(query).name();
            case REQUEST_VOTE: return raftClient.dealWithVote(query).name();
            // 可以是心跳, 更可以在心跳中附加同步信息
            case APPEND_ENTRIES: return raftClient.dealWithHeartBeat(query).name();
            default:break;
        }
        return "";
    }
}
