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

import java.io.Serializable;

/**
 * Wraps a native <code>char</code> as an object.
 *
 * TODO(jat): many of the classification methods implemented here are not
 * correct in that they only handle ASCII characters, and many other methods
 * are not currently implemented.  I think the proper approach is to introduce * a deferred binding parameter which substitutes an implementation using
 * a fully-correct Unicode character database, at the expense of additional
 * data being downloaded.  That way developers that need the functionality
 * can get it without those who don't need it paying for it.
 *
 * <pre>
 * The following methods are still not implemented -- most would require Unicode
 * character db to be useful:
 *  - digit / is* / to*(int codePoint)
 *  - isDefined(char)
 *  - isIdentifierIgnorable(char)
 *  - isJavaIdentifierPart(char)
 *  - isJavaIdentifierStart(char)
 *  - isJavaLetter(char) -- deprecated, so probably not
 *  - isJavaLetterOrDigit(char) -- deprecated, so probably not
 *  - isISOControl(char)
 *  - isMirrored(char)
 *  - isSpaceChar(char)
 *  - isTitleCase(char)
 *  - isUnicodeIdentifierPart(char)
 *  - isUnicodeIdentifierStart(char)
 *  - isWhitespace(char)
 *  - getDirectionality(*)
 *  - getNumericValue(*)
 *  - getType(*)
 *  - reverseBytes(char) -- any use for this at all in the browser?
 *  - toTitleCase(*)
 *  - all the category constants for classification
 *
 * The following do not properly handle characters outside of ASCII:
 *  - digit(char c, int radix)
 *  - isDigit(char c)
 *  - isLetter(char c)
 *  - isLetterOrDigit(char c)
 *  - isLowerCase(char c)
 *  - isUpperCase(char c)
 * </pre>
 */
public final class Character implements Comparable<Character>, Serializable {
  /**
   * Helper class to share code between implementations, by making a char
   * array look like a CharSequence.
   */
  static class CharSequenceAdapter implements CharSequence {
    private char[] charArray;
    private int start;
    private int end;

    public CharSequenceAdapter(char[] charArray) {
      this(charArray, 0, charArray.length);
    }

    public CharSequenceAdapter(char[] charArray, int start, int end) {
      this.charArray = charArray;
      this.start = start;
      this.end = end;
    }

    public char charAt(int index) {
      return charArray[index + start];
    }

    public int length() {
      return end - start;
    }

    public java.lang.CharSequence subSequence(int start, int end) {
      return new CharSequenceAdapter(charArray, this.start + start,
          this.start + end);
    }
  }

  /**
   * Use nested class to avoid clinit on outer.
   */
  private static class BoxedValues {
    // Box values according to JLS - from \u0000 to \u007f
    private static Character[] boxedValues = new Character[128];
  }

  public static final Class<Character> TYPE = Character.class;
  public static final int MIN_RADIX = 2;

  public static final int MAX_RADIX = 36;
  public static final char MIN_VALUE = '\u0000';

  public static final char MAX_VALUE = '\uFFFF';
  public static final char MIN_SURROGATE = '\uD800';
  public static final char MAX_SURROGATE = '\uDFFF';
  public static final char MIN_LOW_SURROGATE = '\uDC00';
  public static final char MAX_LOW_SURROGATE = '\uDFFF';
  public static final char MIN_HIGH_SURROGATE = '\uD800';

  public static final char MAX_HIGH_SURROGATE = '\uDBFF';
  public static final int MIN_SUPPLEMENTARY_CODE_POINT = 0x10000;
  public static final int MIN_CODE_POINT = 0x0000;

  public static final int MAX_CODE_POINT = 0x10FFFF;

  public static final int SIZE = 16;

  public static int charCount(int codePoint) {
    return codePoint >= MIN_SUPPLEMENTARY_CODE_POINT ? 2 : 1;
  }

  public static int codePointAt(char[] a, int index) {
    return codePointAt(new CharSequenceAdapter(a), index, a.length);
  }

  public static int codePointAt(char[] a, int index, int limit) {
    return codePointAt(new CharSequenceAdapter(a), index, limit);
  }

  public static int codePointAt(CharSequence seq, int index) {
    return codePointAt(seq, index, seq.length());
  }

  public static int codePointBefore(char[] a, int index) {
    return codePointBefore(new CharSequenceAdapter(a), index, 0);
  }

  public static int codePointBefore(char[] a, int index, int start) {
    return codePointBefore(new CharSequenceAdapter(a), index, start);
  }

  public static int codePointBefore(CharSequence cs, int index) {
    return codePointBefore(cs, index, 0);
  }

  public static int codePointCount(char[] a, int offset, int count) {
    return codePointCount(new CharSequenceAdapter(a), offset, offset + count);
  }

  public static int codePointCount(CharSequence seq, int beginIndex,
      int endIndex) {
    int count = 0;
    for (int idx = beginIndex; idx < endIndex; ) {
      char ch = seq.charAt(idx++);
      if (isHighSurrogate(ch) && idx < endIndex
          && (isLowSurrogate(seq.charAt(idx)))) {
        // skip the second char of surrogate pairs
        ++idx;
      }
      ++count;
    }
    return count;
  }

  /*
   * TODO: correct Unicode handling.
   */
  public static int digit(char c, int radix) {
    if (radix < MIN_RADIX || radix > MAX_RADIX) {
      return -1;
    }

    if (c >= '0' && c < '0' + Math.min(radix, 10)) {
      return c - '0';
    }

    // The offset by 10 is to re-base the alpha values
    if (c >= 'a' && c < (radix + 'a' - 10)) {
      return c - 'a' + 10;
    }

    if (c >= 'A' && c < (radix + 'A' - 10)) {
      return c - 'A' + 10;
    }

    return -1;
  }

  public static char forDigit(int digit, int radix) {
    if (radix < MIN_RADIX || radix > MAX_RADIX) {
      return 0;
    }

    if (digit < 0 || digit >= radix) {
      return 0;
    }

    final int baseTenMax = 10;
    if (digit < baseTenMax) {
      return (char) ('0' + digit);
    } else {
      return (char) ('a' + digit - baseTenMax);
    }
  }

  /**
   * @skip
   *
   * public for shared implementation with Arrays.hashCode
   */
  public static int hashCode(char c) {
    return c;
  }

  /*
   * TODO: correct Unicode handling.
   */
  public static native boolean isDigit(char c) /*-{
    return (null != String.fromCharCode(c).match(/\d/));
  }-*/;

  public static boolean isHighSurrogate(char ch) {
    return ch >= MIN_HIGH_SURROGATE && ch <= MAX_HIGH_SURROGATE;
  }

  /*
   * TODO: correct Unicode handling.
   */
  public static native boolean isLetter(char c) /*-{
    return (null != String.fromCharCode(c).match(/[A-Z]/i));
  }-*/;

  /*
   * TODO: correct Unicode handling.
   */
  public static native boolean isLetterOrDigit(char c) /*-{
    return (null != String.fromCharCode(c).match(/[A-Z\d]/i));
  }-*/;

  /*
   * TODO: correct Unicode handling.
   */
  public static boolean isLowerCase(char c) {
    return toLowerCase(c) == c && isLetter(c);
  }

  public static boolean isLowSurrogate(char ch) {
    return ch >= MIN_LOW_SURROGATE && ch <= MAX_LOW_SURROGATE;
  }

  /**
   * Deprecated - see isWhitespace(char).
   */
  @Deprecated
  public static boolean isSpace(char c) {
    switch (c) {
      case ' ':
        return true;
      case '\n':
        return true;
      case '\t':
        return true;
      case '\f':
        return true;
      case '\r':
        return true;
      default:
        return false;
    }
  }

  public static boolean isSupplementaryCodePoint(int codePoint) {
    return codePoint >= MIN_SUPPLEMENTARY_CODE_POINT && codePoint <= MAX_CODE_POINT;
  }

  public static boolean isSurrogatePair(char highSurrogate, char lowSurrogate) {
    return isHighSurrogate(highSurrogate) && isLowSurrogate(lowSurrogate);
  }
  
  /*
   * TODO: correct Unicode handling.
   */
  public static boolean isUpperCase(char c) {
    return toUpperCase(c) == c && isLetter(c);
  }
  
  public static boolean isValidCodePoint(int codePoint) {
    return codePoint >= MIN_CODE_POINT && codePoint <= MAX_CODE_POINT;
  }
  
  public static int offsetByCodePoints(char[] a, int start, int count, int index,
      int codePointOffset) {
    return offsetByCodePoints(new CharSequenceAdapter(a, start, count), index,
        codePointOffset);
  }
  
  public static int offsetByCodePoints(CharSequence seq, int index,
      int codePointOffset) {
    if (codePointOffset < 0) {
      // move backwards
      while (codePointOffset < 0) {
        --index;
        if (Character.isLowSurrogate(seq.charAt(index))
            && Character.isHighSurrogate(seq.charAt(index - 1))) {
          --index;
        }
        ++codePointOffset;
      }
    } else {
      // move forwards
      while (codePointOffset > 0) {
        if (Character.isHighSurrogate(seq.charAt(index))
            && Character.isLowSurrogate(seq.charAt(index + 1))) {
          ++index;
        }
        ++index;
        --codePointOffset;
      }
    }
    return index;
  }

  public static char[] toChars(int codePoint) {
    if (codePoint < 0 || codePoint > MAX_CODE_POINT) {
      throw new IllegalArgumentException();
    }
    if (codePoint >= MIN_SUPPLEMENTARY_CODE_POINT) {
      return new char[] {
          getHighSurrogate(codePoint),
          getLowSurrogate(codePoint),
      };
    } else {
      return new char[] {
          (char) codePoint,
      };
    }
  }

  public static int toChars(int codePoint, char[] dst, int dstIndex) {
    if (codePoint < 0 || codePoint > MAX_CODE_POINT) {
      throw new IllegalArgumentException();
    }
    if (codePoint >= MIN_SUPPLEMENTARY_CODE_POINT) {
      dst[dstIndex++] = getHighSurrogate(codePoint);
      dst[dstIndex] = getLowSurrogate(codePoint);
      return 2;
    } else {
      dst[dstIndex] = (char) codePoint;
      return 1;
    }
  }

  public static int toCodePoint(char highSurrogate, char lowSurrogate) {
    /*
     * High and low surrogate chars have the bottom 10 bits to store the value
     * above MIN_SUPPLEMENTARY_CODE_POINT, so grab those bits and add the
     * offset.
     */
    return MIN_SUPPLEMENTARY_CODE_POINT + ((highSurrogate & 1023) << 10) + (lowSurrogate & 1023);
  }

  public static native char toLowerCase(char c) /*-{
    return String.fromCharCode(c).toLowerCase().charCodeAt(0);
  }-*/;

  public static String toString(char x) {
    return String.valueOf(x);
  }

  public static native char toUpperCase(char c) /*-{
    return String.fromCharCode(c).toUpperCase().charCodeAt(0);
  }-*/;

  public static Character valueOf(char c) {
    if (c < 128) {
      Character result = BoxedValues.boxedValues[c];
      if (result == null) {
        result = BoxedValues.boxedValues[c] = new Character(c);
      }
      return result;
    }
    return new Character(c);
  }

  static int codePointAt(CharSequence cs, int index, int limit) {
    char hiSurrogate = cs.charAt(index++);
    char loSurrogate;
    if (Character.isHighSurrogate(hiSurrogate) && index < limit
        && Character.isLowSurrogate(loSurrogate = cs.charAt(index))) {
      return Character.toCodePoint(hiSurrogate, loSurrogate);
    }
    return hiSurrogate;
  }

  static int codePointBefore(CharSequence cs, int index, int start) {
    char loSurrogate = cs.charAt(--index);
    char highSurrogate;
    if (isLowSurrogate(loSurrogate) && index > start
        && isHighSurrogate(highSurrogate = cs.charAt(index - 1))) {
      return toCodePoint(highSurrogate, loSurrogate);
    }
    return loSurrogate;
  }

  /**
   * Computes the high surrogate character of the UTF16 representation of a
   * non-BMP code point. See {@link getLowSurrogate}.
   * 
   * @param codePoint requested codePoint, required to be >=
   *          MIN_SUPPLEMENTARY_CODE_POINT
   * @return high surrogate character
   */
  static char getHighSurrogate(int codePoint) {
    return (char) (MIN_HIGH_SURROGATE
        + (((codePoint - MIN_SUPPLEMENTARY_CODE_POINT) >> 10) & 1023));
  }

  /**
   * Computes the low surrogate character of the UTF16 representation of a
   * non-BMP code point. See {@link getHighSurrogate}.
   * 
   * @param codePoint requested codePoint, required to be >=
   *          MIN_SUPPLEMENTARY_CODE_POINT
   * @return low surrogate character
   */
  static char getLowSurrogate(int codePoint) {
    return (char) (MIN_LOW_SURROGATE + ((codePoint - MIN_SUPPLEMENTARY_CODE_POINT) & 1023));
  }

  private final transient char value;

  public Character(char value) {
    this.value = value;
  }

  public char charValue() {
    return value;
  }

  public int compareTo(Character c) {
    // JLS specifies that the chars are promoted to int before subtraction.
    return value - c.value;
  }

  @Override
  public boolean equals(Object o) {
    return (o instanceof Character) && (((Character) o).value == value);
  }

  @Override
  public int hashCode() {
    return hashCode(value);
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }
}
