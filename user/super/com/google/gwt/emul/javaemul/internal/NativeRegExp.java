/*
 * Copyright 2016 Google Inc.
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
package javaemul.internal;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/**
 * Simple class to work with native js regular expressions.
 */
@JsType(isNative = true, name = "RegExp", namespace = JsPackage.GLOBAL)
public class NativeRegExp {
  public int lastIndex;
  public NativeRegExp(String regex) { }
  public NativeRegExp(String regex, String mode) { }
  public native Object exec(String value);
  public native boolean test(String value);
}
