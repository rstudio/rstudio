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
import com.google.gwt.junit.client.impl.ExceptionWrapper;
import com.google.gwt.junit.client.impl.JUnitHost;
import com.google.gwt.junit.client.impl.JUnitResult;
import com.google.gwt.junit.client.impl.StackTraceWrapper;
import com.google.gwt.user.client.rpc.InvocationException;
import com.google.gwt.user.server.rpc.HybridServiceServlet;
import com.google.gwt.user.server.rpc.RPCServletUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
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

  /**
   * Simple helper method to set inaccessible fields via reflection.
   */
  private static <T> void setField(Class<T> cls, String fieldName, T obj,
      Object value) throws SecurityException, NoSuchFieldException,
      IllegalArgumentException, IllegalAccessException {
    Field fld = cls.getDeclaredField(fieldName);
    fld.setAccessible(true);
    fld.set(obj, value);
  }

  private Map<StrongName, Map<SymbolName, String>> symbolMaps = new HashMap<StrongName, Map<SymbolName, String>>();

  public InitialResponse getTestBlock(int blockIndex, ClientInfo clientInfo)
      throws TimeoutException {
    ClientInfoExt clientInfoExt;
    if (clientInfo.getSessionId() < 0) {
      clientInfoExt = createNewClientInfo(clientInfo.getUserAgent());
    } else {
      clientInfoExt = createClientInfo(clientInfo);
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
      ExceptionWrapper ew = result.getExceptionWrapper();
      result.setException(deserialize(ew));
    }
    JUnitMessageQueue host = getHost();
    ClientInfoExt clientInfoExt = createClientInfo(clientInfo);
    host.reportResults(clientInfoExt, results);
    return host.getTestBlock(clientInfoExt, testBlock,
        TIME_TO_WAIT_FOR_TESTNAME);
  }

  @Override
  protected void service(HttpServletRequest request,
      HttpServletResponse response) throws ServletException, IOException {
    String requestURI = request.getRequestURI();
    if (requestURI.endsWith("/junithost/loadError")) {
      String requestPayload = RPCServletUtils.readContentAsUtf8(request);
      JUnitResult result = new JUnitResult();
      initResult(request, result);
      result.setException(new JUnitFatalLaunchException(requestPayload));
      getHost().reportFatalLaunch(createNewClientInfo(null), result);
    } else {
      super.service(request, response);
    }
  }

  private ClientInfoExt createClientInfo(ClientInfo clientInfo) {
    assert (clientInfo.getSessionId() >= 0);
    return new ClientInfoExt(clientInfo.getSessionId(),
        clientInfo.getUserAgent(), getClientDesc(getThreadLocalRequest()));
  }

  private ClientInfoExt createNewClientInfo(String userAgent) {
    return new ClientInfoExt(createSessionId(), userAgent,
        getClientDesc(getThreadLocalRequest()));
  }

  private int createSessionId() {
    return uniqueSessionId.getAndIncrement();
  }

  /**
   * Deserializes an ExceptionWrapper back into a Throwable.
   */
  private Throwable deserialize(ExceptionWrapper ew) {
    if (ew == null) {
      return null;
    }

    Throwable ex = null;
    Throwable cause = deserialize(ew.cause);
    try {
      Class<?> exClass = Class.forName(ew.typeName);
      try {
        // try ExType(String, Throwable)
        Constructor<?> ctor = exClass.getDeclaredConstructor(String.class,
            Throwable.class);
        ctor.setAccessible(true);
        ex = (Throwable) ctor.newInstance(ew.message, cause);
      } catch (Throwable e) {
        // try ExType(String)
        try {
          Constructor<?> ctor = exClass.getDeclaredConstructor(String.class);
          ctor.setAccessible(true);
          ex = (Throwable) ctor.newInstance(ew.message);
          ex.initCause(cause);
        } catch (Throwable e2) {
          // try ExType(Throwable)
          try {
            Constructor<?> ctor = exClass.getDeclaredConstructor(Throwable.class);
            ctor.setAccessible(true);
            ex = (Throwable) ctor.newInstance(cause);
            setField(Throwable.class, "detailMessage", ex, ew.message);
          } catch (Throwable e3) {
            // try ExType()
            try {
              Constructor<?> ctor = exClass.getDeclaredConstructor();
              ctor.setAccessible(true);
              ex = (Throwable) ctor.newInstance();
              ex.initCause(cause);
              setField(Throwable.class, "detailMessage", ex, ew.message);
            } catch (Throwable e4) {
              // we're out of options
              this.log("Failed to deserialize getException of type '"
                  + ew.typeName + "'; no available constructor", e4);

              // fall through
            }
          }
        }
      }

    } catch (Throwable e) {
      this.log("Failed to deserialize getException of type '" + ew.typeName
          + "'", e);
    }

    if (ex == null) {
      ex = new RuntimeException(ew.typeName + ": " + ew.message, cause);
    }

    ex.setStackTrace(deserialize(ew.stackTrace));
    return ex;
  }

  /**
   * Deserializes a StackTraceWrapper back into a StackTraceElement.
   */
  private StackTraceElement deserialize(StackTraceWrapper stw) {
    StackTraceElement ste = null;

    Object[] args = resymbolize(stw);

    try {
      try {
        // Try the 4-arg ctor (JRE 1.5)
        Constructor<StackTraceElement> ctor = StackTraceElement.class.getDeclaredConstructor(
            String.class, String.class, String.class, int.class);
        ctor.setAccessible(true);
        ste = ctor.newInstance(args);
      } catch (NoSuchMethodException e) {
        // Okay, see if there's a zero-arg ctor we can use instead (JRE 1.4.2)
        Constructor<StackTraceElement> ctor = StackTraceElement.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        ste = ctor.newInstance();
        setField(StackTraceElement.class, "declaringClass", ste, args[0]);
        setField(StackTraceElement.class, "methodName", ste, args[1]);
        setField(StackTraceElement.class, "fileName", ste, args[2]);
        setField(StackTraceElement.class, "lineNumber", ste, args[3]);
      }
    } catch (Throwable e) {
      this.log("Error creating stack trace", e);
    }
    return ste;
  }

  /**
   * Deserializes a StackTraceWrapper[] back into a StackTraceElement[].
   */
  private StackTraceElement[] deserialize(StackTraceWrapper[] stackTrace) {
    int len = stackTrace.length;
    StackTraceElement[] result = new StackTraceElement[len];
    for (int i = 0; i < len; ++i) {
      result[i] = deserialize(stackTrace[i]);
    }
    return result;
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
      while ((line = bin.readLine()) != null) {
        if (line.charAt(0) == '#') {
          continue;
        }
        int idx = line.indexOf(',');
        toReturn.put(new SymbolName(line.substring(0, idx)),
            line.substring(idx + 1));
      }
    } catch (IOException e) {
      toReturn = null;
    }

    symbolMaps.put(strongName, toReturn);
    return toReturn;
  }

  /**
   * @return {className, methodName, fileName, lineNumber}
   */
  private Object[] resymbolize(StackTraceWrapper stw) {
    Object[] toReturn;
    StrongName strongName = new StrongName(getPermutationStrongName());
    Map<SymbolName, String> map = loadSymbolMap(strongName);
    String symbolData = map == null ? null : map.get(new SymbolName(
        stw.methodName));

    if (symbolData != null) {
      // jsniIdent, className, memberName, sourceUri, sourceLine
      String[] parts = symbolData.split(",");
      assert parts.length == 5 : "Expected 5, have " + parts.length;

      JsniRef ref = JsniRef.parse(parts[0].substring(0,
          parts[0].lastIndexOf(')') + 1));
      toReturn = new Object[] {
          ref.className(), ref.memberName(), stw.fileName, stw.lineNumber};

    } else {
      // Use the raw data from the client
      toReturn = new Object[] {
          stw.className, stw.methodName, stw.fileName, stw.lineNumber};
    }
    return toReturn;
  }
}
