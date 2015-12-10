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
package com.google.gwt.core.interop;

import jsinterop.annotations.JsType;

/**
 * A test class that is exported.
 */
@JsType
public class MyExportedClass {

  public static final int EXPORTED_1 = 100;

  public static int foo() {
    return 200;
  }

  public static int replacementFoo() {
    return 1000;
  }

  public static final InnerClass EXPORTED_2 = new InnerClass(5);

  public static int bar(int a, int b) {
    return EXPORTED_2.fun(a, b);
  }

  /**
   * static inner JsType class.
   */
  @JsType
  public static class InnerClass {
    public int field;

    public InnerClass(int field) {
      this.field = field;
    }

    public int fun(int a, int b) {
      // return (a + b + |a - b| + field)
      // prevent optimizations from inlining this function
      int c = a + b;
      if (a > b) {
        c = c + a - b;
      } else {
        c = c + b - a;
      }
      return c + field;
    }
  }

  public static InnerClass newInnerClass(int field) {
    return new InnerClass(field);
  }
}
