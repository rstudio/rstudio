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
 * Utility methods that operate on collections.
 */
public class Collections {

  public static final Set EMPTY_SET = new HashSet();
  public static final Map EMPTY_MAP = new HashMap();
  public static final List EMPTY_LIST = new ArrayList();

  /**
   * Perform a binary search on a sorted List, using natural ordering.
   * 
   * <p>
   * Note: The GWT implementation differs from the JDK implementation in that it
   * does not do an iterator-based binary search for Lists that do not implement
   * RandomAccess.
   * </p>
   * 
   * @param sortedList object array to search
   * @param key value to search for
   * @return the index of an element with a matching value, or a negative number
   *         which is the index of the next larger value (or just past the end
   *         of the array if the searched value is larger than all elements in
   *         the array) minus 1 (to ensure error returns are negative)
   * @throws ClassCastException if <code>key</code> is not comparable to
   *           <code>sortedList</code>'s elements.
   */
  public static int binarySearch(final List sortedList, final Object key) {
    return binarySearch(sortedList, key, Comparators.natural());
  }

  /**
   * Perform a binary search on a sorted List, using a user-specified comparison
   * function.
   * 
   * <p>
   * Note: The GWT implementation differs from the JDK implementation in that it
   * does not do an iterator-based binary search for Lists that do not implement
   * RandomAccess.
   * </p>
   * 
   * @param sortedList List to search
   * @param key value to search for
   * @param comparator comparision function, <code>null</code> indicates
   *          <i>natural ordering</i> should be used.
   * @return the index of an element with a matching value, or a negative number
   *         which is the index of the next larger value (or just past the end
   *         of the array if the searched value is larger than all elements in
   *         the array) minus 1 (to ensure error returns are negative)
   * @throws ClassCastException if <code>key</code> and
   *           <code>sortedList</code>'s elements cannot be compared by
   *           <code>comparator</code>.
   */
  public static int binarySearch(final List sortedList, final Object key,
      Comparator comparator) {
    /*
     * TODO: This doesn't implement the "iterator-based binary search" described
     * in the JDK docs for non-RandomAccess Lists. Until GWT provides a
     * LinkedList, this shouldn't be an issue.
     */
    comparator = comparator != null ? comparator : Comparators.natural();
    int low = 0;
    int high = sortedList.size() - 1;

    while (low <= high) {
      final int mid = low + ((high - low) / 2);
      final Object midVal = sortedList.get(mid);
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

  public static void reverse(List l) {
    int lastPos = l.size() - 1;
    for (int i = 0; i < l.size() / 2; i++) {
      Object element = l.get(i);
      int swapPos = lastPos - i;
      assert (swapPos > i);
      Object swappedWith = l.get(swapPos);
      l.set(i, swappedWith);
      l.set(swapPos, element);
    }
  }

  public static void sort(List target) {
    Object[] x = target.toArray();
    Arrays.sort(x);
    replaceContents(target, x);
  }

  public static void sort(List target, Comparator y) {
    Object[] x = target.toArray();
    Arrays.sort(x, y);
    replaceContents(target, x);
  }

  private static void replaceContents(List target, Object[] x) {
    int size = target.size();
    assert (x.length == size);
    for (int i = 0; i < size; i++) {
      target.set(i, x[i]);
    }
  }
}
