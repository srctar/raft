package com.qyp.raft.rpc;

import java.io.IOException;

import com.qyp.raft.cmd.RaftCommand;

/**
 * 该接口用于主动向远程服务器发起RPC请求用
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
     * 对 Follower 进行心跳监测用(当角色为 Leader 时候用, 同步)
     */
    void heartCheck();

}
