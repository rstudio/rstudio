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
package com.google.gwt.core;

import com.google.gwt.core.interop.JsExportTest;
import com.google.gwt.core.interop.JsFunctionTest;
import com.google.gwt.core.interop.JsMethodTest;
import com.google.gwt.core.interop.JsPropertyTest;
import com.google.gwt.core.interop.JsTypeArrayTest;
import com.google.gwt.core.interop.JsTypeBridgeTest;
import com.google.gwt.core.interop.JsTypeTest;
import com.google.gwt.core.interop.NativeJsTypeTest;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * All core tests that require js interop.
 */
public class CoreJsInteropSuite {
  public static Test suite() {
    TestSuite suite = new TestSuite("All core js interop tests");

    suite.addTestSuite(JsExportTest.class);
    suite.addTestSuite(JsTypeTest.class);
    suite.addTestSuite(JsTypeBridgeTest.class);
    suite.addTestSuite(JsPropertyTest.class);
    suite.addTestSuite(JsMethodTest.class);
    suite.addTestSuite(JsTypeArrayTest.class);
    suite.addTestSuite(JsFunctionTest.class);
    suite.addTestSuite(NativeJsTypeTest.class);

    return suite;
  }
}
