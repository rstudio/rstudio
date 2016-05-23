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
package java.lang;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/**
 * See <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/lang/NullPointerException.html">the
 * official Java API doc</a> for details.
 */
public class NullPointerException extends JsException {

  public NullPointerException() {
  }

  public NullPointerException(String message) {
    super(message);
  }

  NullPointerException(Object typeError) {
    super(typeError);
  }

  @Override
  Object createError(String msg) {
    return new NativeTypeError(msg);
  }

  @JsType(isNative = true, name = "TypeError", namespace = JsPackage.GLOBAL)
  private static class NativeTypeError {
    NativeTypeError(String msg) { }
  }
}
