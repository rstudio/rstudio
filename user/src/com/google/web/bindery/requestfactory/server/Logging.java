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

package com.google.web.bindery.requestfactory.server;

import com.google.gwt.logging.server.RemoteLoggingServiceUtil;
import com.google.gwt.logging.server.RemoteLoggingServiceUtil.RemoteLoggingException;
import com.google.gwt.logging.server.StackTraceDeobfuscator;
import com.google.gwt.user.client.rpc.RpcRequestBuilder;

import javax.servlet.http.HttpServletRequest;

/**
 * Server side object that handles log messages sent by
 * {@link com.google.web.bindery.requestfactory.gwt.client.RequestFactoryLogHandler}
 * .
 */
public class Logging {

  private static StackTraceDeobfuscator deobfuscator = new StackTraceDeobfuscator("");

  /**
   * Logs a message.
   * 
   * @param logRecordJson a json serialized LogRecord, as provided by
   *          {@link com.google.gwt.logging.client.JsonLogRecordClientUtil#logRecordAsJsonObject(LogRecord)}
   * @throws RemoteLoggingException if logging fails
   */
  public static void logMessage(String logRecordJson) throws RemoteLoggingException {
    /*
     * if the header does not exist, we pass null, which is handled gracefully
     * by the deobfuscation code.
     */
    HttpServletRequest threadLocalRequest = RequestFactoryServlet.getThreadLocalRequest();
    String strongName = null;
    if (threadLocalRequest != null) {
      // can be null during tests
      strongName = threadLocalRequest.getHeader(RpcRequestBuilder.STRONG_NAME_HEADER);
    }
    RemoteLoggingServiceUtil.logOnServer(logRecordJson, strongName, deobfuscator, null);
  }

  /**
   * This function is only for server side use which is why it's not in the
   * LoggingRequest interface.
   * 
   * @param dir a directory, specified as a String
   */
  public static void setSymbolMapsDirectory(String dir) {
    deobfuscator.setSymbolMapsDirectory(dir);
  }

  private String id = "";

  private Integer version = 0;

  /**
   * Returns the id of this instance.
   * 
   * @return a String id
   * @see #setId(String)
   */
  public String getId() {
    return this.id;
  }

  /**
   * Returns the version of this instance.
   * 
   * @return an Integer version number
   * @see #setVersion(Integer)
   */
  public Integer getVersion() {
    return this.version;
  }

  /**
   * Sets the id on this instance.
   * 
   * @param id a String id
   * @see #getId()
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Sets the version of this instance.
   * 
   * @param version an Integer version number
   * @see #getVersion()
   */
  public void setVersion(Integer version) {
    this.version = version;
  }
}
