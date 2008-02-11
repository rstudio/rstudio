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

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.junit.client.TestResults;

/**
 * An interface for {@link com.google.gwt.junit.client.GWTTestCase} to
 * communicate with the test process through RPC.
 */
public interface JUnitHost extends RemoteService {

  /**
   * Returned from the server to tell the system what test to run next.
   */
  public static class TestInfo implements IsSerializable {
    private String testClass;
    private String testMethod;

    public TestInfo(String testClass, String testMethod) {
      this.testClass = testClass;
      this.testMethod = testMethod;
    }

    /**
     * Constructor for serialization.
     */
    TestInfo() {
    }

    public String getTestClass() {
      return testClass;
    }

    public String getTestMethod() {
      return testMethod;
    }
  }

  /**
   * Gets the name of next method to run.
   * 
   * @param moduleName the module name of this client
   * @return the next test to run
   */
  TestInfo getFirstMethod(String moduleName);

  /**
   * Reports results for the last method run and gets the name of next method to
   * run.
   * 
   * @param moduleName the module name of this client
   * @param results The results of executing the test
   * @return the next test to run
   */
  TestInfo reportResultsAndGetNextMethod(String moduleName, TestResults results);
}
