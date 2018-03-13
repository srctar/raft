package com.qyp.raft.rpc;

import com.qyp.raft.cmd.RaftCommand;
import com.qyp.raft.cmd.StandardCommand;

/**
 * 该接口用于接受远程服务器发起RPC请求用
 *
 * @author yupeng.qin
 * @since 2018-03-13
 */
public interface RaftRpcReceiveService {

    /**
     * 处理远程服务器的投票请求
     * @param cmd  来自同级服务器的投票请求
     */
    RaftCommand dealWithVote(StandardCommand cmd);

    /**
     * 处理来自Leader的心跳请求. 心跳的作用是让Follower知道, Leader还存活.
     *
     * TODO 此处考虑Case:
     * 当Leader节点与Follower节点之间, 因为网络/Leader高负载原因不通时.
     * 余下Follower节点自行选择新的Leader。此时旧的Leader需要自动转变为Follower。
     *
     * @param cmd   Leader的心跳
     */
    RaftCommand dealWithHeartBeat(StandardCommand cmd);
}
