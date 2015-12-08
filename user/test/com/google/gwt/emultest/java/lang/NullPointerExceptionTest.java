/*
 * Copyright 2015 Google Inc.
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
package com.google.gwt.emultest.java.lang;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.junit.client.GWTTestCase;

import jsinterop.annotations.JsMethod;

/**
 * Unit tests for the GWT emulation of java.lang.NullPointerException class.
 */
public class NullPointerExceptionTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  @DoNotRunWith(Platform.Devel)
  public void testBackingJsObject() {
    Object caughtNative = catchNpeInNative();
    assertTrue(caughtNative instanceof JavaScriptObject);
    assertTrue(caughtNative.toString().startsWith("TypeError:"));
    assertTrue(caughtNative.toString().contains("<my msg>"));
    assertTrue(caughtNative.toString().contains(NullPointerException.class.getName()));
  }

  private native Object catchNpeInNative() /*-{
    try {
      this.throwJavaNpe();
    } catch (e) {
      return e;
    }
  }-*/;

  @JsMethod
  private void throwJavaNpe() {
    throw new NullPointerException("<my msg>");
  }
}
