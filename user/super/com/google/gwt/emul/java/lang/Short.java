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

  public static Short decode(String s) throws NumberFormatException {
    long x = __parseLongInfer(s);
    if (__isLongNaN(x)) {
      throw new NumberFormatException(s);
    } else {
      return new Short((short) x);
    }
  }

  public static short parseShort(String s) throws NumberFormatException {
    return parseShort(s, 10);
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

  public static String toString(short b) {
    return String.valueOf(b);
  }

  public static Short valueOf(String s) throws NumberFormatException {
    return new Short(Short.parseShort(s));
  }

  public static Short valueOf(String s, int radix) throws NumberFormatException {
    return new Short(Short.parseShort(s, radix));
  }

  private final short value;

  public Short(short value) {
    this.value = value;
  }

  public Short(String s) {
    value = parseShort(s);
  }

  public byte byteValue() {
    return (byte) value;
  }

  public int compareTo(Object o) {
    return compareTo((Short) o);
  }

  public int compareTo(Short b) {
    if (value < b.value) {
      return -1;
    } else if (value > b.value) {
      return 1;
    } else {
      return 0;
    }
  }

  public double doubleValue() {
    return value;
  }

  public boolean equals(Object o) {
    return (o instanceof Short) && (((Short) o).value == value);
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
    return value;
  }

  public String toString() {
    return toString(value);
  }
}
