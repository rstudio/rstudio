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
package com.google.gwt.i18n.client.impl;

import com.google.gwt.i18n.client.DefaultCurrencyData;

/**
 * A POJO for currency data.
 */
public final class CurrencyDataImpl extends DefaultCurrencyData {

  /**
   * Public so CurrencyListGenerator can get to them. As usual with an impl
   * package, external code should not rely on these values.
   */
  public static final int DEPRECATED_FLAG = 128;
  public static final int POS_FIXED_FLAG = 16;
  public static final int POS_SUFFIX_FLAG = 8;
  public static final int PRECISION_MASK = 7;
  public static final int SPACE_FORCED_FLAG = 32;
  public static final int SPACING_FIXED_FLAG = 64;

  public static int getDefaultFractionDigits(int flagsAndPrecision) {
    return flagsAndPrecision & PRECISION_MASK;
  }

  public static boolean isDeprecated(int flagsAndPrecision) {
    return (flagsAndPrecision & DEPRECATED_FLAG) != 0;
  }

  public static boolean isSpaceForced(int flagsAndPrecision) {
    return (flagsAndPrecision & SPACE_FORCED_FLAG) != 0;
  }

  public static boolean isSpacingFixed(int flagsAndPrecision) {
    return (flagsAndPrecision & SPACING_FIXED_FLAG) != 0;
  }

  public static boolean isSymbolPositionFixed(int flagsAndPrecision) {
    return (flagsAndPrecision & POS_FIXED_FLAG) != 0;
  }

  public static boolean isSymbolPrefix(int flagsAndPrecision) {
    return (flagsAndPrecision & POS_SUFFIX_FLAG) != 0;
  }

  /**
   * Flags and # of decimal digits.
   * 
   * <pre>
   *       d0-d2: # of decimal digits for this currency, 0-7
   *       d3:    currency symbol goes after number, 0=before
   *       d4:    currency symbol position is based on d3
   *       d5:    space is forced, 0=no space present
   *       d6:    spacing around currency symbol is based on d5
   * </pre>
   */
  private final int flagsAndPrecision;

  /**
   * Portable currency symbol, may be the same as {@link #getCurrencySymbol()}.
   */
  private final String portableCurrencySymbol;

  /**
   * Simple currency symbol, may be the same as {@link #getCurrencySymbol()}.
   */
  private final String simpleCurrencySymbol;

  /**
   * Create a new CurrencyData whose portable symbol is the same as its local
   * symbol.
   */
  public CurrencyDataImpl(String currencyCode, String currencySymbol, int flagsAndPrecision) {
    this(currencyCode, currencySymbol, flagsAndPrecision, null, null);
  }

  /**
   * Create a new CurrencyData whose portable symbol is the same as its local
   * symbol.
   */
  public CurrencyDataImpl(String currencyCode, String currencySymbol, int flagsAndPrecision,
      String portableCurrencySymbol) {
    this(currencyCode, currencySymbol, flagsAndPrecision, portableCurrencySymbol, null);
  }

  public CurrencyDataImpl(String currencyCode, String currencySymbol,
      int flagsAndPrecision, String portableCurrencySymbol, String simpleCurrencySymbol) {
    super(currencyCode, currencySymbol,
        getDefaultFractionDigits(flagsAndPrecision));
    this.flagsAndPrecision = flagsAndPrecision;
    this.portableCurrencySymbol = portableCurrencySymbol == null ? currencySymbol
        : portableCurrencySymbol;
    this.simpleCurrencySymbol = simpleCurrencySymbol == null ? currencySymbol
        : simpleCurrencySymbol;
  }

  @Override
  public int getDefaultFractionDigits() {
    return getDefaultFractionDigits(flagsAndPrecision);
  }

  @Override
  public String getPortableCurrencySymbol() {
    return portableCurrencySymbol;
  }

  @Override
  public String getSimpleCurrencySymbol() {
    return simpleCurrencySymbol;
  }

  @Override
  public boolean isDeprecated() {
    return isDeprecated(flagsAndPrecision);
  }

  @Override
  public boolean isSpaceForced() {
    return isSpaceForced(flagsAndPrecision);
  }

  @Override
  public boolean isSpacingFixed() {
    return isSpacingFixed(flagsAndPrecision);
  }

  @Override
  public boolean isSymbolPositionFixed() {
    return isSymbolPositionFixed(flagsAndPrecision);
  }

  @Override
  public boolean isSymbolPrefix() {
    return isSymbolPrefix(flagsAndPrecision);
  }
}
