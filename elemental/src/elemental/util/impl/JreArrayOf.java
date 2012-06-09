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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import elemental.util.ArrayOf;
import elemental.util.CanCompare;

/**
 * JRE implementation of ArrayOf for server and dev mode.
 */
public class JreArrayOf<T> implements ArrayOf<T> {

  /*
   * TODO(cromwellian): this implemation uses JRE ArrayList. A direct
   * implementation would be more efficient. 
   */
  private ArrayList<T> array;

  public JreArrayOf() {
    array = new ArrayList<T>();
  }

  JreArrayOf(ArrayList<T> array) {
    this.array = array;
  }

  @Override
  public ArrayOf<T> concat(ArrayOf<T> values) {
    assert values instanceof JreArrayOf;
    ArrayList<T> toReturn = new ArrayList<T>(array);
    toReturn.addAll(((JreArrayOf<T>) values).array);
    return new JreArrayOf<T>(toReturn);
  }

  @Override
  public boolean contains(T value) {
    return array.contains(value);
  }

  @Override
  public T get(int index) {
    return index >= length() ? null : array.get(index);
  }

  @Override
  public int indexOf(T value) {
    return array.indexOf(value);
  }

  @Override
  public void insert(int index, T value) {
    if (index >= length()) {
      array.add(value);
    } else {
      if (index < 0) {
        index = index + length();
        if (index < 0) {
          index = 0;
        }
      }
      array.add(index, value);
    }
  }

  @Override
  public boolean isEmpty() {
    return array.isEmpty();
  }

  @Override
  public String join() {
    return join(",");
  }

  @Override
  public String join(String separator) {
    StringBuilder toReturn = new StringBuilder();
    boolean first = true;
    for (T val : array) {
      if (first) {
        first = false;
      } else {
        toReturn.append(separator);
      }
      // JS treats null as "" for purposes of join()
      toReturn.append(val == null ? "" : toStringWithTrim(val));
    }
    return toReturn.toString();
  }

  @Override
  public int length() {
    return array.size();
  }

  @Override
  public T peek() {
    return isEmpty() ? null : array.get(array.size() - 1);
  }

  @Override
  public T pop() {
    return isEmpty() ? null : array.remove(array.size() - 1);
  }

  @Override
  public void push(T value) {
    array.add(value);
  }

  @Override
  public void remove(T value) {
    array.remove(value);
  }

  @Override
  public void removeByIndex(int index) {
    if (index < length()) {
      array.remove(index);
    }
  }

  @Override
  public void set(int index, T value) {
    ensureLength(index);
    array.set(index, value);
  }

  @Override
  public void setLength(int length) {
    if (length > length()) {
      for (int i = length(); i < length; i++) {
        array.add(null);
      }
    } else if (length < length()) {
      for (int i = length(); i > length; i--) {
        array.remove(i - 1);
      }
    }
  }

  @Override
  public T shift() {
    return isEmpty() ? null : array.remove(0);
  }

  @Override
  public void sort(final CanCompare<T> comparator) {
    Collections.sort(array, new Comparator<T>() {
      @Override
      public int compare(T o1, T o2) {
        return comparator.compare(o1, o2);
      }
    });
  }

  @Override
  public ArrayOf<T> splice(int index, int count) {
    ArrayList<T> toReturn = new ArrayList<T>(
        array.subList(index, index + count));
    for (int i = 0; i < count && !isEmpty(); i++) {
      array.remove(index);
    }
    return new JreArrayOf<T>(toReturn);
  }

  @Override
  public void unshift(T value) {
    array.add(0, value);
  }

  private void ensureLength(int index) {
    if (index >= length()) {
      setLength(index + 1);
    }
  }

  static String toStringWithTrim(Object obj) {
    if (obj instanceof Number) {
      String numberStr = obj.toString();
      if (numberStr.endsWith(".0")) {
        numberStr = numberStr.substring(0, numberStr.length() - 2);
      }
      return numberStr;
    }
    return obj.toString();
  }
}
