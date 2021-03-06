# 引言

计划学习 `Paxos` , 无奈太过难以理解。 因此通过 `Raft` 辅助理解。

本工程理论基于 CSDN 翻译版 [《In search of an Understandable Consensus Algorithm (Extended Version)》](https://ramcloud.atlassian.net/wiki/download/attachments/6586375/raft.pdf), 译文见: [RAFT 论文中文翻译(1)](http://blog.csdn.net/luoyhang003/article/details/61915666) 和 [RAFT 论文中文翻译(2)](http://blog.csdn.net/luoyhang003/article/details/61915747)。

实现基于 [Raft Understandable Distributed Consensus](http://thesecretlivesofdata.com/raft/)

目前实现了 raft 协议的下述功能:

- [x] 集群选举.
- [ ] 数据一致性.
- [ ] 集群配置(集群节点信息, 以及集群的数目)更改.
- [ ] 紧急提交.
- [ ] 日志压缩. 

# 使用方式

将系统代码拷贝到本地:`git@github.com:srctar/raft.git`, 通过执行 `mvn clean package` 在`target`目录下得到`raft-1.0.0-SNAPSHOT.jar`这个可执行jar。 

jar有传参, 供三位。
 - 第一位传参为: 运行机器的 ip 地址。
 - 第二位传参为: 运行机器的 开放端口。
 - 第三位传参为: 集群中的机器信息(ip:port)形式。

 例如：`java -jar target\raft-1.0.0-SNAPSHOT.jar 192.168.179.1 40000 ip.txt`
 其中ip.txt内容便是(一个五台机器的集群):
 ```
192.168.179.1:10000
192.168.179.1:20000
192.168.179.1:30000
192.168.179.1:40000
192.168.179.1:50000
 ```

 ## 修改方式

 raft是开源免费的协议, 本工程也是开源且免费的工程. 请在遵守 `LICENSE`  约定的情况下使用。

 通过对 `com.qyp.raft.rpc.RaftRpcLaunchService` 的实现和 `com.qyp.raft.rpc.RaftRpcReceive` 的修改实现。
 `com.qyp.raft.rpc.RaftRpcLaunchService` 是一个发起TCP命令的类, `com.qyp.raft.rpc.RaftRpcReceive`专用于接收前者的TCP命令。

 你可以将它替换成你喜欢的写法, 比如你可以用HTTP协议来实现, 或者你可以通过Dubbo来实现。 如果你的服务刚好位于一个Java Web工程, 你还可以直接使用你的Web开放的端口.
 
 ### 项目结构组成
  
  本项目完全基于原生Raft协议，已实现部分功能。项目组成如下：
  
  - 通讯发起：RaftRpcLaunchService
  - 通讯接收：RaftRpcReceive（贯穿整个Appliction）
  - 接收心跳：HeartBeatTimer
    - 选举器：ElectionTimer
    - 选举实现：LeaderElection
  - 客户端(Follower)：RaftClient, 无日志功能，一次只能同步一条数据，且不可恢复
    - 处理投票请求RaftCommand dealWithVote(StandardCommand)
    - 处理同步请求RaftCommand dealWithSync(StandardCommand)
    - 处理同步提交请求RaftCommand dealWithCommit(StandardCommand)
    - 处理心跳请求RaftCommand dealWithHeartBeat(StandardCommand)
  - 服务端（Leader）：RaftServer，实现在 CommunicateFollower
    - 数据同步RaftCommand sync(TranslateData)
    - 保持心跳heartBeat()

 其中 RaftRpcReceive 是常驻服务，永远接收其他集群中的节点的消息。
 
 节点只会在接收`Leader`心跳超时后发起选举; 
 运行中的集群拒绝其他任何除Leader外节点的心跳、同步请求; 
 节点处于初始、选举状态，一定会接收并赞同高于当前`term`的`Leader`的心跳;
 节点处于`Follower`状态，将拒绝除了`Leader`外的心跳;
 节点处于`Leader`状态，如果心跳`term`高于自身，将扭转为`Follower`;
 `Leader`本身不会给自己发心跳。
 
 节点优先给自己投票，但是只要节点尚未投票，一定赞成请求投票节点的选票; 
 如果节点上次投票某节点, 本次依然投票某节点（心跳超时时间1000ms, 选举前等待时间150ms~300ms）;
 大于4台机器的集群中，Leader宕机，可能导致2台以上的机器申请选举, 只处理`term`高于本身的`term`的请求;
 
 `Leader`在实现上，只要投赞成票数过半，立即成为`Leader`。此时因为节点接收投票原则，一定会立马转化为`Follower`。 
