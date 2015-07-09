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
package java.internal;

/**
 * Forwards array operations to GWT's internal array class.
 */
public class ArrayHelper {

  private static final int ARRAY_PROCESS_BATCH_SIZE = 10000;

  public static native <T> T[] clone(T[] array, int fromIndex, int toIndex) /*-{
    return @com.google.gwt.lang.Array::cloneSubrange(*)(array, fromIndex, toIndex);
  }-*/;

  public static native <T> T[] createFrom(T[] array, int length) /*-{
    return @com.google.gwt.lang.Array::createFrom(*)(array, length);
  }-*/;

  public static void arrayCopy(Object src, int srcOfs, Object dest, int destOfs, int len) {
    arraySplice(src, srcOfs, dest, destOfs, len, true);
  }

  public static void arrayInsert(Object src, int srcOfs, Object dest, int destOfs, int len) {
    arraySplice(src, srcOfs, dest, destOfs, len, false);
  }

  /**
   * A replacement for Array.prototype.splice to overcome the limits imposed to the number of
   * function parameters by browsers.
   */
  private static void arraySplice(
      Object src, int srcOfs, Object dest, int destOfs, int len, boolean overwrite) {
    if (src == dest) {
      // copying to the same array, make a copy first
      src = nativeArraySlice(src, srcOfs, srcOfs + len);
      srcOfs = 0;
    }
    for (int batchStart = srcOfs, end = srcOfs + len; batchStart < end;) {
      // increment in block
      int batchEnd = Math.min(batchStart + ARRAY_PROCESS_BATCH_SIZE, end);
      len = batchEnd - batchStart;
      nativeArraySplice(
          dest, destOfs, overwrite ? len : 0, nativeArraySlice(src, batchStart, batchEnd));
      batchStart = batchEnd;
      destOfs += len;
    }
  }

  private static native Object nativeArraySlice(Object arrayToSclice, int start, int end) /*-{
    return arrayToSclice.slice(start, end);
  }-*/;

  private static native Object nativeArraySplice(Object array, int index, int deleteCount,
      Object arrayToAdd) /*-{
    Array.prototype.splice.apply(array, [index, deleteCount].concat(arrayToAdd));
  }-*/;
}

