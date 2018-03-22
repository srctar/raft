/*
 * Apache License 2.0 见 LICENSE 文档
 *
 * https://github.com/srctar/bconfig-client
 */

package com.qyp.raft.hook;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.qyp.config.file.RemoteCommunicationAdaptor;

/**
 * 用于集中管理系统里面的待销毁对象。
 *
 * @author yupeng.qin
 * @since 2017-07-04
 */
public class DestroyAdaptor {

    private final static DestroyAdaptor DA = new DestroyAdaptor();
    private final List<Destroyable> destroyAbles = new LinkedList();

    private DestroyAdaptor() {
        // 双重保险. 在Servlet环境和普通Java环境中
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                List<Destroyable> list = get();
                if (list.size() > 0) {
                    Iterator<Destroyable> it = list.iterator();
                    while (it.hasNext()) {
                        it.next().destroy();
                        it.remove();
                    }
                }
                RemoteCommunicationAdaptor.getInstance().haltSelf();
            }
        }));
    }

    public static DestroyAdaptor getInstance() {
        return DA;
    }

    public void add(Destroyable destroyable) {
        destroyAbles.add(destroyable);
    }

    public synchronized List<Destroyable> get() {
        return destroyAbles;
    }
}
