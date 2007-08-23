/*
 * Copyright 2007 Google Inc.
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
 * Wraps a primitive <code>long</code> as an object.
 */
public final class Long extends Number implements Comparable<Long> {
  public static final long MIN_VALUE = 0x8000000000000000L;
  public static final long MAX_VALUE = 0x7fffffffffffffffL;

  public static Long decode(String s) throws NumberFormatException {
    return new Long(__decodeAndValidateLong(s, MIN_VALUE, MAX_VALUE));
  }

  public static long parseLong(String s) throws NumberFormatException {
    return parseLong(s, 10);
  }

  public static long parseLong(String s, int radix)
      throws NumberFormatException {
    return __parseAndValidateLong(s, radix, MIN_VALUE, MAX_VALUE);
  }

  public static String toBinaryString(long x) {
    if (x == 0) {
      return "0";
    }
    String binStr = "";
    while (x != 0) {
      int bit = (int) x & 0x1;
      binStr = __hexDigits[bit] + binStr;
      x = x >>> 1;
    }
    return binStr;
  }

  public static String toHexString(long x) {
    if (x == 0) {
      return "0";
    }
    String hexStr = "";
    while (x != 0) {
      int nibble = (int) x & 0xF;
      hexStr = __hexDigits[nibble] + hexStr;
      x = x >>> 4;
    }
    return hexStr;
  }

  public static String toString(long b) {
    return String.valueOf(b);
  }

  public static Long valueOf(String s) throws NumberFormatException {
    return new Long(Long.parseLong(s));
  }

  public static Long valueOf(String s, int radix) throws NumberFormatException {
    return new Long(Long.parseLong(s, radix));
  }

  private final transient long value;

  public Long(long value) {
    this.value = value;
  }

  public Long(String s) {
    value = parseLong(s);
  }

  @Override
  public byte byteValue() {
    return (byte) value;
  }

  public int compareTo(Long b) {
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
    return (o instanceof Long) && (((Long) o).value == value);
  }

  @Override
  public float floatValue() {
    return value;
  }

  @Override
  public int hashCode() {
    return (int) value;
  }

  @Override
  public int intValue() {
    return (int) value;
  }

  @Override
  public long longValue() {
    return value;
  }

  @Override
  public short shortValue() {
    return (short) value;
  }

  @Override
  public String toString() {
    return toString(value);
  }

}
