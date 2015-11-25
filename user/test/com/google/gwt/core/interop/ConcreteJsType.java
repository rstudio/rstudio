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
package com.google.gwt.core.interop;

import jsinterop.annotations.JsType;

/**
 * This concrete test class is *directly* annotated as a @JsType.
 */
@JsType
class ConcreteJsType {
  public int publicMethod() {
    return 10;
  }

  public int publicMethodAlsoExposedAsNonJsMethod() {
    return 100;
  }

  public static void publicStaticMethod() {
  }

  private void privateMethod() {
  }

  protected void protectedMethod() {
  }

  void packageMethod() {
  }

  public int publicField = 10;

  public static int publicStaticField = 10;

  private int privateField = 10;

  protected int protectedField = 10;

  int packageField = 10;

  interface A {
    int x();
  }

  public static class AImpl1 implements A {
    @Override
    public int x() { return 42; }
  }

  public static class AImpl2 implements A {
    @Override
    public int x() { return 101; }
  }

  @SuppressWarnings("unusable-by-js")
  public A notTypeTightenedField = new AImpl1();
}
