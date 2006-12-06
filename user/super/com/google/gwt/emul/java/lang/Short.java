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
 * Wraps a primitive <code>short</code> as an object.
 */
public final class Short extends Number implements Comparable {
  public static final short MIN_VALUE = (short) 0x8000;
  public static final short MAX_VALUE = (short) 0x7fff;

  private final short fValue;

  public Short(short value) {
    fValue = value;
  }

  public Short(String s) {
    fValue = parseShort(s);
  }

  public int compareTo(Object o) {
    return compareTo((Short) o);
  }

  public int hashCode() {
    return fValue;
  }

  public int compareTo(Short b) {
    if (fValue < b.fValue) {
      return -1;
    } else if (fValue > b.fValue) {
      return 1;
    } else {
      return 0;
    }
  }

  public boolean equals(Object o) {
    return (o instanceof Short) && (((Short) o).fValue == fValue);
  }

  public static String toString(short b) {
    return String.valueOf(b);
  }

  public String toString() {
    return toString(fValue);
  }

  public static Short decode(String s) throws NumberFormatException {
    long x = __parseLongInfer(s);
    if (__isLongNaN(x)) {
      throw new NumberFormatException(s);
    } else {
      return new Short((short) x);
    }
  }

  public static Short valueOf(String s) throws NumberFormatException {
    return new Short(Short.parseShort(s));
  }

  public static Short valueOf(String s, int radix) throws NumberFormatException {
    return new Short(Short.parseShort(s, radix));
  }

  public byte byteValue() {
    return (byte) fValue;
  }

  public double doubleValue() {
    return fValue;
  }

  public float floatValue() {
    return fValue;
  }

  public int intValue() {
    return fValue;
  }

  public long longValue() {
    return fValue;
  }

  public short shortValue() {
    return fValue;
  }

  public static short parseShort(String s, int radix)
      throws NumberFormatException {
    long x = __parseLongRadix(s, radix);
    if (__isLongNaN(x)) {
      throw new NumberFormatException(s);
    } else {
      return (short) x;
    }
  }

  public static short parseShort(String s) throws NumberFormatException {
    return parseShort(s, 10);
  }
}
