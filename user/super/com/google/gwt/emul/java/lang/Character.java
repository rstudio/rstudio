/*
 * Copyright 2006 Google Inc.
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
 * Wraps a native <code>char</code> as an object.
 */
public final class Character {

  public static final int MIN_RADIX = 2;
  public static final int MAX_RADIX = 36;

  public static final char MIN_VALUE = '\u0000';
  public static final char MAX_VALUE = '\uFFFF';

  public static int digit(char c, int radix) {
    if (radix < MIN_RADIX || radix > MAX_RADIX) {
      return -1;
    }

    if (c >= '0' && c <= '9') {
      return c - '0';
    } else if (c >= 'a' && c < ('a' + radix)) {
      return c - 'a';
    } else if (c >= 'A' && c < ('A' + radix)) {
      return c - 'A';
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

  public static native boolean isDigit(char c) /*-{
    return (null != String.fromCharCode(c).match(/\d/));
  }-*/;

  public static native boolean isLetter(char c) /*-{
    return (null != String.fromCharCode(c).match(/[A-Z]/i));
  }-*/;

  public static native boolean isLetterOrDigit(char c) /*-{
    return (null != String.fromCharCode(c).match(/[A-Z\d]/i));
  }-*/;

  public static boolean isLowerCase(char c) {
    return toLowerCase(c) == c && isLetter(c);
  }

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

  public static boolean isUpperCase(char c) {
    return toUpperCase(c) == c && isLetter(c);
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

  private final char value;

  public Character(char value) {
    this.value = value;
  }

  public char charValue() {
    return value;
  }

  public int compareTo(Character c) {
    if (value < c.value) {
      return -1;
    } else if (value > c.value) {
      return 1;
    } else {
      return 0;
    }
  }

  public int compareTo(Object o) {
    return compareTo((Character) o);
  }

  public boolean equals(Object o) {
    return (o instanceof Character) && (((Character) o).value == value);
  }

  public int hashCode() {
    return value;
  }

  public String toString() {
    return String.valueOf(value);
  }
}
