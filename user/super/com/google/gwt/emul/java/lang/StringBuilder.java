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
package java.lang;

import static javaemul.internal.InternalPreconditions.checkNotNull;

/**
 * A fast way to create strings using multiple appends.
 *
 * This class is an exact clone of {@link StringBuffer} except for the name.
 * Any change made to one should be mirrored in the other.
 */
public final class StringBuilder extends AbstractStringBuilder {

  public StringBuilder() {
    super("");
  }

  public StringBuilder(CharSequence s) {
    super(s.toString());
  }

  /**
   * This implementation does not track capacity; using this constructor is
   * functionally equivalent to using the zero-argument constructor.
   */
  @SuppressWarnings("unused")
  public StringBuilder(int ignoredCapacity) {
    super("");
  }

  public StringBuilder(String s) {
    super(checkNotNull(s));
  }

  public StringBuilder append(boolean x) {
    string += x;
    return this;
  }

  @Override
  public StringBuilder append(char x) {
    string += x;
    return this;
  }

  public StringBuilder append(char[] x) {
    string += String.valueOf(x);
    return this;
  }

  public StringBuilder append(char[] x, int start, int len) {
    string += String.valueOf(x, start, len);
    return this;
  }

  @Override
  public StringBuilder append(CharSequence x) {
    string += x;
    return this;
  }

  @Override
  public StringBuilder append(CharSequence x, int start, int end) {
    string += String.valueOf(x).substring(start, end);
    return this;
  }

  public StringBuilder append(double x) {
    string += x;
    return this;
  }

  public StringBuilder append(float x) {
    string += x;
    return this;
  }

  public StringBuilder append(int x) {
    string += x;
    return this;
  }

  public StringBuilder append(long x) {
    string += x;
    return this;
  }

  public StringBuilder append(Object x) {
    string += x;
    return this;
  }

  public StringBuilder append(String x) {
    string += x;
    return this;
  }

  public StringBuilder append(StringBuffer x) {
    string += x;
    return this;
  }

  public StringBuilder appendCodePoint(int x) {
    appendCodePoint0(x);
    return this;
  }

  public StringBuilder delete(int start, int end) {
    replace0(start, end, "");
    return this;
  }

  public StringBuilder deleteCharAt(int start) {
    replace0(start, start + 1, "");
    return this;
  }

  public StringBuilder insert(int index, boolean x) {
    return insert(index, String.valueOf(x));
  }

  public StringBuilder insert(int index, char x) {
    return insert(index, String.valueOf(x));
  }

  public StringBuilder insert(int index, char[] x) {
    return insert(index, String.valueOf(x));
  }

  public StringBuilder insert(int index, char[] x, int offset, int len) {
    return insert(index, String.valueOf(x, offset, len));
  }

  public StringBuilder insert(int index, CharSequence chars) {
    return insert(index, String.valueOf(chars));
  }

  public StringBuilder insert(int index, CharSequence chars, int start, int end) {
    return insert(index, String.valueOf(chars).substring(start, end));
  }

  public StringBuilder insert(int index, double x) {
    return insert(index, String.valueOf(x));
  }

  public StringBuilder insert(int index, float x) {
    return insert(index, String.valueOf(x));
  }

  public StringBuilder insert(int index, int x) {
    return insert(index, String.valueOf(x));
  }

  public StringBuilder insert(int index, long x) {
    return insert(index, String.valueOf(x));
  }

  public StringBuilder insert(int index, Object x) {
    return insert(index, String.valueOf(x));
  }

  public StringBuilder insert(int index, String x) {
    replace0(index, index, x);
    return this;
  }

  public StringBuilder replace(int start, int end, String toInsert) {
    replace0(start, end, toInsert);
    return this;
  }

  public StringBuilder reverse() {
    reverse0();
    return this;
  }
}
