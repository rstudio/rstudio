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
package com.google.gwt.core;

import com.google.gwt.core.client.GWTTest;
import com.google.gwt.core.client.HttpThrowableReporterTest;
import com.google.gwt.core.client.JavaScriptExceptionTest;
import com.google.gwt.core.client.JsArrayTest;
import com.google.gwt.core.client.impl.AsyncFragmentLoaderTest;
import com.google.gwt.core.client.impl.EmulatedStackTraceTest;
import com.google.gwt.core.client.impl.StackTraceCreatorTest;
import com.google.gwt.junit.tools.GWTTestSuite;

import junit.framework.Test;

/**
 * All I18N tests.
 */
public class CoreSuite {
  public static Test suite() {
    GWTTestSuite suite = new GWTTestSuite("All core tests");

    // $JUnit-BEGIN$
    suite.addTestSuite(HttpThrowableReporterTest.class);
    suite.addTestSuite(JavaScriptExceptionTest.class);
    suite.addTestSuite(JsArrayTest.class);
    suite.addTestSuite(GWTTest.class);
    suite.addTestSuite(StackTraceCreatorTest.class);
    suite.addTestSuite(EmulatedStackTraceTest.class);
    suite.addTestSuite(AsyncFragmentLoaderTest.class);
    // $JUnit-END$

    return suite;
  }
}
