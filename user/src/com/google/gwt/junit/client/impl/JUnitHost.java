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
package com.google.gwt.junit.client.impl;

import com.google.gwt.junit.client.TimeoutException;
import com.google.gwt.user.client.rpc.RemoteService;

import java.io.Serializable;
import java.util.HashMap;

/**
 * An interface for {@link com.google.gwt.junit.client.GWTTestCase} to
 * communicate with the test process through RPC.
 */
public interface JUnitHost extends RemoteService {

  /**
   * Information about the client browser.
   */
  public static class ClientInfo implements Serializable {
    /**
     * This client's unique session id.
     */
    private int sessionId;

    /**
     * The GWT user.agent property of this client, e.g. "ie6", "safari", etc.
     */
    private String userAgent;

    public ClientInfo(int sessionId, String userAgent) {
      this.sessionId = sessionId;
      this.userAgent = userAgent;
    }

    /**
     * Constructor for serialization.
     */
    ClientInfo() {
    }

    public int getSessionId() {
      return sessionId;
    }

    public String getUserAgent() {
      return userAgent;
    }
  }

  /**
   * An initial response that sets the client session id.
   */
  public static class InitialResponse implements Serializable {
    /**
     * The unique client session id.
     */
    private int sessionId;

    /**
     * The first test block to run.
     */
    private TestBlock testBlock;

    public InitialResponse(int sessionId, TestBlock testBlock) {
      this.sessionId = sessionId;
      this.testBlock = testBlock;
    }

    /**
     * Constructor for serialization.
     */
    InitialResponse() {
    }

    public int getSessionId() {
      return sessionId;
    }

    public TestBlock getTestBlock() {
      return testBlock;
    }
  }

  /**
   * Returned from the server to tell the system what test to run next.
   */
  public static class TestBlock implements Serializable {
    private int index;
    private TestInfo[] tests;

    public TestBlock(TestInfo[] tests, int index) {
      this.tests = tests;
      this.index = index;
    }

    /**
     * Constructor for serialization.
     */
    TestBlock() {
    }

    public int getIndex() {
      return index;
    }

    public TestInfo[] getTests() {
      return tests;
    }
  }

  /**
   * Returned from the server to tell the system what test to run next.
   */
  public static class TestInfo implements Serializable {
    private String testClass;
    private String testMethod;
    private String testModule;

    public TestInfo(String testModule, String testClass, String testMethod) {
      this.testModule = testModule;
      this.testClass = testClass;
      this.testMethod = testMethod;
    }

    /**
     * Constructor for serialization.
     */
    TestInfo() {
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof TestInfo) {
        TestInfo other = (TestInfo) o;
        return getTestModule().equals(other.getTestModule())
            && getTestClass().equals(other.getTestClass())
            && getTestMethod().equals(other.getTestMethod());
      }
      return false;
    }

    public String getTestClass() {
      return testClass;
    }

    public String getTestMethod() {
      return testMethod;
    }

    public String getTestModule() {
      return testModule;
    }

    @Override
    public int hashCode() {
      return toString().hashCode();
    }

    @Override
    public String toString() {
      return testModule + ":" + testClass + "." + testMethod;
    }
  }

  /**
   * Gets a specific block of tests to run.
   * 
   * @param blockIndex the index of the test block to retrieve
   * @param clientInfo the info for this client
   * @return the initial response
   * @throws TimeoutException if the wait for the next method times out.
   */
  InitialResponse getTestBlock(int blockIndex, ClientInfo clientInfo)
      throws TimeoutException;

  /**
   * Reports results for the last method run and gets the name of next method to
   * run.
   * 
   * @param results the results of executing the test
   * @param blockIndex the index of the test block to retrieve
   * @param clientInfo the info for this client
   * @return the next test block
   * @throws TimeoutException if the wait for the next method times out.
   */
  TestBlock reportResultsAndGetTestBlock(
      HashMap<TestInfo, JUnitResult> results, int blockIndex,
      ClientInfo clientInfo) throws TimeoutException;
}
