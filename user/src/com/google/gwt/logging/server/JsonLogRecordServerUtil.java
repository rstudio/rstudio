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

package com.google.gwt.logging.server;

import com.google.gwt.logging.shared.SerializableLogRecord;
import com.google.gwt.logging.shared.SerializableThrowable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * A set of functions to convert standard JSON strings into
 * SerializableLogRecords. The corresponding functions to create the JSON
 * strings are in JsonLogRecordClientUtil.java. This class should only be used
 * in server side code since it imports org.json classes.
 * TODO(unnurg) once there is a unified JSON GWT library, combine this with
 * JsonLogRecordClientUtil.
 */
public class JsonLogRecordServerUtil {
  public static SerializableLogRecord serializableLogRecordFromJson(
      String jsonString) {
    try {
      JSONObject slr = new JSONObject(jsonString);
      String level = slr.getString("level");
      String loggerName = slr.getString("loggerName");
      String msg = slr.getString("msg");
      String strongName = slr.getString("strongName");
      long timestamp = Long.parseLong(slr.getString("timestamp"));
      SerializableThrowable thrown =
        serializableThrowableFromJson(slr.getString("thrown"));
      return new SerializableLogRecord(level, loggerName, msg, thrown,
          timestamp, strongName);
    } catch (JSONException e) {
    }
    return null;
  }
  
  private static StackTraceElement serializableStackTraceElementFromJson(
      String jsonString) {
    try {
      JSONObject ste = new JSONObject(jsonString);
      String className = ste.getString("className");
      String fileName = ste.getString("fileName");
      String methodName = ste.getString("methodName");
      int lineNumber = Integer.parseInt(ste.getString("lineNumber"));
      return new StackTraceElement(className, methodName, fileName,
          lineNumber);
    } catch (JSONException e) {
    }
    return null;
  }
  
  private static SerializableThrowable serializableThrowableFromJson(
      String jsonString) {
    try {
      JSONObject t = new JSONObject(jsonString);
      String message = t.getString("message");
      SerializableThrowable cause =
        serializableThrowableFromJson(t.getString("cause"));
      StackTraceElement[] stackTrace = null;
      JSONArray st = t.getJSONArray("stackTrace");
      if (st.length() > 0) {
        stackTrace = new StackTraceElement[st.length()];
        for (int i = 0; i < st.length(); i++) {
          stackTrace[i] = serializableStackTraceElementFromJson(st.getString(i));
        }
      }
      return new SerializableThrowable(message, cause, stackTrace);
    } catch (JSONException e) {
    }
    return null;
  }
}
