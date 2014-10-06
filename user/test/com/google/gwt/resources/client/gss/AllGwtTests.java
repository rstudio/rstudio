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
package com.google.gwt.resources.client.gss;

import com.google.gwt.junit.tools.GWTTestSuite;

import junit.framework.Test;

/**
 * Suite for all GWT tests.
 */
public class AllGwtTests extends GWTTestSuite {

  public static Test suite() {
    GWTTestSuite suite = new GWTTestSuite();
    suite.addTestSuite(GssResourceTest.class);
    suite.addTestSuite(DebugObfuscationStyleTest.class);
    suite.addTestSuite(PrettyObfuscationStyleTest.class);
    suite.addTestSuite(StableShortTypeObfuscationStyleTest.class);
    suite.addTestSuite(StableNoTypeObfuscationStyleTest.class);
    suite.addTestSuite(StableObfuscationStyleTest.class);
    suite.addTestSuite(AutoConversionTest.class);
    return suite;
  }
}
