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

package com.qyp.raft.util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;

/**
 * 专用于发起TCP请求并同步返回请求结果.
 *
 * @author yupeng.qin
 * @since 2018-03-13
 */
public class SocketUtil {

    /**
     * 使用 SocketUtil 请求 远端接口, 最大响应数据体。
     */
    public static final int MAX_BUFFER_SIZE = 1024;

    public static final String notifyOfString(String host, byte[] bt) throws IOException {
        return Charset.forName("UTF-8").newDecoder().decode(notifyOf(host, bt)).toString();
    }

    public static final byte[] notifyOfByte(String host, byte[] bt) throws IOException {
        ByteBuffer bb = notifyOf(host, bt);
        byte[] b = new byte[bb.remaining()];
        bb.get(b, 0, b.length);
        return b;
    }

    private static final ByteBuffer notifyOf(String host, byte[] bt) throws IOException {
        return notifyOf(host, bt, 100);
    }

    private static final ByteBuffer notifyOf(String host, byte[] bt, int timeOut) throws IOException {

        String[] h = host.split(":");

        Selector selector = null;
        SocketChannel channel = null;
        try {
            selector = Selector.open();
            channel = SocketChannel.open();
            channel.socket().connect(new InetSocketAddress(h[0], Integer.valueOf(h[1])), timeOut);
            channel.configureBlocking(false);
            channel.register(selector, SelectionKey.OP_READ);

            channel.write(ByteBuffer.wrap(bt));
            // 给自身线程超时的反应时间
            if (selector.select(timeOut) > 0) {
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey k = it.next();
                    if (k.isReadable()) {
                        SocketChannel ch = (SocketChannel) k.channel();
                        ByteBuffer ok = ByteBuffer.allocate(MAX_BUFFER_SIZE);
                        ch.read(ok);
                        ok.flip();
                        ch.close();
                        return ok;
                    }
                    it.remove();
                }
            } else {
                throw new SocketTimeoutException("Time " + timeOut);
            }
        } finally {
            if (channel != null) {
                channel.close();
            }
            if (selector != null) {
                selector.close();
            }
        }
        return null;
    }

}
