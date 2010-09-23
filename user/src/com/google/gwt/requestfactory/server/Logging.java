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

package com.google.gwt.requestfactory.server;

import com.google.gwt.logging.server.RemoteLoggingServiceUtil;
import com.google.gwt.logging.server.RemoteLoggingServiceUtil.RemoteLoggingException;
import com.google.gwt.logging.server.StackTraceDeobfuscator;
import com.google.gwt.user.client.rpc.RpcRequestBuilder;

/**
 * Server side object that handles log messages sent by
 * {@link RequestFactoryLogHandler}.
 * 
 * TODO(unnurg): Before the end of Sept 2010, combine this class intelligently
 * with SimpleRemoteLogHandler so they share functionality and patterns.
 */
public class Logging {

  private static StackTraceDeobfuscator deobfuscator =
    new StackTraceDeobfuscator("");
  
  public static void logMessage(String serializedLogRecordJson) throws
  RemoteLoggingException {
    // if the header does not exist, we pass null, which is handled gracefully
    // by the deobfuscation code.
    String strongName =
      RequestFactoryServlet.getThreadLocalRequest().getHeader(
          RpcRequestBuilder.STRONG_NAME_HEADER);
    RemoteLoggingServiceUtil.logOnServer(serializedLogRecordJson,
        strongName, deobfuscator, null);
  }
  
  /**
   * This function is only for server side use which is why it's not in the
   * LoggingRequest interface.
   */
  public static void setSymbolMapsDirectory(String dir) {
    deobfuscator.setSymbolMapsDirectory(dir);
  }
  
  private String id = "";
  
  private Integer version = 0;  
  
  public String getId() {
    return this.id;
  }
  
  public Integer getVersion() {
    return this.version;
  }

  public void setId(String id) {
    this.id = id;
  }
    
  public void setVersion(Integer version) {
    this.version = version;
  }
}

