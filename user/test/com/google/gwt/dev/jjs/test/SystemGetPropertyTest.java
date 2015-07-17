/*
 * Copyright 2015 Google Inc.
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
package com.google.gwt.dev.jjs.test;

import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests System.getProperty with collapsed properties.
 */
public class SystemGetPropertyTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.jjs.SystemGetPropertyTest";
  }

  // In DevMode System.getProperty is routed to the standard Java implementation.
  @DoNotRunWith(Platform.Devel)
  public void testBindingProperties() {
    assertEquals("two", System.getProperty("collapsedProperty"));
    assertEquals("two", System.getProperty("collapsedProperty", "default"));
    assertEquals("blue", System.getProperty("someOtherDynamicProperty"));
    String expectedResult = "safari".equals(System.getProperty("user.agent")) ?
        "InSafari" : "NotInSafari";
    assertEquals(expectedResult, System.getProperty("someDynamicProperty"));
  }
}
