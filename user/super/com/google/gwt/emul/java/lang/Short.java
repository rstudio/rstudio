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
 * Wraps a primitive <code>short</code> as an object.
 */
public final class Short extends Number implements Comparable<Short> {

  public static final short MIN_VALUE = (short) 0x8000;
  public static final short MAX_VALUE = (short) 0x7fff;
  public static final int SIZE = 16;
  public static final Class<Short> TYPE = short.class;

  /**
   * Use nested class to avoid clinit on outer.
   */
  private static class BoxedValues {
    // Box values according to JLS - between -128 and 127
    private static Short[] boxedValues = new Short[256];
  }

  public static Short decode(String s) throws NumberFormatException {
    return Short.valueOf((short) __decodeAndValidateInt(s, MIN_VALUE, MAX_VALUE));
  }

  /**
   * @skip Here for shared implementation with Arrays.hashCode
   */
  public static int hashCode(short s) {
    return s;
  }

  public static short parseShort(String s) throws NumberFormatException {
    return parseShort(s, 10);
  }

  public static short parseShort(String s, int radix)
      throws NumberFormatException {
    return (short) __parseAndValidateInt(s, radix, MIN_VALUE, MAX_VALUE);
  }

  public static short reverseBytes(short s) {
    return (short) (((s & 0xff) << 8) | ((s & 0xff00) >> 8));
  }

  public static String toString(short b) {
    return String.valueOf(b);
  }

  public static Short valueOf(short s) {
    if (s > -129 && s < 128) {
      int rebase = s + 128;
      Short result = BoxedValues.boxedValues[rebase];
      if (result == null) {
        result = BoxedValues.boxedValues[rebase] = new Short(s);
      }
      return result;
    }
    return new Short(s);
  }

  public static Short valueOf(String s) throws NumberFormatException {
    return Short.valueOf(Short.parseShort(s));
  }

  public static Short valueOf(String s, int radix) throws NumberFormatException {
    return Short.valueOf(Short.parseShort(s, radix));
  }

  private final transient short value;

  public Short(short value) {
    this.value = value;
  }

  public Short(String s) {
    value = parseShort(s);
  }

  @Override
  public byte byteValue() {
    return (byte) value;
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

  @Override
  public double doubleValue() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    return (o instanceof Short) && (((Short) o).value == value);
  }

  @Override
  public float floatValue() {
    return value;
  }

  @Override
  public int hashCode() {
    return hashCode(value);
  }

  @Override
  public int intValue() {
    return value;
  }

  @Override
  public long longValue() {
    return value;
  }

  @Override
  public short shortValue() {
    return value;
  }

  @Override
  public String toString() {
    return toString(value);
  }
}
