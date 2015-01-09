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
package com.google.gwt.core.client.interop;

import com.google.gwt.core.client.js.JsExport;

import junit.framework.Assert;

class MyClassImpl3 {
  public static boolean calledFromJsModuleWindow = false;
  public static int foo = 0;
  static {
    // prevent optimizations from inlining this clinit()
    if (Math.random() > -1) {
      foo = 42;
    }
  }

  @JsExport("$wnd.MyClassImpl3")
  public MyClassImpl3() {
      // ensure clinit() is called even when invoked from JS
      Assert.assertEquals(42, foo);
      calledFromJsModuleWindow = true;
  }

  public static boolean calledFromBar = false;

  public static class A {
    void bar() {
      calledFromBar = true;
    }
  }

  // There should be no calls to this method from java.
  @JsExport("$wnd.MyClassImpl3.foo")
  public static void foo(A a) {
    a.bar();
  }

  @JsExport("$wnd.newA")
  private static A newA() {
    return new A();
  }
}
