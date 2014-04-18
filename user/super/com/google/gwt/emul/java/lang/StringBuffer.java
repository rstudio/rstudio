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

/**
 * A fast way to create strings using multiple appends.
 * 
 * This class is an exact clone of {@link StringBuilder} except for the name.
 * Any change made to one should be mirrored in the other.
 */
public final class StringBuffer extends AbstractStringBuilder implements CharSequence, Appendable {

  public StringBuffer() {
    super("");
  }

  public StringBuffer(CharSequence s) {
    super(String.valueOf(s));
  }

  /**
   * This implementation does not track capacity; using this constructor is
   * functionally equivalent to using the zero-argument constructor.
   */
  @SuppressWarnings("unused")
  public StringBuffer(int ignoredCapacity) {
    super("");
  }

  public StringBuffer(String s) {
    super(s);
  }

  public StringBuffer append(boolean x) {
    string += x;
    return this;
  }

  @Override
  public StringBuffer append(char x) {
    string += x;
    return this;
  }

  public StringBuffer append(char[] x) {
    string += String.valueOf(x);
    return this;
  }

  public StringBuffer append(char[] x, int start, int len) {
    string += String.valueOf(x, start, len);
    return this;
  }

  @Override
  public StringBuffer append(CharSequence x) {
    string += x;
    return this;
  }

  @Override
  public StringBuffer append(CharSequence x, int start, int end) {
    append0(x, start, end);
    return this;
  }

  public StringBuffer append(double x) {
    string += x;
    return this;
  }

  public StringBuffer append(float x) {
    string += x;
    return this;
  }

  public StringBuffer append(int x) {
    string += x;
    return this;
  }

  public StringBuffer append(long x) {
    string += x;
    return this;
  }

  public StringBuffer append(Object x) {
    string += x;
    return this;
  }

  public StringBuffer append(String x) {
    string += x;
    return this;
  }

  public StringBuffer append(StringBuffer x) {
    string += x;
    return this;
  }

  public StringBuffer appendCodePoint(int x) {
    appendCodePoint0(x);
    return this;
  }

  public StringBuffer delete(int start, int end) {
    replace0(start, end, "");
    return this;
  }

  public StringBuffer deleteCharAt(int start) {
    replace0(start, start + 1, "");
    return this;
  }

  public StringBuffer insert(int index, boolean x) {
    return insert(index, String.valueOf(x));
  }

  public StringBuffer insert(int index, char x) {
    return insert(index, String.valueOf(x));
  }

  public StringBuffer insert(int index, char[] x) {
    return insert(index, String.valueOf(x));
  }

  public StringBuffer insert(int index, char[] x, int offset, int len) {
    return insert(index, String.valueOf(x, offset, len));
  }

  public StringBuffer insert(int index, CharSequence chars) {
    return insert(index, chars.toString());
  }

  public StringBuffer insert(int index, CharSequence chars, int start, int end) {
    return insert(index, chars.subSequence(start, end).toString());
  }

  public StringBuffer insert(int index, double x) {
    return insert(index, String.valueOf(x));
  }

  public StringBuffer insert(int index, float x) {
    return insert(index, String.valueOf(x));
  }

  public StringBuffer insert(int index, int x) {
    return insert(index, String.valueOf(x));
  }

  public StringBuffer insert(int index, long x) {
    return insert(index, String.valueOf(x));
  }

  public StringBuffer insert(int index, Object x) {
    return insert(index, String.valueOf(x));
  }

  public StringBuffer insert(int index, String x) {
    replace0(index, index, x);
    return this;
  }

  public StringBuffer replace(int start, int end, String toInsert) {
    replace0(start, end, toInsert);
    return this;
  }

  public StringBuffer reverse() {
    reverse0();
    return this;
  }
}
