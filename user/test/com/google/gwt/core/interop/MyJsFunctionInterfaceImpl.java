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

/**
 * A concrete class that implements a JsFunction interface.
 */
public final class MyJsFunctionInterfaceImpl implements MyJsFunctionInterface {

  public int publicField = 10;

  public int callFoo(int a) {
    // to prevent optimizations from inlining function foo.
    return 5 + foo(Math.random() > -1 ? a : -a);
  }

  @Override
  public int foo(int a) {
    return a + 1;
  }
}
