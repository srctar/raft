/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  The ASF licenses
 * this file to you under the Apache License&Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing&software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND&either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qyp.raft.cmd;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import com.qyp.raft.util.Base64Util;

/**
 * Raft集群中&通讯用标准command
 *
 * @author yupeng.qin
 * @since 2018-03-13
 */
public class StandardCommand implements Serializable {

    /**
     * 来源机器
     */
    private String resource;
    /**
     * 目标机器
     */
    private String target;
    /**
     * 命令发布
     * @see RaftCommand
     */
    private String command;
    /**
     * 期序
     */
    private String term;
    /**
     * 数据交互
     */
    private byte[] dataNode;
    /**
     * 时间戳
     */
    private String timestamp;

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public byte[] getDataNode() {
        return dataNode;
    }

    public void setDataNode(byte[] dataNode) {
        this.dataNode = dataNode;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    private static Map<String, Field> fieldMap;
    static {
        Field[] field = StandardCommand.class.getDeclaredFields();
        fieldMap = new HashMap<>();
        for (Field f: field) {
            fieldMap.put(f.getName(), f);
        }
    }

    public static StandardCommand toCommand(String s) throws IllegalAccessException {
        StandardCommand cmd = new StandardCommand();
        String[] fd = s.split("&");
        for (String each : fd){
            Field f;
            int idx = each.indexOf("=");
            if (idx > -1 && idx != each.length() - 1) {
                if ((f = fieldMap.get(each.substring(0, idx))) != null
                        && !"null".equalsIgnoreCase(each.substring(idx + 1))) {
                    if (f.getDeclaringClass() == String.class) {
                        f.set(cmd, each.substring(idx + 1));
                    } else if (f.getDeclaringClass() == byte[].class) {
                        f.set(cmd, Base64Util.decode(each.substring(idx + 1)));
                    }
                }
            }
        }
        return cmd;
    }

    public static StandardCommand toCommand(byte[] bb) throws IOException, ClassNotFoundException {
        return (StandardCommand) new ObjectInputStream(new ByteArrayInputStream(bb)).readObject();
    }

    public byte[] toByte() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(this);
        return bos.toByteArray();
    }

    @Override
    public String toString() {
        return "resource=" + resource + 
                "&target=" + target + 
                "&command=" + command +
                "&dataNode=" + Base64Util.encode(dataNode) +
                "&timestamp=" + timestamp +
                "&term=" + term;
    }
}
