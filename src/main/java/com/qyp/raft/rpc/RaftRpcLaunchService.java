package com.qyp.raft.rpc;

import java.io.IOException;

import com.qyp.raft.cmd.RaftCommand;

/**
 * 该接口用于主动向远程服务器发起RPC请求用
 *
 * 远程接受访问的发起有如下Case:
 * ① 发起投票申请
 * ② 发起心跳
 * ③ Follower 向 Leader 发起日志同步请求     TODO
 * ④ Leader 向 Follower 同步日志请求请求     TODO
 *
 * 有请求就需要有响应
 *
 * @author yupeng.qin
 * @since 2018-03-13
 */
public interface RaftRpcLaunchService {

    /**
     * 主动请求给自己投票.(当角色为 Candidate 时候用, 同步)
     * 返回值仅支持 {@link RaftCommand#ACCEPT} 或者{@link RaftCommand#DENY}
     */
    RaftCommand requestVote(String self, String other, int term) throws IOException;

    /**
     * 周知自身的主角关系
     */
    RaftCommand notifyFollower(String self, String other, int term) throws IOException;

    /**
     * Follower向Leader同步数据.
     */
    RaftCommand syncLeader(String self, String other, Object data) throws IOException;

}
