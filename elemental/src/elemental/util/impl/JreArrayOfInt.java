/*
 * Copyright 2011 Google Inc.
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
package elemental.util.impl;


import elemental.util.ArrayOf;
import elemental.util.ArrayOfInt;
import elemental.util.CanCompare;
import elemental.util.CanCompareInt;
import elemental.util.Collections;

/**
 * JRE implementation of ArrayOfInt for server and dev mode.
 */
public class JreArrayOfInt implements ArrayOfInt {

  /*
   * TODO(cromwellian): this implemation uses JRE ArrayList. A direct
   * implementation would be more efficient.
   */

  public ArrayOfInt concat(ArrayOfInt values) {
    return new JreArrayOfInt(array.concat(((JreArrayOfInt) values).array));
  }

  public boolean contains(int value) {
    return array.contains(value);
  }

  @Override
  public int get(int index) {
    return array.get(index);
  }

  public int indexOf(int value) {
    return array.indexOf(value);
  }

  public void insert(int index, int value) {
    array.insert(index, value);
  }

  @Override
  public boolean isEmpty() {
    return array.isEmpty();
  }

  @Override
  public boolean isSet(int index) {
    return array.get(index) != null;
  }

  @Override
  public String join() {
    return array.join();
  }

  @Override
  public String join(String separator) {
    return array.join(separator);
  }

  @Override
  public int length() {
    return array.length();
  }

  @Override
  public int peek() {
    return array.peek();
  }

  @Override
  public int pop() {
    return array.pop();
  }

  public void push(int value) {
    array.push(value);
  }

  public void remove(int value) {
    array.remove(value);
  }

  @Override
  public void removeByIndex(int index) {
    array.removeByIndex(index);
  }

  public void set(int index, int value) {
    array.set(index, value);
  }

  @Override
  public void setLength(int length) {
    array.setLength(length);
  }

  @Override
  public int shift() {
    return array.shift();
  }

  @Override
  public void sort() {
    array.sort(new CanCompare<Integer>() {
      @Override
      public int compare(Integer a, Integer b) {
        return a == null ? (a == b ? 0 : -1) : a.compareTo(b);
      }
    });
  }

  @Override
  public void sort(final CanCompareInt comparator) {
    array.sort(new CanCompare<Integer>() {
      @Override
      public int compare(Integer a, Integer b) {
        return comparator.compare(a, b);
      }
    });
  }

  @Override
  public ArrayOfInt splice(int index, int count) {
    return new JreArrayOfInt(array.splice(index, count));
  }

  public void unshift(int value) {
    array.unshift(value);
  }

  private ArrayOf<Integer> array;

  public JreArrayOfInt() {
    array = Collections.arrayOf();
  }

  JreArrayOfInt(ArrayOf<Integer> array) {
     this.array = array;
  }
}
