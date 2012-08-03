/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.aria;

import com.google.gwt.aria.client.AttributeTest;
import com.google.gwt.aria.client.RoleImplTest;
import com.google.gwt.junit.tools.GWTTestSuite;

import junit.framework.Test;

/**
 * Tests of the aria package.
 */
public class AriaSuite {
  public static Test suite() {
    GWTTestSuite suite = new GWTTestSuite("Test for suite for aria");
    suite.addTestSuite(AttributeTest.class);
    suite.addTestSuite(RoleImplTest.class);
    return suite;
  }
}
