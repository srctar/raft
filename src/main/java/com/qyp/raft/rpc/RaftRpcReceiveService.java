package com.qyp.raft.rpc;

/**
 * 该接口用于接受远程服务器发起RPC请求用。
 *
 * 需要实现一个TCP Server; 如果不实现, 可以沿用目标系统自带的服务, 然后通过这个类的实现类转发, 得到对远端消息的处理结果
 *
 * 远程访问的发起有如下Case:
 * ① 处理投票申请
 * ② 处理心跳
 * ③ Follower  处理 Leader 日志同步请求     TODO
 * ④ Leader 处理 Follower 同步日志请求     TODO
 *
 * 有请求就需要有响应
 *
 * @author yupeng.qin
 * @since 2018-03-13
 */
public interface RaftRpcReceiveService {

    /**
     *
     * 注入一个运行时的端口配置
     *
     */
    void setConfigPort(int port);

}
