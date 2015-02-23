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
package com.google.gwt.core.client.interop;

import com.google.gwt.core.client.js.JsExport;
import com.google.gwt.core.client.js.JsType;

/**
 * A class which has two public constructors and is annotated by "JsExport". These two constructors
 * have to be annotated by "JsExport" with explicit symbol or by "JsNoExport".
 */
@JsExport
@JsType
public class MyExportedClassWithMultipleConstructors {
  public int a;
  public int b;

  @JsExport("MyClassConstructor1")
  public MyExportedClassWithMultipleConstructors() {
    a = 1;
    b = 2;
  }

  @JsExport("MyClassConstructor2")
  public MyExportedClassWithMultipleConstructors(int a, int b) {
    this.a = a;
    this.b = b;
  }

  public int sum() {
    return a + b;
  }
}
