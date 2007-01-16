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
 * Wraps a primitive <code>int</code> as an object.
 */
public final class Integer extends Number implements Comparable {
  public static final int MIN_VALUE = 0x80000000;
  public static final int MAX_VALUE = 0x7fffffff;

  public static Integer decode(String s) throws NumberFormatException {
    long x = __parseLongInfer(s);
    if (__isLongNaN(x)) {
      throw new NumberFormatException(s);
    } else {
      return new Integer((int) x);
    }
  }

  public static int parseInt(String s) throws NumberFormatException {
    return parseInt(s, 10);
  }

  public static int parseInt(String s, int radix) throws NumberFormatException {
    long x = __parseLongRadix(s, radix);
    if (__isLongNaN(x)) {
      throw new NumberFormatException(s);
    } else {
      return (int) x;
    }
  }

  public static String toBinaryString(int x) {
    return Long.toBinaryString(x);
  }

  public static String toHexString(int x) {
    return Long.toHexString(x);
  }

  public static String toString(int b) {
    return String.valueOf(b);
  }

  public static Integer valueOf(String s) throws NumberFormatException {
    return new Integer(Integer.parseInt(s));
  }

  public static Integer valueOf(String s, int radix)
      throws NumberFormatException {
    return new Integer(Integer.parseInt(s, radix));
  }

  private final int value;

  public Integer(int value) {
    this.value = value;
  }

  public Integer(String s) {
    value = parseInt(s);
  }

  public byte byteValue() {
    return (byte) value;
  }

  public int compareTo(Integer b) {
    if (value < b.value) {
      return -1;
    } else if (value > b.value) {
      return 1;
    } else {
      return 0;
    }
  }

  public int compareTo(Object o) {
    return compareTo((Integer) o);
  }

  public double doubleValue() {
    return value;
  }

  public boolean equals(Object o) {
    return (o instanceof Integer) && (((Integer) o).value == value);
  }

  public float floatValue() {
    return value;
  }

  public int hashCode() {
    return value;
  }

  public int intValue() {
    return value;
  }

  public long longValue() {
    return value;
  }

  public short shortValue() {
    return (short) value;
  }

  public String toString() {
    return toString(value);
  }

}
