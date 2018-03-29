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

/**
 * 消息加载器, 建议 所有的 {@link DataTranslateService} 的实现类都继承于这个类
 *
 * @author yupeng.qin
 * @since 2018-03-29
 */
public abstract class AbstractDataTranslate<T> implements DataTranslateService<T> {

    public AbstractDataTranslate() {
        DataTranslateAdaptor.getInstance().put(support(), this);
    }

    abstract Class support();
}
