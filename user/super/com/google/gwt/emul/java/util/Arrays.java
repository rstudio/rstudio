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

import static javaemul.internal.Coercions.ensureInt;
import static javaemul.internal.InternalPreconditions.checkArgument;
import static javaemul.internal.InternalPreconditions.checkArraySize;
import static javaemul.internal.InternalPreconditions.checkCriticalPositionIndexes;
import static javaemul.internal.InternalPreconditions.checkElementIndex;
import static javaemul.internal.InternalPreconditions.checkNotNull;
import static javaemul.internal.InternalPreconditions.checkPositionIndexes;

import java.io.Serializable;

import javaemul.internal.ArrayHelper;
import javaemul.internal.LongCompareHolder;

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
      checkElementIndex(index, size());
      return array[index];
    }

    @Override
    public E set(int index, E value) {
      E was = get(index);
      array[index] = value;
      return was;
    }

    @Override
    public int size() {
      return array.length;
    }

    @Override
    public Object[] toArray() {
      return toArray(new Object[array.length]);
    }

    /*
     * Faster than the iterator-based implementation in AbstractCollection.
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T[] toArray(T[] out) {
      int size = size();
      if (out.length < size) {
        out = ArrayHelper.createFrom(out, size);
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
    checkArraySize(newLength);
    return copyOfRange(original, 0, newLength);
  }

  public static byte[] copyOf(byte[] original, int newLength) {
    checkArraySize(newLength);
    return copyOfRange(original, 0, newLength);
  }

  public static char[] copyOf(char[] original, int newLength) {
    checkArraySize(newLength);
    return copyOfRange(original, 0, newLength);
  }

  public static double[] copyOf(double[] original, int newLength) {
    checkArraySize(newLength);
    return copyOfRange(original, 0, newLength);
  }

  public static float[] copyOf(float[] original, int newLength) {
    checkArraySize(newLength);
    return copyOfRange(original, 0, newLength);
  }

  public static int[] copyOf(int[] original, int newLength) {
    checkArraySize(newLength);
    return copyOfRange(original, 0, newLength);
  }

  public static long[] copyOf(long[] original, int newLength) {
    checkArraySize(newLength);
    return copyOfRange(original, 0, newLength);
  }

  public static short[] copyOf(short[] original, int newLength) {
    checkArraySize(newLength);
    return copyOfRange(original, 0, newLength);
  }

  public static <T> T[] copyOf(T[] original, int newLength) {
    checkArraySize(newLength);
    checkNotNull(original, "original");
    T[] clone = ArrayHelper.clone(original, 0, newLength);
    ArrayHelper.setLength(clone, newLength);
    return clone;
  }

  public static boolean[] copyOfRange(boolean[] original, int from, int to) {
    int len = getCopyLength(original, from, to);
    boolean[] copy = new boolean[to - from];
    ArrayHelper.copy(original, from, copy, 0, len);
    return copy;
  }

  public static byte[] copyOfRange(byte[] original, int from, int to) {
    int len = getCopyLength(original, from, to);
    byte[] copy = new byte[to - from];
    ArrayHelper.copy(original, from, copy, 0, len);
    return copy;
  }

  public static char[] copyOfRange(char[] original, int from, int to) {
    int len = getCopyLength(original, from, to);
    char[] copy = new char[to - from];
    ArrayHelper.copy(original, from, copy, 0, len);
    return copy;
  }

  public static double[] copyOfRange(double[] original, int from, int to) {
    int len = getCopyLength(original, from, to);
    double[] copy = new double[to - from];
    ArrayHelper.copy(original, from, copy, 0, len);
    return copy;
  }

  public static float[] copyOfRange(float[] original, int from, int to) {
    int len = getCopyLength(original, from, to);
    float[] copy = new float[to - from];
    ArrayHelper.copy(original, from, copy, 0, len);
    return copy;
  }

  public static int[] copyOfRange(int[] original, int from, int to) {
    int len = getCopyLength(original, from, to);
    int[] copy = new int[to - from];
    ArrayHelper.copy(original, from, copy, 0, len);
    return copy;
  }

  public static long[] copyOfRange(long[] original, int from, int to) {
    int len = getCopyLength(original, from, to);
    long[] copy = new long[to - from];
    ArrayHelper.copy(original, from, copy, 0, len);
    return copy;
  }

  public static short[] copyOfRange(short[] original, int from, int to) {
    int len = getCopyLength(original, from, to);
    short[] copy = new short[to - from];
    ArrayHelper.copy(original, from, copy, 0, len);
    return copy;
  }

  public static <T> T[] copyOfRange(T[] original, int from, int to) {
    int len = getCopyLength(original, from, to);
    T[] copy = ArrayHelper.createFrom(original, to - from);
    ArrayHelper.copy(original, from, copy, 0, len);
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

    for (Object obj : a) {
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
      } else {
        hash = Objects.hashCode(obj);
      }

      hashCode = 31 * hashCode + hash;
      hashCode = ensureInt(hashCode); // make sure we don't overflow
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
    for (boolean e : a) {
      hashCode = 31 * hashCode + Boolean.hashCode(e);
      hashCode = ensureInt(hashCode); // make sure we don't overflow
    }
    return hashCode;
  }

  public static int hashCode(byte[] a) {
    if (a == null) {
      return 0;
    }
    int hashCode = 1;
    for (byte e : a) {
      hashCode = 31 * hashCode + Byte.hashCode(e);
      hashCode = ensureInt(hashCode); // make sure we don't overflow
    }
    return hashCode;
  }

  public static int hashCode(char[] a) {
    if (a == null) {
      return 0;
    }
    int hashCode = 1;
    for (char e : a) {
      hashCode = 31 * hashCode + Character.hashCode(e);
      hashCode = ensureInt(hashCode); // make sure we don't overflow
    }
    return hashCode;
  }

  public static int hashCode(double[] a) {
    if (a == null) {
      return 0;
    }
    int hashCode = 1;
    for (double e : a) {
      hashCode = 31 * hashCode + Double.hashCode(e);
      hashCode = ensureInt(hashCode); // make sure we don't overflow
    }
    return hashCode;
  }

  public static int hashCode(float[] a) {
    if (a == null) {
      return 0;
    }
    int hashCode = 1;
    for (float e : a) {
      hashCode = 31 * hashCode + Float.hashCode(e);
      hashCode = ensureInt(hashCode); // make sure we don't overflow
    }
    return hashCode;
  }

  public static int hashCode(int[] a) {
    if (a == null) {
      return 0;
    }
    int hashCode = 1;
    for (int e : a) {
      hashCode = 31 * hashCode + Integer.hashCode(e);
      hashCode = ensureInt(hashCode); // make sure we don't overflow
    }
    return hashCode;
  }

  public static int hashCode(long[] a) {
    if (a == null) {
      return 0;
    }
    int hashCode = 1;
    for (long e : a) {
      hashCode = 31 * hashCode + Long.hashCode(e);
      hashCode = ensureInt(hashCode); // make sure we don't overflow
    }
    return hashCode;
  }

  public static int hashCode(Object[] a) {
    if (a == null) {
      return 0;
    }
    int hashCode = 1;
    for (Object e : a) {
      hashCode = 31 * hashCode + Objects.hashCode(e);
      hashCode = ensureInt(hashCode); // make sure we don't overflow
    }
    return hashCode;
  }

  public static int hashCode(short[] a) {
    if (a == null) {
      return 0;
    }
    int hashCode = 1;
    for (short e : a) {
      hashCode = 31 * hashCode + Short.hashCode(e);
      hashCode = ensureInt(hashCode); // make sure we don't overflow
    }
    return hashCode;
  }

  public static void sort(byte[] array) {
    nativeNumberSort(array);
  }

  public static void sort(byte[] array, int fromIndex, int toIndex) {
    checkPositionIndexes(fromIndex, toIndex, array.length);
    nativeNumberSort(array, fromIndex, toIndex);
  }

  public static void sort(char[] array) {
    nativeNumberSort(array);
  }

  public static void sort(char[] array, int fromIndex, int toIndex) {
    checkPositionIndexes(fromIndex, toIndex, array.length);
    nativeNumberSort(array, fromIndex, toIndex);
  }

  public static void sort(double[] array) {
    nativeNumberSort(array);
  }

  public static void sort(double[] array, int fromIndex, int toIndex) {
    checkPositionIndexes(fromIndex, toIndex, array.length);
    nativeNumberSort(array, fromIndex, toIndex);
  }

  public static void sort(float[] array) {
    nativeNumberSort(array);
  }

  public static void sort(float[] array, int fromIndex, int toIndex) {
    checkPositionIndexes(fromIndex, toIndex, array.length);
    nativeNumberSort(array, fromIndex, toIndex);
  }

  public static void sort(int[] array) {
    nativeNumberSort(array);
  }

  public static void sort(int[] array, int fromIndex, int toIndex) {
    checkPositionIndexes(fromIndex, toIndex, array.length);
    nativeNumberSort(array, fromIndex, toIndex);
  }

  public static void sort(long[] array) {
    nativeLongSort(array, LongCompareHolder.getLongComparator());
  }

  public static void sort(long[] array, int fromIndex, int toIndex) {
    checkPositionIndexes(fromIndex, toIndex, array.length);
    nativeLongSort(array, fromIndex, toIndex);
  }

  public static void sort(Object[] array) {
    mergeSort(array, 0, array.length, Comparators.natural());
  }

  public static void sort(Object[] x, int fromIndex, int toIndex) {
    mergeSort(x, fromIndex, toIndex, Comparators.natural());
  }

  public static void sort(short[] array) {
    nativeNumberSort(array);
  }

  public static void sort(short[] array, int fromIndex, int toIndex) {
    checkPositionIndexes(fromIndex, toIndex, array.length);
    nativeNumberSort(array, fromIndex, toIndex);
  }

  public static <T> void sort(T[] x, Comparator<? super T> c) {
    mergeSort(x, 0, x.length, c);
  }

  public static <T> void sort(T[] x, int fromIndex, int toIndex,
      Comparator<? super T> c) {
    checkPositionIndexes(fromIndex, toIndex, x.length);
    mergeSort(x, fromIndex, toIndex, c);
  }

  public static Spliterator.OfDouble spliterator(double[] array) {
    return Spliterators.spliterator(array, Spliterator.IMMUTABLE | Spliterator.ORDERED);
  }

  public static Spliterator.OfDouble spliterator(double[] array,
      int startInclusive, int endExclusive) {
    return Spliterators.spliterator(array, startInclusive, endExclusive,
        Spliterator.IMMUTABLE | Spliterator.ORDERED);
  }

  public static Spliterator.OfInt spliterator(int[] array) {
    return Spliterators.spliterator(array, Spliterator.IMMUTABLE | Spliterator.ORDERED);
  }

  public static Spliterator.OfInt spliterator(int[] array, int startInclusive, int endExclusive) {
    return Spliterators.spliterator(array, startInclusive, endExclusive,
        Spliterator.IMMUTABLE | Spliterator.ORDERED);
  }

  public static Spliterator.OfLong spliterator(long[] array) {
    return Spliterators.spliterator(array, Spliterator.IMMUTABLE | Spliterator.ORDERED);
  }

  public static Spliterator.OfLong spliterator(long[] array, int startInclusive, int endExclusive) {
    return Spliterators.spliterator(array, startInclusive, endExclusive,
        Spliterator.IMMUTABLE | Spliterator.ORDERED);
  }

  public static <T> Spliterator<T> spliterator(T[] array) {
    return Spliterators.spliterator(array, Spliterator.IMMUTABLE | Spliterator.ORDERED);
  }

  public static <T> Spliterator<T> spliterator(T[] array, int startInclusive, int endExclusive) {
    return Spliterators.spliterator(array, startInclusive, endExclusive,
        Spliterator.IMMUTABLE | Spliterator.ORDERED);
  }

  public static String toString(boolean[] a) {
    if (a == null) {
      return "null";
    }
    StringJoiner joiner = new StringJoiner(", ", "[", "]");
    for (boolean element : a) {
      joiner.add(String.valueOf(element));
    }
    return joiner.toString();
  }

  public static String toString(byte[] a) {
    if (a == null) {
      return "null";
    }
    StringJoiner joiner = new StringJoiner(", ", "[", "]");
    for (byte element : a) {
      joiner.add(String.valueOf(element));
    }
    return joiner.toString();
  }

  public static String toString(char[] a) {
    if (a == null) {
      return "null";
    }
    StringJoiner joiner = new StringJoiner(", ", "[", "]");
    for (char element : a) {
      joiner.add(String.valueOf(element));
    }
    return joiner.toString();
  }

  public static String toString(double[] a) {
    if (a == null) {
      return "null";
    }
    StringJoiner joiner = new StringJoiner(", ", "[", "]");
    for (double element : a) {
      joiner.add(String.valueOf(element));
    }
    return joiner.toString();
  }

  public static String toString(float[] a) {
    if (a == null) {
      return "null";
    }
    StringJoiner joiner = new StringJoiner(", ", "[", "]");
    for (float element : a) {
      joiner.add(String.valueOf(element));
    }
    return joiner.toString();
  }

  public static String toString(int[] a) {
    if (a == null) {
      return "null";
    }
    StringJoiner joiner = new StringJoiner(", ", "[", "]");
    for (int element : a) {
      joiner.add(String.valueOf(element));
    }
    return joiner.toString();
  }

  public static String toString(long[] a) {
    if (a == null) {
      return "null";
    }
    StringJoiner joiner = new StringJoiner(", ", "[", "]");
    for (long element : a) {
      joiner.add(String.valueOf(element));
    }
    return joiner.toString();
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
    StringJoiner joiner = new StringJoiner(", ", "[", "]");
    for (short element : a) {
      joiner.add(String.valueOf(element));
    }
    return joiner.toString();
  }

  /**
   * Recursive helper function for {@link Arrays#deepToString(Object[])}.
   */
  private static String deepToString(Object[] a, Set<Object[]> arraysIveSeen) {
    if (a == null) {
      return "null";
    }

    if (!arraysIveSeen.add(a)) {
      return "[...]";
    }

    StringJoiner joiner = new StringJoiner(", ", "[", "]");
    for (Object obj : a) {
      if (obj != null && obj.getClass().isArray()) {
        if (obj instanceof Object[]) {
          if (arraysIveSeen.contains(obj)) {
            joiner.add("[...]");
          } else {
            Object[] objArray = (Object[]) obj;
            HashSet<Object[]> tempSet = new HashSet<Object[]>(arraysIveSeen);
            joiner.add(deepToString(objArray, tempSet));
          }
        } else if (obj instanceof boolean[]) {
          joiner.add(toString((boolean[]) obj));
        } else if (obj instanceof byte[]) {
          joiner.add(toString((byte[]) obj));
        } else if (obj instanceof char[]) {
          joiner.add(toString((char[]) obj));
        } else if (obj instanceof short[]) {
          joiner.add(toString((short[]) obj));
        } else if (obj instanceof int[]) {
          joiner.add(toString((int[]) obj));
        } else if (obj instanceof long[]) {
          joiner.add(toString((long[]) obj));
        } else if (obj instanceof float[]) {
          joiner.add(toString((float[]) obj));
        } else if (obj instanceof double[]) {
          joiner.add(toString((double[]) obj));
        } else {
          assert false : "Unexpected array type: " + obj.getClass().getName();
        }
      } else {
        joiner.add(String.valueOf(obj));
      }
    }
    return joiner.toString();
  }

  private static int getCopyLength(Object array, int from, int to) {
    checkArgument(from <= to, "%s > %s", from, to);
    int len = ArrayHelper.getLength(array);
    to = Math.min(to, len);
    checkCriticalPositionIndexes(from, to, len);
    return to - from;
  }

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
  private static void mergeSort(Object[] x, int fromIndex, int toIndex, Comparator<?> comp) {
    if (comp == null) {
      comp = Comparators.natural();
    }
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
  private static native void nativeLongSort(Object array, Object compareFunction) /*-{
    array.sort(compareFunction);
  }-*/;

  /**
   * Sort a subset of an array of number primitives.
   */
  private static void nativeLongSort(Object array, int fromIndex, int toIndex) {
    Object temp = ArrayHelper.unsafeClone(array, fromIndex, toIndex);
    nativeLongSort(temp, LongCompareHolder.getLongComparator());
    ArrayHelper.copy(temp, 0, array, fromIndex, toIndex - fromIndex);
  }

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
  private static void nativeNumberSort(Object array, int fromIndex, int toIndex) {
    Object temp = ArrayHelper.unsafeClone(array, fromIndex, toIndex);
    nativeNumberSort(temp);
    ArrayHelper.copy(temp, 0, array, fromIndex, toIndex - fromIndex);
  }
}
