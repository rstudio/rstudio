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
import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;

import java.util.HashMap;

/**
 * An interface for {@link com.google.gwt.junit.client.GWTTestCase} to
 * communicate with the test process through RPC.
 */
public interface JUnitHost extends RemoteService {

  /**
   * Returned from the server to tell the system what test to run next.
   */
  public static class TestBlock implements IsSerializable {
    private TestInfo[] tests;
    private int index;

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
  public static class TestInfo implements IsSerializable {
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
   * @param userAgent the user agent property of this client
   * @return the test block
   * @throws TimeoutException if the wait for the next method times out.
   */
  TestBlock getTestBlock(int blockIndex, String userAgent) throws TimeoutException;

  /**
   * Reports results for the last method run and gets the name of next method to
   * run.
   * 
   * @param results the results of executing the test
   * @param blockIndex the index of the test block to retrieve
   * @param userAgent the user agent property of this client
   * @return the next test block
   * @throws TimeoutException if the wait for the next method times out.
   */
  TestBlock reportResultsAndGetTestBlock(
      HashMap<TestInfo, JUnitResult> results, int blockIndex, String userAgent)
      throws TimeoutException;
}
