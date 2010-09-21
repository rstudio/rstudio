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

import org.json.JSONException;

import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Utilities for classes that accept Remote Logging requests
 */
public class RemoteLoggingServiceUtil {
  /**
   * Exceptions that occur during remote logging
   */
  public static class RemoteLoggingException extends Exception { 
    public RemoteLoggingException(String message) {
      super(message);
    }
    
    public RemoteLoggingException(String message, Throwable t) {
      super(message, t);
    }
  }

  /**
   * Logs a message on the server
   * @param slr LogRecord to be logged
   * @param strongName Permutation name (used for deobfuscation and may be null,
   *        which will only cause deobfuscation to fail)
   * @param deobfuscator used for deobfuscation. May be null, which will only
   *        cause deobfuscation to fail.
   * @param loggerNameOverride logger name for messages logged on server. May be
   *        null, in which case, messages will be logged to a logger
   *        corresponding to the client side logger which triggered them.
   * 
   * @return Empty string when successful, or an error string in case of failure.
   */
  public static void logOnServer(SerializableLogRecord slr, String strongName,
      StackTraceDeobfuscator deobfuscator, String loggerNameOverride) throws 
      RemoteLoggingException {
    if (deobfuscator != null) {
      slr = deobfuscator.deobfuscateLogRecord(slr, strongName);
    }
    LogRecord lr = slr.getLogRecord();
    if (lr == null) {
      // If he SerializableLogRecord is corrupt somehow and conversion fails
      // TODO(unnurg): Set the cause here correctly if we don't remove
      // SerializableLogRecord before 2.1 release.
      throw new RemoteLoggingException(
          "Failed to convert SerializableLogRecord to LogRecord");
    }
    String loggerName = loggerNameOverride == null ? lr.getLoggerName() :
      loggerNameOverride;
    Logger logger = Logger.getLogger(loggerName);
    logger.log(lr);
  }
  
  public static void logOnServer(String serializedLogRecordJson,
      String strongName, StackTraceDeobfuscator deobfuscator,
      String loggerNameOverride) throws RemoteLoggingException {
    SerializableLogRecord slr;
    try {
      slr = JsonLogRecordServerUtil.serializableLogRecordFromJson(
          serializedLogRecordJson);
      logOnServer(slr, strongName, deobfuscator, loggerNameOverride);
    } catch (JSONException e) {
      throw new RemoteLoggingException("Failed to deserialize JSON", e);
    }
  }
}
