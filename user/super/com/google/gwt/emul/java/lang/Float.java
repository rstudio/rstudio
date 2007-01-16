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
 * Wraps a primitve <code>float</code> as an object.
 */
public final class Float extends Number implements Comparable {
  public static final float MAX_VALUE = 3.4028235e+38f;
  public static final float MIN_VALUE = 1.4e-45f;
  public static final float NaN = 0f / 0f;
  public static final float NEGATIVE_INFINITY = -1f / 0f;
  public static final float POSITIVE_INFINITY = 1f / 0f;

  public static int compare(float x, float y) {
    if (x < y) {
      return -1;
    } else if (x > y) {
      return 1;
    } else {
      return 0;
    }
  }

  public static native boolean isInfinite(float x) /*-{
    return !isFinite(x);
  }-*/;

  public static native boolean isNaN(float x) /*-{
    return isNaN(x);
  }-*/;

  public static float parseFloat(String s) throws NumberFormatException {
    float x = __parseFloat(s);
    if (isNaN(x)) {
      throw new NumberFormatException(s);
    } else {
      return x;
    }
  }

  public static String toString(float b) {
    return String.valueOf(b);
  }

  public static Float valueOf(String s) throws NumberFormatException {
    return new Float(Float.parseFloat(s));
  }

  private final float value;

  public Float(float value) {
    this.value = value;
  }

  public Float(String s) {
    value = parseFloat(s);
  }

  public byte byteValue() {
    return (byte) value;
  }

  public int compareTo(Float b) {
    if (value < b.value) {
      return -1;
    } else if (value > b.value) {
      return 1;
    } else {
      return 0;
    }
  }

  public int compareTo(Object o) {
    return compareTo((Float) o);
  }

  public double doubleValue() {
    return value;
  }

  public boolean equals(Object o) {
    return (o instanceof Float) && (((Float) o).value == value);
  }

  public float floatValue() {
    return value;
  }

  /**
   * Performance caution: using Float objects as map keys is not recommended.
   * Using floating point values as keys is generally a bad idea due to
   * difficulty determining exact equality. In addition, there is no efficient
   * JavaScript equivalent of <code>floatToIntBits</code>. As a result, this
   * method computes a hash code by truncating the whole number portion of the
   * float, which may lead to poor performance for certain value sets if Floats
   * are used as keys in a {@link java.util.HashMap}.
   */
  public int hashCode() {
    return (int) value;
  }

  public int intValue() {
    return (int) value;
  }

  public boolean isInfinite() {
    return isInfinite(value);
  }

  public boolean isNaN() {
    return isNaN(value);
  }

  public long longValue() {
    return (long) value;
  }

  public short shortValue() {
    return (short) value;
  }

  public String toString() {
    return toString(value);
  }

}
