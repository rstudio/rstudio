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
  public static final int SIZE = 64;

  // Box values according to JLS - between -128 and 127
  private static Long[] boxedValues = new Long[256];
  
  public static int bitCount (long i) {
    int cnt = 0;
    for (long q = MIN_VALUE; q > 0; q = q >> 1) {
      if ((q & i) != 0) {
        cnt++;
      }
    }
    return cnt;
  }

  public static Long decode(String s) throws NumberFormatException {
    return new Long(__decodeAndValidateLong(s, MIN_VALUE, MAX_VALUE));
  }

  /**
   * @skip Here for shared implementation with Arrays.hashCode
   */
  public static int hashCode(long l) {
    return (int) l;
  }

  public static long highestOneBit (long i) {
    if (i < 0) {
      return MIN_VALUE;
   } else {
      long rtn;
      for (rtn = 0x4000000000000000L; (rtn >> 1) > i; rtn = rtn >> 1) {
        // loop down until smaller
      }
      return rtn;
    }
  }

  public static long lowestOneBit (long i) {
    if (i == 0) {
      return SIZE;
    } else {
      long r = 1;
      while ((r & i) != 0) {
        r = r << 1;
      }
      return r;
    }
  }

  public static int numberOfLeadingZeros(long i) {
    if (i < 0) {
      return 0;
    } else if (i == 0) {
      return SIZE;
    } else {
      return SIZE - 1 - (int)Math.floor(Math.log((double)i) / Math.log(2.0d));
    }
  }

  public static int numberOfTrailingZeros(long i) {
    if (i < 0) {
      return 0;
    } else if (i == 0) {
      return SIZE;
    } else {
      int rtn = 0;
      for (int r = 1; (r & i) != 0; r = r * 2) {
        rtn++;
      }
      return rtn;
    }
  }

  public static long parseLong(String s) throws NumberFormatException {
    return parseLong(s, 10);
  }

  public static long parseLong(String s, int radix)
      throws NumberFormatException {
    return __parseAndValidateLong(s, radix, MIN_VALUE, MAX_VALUE);
  }

  public static long reverse (long i) {
    long acc = 0;
    long front = MIN_VALUE;
    int back = 1;
    int swing = SIZE - 1;
    while (swing > 15) {
      acc = acc | ((i & front) >> swing) | ((i & back) << swing);
      swing--;
      front = front >> 1;
      back = back << 1;
    }
    return acc;
  }

  public static long reverseBytes (long i) {
    return ((i & 0xffL) << 56) | ((i & 0xff00L) << 40)
      | ((i & 0xff0000L) << 24) | ((i & 0xff000000L) << 8)
      | ((i & 0xff00000000L) >> 8) | ((i & 0xff0000000000L) >> 24)
      | ((i & 0xff000000000000L) >> 40) | ((i & 0xff00000000000000L) >> 56);
  }

  public static long rotateLeft (long i, int distance) {
    while (distance-- > 0) {
      i = i << 1 | ((i < 0) ? 1 : 0);
    }
    return i;
  }

  public static long rotateRight (long i, int distance) {
    while (distance-- > 0) {
      i = ((i & 1) == 0 ? 0 : 0x80000000) | i >> 1;
    }
    return i;
  }

  public static int signum (long i) {
    if (i == 0) {
      return 0;
    } else if (i < 0) {
     return -1;
    } else {
      return 1;
    }
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

  public static Long valueOf (long i) {
    if (i > -129 && i < 128) {
      int rebase = (int) i + 128;
      if (boxedValues[rebase] == null) {
        boxedValues[rebase] = new Long(i);
      }
      return boxedValues[rebase];
    }
    return new Long(i);
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
    return hashCode(value);
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
