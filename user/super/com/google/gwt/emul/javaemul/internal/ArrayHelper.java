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
package javaemul.internal;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * Provides utilities to perform operations on Arrays.
 */
public class ArrayHelper {

  public static final int ARRAY_PROCESS_BATCH_SIZE = 10000;

  public static <T> T[] clone(T[] array, int fromIndex, int toIndex) {
    Object result = unsafeClone(array, fromIndex, toIndex);
    return ArrayStamper.stampJavaTypeInfo(result, array);
  }

  /**
   * Unlike clone, this method returns a copy of the array that is not type marked. This is only
   * safe for temp arrays as returned array will not do any type checks.
   */
  public static Object[] unsafeClone(Object array, int fromIndex, int toIndex) {
    return asNativeArray(array).slice(fromIndex, toIndex);
  }

  public static <T> T[] createFrom(T[] array, int length) {
    return ArrayStamper.stampJavaTypeInfo(new NativeArray(length), array);
  }

  public static int getLength(Object array) {
    return asNativeArray(array).length;
  }

  public static void setLength(Object array, int length) {
    asNativeArray(array).length = length;
  }

  public static void removeFrom(Object array, int index, int deleteCount) {
    asNativeArray(array).splice(index, deleteCount);
  }

  public static void insertTo(Object array, int index, Object value) {
    asNativeArray(array).splice(index, 0, value);
  }

  public static void insertTo(Object array, int index, Object[] values) {
    copy(values, 0, array, index, values.length, false);
  }

  public static void copy(Object array, int srcOfs, Object dest, int destOfs, int len) {
    copy(array, srcOfs, dest, destOfs, len, true);
  }

  private static void copy(
      Object src, int srcOfs, Object dest, int destOfs, int len, boolean overwrite) {
    /*
     * Array.prototype.splice is not used directly to overcome the limits imposed to the number of
     * function parameters by browsers.
     */

    if (src == dest) {
      // copying to the same array, make a copy first
      src = unsafeClone(src, srcOfs, srcOfs + len);
      srcOfs = 0;
    }
    NativeArray destArray = asNativeArray(dest);
    for (int batchStart = srcOfs, end = srcOfs + len; batchStart < end;) {
      // increment in block
      int batchEnd = Math.min(batchStart + ARRAY_PROCESS_BATCH_SIZE, end);
      len = batchEnd - batchStart;
      Object[] spliceArgs = unsafeClone(src, batchStart, batchEnd);
      asNativeArray(spliceArgs).splice(0, 0, destOfs, overwrite ? len : 0);
      getSpliceFunction().apply(destArray, spliceArgs);
      batchStart = batchEnd;
      destOfs += len;
    }
  }

  @JsType(isNative = true, name = "Function", namespace = JsPackage.GLOBAL)
  private static class NativeFunction {
    public native String apply(Object thisContext, Object[] argsArray);
  }

  @JsProperty(name = "Array.prototype.splice", namespace = "<window>")
  private static native NativeFunction getSpliceFunction();

  public static NativeArray asNativeArray(Object array) {
    return JsUtils.uncheckedCast(array);
  }
}

