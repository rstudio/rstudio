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
 * Wraps a primitive <code>double</code> as an object.
 */
public final class Double extends Number implements Comparable {
  public static final double MAX_VALUE = 1.7976931348623157e+308;
  public static final double MIN_VALUE = 4.9e-324;
  public static final double NaN = 0d / 0d;
  public static final double NEGATIVE_INFINITY = -1d / 0d;
  public static final double POSITIVE_INFINITY = 1d / 0d;

  public static int compare(double x, double y) {
    if (x < y) {
      return -1;
    } else if (x > y) {
      return 1;
    } else {
      return 0;
    }
  }

  public static native boolean isInfinite(double x) /*-{
    return !isFinite(x);
  }-*/;

  public static native boolean isNaN(double x) /*-{
    return isNaN(x);
  }-*/;

  public static double parseDouble(String s) throws NumberFormatException {
    double x = __parseDouble(s);
    if (isNaN(x)) {
      throw new NumberFormatException(s);
    } else {
      return x;
    }
  }

  public static String toString(double b) {
    return String.valueOf(b);
  }

  public static Double valueOf(String s) throws NumberFormatException {
    return new Double(Double.parseDouble(s));
  }

  private final double value;

  public Double(double value) {
    this.value = value;
  }

  public Double(String s) {
    value = parseDouble(s);
  }

  public byte byteValue() {
    return (byte) value;
  }

  public int compareTo(Double b) {
    if (value < b.value) {
      return -1;
    } else if (value > b.value) {
      return 1;
    } else {
      return 0;
    }
  }

  public int compareTo(Object o) {
    return compareTo((Double) o);
  }

  public double doubleValue() {
    return value;
  }

  public boolean equals(Object o) {
    return (o instanceof Double) && (((Double) o).value == value);
  }

  public float floatValue() {
    return (float) value;
  }

  /**
   * Performance caution: using Double objects as map keys is not recommended.
   * Using double values as keys is generally a bad idea due to difficulty
   * determining exact equality. In addition, there is no efficient JavaScript
   * equivalent of <code>doubleToIntBits</code>. As a result, this method
   * computes a hash code by truncating the whole number portion of the double,
   * which may lead to poor performance for certain value sets if Doubles are
   * used as keys in a {@link java.util.HashMap}.
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
