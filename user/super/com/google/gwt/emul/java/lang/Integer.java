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
 * Wraps a primitive <code>int</code> as an object.
 */
public final class Integer extends Number implements Comparable<Integer> {

  public static final int MIN_VALUE = 0x80000000;
  public static final int MAX_VALUE = 0x7fffffff;
  public static final int SIZE = 32;

  // Box values according to JLS - between -128 and 127
  private static Integer[] boxedValues = new Integer[256];

  public static int bitCount(int x) {
    // Courtesy the University of Kentucky
    // http://aggregate.org/MAGIC/#Population%20Count%20(Ones%20Count)
    x -= ((x >> 1) & 0x55555555);
    x = (((x >> 2) & 0x33333333) + (x & 0x33333333));
    x = (((x >> 4) + x) & 0x0f0f0f0f);
    x += (x >> 8);
    x += (x >> 16);
    return x & 0x0000003f;
  }

  public static Integer decode(String s) throws NumberFormatException {
    return new Integer((int) __decodeAndValidateLong(s, MIN_VALUE, MAX_VALUE));
  }

  /**
   * @skip
   * 
   * Here for shared implementation with Arrays.hashCode
   */
  public static int hashCode(int i) {
    return i;
  }

  public static int highestOneBit(int i) {
    if (i < 0) {
      return MIN_VALUE;
    } else if (i == 0) {
      return 0;
    } else {
      int rtn;
      for (rtn = 0x40000000; (rtn & i) == 0; rtn = rtn >> 1) {
        // loop down until smaller
      }
      return rtn;
    }
  }

  public static int lowestOneBit(int i) {
    if (i == 0) {
      return 0;
    } else if (i == Integer.MIN_VALUE) {
      return 0x80000000;
    } else {
      int r = 1;
      while ((r & i) == 0) {
        r = r * 2;
      }
      return r;
    }
  }

  public static int numberOfLeadingZeros(int i) {
    if (i < 0) {
      return 0;
    } else if (i == 0) {
      return SIZE;
    } else {
      return SIZE - 1 - (int) Math.floor(Math.log(i) / Math.log(2.0d));
    }
  }

  public static int numberOfTrailingZeros(int i) {
    if (i == 0) {
      return 32;
    } else {
      int rtn = 0;
      for (int r = 1; (r & i) == 0; r = r * 2) {
        rtn++;
      }
      return rtn;
    }
  }

  public static int parseInt(String s) throws NumberFormatException {
    return parseInt(s, 10);
  }

  public static int parseInt(String s, int radix) throws NumberFormatException {
    return (int) __parseAndValidateLong(s, radix, MIN_VALUE, MAX_VALUE);
  }

  public static int reverse(int i) {
    int ui = i & 0x7fffffff; // avoid sign extension
    int acc = 0;  
    int front = 0x80000000;
    int back = 1;
    int swing = 31;
    while (swing > 0) {
      acc = acc | ((ui & front) >> swing) | ((ui & back) << swing);
      swing -= 2;
      front = front >> 1;
      back = back << 1;
    }
    if (i < 0) {
      acc = acc | 0x1; // restore the real value of 0x80000000
    }
    return acc;
  }

  public static int reverseBytes(int i) {
    return ((i & 0xff) << 24) | ((i & 0xff00) << 8) | ((i & 0xff0000) >> 8)
        | ((i & 0xff000000) >> 24);
  }

  public static int rotateLeft(int i, int distance) {
    while (distance-- > 0) {
      i = i << 1 | ((i < 0) ? 1 : 0);
    }
    return i;
  }

  public static int rotateRight(int i, int distance) {
    int ui = i & 0x7fffffff; // avoid sign extension
    int carry = (i < 0) ? 0x40000000 : 0; // 0x80000000 rightshifted 1
    while (distance-- > 0) {
      int nextcarry = ui & 1;
      ui = carry | (ui >> 1);
      carry = (nextcarry == 0) ? 0 : 0x40000000;
    }
    if (carry != 0) {
      ui = ui | 0x80000000;
    }
    return ui;
  }

  public static int signum(int i) {
    if (i == 0) {
      return 0;
    } else if (i < 0) {
      return -1;
    } else {
      return 1;
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

  public static Integer valueOf(int i) {
    if (i > -129 && i < 128) {
      int rebase = i + 128;
      if (boxedValues[rebase] == null) {
        boxedValues[rebase] = new Integer(i);
      }
      return boxedValues[rebase];
    }
    return new Integer(i);
  }

  public static Integer valueOf(String s) throws NumberFormatException {
    return new Integer(Integer.parseInt(s));
  }

  public static Integer valueOf(String s, int radix)
      throws NumberFormatException {
    return new Integer(Integer.parseInt(s, radix));
  }

  private final transient int value;

  public Integer(int value) {
    this.value = value;
  }

  public Integer(String s) {
    value = parseInt(s);
  }

  @Override
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

  @Override
  public double doubleValue() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    return (o instanceof Integer) && (((Integer) o).value == value);
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
    return (short) value;
  }

  @Override
  public String toString() {
    return toString(value);
  }

}
