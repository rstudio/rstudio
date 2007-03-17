/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.module.client;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests including an external script through inheritance as well as directly.
 * 
 * @see com.google.gwt.module.client.SingleScriptInjectionTest
 */
public class DoubleScriptInjectionTest extends GWTTestCase {

  public String getModuleName() {
    return "com.google.gwt.module.DoubleScriptInjectionTest";
  }

  /**
   * Ensure both scripts are loaded.
   */
  public void testBothScriptsExist() {
    assertEquals("yes1", SingleScriptInjectionTest.scriptOne());
    assertEquals("yes2", scriptTwo());
  }

  /**
   * The native method called here is defined in ScriptInjectionTest2.
   */
  public static native String scriptTwo() /*-{
    return $wnd.scriptTwo();
  }-*/;
}
