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

/**
 * Utility methods related to native arrays. <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/util/Arrays.html">[Sun
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
   * @param key value to search for
   * @return the index of an element with a matching value, or a negative number
   *         which is the index of the next larger value (or just past the end
   *         of the array if the searched value is larger than all elements in
   *         the array) minus 1 (to ensure error returns are negative)
   * @throws ClassCastException if <code>key</code> is not comparable to
   *             <code>sortedArray</code>'s elements.
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
   * @param key value to search for
   * @param comparator comparision function, <code>null</code> indicates
   *            <i>natural ordering</i> should be used.
   * @return the index of an element with a matching value, or a negative number
   *         which is the index of the next larger value (or just past the end
   *         of the array if the searched value is larger than all elements in
   *         the array) minus 1 (to ensure error returns are negative)
   * @throws ClassCastException if <code>key</code> and
   *             <code>sortedArray</code>'s elements cannot be compared by
   *             <code>comparator</code>.
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

  public static void sort(Object[] x) {
    nativeSort(x, x.length, Comparators.natural());
  }

  public static <T> void sort(T[] x, Comparator<? super T> s) {
    nativeSort(x, x.length, s != null ? s : Comparators.natural());
  }

  public static String toString(Object[] x) {
    if (x == null) {
      return "null";
    }

    StringBuffer b = new StringBuffer("[");
    for (int i = 0; i < x.length; i++) {
      if (i != 0) {
        b.append(", ");
      }
      if (x[i] == null) {
        b.append("null");
      } else {
        b.append(x[i].toString());
      }
    }
    b.append("]");
    return b.toString();
  }

  static void unsafeSort(Object[] x, Comparator<?> s) {
    nativeSort(x, x.length, s != null ? s : Comparators.natural());
  }

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

    for(i = 0; i < size; ++i){
      array[i] = v[i];
    }
  }-*/;

}
