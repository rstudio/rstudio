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

import static com.google.gwt.user.client.rpc.RpcRequestBuilder.MODULE_BASE_HEADER;

import com.google.gwt.junit.linker.JUnitSymbolMapsLinker;
import com.google.gwt.logging.server.StackTraceDeobfuscator;
import com.google.gwt.user.client.rpc.LoggingRPCTest;
import com.google.gwt.user.client.rpc.LoggingRPCTestService;
import com.google.gwt.user.client.rpc.RpcRequestBuilder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

/**
 * Remote service implementation for serialization of GWT core.java.util.logging emulations.
 */
public class LoggingRPCTestServiceImpl extends HybridServiceServlet implements
    LoggingRPCTestService {

  @Override
  public LogRecord deobfuscateLogRecord(LogRecord value)
      throws LoggingRPCTestServiceException {
    // don't deobfuscate DevMode, there's no symbol map
    if ("HostedMode".equals(getPermutationStrongName())) {
      return value;
    }

    StackTraceDeobfuscator deobf = new StackTraceDeobfuscator(getSymbolMapsDir()) {
      @Override
      protected InputStream getSourceMapInputStream(String permutationStrongName,
          int fragmentNumber)
          throws IOException {
        if (symbolMapsDirectory.exists()) {
          return super.getSourceMapInputStream(permutationStrongName,
              fragmentNumber);
        } else {
          return getServletContext().getResourceAsStream(
              getModule() + File.separatorChar + JUnitSymbolMapsLinker.SYMBOL_MAP_DIR
                  + permutationStrongName
                  + "_sourceMap" + fragmentNumber + ".json");
        }
      }

      @Override
      protected InputStream getSymbolMapInputStream(String permutationStrongName)
          throws IOException {
        if (symbolMapsDirectory.exists()) {
          return super.getSymbolMapInputStream(permutationStrongName);
        } else {
          String name =
              getModule() + File.separatorChar + JUnitSymbolMapsLinker.SYMBOL_MAP_DIR
                  + permutationStrongName
                  + ".symbolMap";
          return getServletContext().getResourceAsStream(name);
        }
      }
    };

    HttpServletRequest threadLocalRequest = getThreadLocalRequest();
    String strongName = null;
    if (threadLocalRequest != null) {
      // can be null during tests
      strongName = threadLocalRequest.getHeader(RpcRequestBuilder.STRONG_NAME_HEADER);
    }
    LogRecord newRecord = deobf.deobfuscateLogRecord(value, strongName);
    Logger.getLogger(value.getLoggerName()).log(newRecord);
    return newRecord;
  }

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

  private String getModule() {
    try {
      String header = getThreadLocalRequest().getHeader(MODULE_BASE_HEADER);
      if (header == null) {
        return null;
      }
      String path = new URL(header).getPath();
      String contextPath = getThreadLocalRequest().getContextPath();
      if (!path.startsWith(contextPath)) {
        return null;
      }
      path = path.substring(contextPath.length());
      if (path.endsWith("/")) {
        path = path.substring(0, path.length() - 1);
      }
      return path;
    } catch (MalformedURLException e) {
      return null;
    }
  }

  private String getSymbolMapsDir() {
    String path = getModule();
    if (path == null) {
      return null;
    }
    return "war" + File.separatorChar + getModule() + File.separatorChar
        + JUnitSymbolMapsLinker.SYMBOL_MAP_DIR;
  }
}
