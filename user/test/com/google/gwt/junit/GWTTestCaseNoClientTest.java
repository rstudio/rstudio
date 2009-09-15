/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.junit;

import com.google.gwt.junit.JUnitShell.Strategy;
import com.google.gwt.junit.client.GWTTestCase;

import junit.framework.TestCase;

/**
 * Tests for {@link GWTTestCase} that cannot be run on the client because they
 * have dependencies on non-client code.
 */
public class GWTTestCaseNoClientTest extends TestCase {

  /**
   * A mock version of the {@link GWTTestCase} used for testing.
   */
  private static class MockGWTTestCase extends GWTTestCase {
    @Override
    public String getModuleName() {
      return "com.google.gwt.mock.Mock";
    }
  }

  public void testGetStrategy() {
    GWTTestCase testCase = new MockGWTTestCase();
    Strategy strategy = testCase.getStrategy();
    assertEquals("com.google.gwt.junit.JUnit", strategy.getModuleInherit());
    assertEquals("JUnit", strategy.getSyntheticModuleExtension());
    assertEquals("com.google.gwt.mock.Mock.JUnit",
        testCase.getSyntheticModuleName());
  }
}
