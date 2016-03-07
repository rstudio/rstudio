/*
 * Copyright 2009 Google Inc.
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

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 * INCLUDES MODIFICATIONS BY RICHARD ZSCHECH AS WELL AS GOOGLE.
 */
package java.math;

import static javaemul.internal.InternalPreconditions.checkCriticalArgument;
import static javaemul.internal.InternalPreconditions.checkNotNull;

import java.io.Serializable;

import javaemul.internal.NativeRegExp;

/**
 * Immutable objects describing settings such as rounding mode and digit
 * precision for the numerical operations provided by class {@link BigDecimal}.
 */
public final class MathContext implements Serializable {

  /**
   * A {@code MathContext} which corresponds to the IEEE 754r quadruple decimal
   * precision format: 34 digit precision and {@link RoundingMode#HALF_EVEN}
   * rounding.
   */
  public static final MathContext DECIMAL128 = new MathContext(34,
      RoundingMode.HALF_EVEN);

  /**
   * A {@code MathContext} which corresponds to the IEEE 754r single decimal
   * precision format: 7 digit precision and {@link RoundingMode#HALF_EVEN}
   * rounding.
   */
  public static final MathContext DECIMAL32 = new MathContext(7,
      RoundingMode.HALF_EVEN);

  /**
   * A {@code MathContext} which corresponds to the IEEE 754r double decimal
   * precision format: 16 digit precision and {@link RoundingMode#HALF_EVEN}
   * rounding.
   */
  public static final MathContext DECIMAL64 = new MathContext(16,
      RoundingMode.HALF_EVEN);

  /**
   * A {@code MathContext} for unlimited precision with
   * {@link RoundingMode#HALF_UP} rounding.
   */
  public static final MathContext UNLIMITED = new MathContext(0,
      RoundingMode.HALF_UP);

  /**
   * This is the serialVersionUID used by the sun implementation.
   */
  private static final long serialVersionUID = 5579720004786848255L;

  /**
   * The number of digits to be used for an operation; results are rounded to
   * this precision.
   */
  private int precision;

  /**
   * A {@code RoundingMode} object which specifies the algorithm to be used for
   * rounding.
   */
  private RoundingMode roundingMode;

  /**
   * Constructs a new {@code MathContext} with the specified precision and with
   * the rounding mode {@link RoundingMode#HALF_UP HALF_UP}. If the precision
   * passed is zero, then this implies that the computations have to be
   * performed exact, the rounding mode in this case is irrelevant.
   *
   * @param precision the precision for the new {@code MathContext}.
   * @throws IllegalArgumentException if {@code precision < 0}.
   */
  public MathContext(int precision) {
    this(precision, RoundingMode.HALF_UP);
  }

  /**
   * Constructs a new {@code MathContext} with the specified precision and with
   * the specified rounding mode. If the precision passed is zero, then this
   * implies that the computations have to be performed exact, the rounding mode
   * in this case is irrelevant.
   *
   * @param precision the precision for the new {@code MathContext}.
   * @param roundingMode the rounding mode for the new {@code MathContext}.
   * @throws IllegalArgumentException if {@code precision < 0}.
   * @throws NullPointerException if {@code roundingMode} is {@code null}.
   */
  public MathContext(int precision, RoundingMode roundingMode) {
    checkCriticalArgument(precision >= 0, "Digits < 0");
    checkNotNull(roundingMode, "null RoundingMode");

    this.precision = precision;
    this.roundingMode = roundingMode;
  }

  /**
   * Constructs a new {@code MathContext} from a string. The string has to
   * specify the precision and the rounding mode to be used and has to follow
   * the following syntax:
   * "precision=&lt;precision&gt; roundingMode=&lt;roundingMode&gt;" This is the
   * same form as the one returned by the {@link #toString} method.
   *
   * @param val a string describing the precision and rounding mode for the new
   *          {@code MathContext}.
   * @throws IllegalArgumentException if the string is not in the correct format
   *           or if the precision specified is < 0.
   */
  public MathContext(String val) {
    checkNotNull(val, "null string");

    Object[] extractedValues = (Object[]) createParseRegexp().exec(val);
    if (extractedValues == null || extractedValues.length != 3) {
      throw new IllegalArgumentException("bad string format");
    }

    try {
      this.precision = Integer.parseInt((String) extractedValues[1]);
      // Can use RoundingMode.valueOf here because it is blacklisted in enum obfuscation.
      this.roundingMode = RoundingMode.valueOf((String) extractedValues[2]);
    } catch (RuntimeException re) {
      // Ensure that we only throw IllegalArgumentException for any illegal value.
      throw new IllegalArgumentException("bad string format");
    }

    checkCriticalArgument(this.precision >= 0, "Digits < 0");
  }

  private static native NativeRegExp createParseRegexp() /*-{
    return /^precision=(\d+)\ roundingMode=(\w+)$/;
  }-*/;

  /* Public Methods */

  /**
   * Returns true if x is a {@code MathContext} with the same precision setting
   * and the same rounding mode as this {@code MathContext} instance.
   *
   * @param x object to be compared.
   * @return {@code true} if this {@code MathContext} instance is equal to the
   *         {@code x} argument; {@code false} otherwise.
   */
  @Override
  public boolean equals(Object x) {
    return ((x instanceof MathContext)
        && (((MathContext) x).getPrecision() == precision)
        && (((MathContext) x).getRoundingMode() == roundingMode));
  }

  /**
   * Returns the precision. The precision is the number of digits used for an
   * operation. Results are rounded to this precision. The precision is
   * guaranteed to be non negative. If the precision is zero, then the
   * computations have to be performed exact, results are not rounded in this
   * case.
   *
   * @return the precision.
   */
  public int getPrecision() {
    return precision;
  }

  /**
   * Returns the rounding mode. The rounding mode is the strategy to be used to
   * round results.
   * <p>
   * The rounding mode is one of {@link RoundingMode#UP},
   * {@link RoundingMode#DOWN}, {@link RoundingMode#CEILING},
   * {@link RoundingMode#FLOOR}, {@link RoundingMode#HALF_UP},
   * {@link RoundingMode#HALF_DOWN}, {@link RoundingMode#HALF_EVEN}, or
   * {@link RoundingMode#UNNECESSARY}.
   *
   * @return the rounding mode.
   */
  public RoundingMode getRoundingMode() {
    return roundingMode;
  }

  /**
   * Returns the hash code for this {@code MathContext} instance.
   *
   * @return the hash code for this {@code MathContext}.
   */
  @Override
  public int hashCode() {
    // Make place for the necessary bits to represent 8 rounding modes
    return ((precision << 3) | roundingMode.ordinal());
  }

  /**
   * Returns the string representation for this {@code MathContext} instance.
   * The string has the form {@code
   * "precision=<precision> roundingMode=<roundingMode>" * } where
   * {@code <precision>} is an integer describing the number of digits
   * used for operations and {@code <roundingMode>} is the string
   * representation of the rounding mode.
   *
   * @return a string representation for this {@code MathContext} instance
   */
  @Override
  public String toString() {
    return "precision=" + precision + " roundingMode=" + roundingMode;
  }
}
