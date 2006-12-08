/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.lang;

import com.google.gwt.core.client.JavaScriptException;

/**
 * This is a magic class the compiler uses to throw and check exceptions.
 */
final class Exceptions {

  static Object caught(Object e) {
    if (e instanceof Throwable) {
      return e;
    }
    return new JavaScriptException(javaScriptExceptionName(e),
        javaScriptExceptionDescription(e));
  }

  /**
   * Easily throw a ClassCastException from native or generated code.
   */
  static void throwClassCastException() throws ClassCastException {
    throw new ClassCastException();
  }

  /**
   * Easily throw a NullPointerException from native or generated code.
   */
  static void throwNullPointerException() throws NullPointerException {
    throw new NullPointerException();
  }

  /**
   * Returns the description of an unexpected JavaScript exception (not a normal
   * Java one).
   */
  private static native String javaScriptExceptionDescription(Object e) /*-{
    return e.message;
  }-*/;

  /**
   * Returns the name of an unexpected JavaScript exception (not a normal Java
   * one).
   */
  private static native String javaScriptExceptionName(Object e) /*-{
    return e.name;
  }-*/;

}
