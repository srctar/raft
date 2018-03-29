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

import java.util.HashMap;
import java.util.Map;

/**
 * 数据处理转换器
 *
 * @author yupeng.qin
 * @since 2018-03-29
 */
public class DataTranslateAdaptor {

    private static final DataTranslateAdaptor adaptor = new DataTranslateAdaptor();
    private final Map<Class, DataTranslateService> map;

    public static DataTranslateAdaptor getInstance() {
        return adaptor;
    }

    private DataTranslateAdaptor() {
        map = new HashMap<>();
    }

    public void put(Class clazz, DataTranslateService service) {
        map.put(clazz, service);
    }

    public DataTranslateService get(Class clazz) {
        return map.get(clazz);
    }

}
