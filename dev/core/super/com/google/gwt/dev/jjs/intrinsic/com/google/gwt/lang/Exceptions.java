/*
 * Copyright 2008 Google Inc.
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
    return new JavaScriptException(e);
  }

  static boolean throwAssertionError() {
    throw new AssertionError();
  }

  /*
   * We use nonstandard naming here so it's easy for the compiler to map to
   * method names based on primitive type name.
   */
  // CHECKSTYLE_OFF
  static boolean throwAssertionError_boolean(boolean message) {
    throw new AssertionError(message);
  }

  static boolean throwAssertionError_char(char message) {
    throw new AssertionError(message);
  }

  static boolean throwAssertionError_double(double message) {
    throw new AssertionError(message);
  }

  static boolean throwAssertionError_float(float message) {
    throw new AssertionError(message);
  }

  static boolean throwAssertionError_int(int message) {
    throw new AssertionError(message);
  }

  static boolean throwAssertionError_long(long message) {
    throw new AssertionError(message);
  }

  static boolean throwAssertionError_Object(Object message) {
    throw new AssertionError(message);
  }
  // CHECKSTYLE_ON
}
