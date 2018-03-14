package com.qyp.raft.data;

/**
 * 集群的状态信息
 *
 * @author yupeng.qin
 * @since 2018-03-14
 */
public enum ClusterRole {

    /**
     * 选举态
     */
    ELECTION,

    /**
     * 正常工作态
     */
    PROCESSING;

}
