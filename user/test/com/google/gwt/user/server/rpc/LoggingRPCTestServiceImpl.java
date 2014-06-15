/*
 * Copyright 2011 Google Inc.
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

package com.google.gwt.user.server.rpc;

import com.google.gwt.core.server.StackTraceDeobfuscator;
import com.google.gwt.junit.linker.JUnitSymbolMapsLinker;
import com.google.gwt.logging.server.RemoteLoggingServiceUtil;
import com.google.gwt.user.client.rpc.LoggingRPCTest;
import com.google.gwt.user.client.rpc.LoggingRPCTestService;
import com.google.gwt.user.client.rpc.RpcRequestBuilder;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

/**
 * Remote service implementation for serialization of GWT core.java.util.logging emulations.
 */
public class LoggingRPCTestServiceImpl extends RemoteServiceServlet implements
    LoggingRPCTestService {

  @Override
  public LogRecord deobfuscateLogRecord(LogRecord value) {
    // don't deobfuscate DevMode, there's no symbol map
    if ("HostedMode".equals(getPermutationStrongName())) {
      return value;
    }

    StackTraceDeobfuscator deobf = StackTraceDeobfuscator.fromUrl(getSymbolMapUrl());

    HttpServletRequest threadLocalRequest = getThreadLocalRequest();
    String strongName = null;
    if (threadLocalRequest != null) {
      // can be null during tests
      strongName = threadLocalRequest.getHeader(RpcRequestBuilder.STRONG_NAME_HEADER);
    }
    LogRecord newRecord = RemoteLoggingServiceUtil.deobfuscateLogRecord(deobf, value, strongName);
    Logger.getLogger(value.getLoggerName()).log(newRecord);
    return newRecord;
  }

  private URL getSymbolMapUrl() {
    File symbolMapsDirectory = new File("war/" + getJunitSymbolMapsPath());
    try {
      return symbolMapsDirectory.exists() ? symbolMapsDirectory.toURI().toURL()
          : getServletContext().getResource(getJunitSymbolMapsPath());
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public LogRecord echoLogRecord(LogRecord value) throws LoggingRPCTestServiceException {
    /*
     * Don't check the stack trace on the server side, because the expected
     * result it is comparing against came from a server-side instance of
     * LoggingRPCTest, and hence has a different stack trace from the streamed
     * version.
     */
    if (!LoggingRPCTest.isValid(value)) {
      throw new LoggingRPCTestServiceException();
    }

    return value;
  }

  private String getJunitSymbolMapsPath() {
    String path = getRequestModuleBasePath();
    if (!path.endsWith("/")) {
      path += "/";
    }
    path += JUnitSymbolMapsLinker.SYMBOL_MAP_DIR;
    return path;
  }
}
