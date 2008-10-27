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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.impl.StringBufferImpl;

/**
 * A fast way to create strings using multiple appends. This is implemented
 * using a {@link StringBufferImpl} that is chosen with deferred binding.
 * 
 * Most methods will give expected performance results. Exceptions are
 * {@link #setCharAt(int, char)}, which is O(n), and {@link #length()}, which
 * forces a {@link #toString()} and thus should not be used many times on the
 * same <code>StringBuffer</code>.
 * 
 * This class is an exact clone of {@link StringBuilder} except for the name.
 * Any change made to one should be mirrored in the other.
 */
public class StringBuffer implements CharSequence, Appendable {
  private final StringBufferImpl impl = GWT.create(StringBufferImpl.class);
  private final Object data = impl.createData();

  public StringBuffer() {
  }

  public StringBuffer(CharSequence s) {
    this(s.toString());
  }

  /**
   * This implementation does not track capacity; using this constructor is
   * functionally equivalent to using the zero-argument constructor.
   */
  @SuppressWarnings("unused")
  public StringBuffer(int ignoredCapacity) {
  }

  public StringBuffer(String s) {
    append(s);
  }

  public StringBuffer append(boolean x) {
    impl.append(data, x);
    return this;
  }

  public StringBuffer append(char x) {
    impl.appendNonNull(data, String.valueOf(x));
    return this;
  }

  public StringBuffer append(char[] x) {
    impl.appendNonNull(data, String.valueOf(x));
    return this;
  }

  public StringBuffer append(char[] x, int start, int len) {
    impl.appendNonNull(data, String.valueOf(x, start, len));
    return this;
  }

  public StringBuffer append(CharSequence x) {
    impl.append(data, x);
    return this;
  }

  public StringBuffer append(CharSequence x, int start, int end) {
    if (x == null) {
      x = "null";
    }
    impl.append(data, x.subSequence(start, end));
    return this;
  }

  public StringBuffer append(double x) {
    impl.append(data, x);
    return this;
  }

  public StringBuffer append(float x) {
    impl.append(data, x);
    return this;
  }

  public StringBuffer append(int x) {
    impl.append(data, x);
    return this;
  }

  public StringBuffer append(long x) {
    impl.appendNonNull(data, String.valueOf(x));
    return this;
  }

  public StringBuffer append(Object x) {
    impl.append(data, x);
    return this;
  }

  public StringBuffer append(String x) {
    impl.append(data, x);
    return this;
  }

  public StringBuffer append(StringBuffer x) {
    impl.append(data, x);
    return this;
  }

  /**
   * This implementation does not track capacity; always returns
   * {@link Integer#MAX_VALUE}.
   */
  public int capacity() {
    return Integer.MAX_VALUE;
  }

  public char charAt(int index) {
    return toString().charAt(index);
  }

  public StringBuffer delete(int start, int end) {
    return replace(start, end, "");
  }

  public StringBuffer deleteCharAt(int start) {
    return delete(start, start + 1);
  }

  /**
   * This implementation does not track capacity; calling this method has no
   * effect.
   */
  @SuppressWarnings("unused")
  public void ensureCapacity(int ignoredCapacity) {
  }

  public void getChars(int srcStart, int srcEnd, char[] dst, int dstStart) {
    String.__checkBounds(length(), srcStart, srcEnd);
    String.__checkBounds(dst.length, dstStart, dstStart + (srcEnd - srcStart));
    String s = toString();
    while (srcStart < srcEnd) {
      dst[dstStart++] = s.charAt(srcStart++);
    }
  }

  public int indexOf(String x) {
    return toString().indexOf(x);
  }

  public int indexOf(String x, int start) {
    return toString().indexOf(x, start);
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
    return replace(index, index, x);
  }

  public int lastIndexOf(String s) {
    return toString().lastIndexOf(s);
  }

  public int lastIndexOf(String s, int start) {
    return toString().lastIndexOf(s, start);
  }

  public int length() {
    return impl.length(data);
  }

  public StringBuffer replace(int start, int end, String toInsert) {
    impl.replace(data, start, end, toInsert);
    return this;
  }

  /**
   * Warning! This method is <b>much</b> slower than the JRE implementation. If
   * you need to do character level manipulation, you are strongly advised to
   * use a char[] directly.
   */
  public void setCharAt(int index, char x) {
    replace(index, index + 1, String.valueOf(x));
  }

  public void setLength(int newLength) {
    int oldLength = length();
    if (newLength < oldLength) {
      delete(newLength, oldLength);
    } else if (newLength > oldLength) {
      append(new char[newLength - oldLength]);
    }
  }

  public CharSequence subSequence(int start, int end) {
    return this.substring(start, end);
  }

  public String substring(int begin) {
    return toString().substring(begin);
  }

  public String substring(int begin, int end) {
    return toString().substring(begin, end);
  }

  @Override
  public String toString() {
    return impl.toString(data);
  }

  public void trimToSize() {
  }
}
