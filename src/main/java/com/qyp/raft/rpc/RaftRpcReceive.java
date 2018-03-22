/*
 * 链家集团, 版权所有
 * ©2010-2017 Lianjia, Inc. All rights reserved.
 * 
 * http://www.lianjia.com 
 *
 */

package com.qyp.raft.rpc;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import java.util.Random;

import com.qyp.raft.RaftClient;
import com.qyp.raft.RaftServer;
import com.qyp.raft.Singleton;
import com.qyp.raft.hook.DestroyAdaptor;
import com.qyp.raft.hook.Destroyable;

/**
 * 对于来自于集群其它节点的请求的访问
 * <p>
 * 远程访问的发起有如下Case:
 * ① 发起投票申请
 * ② 发起心跳
 * ③ Follower 向 Leader 发起日志同步请求     TODO
 * ④ Leader 向 Follower 同步日志请求请求     TODO
 * <p>
 * 有请求就需要有响应
 *
 * 当前代码不具备守护功能
 *
 * @author yupeng.qin
 * @since 2018-03-21
 */
@Singleton
public class RaftRpcReceive implements RaftRpcReceiveService {

    // 超时时间，单位毫秒
    // 心跳最低力度100ms, 因此 Accept 超时设置为 200 ms
    private static final int TIME_OUT = 200;
    private final TCPMessageHandler HANDLER;

    private int configPort;
    // 本地监听端口
    private static int listenPort = -1;
    private static boolean canAlive = true;
    private static final int minPort = 11111;
    private static final int maxPort = 55555;

    private static volatile boolean inService = false;

    private Selector selector;
    private ServerSocketChannel listenerChannel;

    public RaftRpcReceive(RaftClient raftClient, RaftServer raftServer) {
        HANDLER = new TCPMessageHandler(raftClient, raftServer);
        DestroyAdaptor.getInstance().add(new Destroyable() {
            @Override
            public void destroy() {
                canAlive = false;
            }
        });
    }

    public synchronized void getStart() {
        if (!inService) {
            start();
            inService = true;
        }
    }

    public void setConfigPort(int port) {
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("Port is in 1~65535!");
        }
        this.configPort = port;
    }

    public int getPort() {
        if (!inService) {
            getStart();
        }
        return listenPort;
    }

    private void start() {
        try {
            init();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                bind();
            }
        }, "Raft-TCP-Server").start();
    }

    /**
     * 创建好的一个 Selector. 绑定指定端口.
     *
     * @throws IOException
     */
    private void init() throws IOException {
        selector = Selector.open();
        // 打开监听信道 目前只做TCP支持
        listenerChannel = ServerSocketChannel.open();
        // 与本地端口绑定
        do {
            int port = -1;
            if (configPort > 0) {
                port = configPort;
            } else {
                Random random = new Random();
                // 在重启的情况下不修改端口号.
             port = (listenPort == -1 ?
                        (random.nextInt(maxPort) % (maxPort - minPort + 1) + minPort) : listenPort);
            }
            try {
                listenerChannel.socket().bind(new InetSocketAddress(port));
                listenPort = port;
            } catch (IOException e) {
            }
        } while (listenPort == -1);
        // 设置为非阻塞模式
        listenerChannel.configureBlocking(false);
        // 将选择器绑定到监听信道,只有非阻塞信道才可以注册选择器.并在注册过程中指出该信道可以进行Accept操作
        listenerChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    /**
     * 对于数据的处理依然是同步单线程在处理。 本服务的IO要求并不高
     */
    private void bind() {
        while (canAlive) {
            // 等待某信道就绪(或超时)
            try {
                if (selector.select(TIME_OUT) == 0) {
                    continue;
                }
            } catch (IOException e) {
            }
            // 取得迭代器.selectedKeys()中包含了每个准备好某一I/O操作的信道的SelectionKey
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();

            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                try {
                    HANDLER.handleKey(key);
                } catch (IOException e) {
                }

                iterator.remove();
            }
        }
        try {
            listenerChannel.close();
            selector.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
