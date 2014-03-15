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
package com.google.gwt.user;

import com.google.gwt.dev.BootStrapPlatform;
import com.google.gwt.junit.tools.GWTTestSuite;
import com.google.gwt.user.client.rpc.LoggingRPCTest;

import junit.framework.Test;

/**
 * A collection of TestCases for the RPC system.
 */
public class LoggingRPCSuite {

  static {
    /*
     * Required for OS X Leopard. This call ensures we have a valid context
     * ClassLoader. Many of the tests test low-level RPC mechanisms and rely on
     * a ClassLoader to resolve classes and resources.
     */
    BootStrapPlatform.applyPlatformHacks();
  }

  public static Test suite() {
    GWTTestSuite suite = new GWTTestSuite(
        "Test for com.google.gwt.user.client.rpc.core.java.util.logging");

    // GWTTestCases
    suite.addTestSuite(LoggingRPCTest.class);

    return suite;
  }
}
