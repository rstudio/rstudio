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

import com.google.gwt.testing.TestUtils;

import jsinterop.annotations.JsType;

/**
 * Unit tests for the GWT emulation of java.lang.NullPointerException class.
 */
public class NullPointerExceptionTest extends ThrowableTestBase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  @JsType(isNative = true, namespace = "<window>")
  private static class TypeError { }

  public void testBackingJsObject() {
    // Do not run the test in JVM.
    if (TestUtils.isJvm()) {
      return;
    }

    Object caughtNative = catchNative(createThrower(new NullPointerException("<my msg>")));
    assertTrue(caughtNative instanceof TypeError);
    assertTrue(caughtNative.toString().startsWith("TypeError:"));
    assertTrue(caughtNative.toString().contains("<my msg>"));
    assertTrue(caughtNative.toString().contains(NullPointerException.class.getName()));
  }
}
