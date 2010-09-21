/*
 * Copyright 2010 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.gwt.logging.client;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.logging.shared.SerializableLogRecord;
import com.google.gwt.logging.shared.SerializableThrowable;

/**
 * A set of functions to convert SerializableLogRecords into JSON strings.
 * The corresponding functions to convert them back are in
 * JsonLogRecordServerUtil.java.  This class should only be used in client
 * side code since it imports com.google.gwt.json.client classes.
 * TODO(unnurg) once there is a unified JSON GWT library, combine this with
 * JsonLogRecordServerUtil.
 */
public class JsonLogRecordClientUtil {

  public static String serializableLogRecordAsJson(SerializableLogRecord slr) {
    return serializableLogRecordAsJsonObject(slr).toString();
  }
  
  private static JSONString getJsonString(String s) {
    if (s == null) {
      return new JSONString("");
    }
    return new JSONString(s);
  }

  private static JSONObject serializableLogRecordAsJsonObject(
      SerializableLogRecord slr) {
    JSONObject obj = new JSONObject();
    obj.put("level", getJsonString(slr.getLevel()));
    obj.put("loggerName", getJsonString(slr.getLoggerName()));
    obj.put("msg", getJsonString(slr.getMsg()));
    if (slr.getTimestamp() != null) {
      obj.put("timestamp", new JSONString(slr.getTimestamp().toString()));
    }
    obj.put("thrown", serializableThrowableAsJsonObject(slr.getThrown()));
    return obj;
  }
  
  private static JSONObject serializableStackTraceElementAsJsonObject(
      StackTraceElement e) {
    JSONObject obj = new JSONObject();
    if (e != null) {
      obj.put("className", getJsonString(e.getClassName()));
      obj.put("fileName", getJsonString(e.getFileName()));
      obj.put("methodName", getJsonString(e.getMethodName()));
      obj.put("lineNumber", getJsonString("" + e.getLineNumber()));
    }
    return obj;
  }
  
  private static JSONObject serializableThrowableAsJsonObject(
      SerializableThrowable t) {
    JSONObject obj = new JSONObject();
    if (t != null) {
      obj.put("message", getJsonString(t.getMessage()));
      obj.put("cause", serializableThrowableAsJsonObject(t.getCause()));
      StackTraceElement[] stackTrace = t.getStackTrace();
      if (stackTrace != null && stackTrace.length > 0) {
        JSONArray arr = new JSONArray();
        for (int i = 0; i < stackTrace.length; i++) {
          arr.set(i, serializableStackTraceElementAsJsonObject(stackTrace[i]));
        }
        obj.put("stackTrace", arr);
      }
    }
    return obj;
  }
}
