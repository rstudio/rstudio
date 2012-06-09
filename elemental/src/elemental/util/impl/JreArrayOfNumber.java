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
import elemental.util.ArrayOfNumber;
import elemental.util.CanCompare;
import elemental.util.CanCompareNumber;
import elemental.util.Collections;

/**
 * JRE implementation of ArrayOfInt for server and dev mode.
 */
public class JreArrayOfNumber implements ArrayOfNumber {

  /*
   * TODO(cromwellian): this implemation uses JRE ArrayList. A direct
   * implementation would be more efficient.
   */

  public ArrayOfNumber concat(ArrayOfNumber values) {
    return new JreArrayOfNumber(array.concat(((JreArrayOfNumber) values).array));
  }

  public boolean contains(double value) {
    return array.contains(value);
  }

  @Override
  public double get(int index) {
    return array.get(index);
  }

  public int indexOf(double value) {
    return array.indexOf(value);
  }

  public void insert(int index, double value) {
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
  public double peek() {
    return array.peek();
  }

  @Override
  public double pop() {
    return array.pop();
  }

  public void push(double value) {
    array.push(value);
  }

  public void remove(double value) {
    array.remove(value);
  }

  @Override
  public void removeByIndex(int index) {
    array.removeByIndex(index);
  }

  public void set(int index, double value) {
    array.set(index, value);
  }

  @Override
  public void setLength(int length) {
    array.setLength(length);
  }

  @Override
  public double shift() {
    return array.shift();
  }

  @Override
  public void sort() {
    array.sort(new CanCompare<Double>() {
      @Override
      public int compare(Double a, Double b) {
        return a == null ? (a == b ? 0 : -1) : a.compareTo(b);
      }
    });
  }

  @Override
  public void sort(final CanCompareNumber comparator) {
    array.sort(new CanCompare<Double>() {
      @Override
      public int compare(Double a, Double b) {
        return comparator.compare(a, b);
      }
    });
  }

  @Override
  public ArrayOfNumber splice(int index, int count) {
    return new JreArrayOfNumber(array.splice(index, count));
  }

  public void unshift(double value) {
    array.unshift(value);
  }

  private ArrayOf<Double> array;

  public JreArrayOfNumber() {
    array = Collections.arrayOf();
  }

  JreArrayOfNumber(ArrayOf<Double> array) {
     this.array = array;
  }
}
