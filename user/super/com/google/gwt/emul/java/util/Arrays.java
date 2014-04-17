/*
 * Copyright 2008 Google Inc.
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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.UnsafeNativeLong;
import com.google.gwt.lang.Array;

import java.io.Serializable;

/**
 * Utility methods related to native arrays. <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/util/Arrays.html">[Sun
 * docs]</a>
 */
public class Arrays {

  private static final class ArrayList<E> extends AbstractList<E> implements
      RandomAccess, Serializable {

    /**
     * The only reason this is non-final is so that E[] (and E) will be exposed
     * for serialization.
     */
    private E[] array;

    ArrayList(E[] array) {
      assert (array != null);
      this.array = array;
    }

    @Override
    public boolean contains(Object o) {
      return (indexOf(o) != -1);
    }

    @Override
    public E get(int index) {
      checkIndex(index, size());
      return array[index];
    }

    @Override
    public E set(int index, E value) {
      checkIndex(index, size());
      E was = array[index];
      array[index] = value;
      return was;
    }

    @Override
    public int size() {
      return array.length;
    }

    /*
     * Semantics are to return an array of identical type.
     */
    @Override
    public Object[] toArray() {
      return Array.clone(array);
    }

    /*
     * Faster than the iterator-based implementation in AbstractCollection.
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T[] toArray(T[] out) {
      int size = size();
      if (out.length < size) {
        out = Array.createFrom(out, size);
      }
      for (int i = 0; i < size; ++i) {
        out[i] = (T) array[i];
      }
      if (out.length > size) {
        out[size] = null;
      }
      return out;
    }
  }

  public static <T> List<T> asList(T... array) {
    return new ArrayList<T>(array);
  }

  /**
   * Perform a binary search on a sorted byte array.
   *
   * @param sortedArray byte array to search
   * @param key value to search for
   * @return the index of an element with a matching value, or a negative number
   *         which is the index of the next larger value (or just past the end
   *         of the array if the searched value is larger than all elements in
   *         the array) minus 1 (to ensure error returns are negative)
   */
  public static int binarySearch(final byte[] sortedArray, final byte key) {
    int low = 0;
    int high = sortedArray.length - 1;

    while (low <= high) {
      final int mid = low + ((high - low) >> 1);
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
   * @param a char array to search
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
      final int mid = low + ((high - low) >> 1);
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
   * @param key value to search for
   * @return the index of an element with a matching value, or a negative number
   *         which is the index of the next larger value (or just past the end
   *         of the array if the searched value is larger than all elements in
   *         the array) minus 1 (to ensure error returns are negative)
   */
  public static int binarySearch(final double[] sortedArray, final double key) {
    int low = 0;
    int high = sortedArray.length - 1;

    while (low <= high) {
      final int mid = low + ((high - low) >> 1);
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
   * @param key value to search for
   * @return the index of an element with a matching value, or a negative number
   *         which is the index of the next larger value (or just past the end
   *         of the array if the searched value is larger than all elements in
   *         the array) minus 1 (to ensure error returns are negative)
   */
  public static int binarySearch(final float[] sortedArray, final float key) {
    int low = 0;
    int high = sortedArray.length - 1;

    while (low <= high) {
      final int mid = low + ((high - low) >> 1);
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
   * @param key value to search for
   * @return the index of an element with a matching value, or a negative number
   *         which is the index of the next larger value (or just past the end
   *         of the array if the searched value is larger than all elements in
   *         the array) minus 1 (to ensure error returns are negative)
   */
  public static int binarySearch(final int[] sortedArray, final int key) {
    int low = 0;
    int high = sortedArray.length - 1;

    while (low <= high) {
      final int mid = low + ((high - low) >> 1);
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
   * @param key value to search for
   * @return the index of an element with a matching value, or a negative number
   *         which is the index of the next larger value (or just past the end
   *         of the array if the searched value is larger than all elements in
   *         the array) minus 1 (to ensure error returns are negative)
   */
  public static int binarySearch(final long[] sortedArray, final long key) {
    int low = 0;
    int high = sortedArray.length - 1;

    while (low <= high) {
      final int mid = low + ((high - low) >> 1);
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
   * @param key value to search for
   * @return the index of an element with a matching value, or a negative number
   *         which is the index of the next larger value (or just past the end
   *         of the array if the searched value is larger than all elements in
   *         the array) minus 1 (to ensure error returns are negative)
   * @throws ClassCastException if <code>key</code> is not comparable to
   *           <code>sortedArray</code>'s elements.
   */
  public static int binarySearch(final Object[] sortedArray, final Object key) {
    return binarySearch(sortedArray, key, Comparators.natural());
  }

  /**
   * Perform a binary search on a sorted short array.
   *
   * @param sortedArray short array to search
   * @param key value to search for
   * @return the index of an element with a matching value, or a negative number
   *         which is the index of the next larger value (or just past the end
   *         of the array if the searched value is larger than all elements in
   *         the array) minus 1 (to ensure error returns are negative)
   */
  public static int binarySearch(final short[] sortedArray, final short key) {
    int low = 0;
    int high = sortedArray.length - 1;

    while (low <= high) {
      final int mid = low + ((high - low) >> 1);
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
   * @param key value to search for
   * @param comparator comparision function, <code>null</code> indicates
   *          <i>natural ordering</i> should be used.
   * @return the index of an element with a matching value, or a negative number
   *         which is the index of the next larger value (or just past the end
   *         of the array if the searched value is larger than all elements in
   *         the array) minus 1 (to ensure error returns are negative)
   * @throws ClassCastException if <code>key</code> and
   *           <code>sortedArray</code>'s elements cannot be compared by
   *           <code>comparator</code>.
   */
  public static <T> int binarySearch(final T[] sortedArray, final T key,
      Comparator<? super T> comparator) {
    if (comparator == null) {
      comparator = Comparators.natural();
    }
    int low = 0;
    int high = sortedArray.length - 1;

    while (low <= high) {
      final int mid = low + ((high - low) >> 1);
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

  public static boolean[] copyOf(boolean[] original, int newLength) {
    checkArrayLength(newLength);
    return copyOfRange(original, 0, newLength);
  }

  public static byte[] copyOf(byte[] original, int newLength) {
    checkArrayLength(newLength);
    return copyOfRange(original, 0, newLength);
  }

  public static char[] copyOf(char[] original, int newLength) {
    checkArrayLength(newLength);
    return copyOfRange(original, 0, newLength);
  }

  public static double[] copyOf(double[] original, int newLength) {
    checkArrayLength(newLength);
    return copyOfRange(original, 0, newLength);
  }

  public static float[] copyOf(float[] original, int newLength) {
    checkArrayLength(newLength);
    return copyOfRange(original, 0, newLength);
  }

  public static int[] copyOf(int[] original, int newLength) {
    checkArrayLength(newLength);
    return copyOfRange(original, 0, newLength);
  }

  public static long[] copyOf(long[] original, int newLength) {
    checkArrayLength(newLength);
    return copyOfRange(original, 0, newLength);
  }

  public static short[] copyOf(short[] original, int newLength) {
    checkArrayLength(newLength);
    return copyOfRange(original, 0, newLength);
  }

  public static <T> T[] copyOf(T[] original, int newLength) {
    checkArrayLength(newLength);
    return copyOfRange(original, 0, newLength);
  }

  public static boolean[] copyOfRange(boolean[] original, int from, int to) {
    int newLength = getLengthFromRange(from, to);
    boolean[] copy = new boolean[newLength];
    System.arraycopy(original, from, copy, 0,
        Math.min(original.length - from, newLength));
    return copy;
  }

  public static byte[] copyOfRange(byte[] original, int from, int to) {
    int newLength = getLengthFromRange(from, to);
    byte[] copy = new byte[newLength];
    System.arraycopy(original, from, copy, 0,
        Math.min(original.length - from, newLength));
    return copy;
  }

  public static char[] copyOfRange(char[] original, int from, int to) {
    int newLength = getLengthFromRange(from, to);
    char[] copy = new char[newLength];
    System.arraycopy(original, from, copy, 0,
        Math.min(original.length - from, newLength));
    return copy;
  }

  public static double[] copyOfRange(double[] original, int from, int to) {
    int newLength = getLengthFromRange(from, to);
    double[] copy = new double[newLength];
    System.arraycopy(original, from, copy, 0,
        Math.min(original.length - from, newLength));
    return copy;
  }

  public static float[] copyOfRange(float[] original, int from, int to) {
    int newLength = getLengthFromRange(from, to);
    float[] copy = new float[newLength];
    System.arraycopy(original, from, copy, 0,
        Math.min(original.length - from, newLength));
    return copy;
  }

  public static int[] copyOfRange(int[] original, int from, int to) {
    int newLength = getLengthFromRange(from, to);
    int[] copy = new int[newLength];
    System.arraycopy(original, from, copy, 0,
        Math.min(original.length - from, newLength));
    return copy;
  }

  public static long[] copyOfRange(long[] original, int from, int to) {
    int newLength = getLengthFromRange(from, to);
    long[] copy = new long[newLength];
    System.arraycopy(original, from, copy, 0,
        Math.min(original.length - from, newLength));
    return copy;
  }

  public static short[] copyOfRange(short[] original, int from, int to) {
    int newLength = getLengthFromRange(from, to);
    short[] copy = new short[newLength];
    System.arraycopy(original, from, copy, 0,
        Math.min(original.length - from, newLength));
    return copy;
  }

  public static <T> T[] copyOfRange(T[] original, int from, int to) {
    int newLength = getLengthFromRange(from, to);
    T[] copy = Array.createFrom(original, newLength);
    System.arraycopy(original, from, copy, 0,
        Math.min(original.length - from, newLength));
    return copy;
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
      if (!Objects.deepEquals(a1[i], a2[i])) {
        return false;
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
        hash = deepHashCode((Object[]) obj);
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
      } else if (obj != null) {
        hash = obj.hashCode();
      } else {
        hash = 0;
      }

      // nasty trick related to JS and lack of integer rollover
      hashCode = (31 * hashCode + hash) | 0;
    }

    return hashCode;
  }

  public static String deepToString(Object[] a) {
    return deepToString(a, new HashSet<Object[]>());
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
      if (!Objects.equals(val1, val2)) {
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

  public static void fill(boolean[] a, boolean val) {
    fill(a, 0, a.length, val);
  }

  public static void fill(boolean[] a, int fromIndex, int toIndex, boolean val) {
    for (int i = fromIndex; i < toIndex; ++i) {
      a[i] = val;
    }
  }

  public static void fill(byte[] a, byte val) {
    fill(a, 0, a.length, val);
  }

  public static void fill(byte[] a, int fromIndex, int toIndex, byte val) {
    for (int i = fromIndex; i < toIndex; ++i) {
      a[i] = val;
    }
  }

  public static void fill(char[] a, char val) {
    fill(a, 0, a.length, val);
  }

  public static void fill(char[] a, int fromIndex, int toIndex, char val) {
    for (int i = fromIndex; i < toIndex; ++i) {
      a[i] = val;
    }
  }

  public static void fill(double[] a, double val) {
    fill(a, 0, a.length, val);
  }

  public static void fill(double[] a, int fromIndex, int toIndex, double val) {
    for (int i = fromIndex; i < toIndex; ++i) {
      a[i] = val;
    }
  }

  public static void fill(float[] a, float val) {
    fill(a, 0, a.length, val);
  }

  public static void fill(float[] a, int fromIndex, int toIndex, float val) {
    for (int i = fromIndex; i < toIndex; ++i) {
      a[i] = val;
    }
  }

  public static void fill(int[] a, int val) {
    fill(a, 0, a.length, val);
  }

  public static void fill(int[] a, int fromIndex, int toIndex, int val) {
    for (int i = fromIndex; i < toIndex; ++i) {
      a[i] = val;
    }
  }

  public static void fill(long[] a, int fromIndex, int toIndex, long val) {
    for (int i = fromIndex; i < toIndex; ++i) {
      a[i] = val;
    }
  }

  public static void fill(long[] a, long val) {
    fill(a, 0, a.length, val);
  }

  public static void fill(Object[] a, int fromIndex, int toIndex, Object val) {
    for (int i = fromIndex; i < toIndex; ++i) {
      a[i] = val;
    }
  }

  public static void fill(Object[] a, Object val) {
    fill(a, 0, a.length, val);
  }

  public static void fill(short[] a, int fromIndex, int toIndex, short val) {
    for (int i = fromIndex; i < toIndex; ++i) {
      a[i] = val;
    }
  }

  public static void fill(short[] a, short val) {
    fill(a, 0, a.length, val);
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

  public static int hashCode(Object[] a) {
    if (a == null) {
      return 0;
    }
    int hashCode = 1;
    for (Object e : a) {
      hashCode = (31 * hashCode + (e == null ? 0 : e.hashCode())) | 0;
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

  public static void sort(byte[] array) {
    nativeNumberSort(array);
  }

  public static void sort(byte[] array, int fromIndex, int toIndex) {
    verifySortIndices(fromIndex, toIndex, array.length);
    nativeNumberSort(array, fromIndex, toIndex);
  }

  public static void sort(char[] array) {
    nativeNumberSort(array);
  }

  public static void sort(char[] array, int fromIndex, int toIndex) {
    verifySortIndices(fromIndex, toIndex, array.length);
    nativeNumberSort(array, fromIndex, toIndex);
  }

  public static void sort(double[] array) {
    nativeNumberSort(array);
  }

  public static void sort(double[] array, int fromIndex, int toIndex) {
    verifySortIndices(fromIndex, toIndex, array.length);
    nativeNumberSort(array, fromIndex, toIndex);
  }

  public static void sort(float[] array) {
    nativeNumberSort(array);
  }

  public static void sort(float[] array, int fromIndex, int toIndex) {
    verifySortIndices(fromIndex, toIndex, array.length);
    nativeNumberSort(array, fromIndex, toIndex);
  }

  public static void sort(int[] array) {
    nativeNumberSort(array);
  }

  public static void sort(int[] array, int fromIndex, int toIndex) {
    verifySortIndices(fromIndex, toIndex, array.length);
    nativeNumberSort(array, fromIndex, toIndex);
  }

  public static void sort(long[] array) {
    nativeLongSort(array);
  }

  public static void sort(long[] array, int fromIndex, int toIndex) {
    verifySortIndices(fromIndex, toIndex, array.length);
    nativeLongSort(array, fromIndex, toIndex);
  }

  public static void sort(Object[] array) {
    // Can't use native JS sort because it isn't stable.

    // -- Commented out implementation that uses the native sort with a fixup.
    // nativeObjSort(array, 0, array.length, getNativeComparator(array,
    //     Comparators.natural()));
    mergeSort(array, 0, array.length, Comparators.natural());
  }

  public static void sort(Object[] x, int fromIndex, int toIndex) {
    // Can't use native JS sort because it isn't stable.

    // -- Commented out implementation that uses the native sort with a fixup.
    // nativeObjSort(x, fromIndex, toIndex, getNativeComparator(x,
    //     Comparators.natural()));
    mergeSort(x, fromIndex, toIndex, Comparators.natural());
  }

  public static void sort(short[] array) {
    nativeNumberSort(array);
  }

  public static void sort(short[] array, int fromIndex, int toIndex) {
    verifySortIndices(fromIndex, toIndex, array.length);
    nativeNumberSort(array, fromIndex, toIndex);
  }

  public static <T> void sort(T[] x, Comparator<? super T> c) {
    // Commented out implementation that uses the native sort with a fixup.

    // nativeObjSort(x, 0, x.length, getNativeComparator(x, c != null ? c :
    // Comparators.natural()));
    mergeSort(x, 0, x.length, c != null ? c : Comparators.natural());
  }

  public static <T> void sort(T[] x, int fromIndex, int toIndex,
      Comparator<? super T> c) {
    // Commented out implementation that uses the native sort with a fixup.

    verifySortIndices(fromIndex, toIndex, x.length);
    // nativeObjSort(x, fromIndex, toIndex, getNativeComparator(x, c != null ? c
    // : Comparators.natural()));
    mergeSort(x, fromIndex, toIndex, c != null ? c : Comparators.natural());
  }

  public static String toString(boolean[] a) {
    if (a == null) {
      return "null";
    }

    StringBuilder b = new StringBuilder("[");
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

    StringBuilder b = new StringBuilder("[");
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

    StringBuilder b = new StringBuilder("[");
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

    StringBuilder b = new StringBuilder("[");
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

    StringBuilder b = new StringBuilder("[");
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

    StringBuilder b = new StringBuilder("[");
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

    StringBuilder b = new StringBuilder("[");
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

  public static String toString(short[] a) {
    if (a == null) {
      return "null";
    }

    StringBuilder b = new StringBuilder("[");
    for (int i = 0; i < a.length; i++) {
      if (i != 0) {
        b.append(", ");
      }
      b.append(String.valueOf(a[i]));
    }
    b.append("]");
    return b.toString();
  }

  private static void checkArrayLength(int length) {
    if (length < 0) {
      throw new NegativeArraySizeException();
    }
  }

  /**
   * Recursive helper function for {@link Arrays#deepToString(Object[])}.
   */
  private static String deepToString(Object[] a, Set<Object[]> arraysIveSeen) {
    if (a == null) {
      return "null";
    }

    if (arraysIveSeen.contains(a)) {
      return "[...]";
    }

    arraysIveSeen.add(a);

    StringBuilder b = new StringBuilder("[");
    for (int i = 0; i < a.length; i++) {
      if (i != 0) {
        b.append(", ");
      }
      Object obj = a[i];
      if (obj == null) {
        b.append("null");
      } else if (obj.getClass().isArray()) {
        if (obj instanceof Object[]) {
          if (arraysIveSeen.contains(obj)) {
            b.append("[...]");
          } else {
            Object[] objArray = (Object[]) obj;
            HashSet<Object[]> tempSet = new HashSet<Object[]>(arraysIveSeen);
            b.append(deepToString(objArray, tempSet));
          }
        } else if (obj instanceof boolean[]) {
          b.append(toString((boolean[]) obj));
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

        assert false : "Unexpected array type: " + obj.getClass().getName();
      } else {
        b.append(String.valueOf(obj));
      }
    }
    b.append("]");
    return b.toString();
  }

  private static int getLengthFromRange(int from, int to) {
    int length = to - from;
    if (length < 0) {
      throw new IllegalArgumentException(from + " > " + to);
    }
    return length;
  }

  /**
   * Return a JavaScript function object which will compare elements of the
   * specified object array.
   *
   * Note that this function isn't currently used but is kept because the native
   * sort/fixup approach is faster everywhere but IE. In the future, we may
   * choose to use deferred binding in the JRE to make those platforms faster.
   *
   * @param array the array of objects to compare
   * @param comp the Comparator to use to compare individual objects.
   * @return a JavaScript function object taking indices into the array to
   *         compare. Returns the result of the comparator, or the comparison of
   *         the indices if the comparator indicates equality so the sort is
   *         stable. The comparator has a property <code>swap</code> which is
   *         true if any elements were discovered to be out of order.
   */
  @SuppressWarnings("unused")
  // see above
  private static native JavaScriptObject getNativeComparator(Object array,
      Comparator<?> comp) /*-{
    function compare(a, b) {
      var elementCompare = comp.@java.util.Comparator::compare(Ljava/lang/Object;Ljava/lang/Object;)(array[a], array[b]);
      var indexCompare = a - b;
      // If elements compare equal, use the index comparison.
      elementCompare = elementCompare || indexCompare;
      // Keep track of having seen out-of-order elements.  Note that we don't
      // have to worry about the sort algorithm comparing an element to itself
      // since it can't be swapped anyway, so we can just check for less-than.
      compare.swap = compare.swap || (elementCompare < 0 != indexCompare < 0);
      return elementCompare;
    }
    compare.swap = false;
    return compare;
  }-*/;

  /**
   * Sort a small subsection of an array by insertion sort.
   *
   * @param array array to sort
   * @param low lower bound of range to sort
   * @param high upper bound of range to sort
   * @param comp comparator to use
   */
  private static void insertionSort(Object[] array, int low, int high,
      Comparator<Object> comp) {
    for (int i = low + 1; i < high; ++i) {
      for (int j = i; j > low && comp.compare(array[j - 1], array[j]) > 0; --j) {
        Object t = array[j];
        array[j] = array[j - 1];
        array[j - 1] = t;
      }
    }
  }

  /**
   * Merge the two sorted subarrays (srcLow,srcMid] and (srcMid,srcHigh] into
   * dest.
   *
   * @param src source array for merge
   * @param srcLow lower bound of bottom sorted half
   * @param srcMid upper bound of bottom sorted half & lower bound of top sorted
   *          half
   * @param srcHigh upper bound of top sorted half
   * @param dest destination array for merge
   * @param destLow lower bound of destination
   * @param destHigh upper bound of destination
   * @param comp comparator to use
   */
  private static void merge(Object[] src, int srcLow, int srcMid, int srcHigh,
      Object[] dest, int destLow, int destHigh, Comparator<Object> comp) {
    // can't destroy srcMid because we need it as a bound on the lower half
    int topIdx = srcMid;
    while (destLow < destHigh) {
      if (topIdx >= srcHigh
          || (srcLow < srcMid && comp.compare(src[srcLow], src[topIdx]) <= 0)) {
        dest[destLow++] = src[srcLow++];
      } else {
        dest[destLow++] = src[topIdx++];
      }
    }
  }

  /**
   * Performs a merge sort on the specified portion of an object array.
   *
   * Uses O(n) temporary space to perform the merge, but is stable.
   */
  @SuppressWarnings("unchecked")
  private static void mergeSort(Object[] x, int fromIndex, int toIndex,
      Comparator<?> comp) {
    Object[] temp = copyOfRange(x, fromIndex, toIndex);
    mergeSort(temp, x, fromIndex, toIndex, -fromIndex,
        (Comparator<Object>) comp);
  }

  /**
   * Recursive helper function for
   * {@link Arrays#mergeSort(Object[], int, int, Comparator)}.
   *
   * @param temp temporary space, as large as the range of elements being
   *          sorted. On entry, temp should contain a copy of the sort range
   *          from array.
   * @param array array to sort
   * @param low lower bound of range to sort
   * @param high upper bound of range to sort
   * @param ofs offset to convert an array index into a temp index
   * @param comp comparison function
   */
  private static void mergeSort(Object[] temp, Object[] array, int low,
      int high, int ofs, Comparator<Object> comp) {
    int length = high - low;

    // insertion sort for small arrays
    if (length < 7) {
      insertionSort(array, low, high, comp);
      return;
    }

    // recursively sort both halves, using the array as temp space
    int tempLow = low + ofs;
    int tempHigh = high + ofs;
    int tempMid = tempLow + ((tempHigh - tempLow) >> 1);
    mergeSort(array, temp, tempLow, tempMid, -ofs, comp);
    mergeSort(array, temp, tempMid, tempHigh, -ofs, comp);

    // Skip merge if already in order - just copy from temp
    if (comp.compare(temp[tempMid - 1], temp[tempMid]) <= 0) {
      // TODO(jat): use System.arraycopy when that is implemented and more
      // efficient than this
      while (low < high) {
        array[low++] = temp[tempLow++];
      }
      return;
    }

    // merge sorted halves
    merge(temp, tempLow, tempMid, tempHigh, array, low, high, comp);
  }

  /**
   * Sort an entire array of number primitives.
   */
  @UnsafeNativeLong
  private static native void nativeLongSort(Object array) /*-{
    array.sort(@com.google.gwt.lang.LongLib::compare(Lcom/google/gwt/lang/LongLibBase$LongEmul;Lcom/google/gwt/lang/LongLibBase$LongEmul;));
  }-*/;

  /**
   * Sort a subset of an array of number primitives.
   */
  @UnsafeNativeLong
  private static native void nativeLongSort(Object array, int fromIndex,
      int toIndex) /*-{
    var temp = array.slice(fromIndex, toIndex);
    temp.sort(@com.google.gwt.lang.LongLib::compare(Lcom/google/gwt/lang/LongLibBase$LongEmul;Lcom/google/gwt/lang/LongLibBase$LongEmul;));
    var n = toIndex - fromIndex;
    @com.google.gwt.lang.Array::nativeArraycopy(Ljava/lang/Object;ILjava/lang/Object;II)(
        temp, 0, array, fromIndex, n)
  }-*/;

  /**
   * Sort an entire array of number primitives.
   */
  private static native void nativeNumberSort(Object array) /*-{
    array.sort(function(a, b) {
      return a - b;
    });
  }-*/;

  /**
   * Sort a subset of an array of number primitives.
   */
  private static native void nativeNumberSort(Object array, int fromIndex,
      int toIndex) /*-{
    var temp = array.slice(fromIndex, toIndex);
    temp.sort(function(a, b) {
      return a - b;
    });
    var n = toIndex - fromIndex;
    @com.google.gwt.lang.Array::nativeArraycopy(Ljava/lang/Object;ILjava/lang/Object;II)(
        temp, 0, array, fromIndex, n)
  }-*/;

  /**
   * Sort a subset of an array with the specified comparison function. Note that
   * the array is also referenced via closure in the comparison function.
   *
   * This implementation sorts it using the native (unstable) sort using an
   * index array and comparing the indices if they are otherwise equal, then
   * making another pass through the array to put them into the proper order.
   * This adds O(2*n) space for the index array and a temporary copy for
   * re-ordering (one of which is required anyway since JavaScript can't sort
   * subsets of an array), and the re-order pass takes O(n) time.
   *
   * Note that this function isn't currently used but is kept because the native
   * sort/fixup approach is faster everywhere but IE. In the future, we may
   * choose to use deferred binding in the JRE to make those platforms faster.
   *
   * @param array an array of either Java primitives or Object references
   * @param fromIndex the start of the range to sort
   * @param toIndex one past the end of the range to sort
   * @param comp a JavaScript comparison function (which holds reference to the
   *          array to sort), which will be passed indices into the array. The
   *          comparison function must also have a property swap which is true
   *          if any elements were out of order.
   */
  @SuppressWarnings("unused")
  // Currently unused, but useful for future; see above comment.
  private static native void nativeObjSort(Object array, int fromIndex,
      int toIndex, JavaScriptObject comp) /*-{
    var n = toIndex - fromIndex;
    var indexArray = new Array(n);
    var arrayIdx = fromIndex;
    for ( var i = 0; i < n; ++i) {
      indexArray[i] = arrayIdx++;
    }
    indexArray.sort(comp);
    if (comp.swap) { // only reorder elements if we made a swap
      var temp = array.slice(fromIndex, toIndex);
      arrayIdx = fromIndex;
      for ( var i = 0; i < n; ++i) {
        array[arrayIdx++] = temp[indexArray[i] - fromIndex];
      }
    }
  }-*/;

  /**
   * Performs the checks specified by the JRE docs and throws appropriate
   * exceptions.
   *
   * @param fromIndex beginning of the range to sort
   * @param toIndex past the end of the range to sort
   * @param length size of the array to sort
   *
   * @throws IllegalArgumentException if fromIndex > toIndex
   * @throws ArrayIndexOutOfBoundsException if fromIndex < 0 or toIndex > length
   */
  private static void verifySortIndices(int fromIndex, int toIndex, int length) {
    if (fromIndex > toIndex) {
      throw new IllegalArgumentException("fromIndex(" + fromIndex
          + ") > toIndex(" + toIndex + ")");
    }
    if (fromIndex < 0 || toIndex > length) {
      throw new ArrayIndexOutOfBoundsException("fromIndex(" + fromIndex
          + ") or toIndex(" + toIndex + ") out of bounds (0 - " + length + ")");
    }
  }
}
