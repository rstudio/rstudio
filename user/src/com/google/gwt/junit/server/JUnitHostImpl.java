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

import com.google.gwt.core.server.StackTraceDeobfuscator;
import com.google.gwt.junit.JUnitFatalLaunchException;
import com.google.gwt.junit.JUnitMessageQueue;
import com.google.gwt.junit.JUnitMessageQueue.ClientInfoExt;
import com.google.gwt.junit.JUnitShell;
import com.google.gwt.junit.client.TimeoutException;
import com.google.gwt.junit.client.impl.JUnitHost;
import com.google.gwt.junit.client.impl.JUnitResult;
import com.google.gwt.junit.linker.JUnitSymbolMapsLinker;
import com.google.gwt.user.client.rpc.InvocationException;
import com.google.gwt.user.server.rpc.RPCServletUtils;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * An RPC servlet that serves as a proxy to JUnitTestShell. Enables
 * communication between the unit test code running in a browser and the real
 * test process.
 */
public class JUnitHostImpl extends RemoteServiceServlet implements JUnitHost {

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

  private StackTraceDeobfuscator deobfuscator;

  public InitialResponse getTestBlock(int blockIndex, ClientInfo clientInfo)
      throws TimeoutException {
    ClientInfoExt clientInfoExt;
    HttpServletRequest request = getThreadLocalRequest();
    if (clientInfo.getSessionId() < 0) {
      clientInfoExt = createNewClientInfo(request);
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
    if (requestURI.endsWith("/junithost/error")) {
      String msg = RPCServletUtils.readContentAsGwtRpc(request);
      System.err.println("Warning: " + msg);
    } else if (requestURI.endsWith("/junithost/error/fatal")) {
      String msg = RPCServletUtils.readContentAsGwtRpc(request);
      System.err.println("Fatal error: " + msg);
      System.exit(1);
    } else if (requestURI.endsWith("/junithost/error/launch")) {
      String requestPayload = RPCServletUtils.readContentAsGwtRpc(request);
      JUnitResult result = new JUnitResult();
      initResult(request, result);
      result.setException(new JUnitFatalLaunchException(requestPayload));
      getHost().reportFatalLaunch(createNewClientInfo(request), result);
    } else {
      super.service(request, response);
    }
  }

  private ClientInfoExt createClientInfo(ClientInfo clientInfo, HttpServletRequest request) {
    assert (clientInfo.getSessionId() >= 0);
    return new ClientInfoExt(clientInfo.getSessionId(), getClientDesc(request));
  }

  private ClientInfoExt createNewClientInfo(HttpServletRequest request) {
    return new ClientInfoExt(createSessionId(), getClientDesc(request));
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
    result.setAgent(request.getHeader("User-Agent"));
    result.setHost(request.getRemoteHost());
    Throwable throwable = result.getException();
    if (throwable != null) {
      deobfuscateStackTrace(throwable);
    }
  }

  private void deobfuscateStackTrace(Throwable throwable) {
    try {
      getDeobfuscator().deobfuscateStackTrace(throwable, getPermutationStrongName());
    } catch (IOException e) {
      System.err.println("Unable to deobfuscate a stack trace due to an error:");
      e.printStackTrace();
    }
  }

  private StackTraceDeobfuscator getDeobfuscator() throws IOException {
    if (deobfuscator == null) {
      String path = getRequestModuleBasePath() + "/" + JUnitSymbolMapsLinker.SYMBOL_MAP_DIR;
      deobfuscator = StackTraceDeobfuscator.fromUrl(getServletContext().getResource(path));
    }
    return deobfuscator;
  }
}
