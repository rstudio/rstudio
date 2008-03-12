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
 * A fast way to create strings using multiple appends. This
 * implementation is optimized for fast appends. Most methods will give expected
 * performance results, with the notable exception of
 * {@link #setCharAt(int, char)}, which is extremely slow and should be avoided
 * if possible.
 */
public class StringBuilder implements CharSequence {

  private static native String join(String[] stringArray) /*-{
    return stringArray.join('');
  }-*/;

  private static native String setLength(String[] stringArray, int length) /*-{
    stringArray.length = length;
  }-*/;

  private int arrayLen = 0;

  private String[] stringArray = new String[0];

  private int stringLength = 0;

  public StringBuilder() {
  }

  public StringBuilder(CharSequence s) {
    this(s.toString());
  }

  /**
   * This implementation does not track capacity; using this constructor is
   * functionally equivalent to using the zero-argument constructor.
   */
  @SuppressWarnings("unused")
  public StringBuilder(int ignoredCapacity) {
  }

  public StringBuilder(String s) {
    append(s);
  }

  public StringBuilder append(boolean x) {
    return append(String.valueOf(x));
  }

  public StringBuilder append(char x) {
    return append(String.valueOf(x));
  }

  public StringBuilder append(char[] x) {
    return append(String.valueOf(x));
  }

  public StringBuilder append(char[] x, int start, int len) {
    return append(String.valueOf(x, start, len));
  }

  public StringBuilder append(CharSequence x) {
    return append(x.toString());
  }

  public StringBuilder append(CharSequence x, int start, int end) {
    return append(x.subSequence(start, end));
  }

  public StringBuilder append(double x) {
    return append(String.valueOf(x));
  }

  public StringBuilder append(float x) {
    return append(String.valueOf(x));
  }

  public StringBuilder append(int x) {
    return append(String.valueOf(x));
  }

  public StringBuilder append(long x) {
    return append(String.valueOf(x));
  }

  public StringBuilder append(Object x) {
    return append(String.valueOf(x));
  }

  public StringBuilder append(String toAppend) {
    // Coerce to "null" if null.
    if (toAppend == null) {
      toAppend = "null";
    }
    int appendLength = toAppend.length();
    if (appendLength > 0) {
      stringArray[arrayLen++] = toAppend;
      stringLength += appendLength;
      /*
       * If we hit 1k elements, let's do a join to reduce the array size. This
       * number was arrived at experimentally through benchmarking.
       */
      if (arrayLen > 1024) {
        toString();
        // Preallocate the next 1024 (faster on FF).
        setLength(stringArray, 1024);
      }
    }
    return this;
  };

  public StringBuilder append(StringBuffer x) {
    return append(String.valueOf(x));
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

  public StringBuilder delete(int start, int end) {
    return replace(start, end, "");
  }

  public StringBuilder deleteCharAt(int start) {
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
    String.__checkBounds(stringLength, srcStart, srcEnd);
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
    return insert(index, chars.toString());
  }

  public StringBuilder insert(int index, CharSequence chars, int start, int end) {
    return insert(index, chars.subSequence(start, end).toString());
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
    return replace(index, index, x);
  }

  public int lastIndexOf(String s) {
    return toString().lastIndexOf(s);
  }

  public int lastIndexOf(String s, int start) {
    return toString().lastIndexOf(s, start);
  }

  public int length() {
    return stringLength;
  }

  public StringBuilder replace(int start, int end, String toInsert) {
    // Get the joined string.
    String s = toString();

    // Build a new buffer in pieces (will throw exceptions).
    stringArray = new String[] {
        s.substring(0, start), toInsert, s.substring(end)};
    arrayLen = 3;

    // Calculate the new string length.
    stringLength += toInsert.length() - (end - start);

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
    int oldLength = stringLength;
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
    /*
     * Normalize the array to exactly one element (even if it's completely
     * empty), so we can unconditionally grab the first element.
     */
    if (arrayLen != 1) {
      setLength(stringArray, arrayLen);
      String s = join(stringArray);
      // Create a new array to allow everything to get GC'd.
      stringArray = new String[] {s};
      arrayLen = 1;
    }
    return stringArray[0];
  }

  public void trimToSize() {
  }
}
