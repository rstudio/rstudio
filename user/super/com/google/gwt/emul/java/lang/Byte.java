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
 * Wraps native byte as an object.
 */
public final class Byte extends Number implements Comparable {
  public static final byte MIN_VALUE = (byte) 0x80;
  public static final byte MAX_VALUE = (byte) 0x7F;

  private final byte fValue;

  public Byte(byte value) {
    fValue = value;
  }

  public Byte(String s) {
    fValue = parseByte(s);
  }

  public int compareTo(Object o) {
    return compareTo((Byte) o);
  }

  public int hashCode() {
    return fValue;
  }

  public int compareTo(Byte b) {
    if (fValue < b.fValue) {
      return -1;
    } else if (fValue > b.fValue) {
      return 1;
    } else {
      return 0;
    }
  }

  public boolean equals(Object o) {
    return (o instanceof Byte) && (((Byte) o).fValue == fValue);
  }

  public static String toString(byte b) {
    return String.valueOf(b);
  }

  public String toString() {
    return toString(fValue);
  }

  public static Byte decode(String s) throws NumberFormatException {
    long x = __parseLongInfer(s);
    if (__isLongNaN(x)) {
      throw new NumberFormatException(s);
    } else {
      return new Byte((byte) x);
    }
  }

  public static Byte valueOf(String s) throws NumberFormatException {
    return new Byte(Byte.parseByte(s));
  }

  public static Byte valueOf(String s, int radix) throws NumberFormatException {
    return new Byte(Byte.parseByte(s, radix));
  }

  public byte byteValue() {
    return fValue;
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

  public static byte parseByte(String s, int radix)
      throws NumberFormatException {
    long x = __parseLongRadix(s, radix);
    if (__isLongNaN(x)) {
      throw new NumberFormatException(s);
    } else {
      return (byte) x;
    }
  }

  public static byte parseByte(String s) throws NumberFormatException {
    final int baseTen = 10;
    return parseByte(s, baseTen);
  }
}
