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

  private final int fValue;

  public Integer(int value) {
    fValue = value;
  }

  public Integer(String s) {
    fValue = parseInt(s);
  }

  public byte byteValue() {
    return (byte) fValue;
  }

  public int compareTo(Integer b) {
    if (fValue < b.fValue) {
      return -1;
    } else if (fValue > b.fValue) {
      return 1;
    } else {
      return 0;
    }
  }

  public int compareTo(Object o) {
    return compareTo((Integer) o);
  }

  public double doubleValue() {
    return fValue;
  }

  public boolean equals(Object o) {
    return (o instanceof Integer) && (((Integer) o).fValue == fValue);
  }

  public float floatValue() {
    return fValue;
  }

  public int hashCode() {
    return fValue;
  }

  public int intValue() {
    return fValue;
  }

  public long longValue() {
    return fValue;
  }

  public short shortValue() {
    return (short) fValue;
  }

  public String toString() {
    return toString(fValue);
  }

}
