/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.regexp;

import com.google.gwt.junit.tools.GWTTestSuite;
import com.google.gwt.regexp.shared.GwtRegExpTest;
import com.google.gwt.regexp.shared.RegExpTest;

import junit.framework.Test;

/**
 * All RegExp tests.
 */
public class RegExpSuite {
  public static Test suite() {
    GWTTestSuite suite = new GWTTestSuite("All RegExp tests");

    // $JUnit-BEGIN$
    suite.addTestSuite(RegExpTest.class);
    suite.addTestSuite(GwtRegExpTest.class);
    // $JUnit-END$

    return suite;
  }
}
