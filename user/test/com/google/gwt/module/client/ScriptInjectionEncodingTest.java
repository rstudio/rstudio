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
 * Tests encoding with an external script.
 */
public class ScriptInjectionEncodingTest extends GWTTestCase {

  public String getModuleName() {
    return "com.google.gwt.module.ScriptInjectionEncodingTest";
  }

  /**
   * Ensure the script is loaded with the correct encoding (UTF-8).
   */
  public void testScriptExists() {
    assertEquals("à", scriptEncoding());
  }

  /**
   * The native method called here is defined in ScriptInjectionEncodingTest.js.
   */
  public static native String scriptEncoding() /*-{
    return $wnd.scriptEncoding();
  }-*/;
}
