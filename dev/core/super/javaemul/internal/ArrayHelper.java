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
  public static native Object[] unsafeClone(Object array, int fromIndex, int toIndex) /*-{
    return array.slice(fromIndex, toIndex);
  }-*/;

  public static <T> T[] createFrom(T[] array, int length) {
    Object result = createNativeArray(length);
    return ArrayStamper.stampJavaTypeInfo(result, array);
  }

  private static native Object createNativeArray(int length)/*-{
    return new Array(length);
  }-*/;

  public static native int getLength(Object array) /*-{
    return array.length;
  }-*/;

  public static native void setLength(Object array, int length)/*-{
    array.length = length;
  }-*/;

  public static native void removeFrom(Object array, int index, int deleteCount) /*-{
    array.splice(index, deleteCount);
  }-*/;

  public static native void insertTo(Object array, int index, Object value) /*-{
    array.splice(index, 0, value);
  }-*/;

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
    for (int batchStart = srcOfs, end = srcOfs + len; batchStart < end;) {
      // increment in block
      int batchEnd = Math.min(batchStart + ARRAY_PROCESS_BATCH_SIZE, end);
      len = batchEnd - batchStart;
      applySplice(dest, destOfs, overwrite ? len : 0, unsafeClone(src, batchStart, batchEnd));
      batchStart = batchEnd;
      destOfs += len;
    }
  }

  private static native void applySplice(Object array, int index, int deleteCount,
      Object arrayToAdd) /*-{
    Array.prototype.splice.apply(array, [index, deleteCount].concat(arrayToAdd));
  }-*/;
}

