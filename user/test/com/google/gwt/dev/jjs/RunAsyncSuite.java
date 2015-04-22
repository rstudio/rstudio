/*
 * Copyright 2014 Google Inc.
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
package com.google.gwt.dev.jjs;

import com.google.gwt.core.client.prefetch.RunAsyncCodeTest;
import com.google.gwt.dev.jjs.test.CodeSplitterCollapsedPropertiesTest;
import com.google.gwt.dev.jjs.test.RunAsyncFailureTest;
import com.google.gwt.dev.jjs.test.RunAsyncMetricsIntegrationTest;
import com.google.gwt.dev.jjs.test.RunAsyncTest;
import com.google.gwt.dev.jjs.test.SystemGetPropertyTest;
import com.google.gwt.junit.tools.GWTTestSuite;

import junit.framework.Test;

/**
 * The RunAsync suite for both compiler and core.
 */
public class RunAsyncSuite {

  public static Test suite() {
    GWTTestSuite suite = new GWTTestSuite("RunAsync test");

    // $JUnit-BEGIN$
    suite.addTestSuite(SystemGetPropertyTest.class);
    suite.addTestSuite(CodeSplitterCollapsedPropertiesTest.class);
    suite.addTestSuite(RunAsyncCodeTest.class);
    suite.addTestSuite(RunAsyncFailureTest.class);
    suite.addTestSuite(RunAsyncMetricsIntegrationTest.class);
    suite.addTestSuite(RunAsyncTest.class);
    // $JUnit-END$

    return suite;
  }
}
