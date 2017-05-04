/*
 * Copyright 2017 Google Inc.
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

import javaemul.internal.annotations.DoNotAutobox;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsType;

/**
 * Simple class to work with native array API.
 */
@JsType(isNative = true, name = "Array", namespace = "<window>")
public class NativeArray {
  /**
   * Compare function for sort.
   */
  @JsFunction
  public interface CompareFunction {
    double compare(Object d1, Object d2);
  }

  public int length;
  public NativeArray() { }
  public NativeArray(int length) { }
  public native Object concat(Object arrayToAdd);
  public native Object[] slice(int fromIndex, int toIndex);
  public native void splice(int index, int deleteCount, @DoNotAutobox Object... value);
  public native <T> void sort(CompareFunction compareFunction);
}
