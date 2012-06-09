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
import elemental.util.ArrayOfBoolean;
import elemental.util.Collections;

/**
 * JRE implementation of ArrayOfBoolean for server and dev mode.
 */
public class JreArrayOfBoolean implements ArrayOfBoolean {

  /*
   * TODO(cromwellian): this implemation uses JRE ArrayList. A direct
   * implementation would be more efficient.
   */

  public ArrayOfBoolean concat(ArrayOfBoolean values) {
    return new JreArrayOfBoolean(array.concat(((JreArrayOfBoolean) values).array));
  }

  public boolean contains(boolean value) {
    return array.contains(value);
  }

  @Override
  public boolean get(int index) {
    return array.get(index);
  }

  public int indexOf(boolean value) {
    return array.indexOf(value);
  }

  public void insert(int index, boolean value) {
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
  public boolean peek() {
    return array.peek();
  }

  @Override
  public boolean pop() {
    return array.pop();
  }

  public void push(boolean value) {
    array.push(value);
  }

  public void remove(boolean value) {
    array.remove(value);
  }

  @Override
  public void removeByIndex(int index) {
    array.removeByIndex(index);
  }

  public void set(int index, boolean value) {
    array.set(index, value);
  }

  @Override
  public void setLength(int length) {
    array.setLength(length);
  }

  @Override
  public boolean shift() {
    return array.shift();
  }

  @Override
  public ArrayOfBoolean splice(int index, int count) {
    return new JreArrayOfBoolean(array.splice(index, count));
  }

  public void unshift(boolean value) {
    array.unshift(value);
  }

  private ArrayOf<Boolean> array;

  public JreArrayOfBoolean() {
    array = Collections.arrayOf();
  }

  JreArrayOfBoolean(ArrayOf<Boolean> array) {
     this.array = array;
  }
}
