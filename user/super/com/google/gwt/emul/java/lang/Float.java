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
 * Wraps a primitve <code>float</code> as an object.
 */
public final class Float extends Number implements Comparable<Float> {
  public static final float MAX_VALUE = 3.4028235e+38f;
  public static final float MIN_VALUE = 1.4e-45f;
  public static final float MAX_EXPONENT = 127;
  public static final float MIN_EXPONENT = -126;
  public static final float MIN_NORMAL = 1.1754943508222875E-38f;
  public static final float NaN = 0f / 0f;
  public static final float NEGATIVE_INFINITY = -1f / 0f;
  public static final float POSITIVE_INFINITY = 1f / 0f;
  public static final int SIZE = 32;

  public static int compare(float x, float y) {
    if (x < y) {
      return -1;
    } else if (x > y) {
      return 1;
    } else {
      return 0;
    }
  }

  /**
   * @skip Here for shared implementation with Arrays.hashCode
   */
  public static int hashCode(float f) {
    return (int) f;
  }

  public static native boolean isInfinite(float x) /*-{
    return !isFinite(x);
  }-*/;

  public static native boolean isNaN(float x) /*-{
    return isNaN(x);
  }-*/;

  public static float parseFloat(String s) throws NumberFormatException {
    return (float) __parseAndValidateDouble(s);
  }

  public static String toString(float b) {
    return String.valueOf(b);
  }

  public static Float valueOf(float f) {
    return new Float(f);
  }

  public static Float valueOf(String s) throws NumberFormatException {
    return new Float(Float.parseFloat(s));
  }

  private final transient float value;

  public Float(double value) {
    this.value = (float) value;
  }

  public Float(float value) {
    this.value = value;
  }

  public Float(String s) {
    value = parseFloat(s);
  }

  @Override
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

  @Override
  public double doubleValue() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    return (o instanceof Float) && (((Float) o).value == value);
  }

  @Override
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
  @Override
  public int hashCode() {
    return hashCode(value);
  }

  @Override
  public int intValue() {
    return (int) value;
  }

  public boolean isInfinite() {
    return isInfinite(value);
  }

  public boolean isNaN() {
    return isNaN(value);
  }

  @Override
  public long longValue() {
    return (long) value;
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
