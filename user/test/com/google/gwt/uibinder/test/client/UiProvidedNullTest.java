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
package com.google.gwt.uibinder.test.client;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Test UiFields(provided = true) give meaningful errors when a field
 * is not initialized. This test is only meant to be run when
 * assertions are turned on.
 */
public class UiProvidedNullTest extends GWTTestCase {

  boolean assertionsEnabled;

  @Override
  public String getModuleName() {
    return "com.google.gwt.uibinder.test.LazyWidgetBuilderSuite";
  }

  @Override
  public void gwtSetUp() {
    try {
      assert false;
      assertionsEnabled = false;
    } catch (AssertionError e) {
      assertionsEnabled = true;
    }
  }

  public void testNullFieldAssertion() {
    if (!assertionsEnabled) {
      return;
    }
    try {
      new UiProvidedNullUi();
      fail("Expected an Assertion Error");
    } catch (AssertionError e) {
      assertEquals("UiField myButton with 'provided = true' was null", e.getMessage());
    } catch (NullPointerException e) {
      fail("Expected an Assertion Error");
    }
  }

}
