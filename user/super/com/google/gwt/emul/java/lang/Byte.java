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
 * Wraps native <code>byte</code> as an object.
 */
public final class Byte extends Number implements Comparable {
  public static final byte MIN_VALUE = (byte) 0x80;
  public static final byte MAX_VALUE = (byte) 0x7F;

  public static Byte decode(String s) throws NumberFormatException {
    long x = __parseLongInfer(s);
    if (__isLongNaN(x)) {
      throw new NumberFormatException(s);
    } else {
      return new Byte((byte) x);
    }
  }

  public static byte parseByte(String s) throws NumberFormatException {
    final int baseTen = 10;
    return parseByte(s, baseTen);
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

  public static String toString(byte b) {
    return String.valueOf(b);
  }

  public static Byte valueOf(String s) throws NumberFormatException {
    return new Byte(Byte.parseByte(s));
  }

  public static Byte valueOf(String s, int radix) throws NumberFormatException {
    return new Byte(Byte.parseByte(s, radix));
  }

  private final byte value;

  public Byte(byte value) {
    this.value = value;
  }

  public Byte(String s) {
    value = parseByte(s);
  }

  public byte byteValue() {
    return value;
  }

  public int compareTo(Byte b) {
    if (value < b.value) {
      return -1;
    } else if (value > b.value) {
      return 1;
    } else {
      return 0;
    }
  }

  public int compareTo(Object o) {
    return compareTo((Byte) o);
  }

  public double doubleValue() {
    return value;
  }

  public boolean equals(Object o) {
    return (o instanceof Byte) && (((Byte) o).value == value);
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
