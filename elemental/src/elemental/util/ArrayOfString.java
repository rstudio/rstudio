/*
 * Copyright 2010 Google Inc.
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
package elemental.util;

/**
 * A lightweight array of Strings.
 * 
 * @see elemental.js.util.JsArrayOfString
 */
public interface ArrayOfString {
  /**
   * Returns a new array that is the concatenation of this array and <code>
   * values</code>. This method does not mutate the current array.
   */
  ArrayOfString concat(ArrayOfString values);

  /**
   * Indicates whether the array contains the specified value.
   */
  boolean contains(String value);

  /**
   * Gets the value at a given index.
   * 
   * @param index the index to be retrieved
   * @return the value at the given index, or <code>null</code> if none exists
   */
  String get(int index);

  /**
   * Returns the index of the specified value or <code>-1</code> if the value is
   * not found.
   */
  int indexOf(String value);

  /**
   * Inserts a new element into the array at the specified index.
   *
   * Note: If index >= the length of the array, the element will be appended to
   * the end. Also if the index is negative, the element will be inserted
   * starting from the end of the array.
   */  
  void insert(int index, String value);

  /**
   * Returns true if the length of the array is zero.
   *
   * @return true when length is zero
   */
  boolean isEmpty();

  /**
   * Convert each element of the array to a String and join them with a comma
   * separator. The value returned from this method may vary between browsers
   * based on how JavaScript values are converted into strings.
   */
  String join();

  /**
   * Convert each element of the array to a String and join them with a comma
   * separator. The value returned from this method may vary between browsers
   * based on how JavaScript values are converted into strings.
   */
  String join(String separator);

  /**
   * Gets the length of the array.
   * 
   * @return the array length
   */
  int length();

  /**
   * Returns the last value of the array;
   *
   * @return the last value
   */
  String peek();

  /**
   * Remove and return the element from the end of the array.
   *
   * @return the removed value
   */
  String pop();

  /**
   * Pushes the given value onto the end of the array.
   */
  void push(String value);

  /**
   * Searches for the specified value in the array and removes the first
   * occurrence if found.
   */
  void remove(String value);

  /**
   * Removes the element at the specified index.
   */
  void removeByIndex(int index);

  /**
   * Sets the value value at a given index.
   * 
   * If the index is out of bounds, the value will still be set. The array's
   * length will be updated to encompass the bounds implied by the added value.
   * 
   * @param index the index to be set
   * @param value the value to be stored
   */
  void set(int index, String value);
  
  /**
   * Reset the length of the array.
   * 
   * @param length the new length of the array
   */
  void setLength(int length);
  
  /**
   * Shifts the first value off the array.
   * 
   * @return the shifted value
   */
  String shift();
  
  /**
   * Sorts the contents of the array in ascending order.
   */
  void sort();

  /**
   * Sorts the contents of the Array based on the {@link CanCompareString}.
   *
   * @param comparator
   */
  void sort(CanCompareString comparator);

  /**
   * Removes the specified number of elements starting at index and returns the
   * removed elements.
   */
  ArrayOfString splice(int index, int count);

  /**
   * Shifts a value onto the beginning of the array.
   * 
   * @param value the value to the stored
   */
  void unshift(String value);
}
