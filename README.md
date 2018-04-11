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
 
 ## 与其它功能的集成（以Spring为例）
 
 > Raft 提供了一个功能不算是很强悍的启动器, 它位于 `com.qyp.raft.Launcher`, 里面详细的描述了Raft的启动流程. 如果你对这个启动器有什么不满意的地方, 请改变它.
 > 且看代码结构:
 
![代码结构图](https://dewqcsacre.oss-cn-beijing.aliyuncs.com/18-2-1/raft.png)
 
 当然你不会只需要一个仅能选举的集群, 你还需要将他放在可运行代码里, 使之协助你的集群成为一个分布式的集群. 你需要如下前期准备：
 
  - 服务发现: 你需要一定的方式, 使得你的集群中的每台机器可以互相发现, 方式不仅限于存在文件里、代码固定。
  - 同步使用: 通过调用`Launcher.getSync().sync(Object);` 来开始你的集群同步。
  - 接收同步: 通过继承并实现`com.qyp.raft.RaftWatcher`, 你便可以感知到每一次的同步。
  
 针对如上三个问题, 请看下面的详细解释:
 