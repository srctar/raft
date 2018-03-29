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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * 使用 ObjectInputStream 和 ObjectOutputStream 序列化/反序列化
 *
 * @author yupeng.qin
 * @since 2018-03-29
 */
public class ObjectDataTranslateService extends AbstractDataTranslate<Object>
        implements DataTranslateService<Object> {

    @Override
    public byte[] encode(Object o) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(o);
        return bos.toByteArray();
    }

    @Override
    public Object decode(byte[] data, Class type) throws IOException, ClassNotFoundException {
        if (data == null) {
            return null;
        }
        return new ObjectInputStream(new ByteArrayInputStream(data)).readObject();
    }

    @Override
    Class support() {
        return Object.class;
    }
}
