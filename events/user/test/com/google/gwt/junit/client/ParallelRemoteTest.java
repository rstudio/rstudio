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
package com.google.gwt.junit.client;

/**
 * This class tests the -remoteweb parallel execution features in GWT's JUnit
 * support. This test should not be part of the automatically run test suite,
 * because it intentionally generates failures at different browser clients.
 * 
 * What we're looking for in the output of this test is that the failures
 * additionally contain the host and browser at which the test failed.
 * 
 * To run this test correctly, you should be using the -remoteweb option with at
 * least three different clients.
 * 
 */
public class ParallelRemoteTest extends GWTTestCase {

  public String getModuleName() {
    return "com.google.gwt.junit.JUnit";
  }

  public void testAssertFailsOnNotIE() {
    String agent = getAgent().toLowerCase();
    if (agent.indexOf("msie") == -1) {
      fail("Browser is not IE.");
    }
  }

  public void testAssertFailsOnNotSafari() {
    String agent = getAgent().toLowerCase();
    if (agent.indexOf("safari") == -1) {
      fail("Browser is not Safari.");
    }
  }

  public void testExceptionFailsOnNotIE() {
    String agent = getAgent().toLowerCase();
    if (agent.indexOf("msie") == -1) {
      throw new RuntimeException("Browser is not IE.");
    }
  }

  public void testExceptionFailsOnNotSafari() {
    String agent = getAgent().toLowerCase();
    if (agent.indexOf("safari") == -1) {
      throw new RuntimeException("Browser is not Safari.");
    }
  }

  private native String getAgent() /*-{
    return navigator.userAgent.toString();
  }-*/;
}
