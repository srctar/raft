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

package com.qyp.raft.util;

import java.util.Base64;

/**
 * 用于 base64 的压缩和解压
 *
 * @author yupeng.qin
 * @since 2018-03-13
 */
public class Base64Util {

    public static final String encode(byte[] bb) {
        if (bb == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(bb);
    }

    public static final byte[] decode(String ss) {
        return Base64.getDecoder().decode(ss);
    }

}
