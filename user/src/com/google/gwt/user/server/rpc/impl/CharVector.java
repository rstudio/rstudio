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
package com.google.gwt.user.server.rpc.impl;

/**
 * A vector of primitive characters.
 */
class CharVector {
  private int capacityIncrement;
  private char chars[];
  private int size;

  public CharVector(int initialCapacity, int capacityIncrement) {
    assert (initialCapacity >= 0);
    assert (capacityIncrement >= 0);

    this.capacityIncrement = capacityIncrement;
    chars = new char[initialCapacity];
  }

  public void add(char ch) {
    if (size >= chars.length) {
      int growBy = (capacityIncrement == 0) ? chars.length * 2
          : capacityIncrement;
      char newChars[] = new char[(chars.length + growBy)];
      System.arraycopy(chars, 0, newChars, 0, size);
      chars = newChars;
    }

    chars[size++] = ch;
  }

  public char[] asArray() {
    return chars;
  }

  public char get(int index) {
    assert (index < size);

    return chars[index];
  }

  public int getSize() {
    return size;
  }

  public void set(int index, char ch) {
    assert (index >= 0 && index < size);

    chars[index] = ch;
    ++size;
  }
}