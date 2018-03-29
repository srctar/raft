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

package com.qyp.raft.data.t;

import java.io.IOException;

/**
 * 数据翻译服务, 提供网间的数据序列化、反序列化工作.
 * 需要注意, 这里的序列化和反序列化必须能对应上.
 *
 * 消息加载器: {@link AbstractDataTranslate} 建议 所有的
 * {@link DataTranslateService} 的实现类都继承于这个类{@link AbstractDataTranslate}
 *
 * @author yupeng.qin
 * @since 2018-03-29
 */
public interface DataTranslateService<T> {

    /**
     * 将任意提供的对象序列化成 byte array。
     * 例如, 如果实现方式是 json 序列化, 需要将 String 转化为 byte[]
     * @param o
     * @return
     */
    byte[] encode(Object o) throws IOException;

    T decode(byte[] data, Class type) throws IOException, ClassNotFoundException;
}
