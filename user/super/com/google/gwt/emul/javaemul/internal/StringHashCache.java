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

import static javaemul.internal.Coercions.ensureInt;

/**
 * Hashcode caching for strings.
 */
class StringHashCache {
  /**
   * The "old" cache; it will be dumped when front is full.
   */
  private static Object back = createMap();
  /**
   * Tracks the number of entries in front.
   */
  private static int count = 0;
  /**
   * The "new" cache; it will become back when it becomes full.
   */
  private static Object front = createMap();
  /**
   * Pulled this number out of thin air.
   */
  private static final int MAX_CACHE = 256;

  public static int getHashCode(String str) {
    // Accesses must to be prefixed with ':' to prevent conflict with built-in
    // JavaScript properties.
    String key = ":" + str;

    // Check the front store.
    Object result = get(front, key);
    if (!isUndefined(result)) {
      return cast(result);
    }
    // Check the back store.
    result = get(back, key);
    int hashCode = isUndefined(result) ? compute(str) : cast(result);
    // Increment can trigger the swap/flush; call after checking back but
    // before writing to front.
    increment();
    set(front, key, hashCode);

    return hashCode;
  }

  private static int compute(String str) {
    int hashCode = 0;
    int n = str.length();
    int nBatch = n - 4;
    int i = 0;

    // Process batches of 4 characters at a time and add them to the hash coercing to 32 bits
    while (i < nBatch) {
      hashCode = str.charAt(i + 3)
          + 31 * (str.charAt(i + 2)
          + 31 * (str.charAt(i + 1)
          + 31 * (str.charAt(i)
          + 31 * hashCode)));

      hashCode = ensureInt(hashCode); // make sure we don't overflow
      i += 4;
    }

    // Now process the leftovers
    while (i < n) {
      hashCode = hashCode * 31 + str.charAt(i++);
    }
    hashCode = ensureInt(hashCode); // make sure we don't overflow

    return hashCode;
  }

  private static void increment() {
    if (count == MAX_CACHE) {
      back = front;
      front = createMap();
      count = 0;
    }
    ++count;
  }

  private static native Object get(Object map, String key) /*-{
    return map[key];
  }-*/;

  private static native Object set(Object map, String key, int value) /*-{
    map[key] = value;
  }-*/;

  // Note: we are explicitly checking for undefined since '0 == null' equals true in JavaScript
  private static native boolean isUndefined(Object o) /*-{
    return o === undefined;
  }-*/;

  private static native int cast(Object o) /*-{
    return o;
  }-*/;

  private static native Object createMap() /*-{
    return {};
  }-*/;
}

