/*
 * Copyright 2013 Google Inc.
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
package java.util;

import java.util.function.Supplier;

/**
 * See <a
 * href="http://docs.oracle.com/javase/8/docs/api/java/util/Objects.html">the
 * official Java API doc</a> for details.
 */
public final class Objects {
  private Objects() {
  }

  public static <T> int compare(T a, T b, Comparator<? super T> c) {
    return a == b ? 0 : c.compare(a, b);
  }

  public static boolean deepEquals(Object a, Object b) {
    if (a == b) {
      return true;
    }
    if (a == null || b == null) {
      return false;
    }

    // Use object equality check if any of these objects is not an array.
    Class<?> class1 = a.getClass();
    Class<?> class2 = b.getClass();
    if (!class1.isArray() || !class2.isArray()) {
      return a.equals(b);
    }

    // Use object array equality check if these objects are object arrays;
    // if one of these objects is an object array and the other is not, just return false.
    boolean isObjectArray1 = a instanceof Object[];
    boolean isObjectArray2 = b instanceof Object[];
    if (isObjectArray1 || isObjectArray2) {
      return isObjectArray1 && isObjectArray2 && Arrays.deepEquals((Object[]) a, (Object[]) b);
    }

    // At this point a and b are primitive arrays so we just check that they have same types.
    if (!class1.equals(class2)) {
      return false;
    }

    if (a instanceof boolean[]) {
      return Arrays.equals((boolean[]) a, (boolean[]) b);
    }
    if (a instanceof byte[]) {
      return Arrays.equals((byte[]) a, (byte[]) b);
    }
    if (a instanceof char[]) {
      return Arrays.equals((char[]) a, (char[]) b);
    }
    if (a instanceof short[]) {
      return Arrays.equals((short[]) a, (short[]) b);
    }
    if (a instanceof int[]) {
      return Arrays.equals((int[]) a, (int[]) b);
    }
    if (a instanceof long[]) {
      return Arrays.equals((long[]) a, (long[]) b);
    }
    if (a instanceof float[]) {
      return Arrays.equals((float[]) a, (float[]) b);
    }
    // could only be double[]
    return Arrays.equals((double[]) a, (double[]) b);
  }

  public static boolean equals(Object a, Object b) {
    return (a == b) || (a != null && a.equals(b));
  }

  @SuppressWarnings("StringEquality")
  public static boolean equals(String a, String b) {
    return a == b;
  }

  public static int hashCode(Object o) {
    return o != null ? o.hashCode() : 0;
  }

  public static int hash(Object... values) {
    return Arrays.hashCode(values);
  }

  public static boolean isNull(Object obj) {
    return obj == null;
  }

  public static boolean nonNull(Object obj) {
    return obj != null;
  }

  public static <T> T requireNonNull(T obj) {
    if (obj == null) {
      throw new NullPointerException();
    }
    return obj;
  }

  public static <T> T requireNonNull(T obj, String message) {
    if (obj == null) {
      throw new NullPointerException(message);
    }
    return obj;
  }

  public static <T> T requireNonNull(T obj, Supplier<String> messageSupplier) {
    if (obj == null) {
      throw new NullPointerException(messageSupplier.get());
    }
    return obj;
  }

  public static String toString(Object o) {
    return String.valueOf(o);
  }

  public static String toString(Object o, String nullDefault) {
    return o != null ? o.toString() : nullDefault;
  }
}
