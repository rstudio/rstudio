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

package java.util;

import com.google.gwt.core.client.GWT;

/**
 * Utility methods related to native arrays. <a href="http://java.sun.com/j2se/1.5.0/docs/api/java/util/Arrays.html">[Sun
 * docs]</a>
 */
public class Arrays {

  public static <T> List<T> asList(T... array) {
    List<T> accum = new ArrayList<T>();
    for (int i = 0; i < array.length; i++) {
      accum.add(array[i]);
    }
    return accum;
  }

  /**
   * Perform a binary search on a sorted byte array.
   *
   * @param sortedArray byte array to search
   * @param key         value to search for
   * @return the index of an element with a matching value, or a negative number
   *         which is the index of the next larger value (or just past the end
   *         of the array if the searched value is larger than all elements in
   *         the array) minus 1 (to ensure error returns are negative)
   */
  public static int binarySearch(final byte[] sortedArray, final byte key) {
    int low = 0;
    int high = sortedArray.length - 1;

    while (low <= high) {
      final int mid = low + ((high - low) / 2);
      final byte midVal = sortedArray[mid];

      if (midVal < key) {
        low = mid + 1;
      } else if (midVal > key) {
        high = mid - 1;
      } else {
        // key found
        return mid;
      }
    }
    // key not found.
    return -low - 1;
  }

  /**
   * Perform a binary search on a sorted char array.
   *
   * @param a   char array to search
   * @param key value to search for
   * @return the index of an element with a matching value, or a negative number
   *         which is the index of the next larger value (or just past the end
   *         of the array if the searched value is larger than all elements in
   *         the array) minus 1 (to ensure error returns are negative)
   */
  public static int binarySearch(final char[] a, final char key) {
    int low = 0;
    int high = a.length - 1;

    while (low <= high) {
      final int mid = low + ((high - low) / 2);
      final char midVal = a[mid];

      if (midVal < key) {
        low = mid + 1;
      } else if (midVal > key) {
        high = mid - 1;
      } else {
        // key found
        return mid;
      }
    }
    // key not found.
    return -low - 1;
  }

  /**
   * Perform a binary search on a sorted double array.
   *
   * @param sortedArray double array to search
   * @param key         value to search for
   * @return the index of an element with a matching value, or a negative number
   *         which is the index of the next larger value (or just past the end
   *         of the array if the searched value is larger than all elements in
   *         the array) minus 1 (to ensure error returns are negative)
   */
  public static int binarySearch(final double[] sortedArray, final double key) {
    int low = 0;
    int high = sortedArray.length - 1;

    while (low <= high) {
      final int mid = low + ((high - low) / 2);
      final double midVal = sortedArray[mid];

      if (midVal < key) {
        low = mid + 1;
      } else if (midVal > key) {
        high = mid - 1;
      } else {
        // key found
        return mid;
      }
    }
    // key not found.
    return -low - 1;
  }

  /**
   * Perform a binary search on a sorted float array.
   *
   * Note that some underlying JavaScript interpreters do not actually implement
   * floats (using double instead), so you may get slightly different behavior
   * regarding values that are very close (or equal) since conversion errors
   * to/from double may change the values slightly.
   *
   * @param sortedArray float array to search
   * @param key         value to search for
   * @return the index of an element with a matching value, or a negative number
   *         which is the index of the next larger value (or just past the end
   *         of the array if the searched value is larger than all elements in
   *         the array) minus 1 (to ensure error returns are negative)
   */
  public static int binarySearch(final float[] sortedArray, final float key) {
    int low = 0;
    int high = sortedArray.length - 1;

    while (low <= high) {
      final int mid = low + ((high - low) / 2);
      final float midVal = sortedArray[mid];

      if (midVal < key) {
        low = mid + 1;
      } else if (midVal > key) {
        high = mid - 1;
      } else {
        // key found
        return mid;
      }
    }
    // key not found.
    return -low - 1;
  }

  /**
   * Perform a binary search on a sorted int array.
   *
   * @param sortedArray int array to search
   * @param key         value to search for
   * @return the index of an element with a matching value, or a negative number
   *         which is the index of the next larger value (or just past the end
   *         of the array if the searched value is larger than all elements in
   *         the array) minus 1 (to ensure error returns are negative)
   */
  public static int binarySearch(final int[] sortedArray, final int key) {
    int low = 0;
    int high = sortedArray.length - 1;

    while (low <= high) {
      final int mid = low + ((high - low) / 2);
      final int midVal = sortedArray[mid];

      if (midVal < key) {
        low = mid + 1;
      } else if (midVal > key) {
        high = mid - 1;
      } else {
        // key found
        return mid;
      }
    }
    // key not found.
    return -low - 1;
  }

  /**
   * Perform a binary search on a sorted long array.
   *
   * Note that most underlying JavaScript interpreters do not actually implement
   * longs, so the values must be stored in doubles instead. This means that
   * certain legal values cannot be represented, and comparison of two unequal
   * long values may result in unexpected results if they are not also
   * representable as doubles.
   *
   * @param sortedArray long array to search
   * @param key         value to search for
   * @return the index of an element with a matching value, or a negative number
   *         which is the index of the next larger value (or just past the end
   *         of the array if the searched value is larger than all elements in
   *         the array) minus 1 (to ensure error returns are negative)
   */
  public static int binarySearch(final long[] sortedArray, final long key) {
    int low = 0;
    int high = sortedArray.length - 1;

    while (low <= high) {
      final int mid = low + ((high - low) / 2);
      final long midVal = sortedArray[mid];

      if (midVal < key) {
        low = mid + 1;
      } else if (midVal > key) {
        high = mid - 1;
      } else {
        // key found
        return mid;
      }
    }
    // key not found.
    return -low - 1;
  }

  /**
   * Perform a binary search on a sorted object array, using natural ordering.
   *
   * @param sortedArray object array to search
   * @param key         value to search for
   * @return the index of an element with a matching value, or a negative number
   *         which is the index of the next larger value (or just past the end
   *         of the array if the searched value is larger than all elements in
   *         the array) minus 1 (to ensure error returns are negative)
   * @throws ClassCastException if <code>key</code> is not comparable to
   *                            <code>sortedArray</code>'s elements.
   */
  public static int binarySearch(final Object[] sortedArray, final Object key) {
    return binarySearch(sortedArray, key, Comparators.natural());
  }

  /**
   * Perform a binary search on a sorted short array.
   *
   * @param sortedArray short array to search
   * @param key         value to search for
   * @return the index of an element with a matching value, or a negative number
   *         which is the index of the next larger value (or just past the end
   *         of the array if the searched value is larger than all elements in
   *         the array) minus 1 (to ensure error returns are negative)
   */
  public static int binarySearch(final short[] sortedArray, final short key) {
    int low = 0;
    int high = sortedArray.length - 1;

    while (low <= high) {
      final int mid = low + ((high - low) / 2);
      final short midVal = sortedArray[mid];

      if (midVal < key) {
        low = mid + 1;
      } else if (midVal > key) {
        high = mid - 1;
      } else {
        // key found
        return mid;
      }
    }
    // key not found.
    return -low - 1;
  }

  /**
   * Perform a binary search on a sorted object array, using a user-specified
   * comparison function.
   *
   * @param sortedArray object array to search
   * @param key         value to search for
   * @param comparator  comparision function, <code>null</code> indicates
   *                    <i>natural ordering</i> should be used.
   * @return the index of an element with a matching value, or a negative number
   *         which is the index of the next larger value (or just past the end
   *         of the array if the searched value is larger than all elements in
   *         the array) minus 1 (to ensure error returns are negative)
   * @throws ClassCastException if <code>key</code> and <code>sortedArray</code>'s
   *                            elements cannot be compared by <code>comparator</code>.
   */
  public static <T> int binarySearch(final T[] sortedArray, final T key,
      Comparator<? super T> comparator) {
    if (comparator == null) {
      comparator = Comparators.natural();
    }
    int low = 0;
    int high = sortedArray.length - 1;

    while (low <= high) {
      final int mid = low + ((high - low) / 2);
      final T midVal = sortedArray[mid];
      final int compareResult = comparator.compare(midVal, key);

      if (compareResult < 0) {
        low = mid + 1;
      } else if (compareResult > 0) {
        high = mid - 1;
      } else {
        // key found
        return mid;
      }
    }
    // key not found.
    return -low - 1;
  }

  public static boolean deepEquals(Object[] a1, Object[] a2) {
    if (a1 == a2) {
      return true;
    }

    if (a1 == null || a2 == null) {
      return false;
    }

    if (a1.length != a2.length) {
      return false;
    }

    for (int i = 0, n = a1.length; i < n; ++i) {

      Object obj1 = a1[i];
      Object obj2 = a2[i];
      if (obj1 == obj2 || obj1.equals(obj2)) {
        continue;
      }
      String class1 = GWT.getTypeName(obj1);
      String class2 = GWT.getTypeName(obj2);

      // We have to test and see if these are two arrays of the same type,
      // then see what types of arrays they are and dispatch to the
      // appropriate equals

      if (!class1.startsWith("[") || !class1.equals(class2)) {
        return false;
      }

      if (obj1 instanceof Object[]) {
        if (!deepEquals((Object[]) obj1, (Object[]) obj2)) {
          return false;
        }
      } else if (obj1 instanceof boolean[]) {
        if (!equals((boolean[]) obj1, (boolean[]) obj2)) {
          return false;
        }
      } else if (obj1 instanceof byte[]) {
        if (!equals((byte[]) obj1, (byte[]) obj2)) {
          return false;
        }
      } else if (obj1 instanceof char[]) {
        if (!equals((char[]) obj1, (char[]) obj2)) {
          return false;
        }
      } else if (obj1 instanceof short[]) {
        if (!equals((short[]) obj1, (short[]) obj2)) {
          return false;
        }
      } else if (obj1 instanceof int[]) {
        if (!equals((int[]) obj1, (int[]) obj2)) {
          return false;
        }
      } else if (obj1 instanceof long[]) {
        if (!equals((long[]) obj1, (long[]) obj2)) {
          return false;
        }
      } else if (obj1 instanceof float[]) {
        if (!equals((float[]) obj1, (float[]) obj2)) {
          return false;
        }
      } else if (obj1 instanceof double[]) {
        if (!equals((double[]) obj1, (double[]) obj2)) {
          return false;
        }
      }
    }

    return true;
  }

  public static int deepHashCode(Object[] a) {
    if (a == null) {
      return 0;
    }

    int hashCode = 1;

    for (int i = 0, n = a.length; i < n; ++i) {
      Object obj = a[i];
      int hash;

      if (obj instanceof Object[]) {
        hash = deepHashCode((Object[]) a);
      } else if (obj instanceof boolean[]) {
        hash = hashCode((boolean[]) obj);
      } else if (obj instanceof byte[]) {
        hash = hashCode((byte[]) obj);
      } else if (obj instanceof char[]) {
        hash = hashCode((char[]) obj);
      } else if (obj instanceof short[]) {
        hash = hashCode((short[]) obj);
      } else if (obj instanceof int[]) {
        hash = hashCode((int[]) obj);
      } else if (obj instanceof long[]) {
        hash = hashCode((long[]) obj);
      } else if (obj instanceof float[]) {
        hash = hashCode((float[]) obj);
      } else if (obj instanceof double[]) {
        hash = hashCode((double[]) obj);
      } else {
        hash = obj.hashCode();
      }

      // nasty trick related to JS and lack of integer rollover
      hashCode = (31 * hashCode + hash) | 0;
    }

    return hashCode;
  }

  public static String deepToString(Object[] a) {
    return deepToString(a, new HashSet<Object[]>());
  }

  public static boolean equals(Object[] array1, Object[] array2) {
    if (array1 == array2) {
      return true;
    }

    if (array1 == null || array2 == null) {
      return false;
    }

    if (array1.length != array2.length) {
      return false;
    }

    for (int i = 0; i < array1.length; ++i) {
      Object val1 = array1[i];
      Object val2 = array2[i];
      if (!val1.equals(val2)) {
        return false;
      }
    }

    return true;
  }

  public static boolean equals(boolean[] array1, boolean[] array2) {
    if (array1 == array2) {
      return true;
    }

    if (array1 == null || array2 == null) {
      return false;
    }

    if (array1.length != array2.length) {
      return false;
    }

    for (int i = 0; i < array1.length; ++i) {
      if (array1[i] != array2[i]) {
        return false;
      }
    }

    return true;
  }

  public static boolean equals(byte[] array1, byte[] array2) {
    if (array1 == array2) {
      return true;
    }

    if (array1 == null || array2 == null) {
      return false;
    }

    if (array1.length != array2.length) {
      return false;
    }

    for (int i = 0; i < array1.length; ++i) {
      if (array1[i] != array2[i]) {
        return false;
      }
    }

    return true;
  }

  public static boolean equals(char[] array1, char[] array2) {
    if (array1 == array2) {
      return true;
    }

    if (array1 == null || array2 == null) {
      return false;
    }

    if (array1.length != array2.length) {
      return false;
    }

    for (int i = 0; i < array1.length; ++i) {
      if (array1[i] != array2[i]) {
        return false;
      }
    }

    return true;
  }

  public static boolean equals(short[] array1, short[] array2) {
    if (array1 == array2) {
      return true;
    }

    if (array1 == null || array2 == null) {
      return false;
    }

    if (array1.length != array2.length) {
      return false;
    }

    for (int i = 0; i < array1.length; ++i) {
      if (array1[i] != array2[i]) {
        return false;
      }
    }

    return true;
  }

  public static boolean equals(int[] array1, int[] array2) {
    if (array1 == array2) {
      return true;
    }

    if (array1 == null || array2 == null) {
      return false;
    }

    if (array1.length != array2.length) {
      return false;
    }

    for (int i = 0; i < array1.length; ++i) {
      if (array1[i] != array2[i]) {
        return false;
      }
    }

    return true;
  }

  public static boolean equals(long[] array1, long[] array2) {
    if (array1 == array2) {
      return true;
    }

    if (array1 == null || array2 == null) {
      return false;
    }

    if (array1.length != array2.length) {
      return false;
    }

    for (int i = 0; i < array1.length; ++i) {
      if (array1[i] != array2[i]) {
        return false;
      }
    }

    return true;
  }

  public static boolean equals(float[] array1, float[] array2) {
    if (array1 == array2) {
      return true;
    }

    if (array1 == null || array2 == null) {
      return false;
    }

    if (array1.length != array2.length) {
      return false;
    }

    for (int i = 0; i < array1.length; ++i) {
      if (array1[i] != array2[i]) {
        return false;
      }
    }

    return true;
  }

  public static boolean equals(double[] array1, double[] array2) {
    if (array1 == array2) {
      return true;
    }

    if (array1 == null || array2 == null) {
      return false;
    }

    if (array1.length != array2.length) {
      return false;
    }

    for (int i = 0; i < array1.length; ++i) {
      if (array1[i] != array2[i]) {
        return false;
      }
    }

    return true;
  }

  public static void fill(boolean[] a, boolean val) {
    fill(a, 0, a.length, val);
  }

  public static void fill(boolean[] a, int fromIndex, int toIndex,
      boolean val) {
    for (int i = fromIndex; i < toIndex; ++i) {
      a[i] = val;
    }
  }

  public static void fill(byte[] a, byte val) {
    fill(a, 0, a.length, val);
  }

  public static void fill(byte[] a, int fromIndex, int toIndex,
      byte val) {
    for (int i = fromIndex; i < toIndex; ++i) {
      a[i] = val;
    }
  }

  public static void fill(char[] a, char val) {
    fill(a, 0, a.length, val);
  }

  public static void fill(char[] a, int fromIndex, int toIndex,
      char val) {
    for (int i = fromIndex; i < toIndex; ++i) {
      a[i] = val;
    }
  }

  public static void fill(short[] a, short val) {
    fill(a, 0, a.length, val);
  }

  public static void fill(short[] a, int fromIndex, int toIndex,
      short val) {
    for (int i = fromIndex; i < toIndex; ++i) {
      a[i] = val;
    }
  }

  public static void fill(int[] a, int val) {
    fill(a, 0, a.length, val);
  }

  public static void fill(int[] a, int fromIndex, int toIndex,
      int val) {
    for (int i = fromIndex; i < toIndex; ++i) {
      a[i] = val;
    }
  }

  public static void fill(long[] a, long val) {
    fill(a, 0, a.length, val);
  }

  public static void fill(long[] a, int fromIndex, int toIndex,
      long val) {
    for (int i = fromIndex; i < toIndex; ++i) {
      a[i] = val;
    }
  }

  public static void fill(float[] a, float val) {
    fill(a, 0, a.length, val);
  }

  public static void fill(float[] a, int fromIndex, int toIndex,
      float val) {
    for (int i = fromIndex; i < toIndex; ++i) {
      a[i] = val;
    }
  }

  public static void fill(double[] a, double val) {
    fill(a, 0, a.length, val);
  }

  public static void fill(double[] a, int fromIndex, int toIndex,
      double val) {
    for (int i = fromIndex; i < toIndex; ++i) {
      a[i] = val;
    }
  }

  public static void fill(Object[] a, Object val) {
    fill(a, 0, a.length, val);
  }

  public static void fill(Object[] a, int fromIndex, int toIndex,
      Object val) {
    for (int i = fromIndex; i < toIndex; ++i) {
      a[i] = val;
    }
  }

  public static int hashCode(boolean[] a) {
    if (a == null) {
      return 0;
    }
    int hashCode = 1;
    for (int i = 0, n = a.length; i < n; ++i) {
      hashCode = (31 * hashCode + (Boolean.valueOf(a[i]).hashCode())) | 0;
    }

    return hashCode;
  }

  public static int hashCode(byte[] a) {
    if (a == null) {
      return 0;
    }
    int hashCode = 1;
    for (int i = 0, n = a.length; i < n; ++i) {
      hashCode = (31 * hashCode + Byte.hashCode(a[i])) | 0;
    }

    return hashCode;
  }

  public static int hashCode(char[] a) {
    if (a == null) {
      return 0;
    }
    int hashCode = 1;
    for (int i = 0, n = a.length; i < n; ++i) {
      hashCode = (31 * hashCode + Character.hashCode(a[i])) | 0;
    }

    return hashCode;
  }

  public static int hashCode(short[] a) {
    if (a == null) {
      return 0;
    }
    int hashCode = 1;
    for (int i = 0, n = a.length; i < n; ++i) {
      hashCode = (31 * hashCode + Short.hashCode(a[i])) | 0;
    }

    return hashCode;
  }

  public static int hashCode(int[] a) {
    if (a == null) {
      return 0;
    }
    int hashCode = 1;
    for (int i = 0, n = a.length; i < n; ++i) {
      hashCode = (31 * hashCode + Integer.hashCode(a[i])) | 0;
    }

    return hashCode;
  }

  public static int hashCode(long[] a) {
    if (a == null) {
      return 0;
    }
    int hashCode = 1;
    for (int i = 0, n = a.length; i < n; ++i) {
      hashCode = (31 * hashCode + Long.hashCode(a[i])) | 0;
    }

    return hashCode;
  }

  public static int hashCode(float[] a) {
    if (a == null) {
      return 0;
    }
    int hashCode = 1;
    for (int i = 0, n = a.length; i < n; ++i) {
      hashCode = (31 * hashCode + Float.hashCode(a[i])) | 0;
    }

    return hashCode;
  }

  public static int hashCode(double[] a) {
    if (a == null) {
      return 0;
    }
    int hashCode = 1;
    for (int i = 0, n = a.length; i < n; ++i) {
      hashCode = (31 * hashCode + Double.hashCode(a[i])) | 0;
    }

    return hashCode;
  }

  public static int hashCode(Object[] a) {
    if (a == null) {
      return 0;
    }
    int hashCode = 1;
    for (int i = 0, n = a.length; i < n; ++i) {
      hashCode = (31 * hashCode + a[i].hashCode()) | 0;
    }

    return hashCode;
  }

  public static void sort(byte[] a) {
    sort(a, 0, a.length);
  }

  public static void sort(byte[] a, int fromIndex, int toIndex) {
    verifySortIndices(fromIndex, toIndex);
    nativeSort(a, fromIndex, toIndex);
  }

  public static void sort(char[] a) {
    sort(a, 0, a.length);
  }

  public static void sort(char[] a, int fromIndex, int toIndex) {
    verifySortIndices(fromIndex, toIndex);
    nativeSort(a, fromIndex, toIndex);
  }

  public static void sort(short[] a) {
    sort(a, 0, a.length);
  }

  public static void sort(short[] a, int fromIndex, int toIndex) {
    verifySortIndices(fromIndex, toIndex);
    nativeSort(a, fromIndex, toIndex);
  }

  public static void sort(int[] a) {
    sort(a, 0, a.length);
  }

  public static void sort(int[] a, int fromIndex, int toIndex) {
    verifySortIndices(fromIndex, toIndex);
    nativeSort(a, fromIndex, toIndex);
  }

  public static void sort(long[] a) {
    sort(a, 0, a.length);
  }

  public static void sort(long[] a, int fromIndex, int toIndex) {
    verifySortIndices(fromIndex, toIndex);
    nativeSort(a, fromIndex, toIndex);
  }

  public static void sort(float[] a) {
    sort(a, 0, a.length);
  }

  public static void sort(float[] a, int fromIndex, int toIndex) {
    verifySortIndices(fromIndex, toIndex);
    nativeSort(a, fromIndex, toIndex);
  }

  public static void sort(double[] a) {
    sort(a, 0, a.length);
  }

  public static void sort(double[] a, int fromIndex, int toIndex) {
    verifySortIndices(fromIndex, toIndex);
    nativeSort(a, fromIndex, toIndex);
  }

  public static void sort(Object[] x) {
    nativeSort(x, x.length, Comparators.natural());
  }

  public static <T> void sort(T[] x, Comparator<? super T> s) {
    nativeSort(x, x.length, s != null ? s : Comparators.natural());
  }

  public static String toString(boolean[] a) {
    if (a == null) {
      return "null";
    }

    StringBuffer b = new StringBuffer("[");
    for (int i = 0; i < a.length; i++) {
      if (i != 0) {
        b.append(", ");
      }
      b.append(String.valueOf(a[i]));
    }
    b.append("]");
    return b.toString();
  }

  public static String toString(byte[] a) {
    if (a == null) {
      return "null";
    }

    StringBuffer b = new StringBuffer("[");
    for (int i = 0; i < a.length; i++) {
      if (i != 0) {
        b.append(", ");
      }
      b.append(String.valueOf(a[i]));
    }
    b.append("]");
    return b.toString();
  }

  public static String toString(char[] a) {
    if (a == null) {
      return "null";
    }

    StringBuffer b = new StringBuffer("[");
    for (int i = 0; i < a.length; i++) {
      if (i != 0) {
        b.append(", ");
      }
      b.append(String.valueOf(a[i]));
    }
    b.append("]");
    return b.toString();
  }

  public static String toString(short[] a) {
    if (a == null) {
      return "null";
    }

    StringBuffer b = new StringBuffer("[");
    for (int i = 0; i < a.length; i++) {
      if (i != 0) {
        b.append(", ");
      }
      b.append(String.valueOf(a[i]));
    }
    b.append("]");
    return b.toString();
  }

  public static String toString(int[] a) {
    if (a == null) {
      return "null";
    }

    StringBuffer b = new StringBuffer("[");
    for (int i = 0; i < a.length; i++) {
      if (i != 0) {
        b.append(", ");
      }
      b.append(String.valueOf(a[i]));
    }
    b.append("]");
    return b.toString();
  }

  public static String toString(long[] a) {
    if (a == null) {
      return "null";
    }

    StringBuffer b = new StringBuffer("[");
    for (int i = 0; i < a.length; i++) {
      if (i != 0) {
        b.append(", ");
      }
      b.append(String.valueOf(a[i]));
    }
    b.append("]");
    return b.toString();
  }

  public static String toString(float[] a) {
    if (a == null) {
      return "null";
    }

    StringBuffer b = new StringBuffer("[");
    for (int i = 0; i < a.length; i++) {
      if (i != 0) {
        b.append(", ");
      }
      b.append(String.valueOf(a[i]));
    }
    b.append("]");
    return b.toString();
  }

  public static String toString(double[] a) {
    if (a == null) {
      return "null";
    }

    StringBuffer b = new StringBuffer("[");
    for (int i = 0; i < a.length; i++) {
      if (i != 0) {
        b.append(", ");
      }
      b.append(String.valueOf(a[i]));
    }
    b.append("]");
    return b.toString();
  }

  public static String toString(Object[] x) {
    if (x == null) {
      return "null";
    }

    return Arrays.asList(x).toString();
  }

  static void unsafeSort(Object[] x, Comparator<?> s) {
    nativeSort(x, x.length, s != null ? s : Comparators.natural());
  }

  private static String deepToString(Object[] a, Set<Object[]> arraysIveSeen) {
    if (a == null) {
      return "null";
    }

    if (arraysIveSeen.contains(a)) {
      return "[...]";
    }

    arraysIveSeen.add(a);

    StringBuffer b = new StringBuffer("[");
    for (int i = 0; i < a.length; i++) {
      if (i != 0) {
        b.append(", ");
      }
      Object obj = a[i];

      if (GWT.getTypeName(obj).startsWith("[")) {

        if (obj instanceof Object[]) {
          if (arraysIveSeen.contains(obj)) {
            b.append("[...]");
          } else {
            Object[] objArray = (Object[])obj;
            HashSet<Object[]> tempSet = new HashSet<Object[]>(arraysIveSeen);
            b.append(deepToString(objArray, tempSet));
          }
        } else if (obj instanceof boolean[]) {
          b.append(toString((byte[]) obj));
        } else if (obj instanceof byte[]) {
          b.append(toString((byte[]) obj));
        } else if (obj instanceof char[]) {
          b.append(toString((char[]) obj));
        } else if (obj instanceof short[]) {
          b.append(toString((short[]) obj));
        } else if (obj instanceof int[]) {
          b.append(toString((int[]) obj));
        } else if (obj instanceof long[]) {
          b.append(toString((long[]) obj));
        } else if (obj instanceof float[]) {
          b.append(toString((float[]) obj));
        } else if (obj instanceof double[]) {
          b.append(toString((double[]) obj));
        }

        assert false : "Unexpected array type: " + GWT.getTypeName(obj);
      } else {
        b.append(String.valueOf(obj));
      }
    }
    b.append("]");
    return b.toString();
  }

  private static native void nativeSort(byte[] array, int fromIndex,
      int toIndex) /*-{
    var v = new Array();
    for(var i = fromIndex; i < toIndex; ++i){
      v[i - fromIndex] = array[i];
    }

    v.sort();

    for(var i = fromIndex; i < toIndex; ++i){
      array[i] = v[i - fromIndex];
    }
  }-*/;

  private static native void nativeSort(char[] array, int fromIndex,
      int toIndex) /*-{
    var v = new Array();
    for(var i = fromIndex; i < toIndex; ++i){
      v[i - fromIndex] = array[i];
    }

    v.sort();

    for(var i = fromIndex; i < toIndex; ++i){
      array[i] = v[i - fromIndex];
    }
  }-*/;

  private static native void nativeSort(short[] array, int fromIndex,
      int toIndex) /*-{
    var v = new Array();
    for(var i = fromIndex; i < toIndex; ++i){
      v[i - fromIndex] = array[i];
    }

    v.sort();

    for(var i = fromIndex; i < toIndex; ++i){
      array[i] = v[i - fromIndex];
    }
  }-*/;

  private static native void nativeSort(int[] array, int fromIndex, int toIndex) /*-{
    var v = new Array();
    for(var i = fromIndex; i < toIndex; ++i){
      v[i - fromIndex] = array[i];
    }

    v.sort();

    for(var i = fromIndex; i < toIndex; ++i){
      array[i] = v[i - fromIndex];
    }
  }-*/;

  private static native void nativeSort(long[] array, int fromIndex,
      int toIndex) /*-{
    var v = new Array();
    for(var i = fromIndex; i < toIndex; ++i){
      v[i - fromIndex] = array[i];
    }

    v.sort();

    for(var i = fromIndex; i < toIndex; ++i){
      array[i] = v[i - fromIndex];
    }
  }-*/;

  private static native void nativeSort(float[] array, int fromIndex,
      int toIndex) /*-{
    var v = new Array();
    for(var i = fromIndex; i < toIndex; ++i){
      v[i - fromIndex] = array[i];
    }

    v.sort();

    for(var i = fromIndex; i < toIndex; ++i){
      array[i] = v[i - fromIndex];
    }
  }-*/;

  private static native void nativeSort(double[] array, int fromIndex,
      int toIndex) /*-{
    var v = new Array();
    for(var i = fromIndex; i < toIndex; ++i){
      v[i - fromIndex] = array[i];
    }

    v.sort();

    for(var i = fromIndex; i < toIndex; ++i){
      array[i] = v[i - fromIndex];
    }
  }-*/;

  private static native void nativeSort(Object[] array, int size,
      Comparator<?> compare) /*-{ 
    if (size == 0) {
      return;
    }
   
    var v = new Array();
    for(var i = 0; i < size; ++i){
      v[i] = array[i];
    }
  
    if(compare != null) {
      var f = function(a,b) {
        var c = compare.@java.util.Comparator::compare(Ljava/lang/Object;Ljava/lang/Object;)(a,b);
        return c;
      }
      v.sort(f);
    } else {
      v.sort();
    }

    for(var i = 0; i < size; ++i){
      array[i] = v[i];
    }
  }-*/;

  private static void verifySortIndices(int fromIndex, int toIndex) {
    if (fromIndex > toIndex) {
      throw new IllegalArgumentException(
          "fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")");
    }
  }
}
