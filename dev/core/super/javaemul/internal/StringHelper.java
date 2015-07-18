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
 * StringHelper contains utility methods for String processing that are used by multiple classes
 * in GWT's emul.
 */
public class StringHelper {
  /**
   * Checks that bounds are correct.
   *
   * @param legalCount the end of the legal range
   * @param start must be >= 0
   * @param end must be <= legalCount and must be >= start
   * @throw StringIndexOutOfBoundsException if the range is not legal
   * @skip
   */
  public static void checkBounds(int legalCount, int start, int end) {
    if (start < 0) {
      throw new StringIndexOutOfBoundsException(start);
    }
    if (end < start) {
      throw new StringIndexOutOfBoundsException(end - start);
    }
    if (end > legalCount) {
      throw new StringIndexOutOfBoundsException(end);
    }
  }

  public static String valueOf(char x[], int start, int end) {
    // Work around function.prototype.apply call stack size limits:
    // https://code.google.com/p/v8/issues/detail?id=2896
    // Performance: http://jsperf.com/string-fromcharcode-test/13
    int batchSize = ArrayHelper.ARRAY_PROCESS_BATCH_SIZE;
    String s = "";
    for (int batchStart = start; batchStart < end;) {
      int batchEnd = Math.min(batchStart + batchSize, end);
      Object slicedArray = ArrayHelper.nativeArraySlice(x, batchStart, batchEnd);
      s += fromCharCode(slicedArray);
      batchStart = batchEnd;
    }
    return s;
  }

  private static native String fromCharCode(Object array) /*-{
    return String.fromCharCode.apply(null, array);
  }-*/;
}

