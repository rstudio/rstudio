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

import elemental.util.ArrayOf;
import elemental.util.ArrayOfString;
import elemental.util.CanCompare;
import elemental.util.CanCompareString;
import elemental.util.Collections;

/**
 * JRE implementation of ArrayOfString for server and dev mode.
 */
public class JreArrayOfString implements ArrayOfString {

  private ArrayOf<String> array;

  public JreArrayOfString(ArrayList<String> array) {
    this((new JreArrayOf<String>(array)));
  }

  public JreArrayOfString() {
    array = Collections.arrayOf();
  }

  JreArrayOfString(ArrayOf<String> array) {
     this.array = array;
  }

  /*
   * TODO(cromwellian): this implemation uses JRE ArrayList. A direct
   * implementation would be more efficient.
   */

  public ArrayOfString concat(ArrayOfString values) {
    return new JreArrayOfString(array.concat(((JreArrayOfString) values).array));
  }

  public boolean contains(String value) {
    return array.contains(value);
  }

  @Override
  public String get(int index) {
    return array.get(index);
  }

  public int indexOf(String value) {
    return array.indexOf(value);
  }

  public void insert(int index, String value) {
    array.insert(index, value);
  }

  @Override
  public boolean isEmpty() {
    return array.isEmpty();
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
  public String peek() {
    return array.peek();
  }

  @Override
  public String pop() {
    return array.pop();
  }

  public void push(String value) {
    array.push(value);
  }

  public void remove(String value) {
    array.remove(value);
  }

  @Override
  public void removeByIndex(int index) {
    array.removeByIndex(index);
  }

  public void set(int index, String value) {
    array.set(index, value);
  }

  @Override
  public void setLength(int length) {
    array.setLength(length);
  }

  @Override
  public String shift() {
    return array.shift();
  }

  @Override
  public void sort() {
    array.sort(new CanCompare<String>() {
      @Override
      public int compare(String a, String b) {
        return a == null ? (a == b ? 0 : -1) : a.compareTo(b);
      }
    });
  }

  @Override
  public void sort(final CanCompareString comparator) {
    array.sort(new CanCompare<String>() {
      @Override
      public int compare(String a, String b) {
        return comparator.compare(a, b);
      }
    });
  }

  @Override
  public ArrayOfString splice(int index, int count) {
    return new JreArrayOfString(array.splice(index, count));
  }

  public void unshift(String value) {
    array.unshift(value);
  }
}
