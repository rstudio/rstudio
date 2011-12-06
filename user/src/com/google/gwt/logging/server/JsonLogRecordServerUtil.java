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

import com.google.gwt.core.client.impl.SerializableThrowable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * A set of functions to convert standard JSON strings into
 * LogRecords. The corresponding functions to create the JSON
 * strings are in JsonLogRecordClientUtil.java. This class should only be used
 * in server side code since it imports org.json classes.
 * TODO(unnurg) once there is a unified JSON GWT library, combine this with
 * JsonLogRecordClientUtil.
 */
public class JsonLogRecordServerUtil {
  private static Logger logger =
    Logger.getLogger(JsonLogRecordServerUtil.class.getName());
  public static LogRecord logRecordFromJson(String jsonString)
  throws JSONException {
    JSONObject lro = new JSONObject(jsonString);
    String level = lro.getString("level");
    String loggerName = lro.getString("loggerName");
    String msg = lro.getString("msg");
    long timestamp = Long.parseLong(lro.getString("timestamp"));
    Throwable thrown =
      throwableFromJson(lro.getString("thrown"));
    LogRecord lr = new LogRecord(Level.parse(level), msg);
    lr.setLoggerName(loggerName);
    lr.setThrown(thrown);
    lr.setMillis(timestamp);
    return lr;
  }

  private static StackTraceElement stackTraceElementFromJson(
      String jsonString) throws JSONException {
    JSONObject ste = new JSONObject(jsonString);
    String className = ste.getString("className");
    String fileName = ste.getString("fileName");
    String methodName = ste.getString("methodName");
    int lineNumber = Integer.parseInt(ste.getString("lineNumber"));
    return new StackTraceElement(className, methodName, fileName, lineNumber);
  }

  private static Throwable throwableFromJson(String jsonString)
  throws JSONException {
    if (jsonString.equals("{}")) {
      return null;
    }
    JSONObject t = new JSONObject(jsonString);
    String message = t.getString("message");
    Throwable cause =
      throwableFromJson(t.getString("cause"));
    StackTraceElement[] stackTrace = null;
    if (t.has("stackTrace")) {
      JSONArray st = t.getJSONArray("stackTrace");
      if (st.length() > 0) {
        stackTrace = new StackTraceElement[st.length()];
        for (int i = 0; i < st.length(); i++) {
          stackTrace[i] = stackTraceElementFromJson(st.getString(i));
        }
      }
    } else {
      stackTrace = new StackTraceElement[0];
    }
    String exceptionClass = t.getString("type");
    SerializableThrowable.ThrowableWithClassName thrown = 
        new SerializableThrowable.ThrowableWithClassName(message, cause, exceptionClass);
    thrown.setStackTrace(stackTrace);
    return thrown;
  }
}
