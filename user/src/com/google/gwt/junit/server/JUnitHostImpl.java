/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.junit.server;

import com.google.gwt.dev.util.JsniRef;
import com.google.gwt.dev.util.StringKey;
import com.google.gwt.junit.JUnitFatalLaunchException;
import com.google.gwt.junit.JUnitMessageQueue;
import com.google.gwt.junit.JUnitShell;
import com.google.gwt.junit.JUnitMessageQueue.ClientInfoExt;
import com.google.gwt.junit.client.TimeoutException;
import com.google.gwt.junit.client.impl.JUnitHost;
import com.google.gwt.junit.client.impl.JUnitResult;
import com.google.gwt.user.client.rpc.InvocationException;
import com.google.gwt.user.server.rpc.HybridServiceServlet;
import com.google.gwt.user.server.rpc.RPCServletUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * An RPC servlet that serves as a proxy to JUnitTestShell. Enables
 * communication between the unit test code running in a browser and the real
 * test process.
 */
public class JUnitHostImpl extends HybridServiceServlet implements JUnitHost {

  private static class StrongName extends StringKey {
    protected StrongName(String value) {
      super(value);
    }
  }

  private static class SymbolName extends StringKey {
    protected SymbolName(String value) {
      super(value);
    }
  }

  /**
   * A hook into GWTUnitTestShell, the underlying unit test process.
   */
  private static JUnitMessageQueue sHost = null;

  /**
   * A maximum timeout to wait for the test system to respond with the next
   * test. The test system should respond nearly instantly if there are further
   * tests to run, unless the tests have not yet been compiled.
   */
  private static final int TIME_TO_WAIT_FOR_TESTNAME = 300000;

  /**
   * Monotonic increase counter to create unique client session ids.
   */
  private static final AtomicInteger uniqueSessionId = new AtomicInteger();

  /**
   * Tries to grab the GWTUnitTestShell sHost environment to communicate with
   * the real test process.
   */
  private static synchronized JUnitMessageQueue getHost() {
    if (sHost == null) {
      sHost = JUnitShell.getMessageQueue();
      if (sHost == null) {
        throw new InvocationException(
            "Unable to find JUnitShell; is this servlet running under GWTTestCase?");
      }
    }
    return sHost;
  }

  private Map<StrongName, Map<SymbolName, String>> symbolMaps = new HashMap<StrongName, Map<SymbolName, String>>();

  public InitialResponse getTestBlock(int blockIndex, ClientInfo clientInfo)
      throws TimeoutException {
    ClientInfoExt clientInfoExt;
    HttpServletRequest request = getThreadLocalRequest();
    if (clientInfo.getSessionId() < 0) {
      clientInfoExt = createNewClientInfo(clientInfo.getUserAgent(), request);
    } else {
      clientInfoExt = createClientInfo(clientInfo, request);
    }
    TestBlock initialTestBlock = getHost().getTestBlock(clientInfoExt,
        blockIndex, TIME_TO_WAIT_FOR_TESTNAME);
    // Send back the updated session id.
    return new InitialResponse(clientInfoExt.getSessionId(), initialTestBlock);
  }

  public TestBlock reportResultsAndGetTestBlock(
      HashMap<TestInfo, JUnitResult> results, int testBlock,
      ClientInfo clientInfo) throws TimeoutException {
    for (JUnitResult result : results.values()) {
      initResult(getThreadLocalRequest(), result);
      resymbolize(result.getException());
    }
    JUnitMessageQueue host = getHost();
    ClientInfoExt clientInfoExt = createClientInfo(clientInfo,
        getThreadLocalRequest());
    host.reportResults(clientInfoExt, results);
    return host.getTestBlock(clientInfoExt, testBlock,
        TIME_TO_WAIT_FOR_TESTNAME);
  }

  @Override
  protected void service(HttpServletRequest request,
      HttpServletResponse response) throws ServletException, IOException {
    String requestURI = request.getRequestURI();
    if (requestURI.endsWith("/junithost/loadError")) {
      String requestPayload = RPCServletUtils.readContentAsGwtRpc(request);
      JUnitResult result = new JUnitResult();
      initResult(request, result);
      result.setException(new JUnitFatalLaunchException(requestPayload));
      getHost().reportFatalLaunch(createNewClientInfo(null, request), result);
    } else {
      super.service(request, response);
    }
  }

  private ClientInfoExt createClientInfo(ClientInfo clientInfo,
      HttpServletRequest request) {
    assert (clientInfo.getSessionId() >= 0);
    return new ClientInfoExt(clientInfo.getSessionId(),
        clientInfo.getUserAgent(), getClientDesc(request));
  }

  private ClientInfoExt createNewClientInfo(String userAgent,
      HttpServletRequest request) {
    return new ClientInfoExt(createSessionId(), userAgent,
        getClientDesc(request));
  }

  private int createSessionId() {
    return uniqueSessionId.getAndIncrement();
  }

  /**
   * Returns a client description for the current request.
   */
  private String getClientDesc(HttpServletRequest request) {
    String machine = request.getRemoteHost();
    String agent = request.getHeader("User-Agent");
    return machine + " / " + agent;
  }

  private void initResult(HttpServletRequest request, JUnitResult result) {
    String agent = request.getHeader("User-Agent");
    result.setAgent(agent);
    String machine = request.getRemoteHost();
    result.setHost(machine);
  }

  private synchronized Map<SymbolName, String> loadSymbolMap(
      StrongName strongName) {
    Map<SymbolName, String> toReturn = symbolMaps.get(strongName);
    if (toReturn != null) {
      return toReturn;
    }
    toReturn = new HashMap<SymbolName, String>();

    /*
     * Collaborate with SymbolMapsLinker for the location of the symbol data
     * because the -aux directory isn't accessible via the servlet context.
     */
    String path = getRequestModuleBasePath() + "/.junit_symbolMaps/"
        + strongName.get() + ".symbolMap";
    InputStream in = getServletContext().getResourceAsStream(path);
    if (in == null) {
      symbolMaps.put(strongName, null);
      return null;
    }

    BufferedReader bin = new BufferedReader(new InputStreamReader(in));
    String line;
    try {
      try {
        while ((line = bin.readLine()) != null) {
          if (line.charAt(0) == '#') {
            continue;
          }
          int idx = line.indexOf(',');
          toReturn.put(new SymbolName(line.substring(0, idx)),
                       line.substring(idx + 1));
        }
      } finally {
        bin.close();
      }
    } catch (IOException e) {
      toReturn = null;
    }

    symbolMaps.put(strongName, toReturn);
    return toReturn;
  }

  /**
   * Resymbolizes a trace from obfuscated symbols to Java names.
   */
  private void resymbolize(Throwable exception) {
    if (exception == null) {
      return;
    }
    StackTraceElement[] stackTrace = exception.getStackTrace();
    StrongName strongName = new StrongName(getPermutationStrongName());
    Map<SymbolName, String> map = loadSymbolMap(strongName);
    if (map == null) {
      return;
    }
    for (int i = 0; i < stackTrace.length; ++i) {
      StackTraceElement ste = stackTrace[i];
      String symbolData = map.get(new SymbolName(ste.getMethodName()));
      if (symbolData != null) {
        // jsniIdent, className, memberName, sourceUri, sourceLine
        String[] parts = symbolData.split(",");
        assert parts.length == 6 : "Expected 6, have " + parts.length;

        JsniRef ref = JsniRef.parse(parts[0].substring(0,
            parts[0].lastIndexOf(')') + 1));
        stackTrace[i] = new StackTraceElement(ref.className(),
            ref.memberName(), ste.getFileName(), ste.getLineNumber());
      }
    }
    exception.setStackTrace(stackTrace);
    resymbolize(exception.getCause());
  }
}
