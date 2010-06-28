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

import com.google.gwt.logging.shared.RemoteLoggingService;
import com.google.gwt.logging.shared.SerializableLogRecord;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import java.util.logging.Logger;

/**
 * Server side code for the remote log handler.
 */
public class RemoteLoggingServiceImpl extends RemoteServiceServlet implements
    RemoteLoggingService {
  private static Logger logger = Logger.getLogger("gwt.remote");

  public final String logOnServer(SerializableLogRecord record) {
    try {
      logger.log(record.getLogRecord());
    } catch (RuntimeException e) {
      String exceptionString = e.toString();
      String failureMessage = "Failed to log message due to " + exceptionString;
      System.err.println(failureMessage);
      e.printStackTrace();
      
      // Return the exception description so that the client code can
      // print or log it if it wants.
      return e.toString();
    }
    return "";
  }
}
