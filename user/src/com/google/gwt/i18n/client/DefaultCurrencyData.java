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
package com.google.gwt.i18n.client;

/**
 * A default {@link CurrencyData} implementation, so new methods can be added
 * to the interface without breaking implementors if a reasonable default is
 * available.
 */
public class DefaultCurrencyData implements CurrencyData {

  private final String currencyCode;
  private final String currencySymbol;
  private final int fractionDigits;

  /**
   * Create a default default {@link CurrencyData} instance, returning {@code
   * false} for all {@code isFoo} methods, having 2 fractional digits by
   * default, and using the standard symbol for the portable symbol.
   * 
   * @param currencyCode ISO 4217 currency code
   * @param currencySymbol symbol to use for this currency
   */
  public DefaultCurrencyData(String currencyCode, String currencySymbol) {
    this(currencyCode, currencySymbol, 2);
  }

  /**
   * Create a default default {@link CurrencyData} instance, returning {@code
   * false} for all {@code isFoo} methods and using the standard symbol for the
   * portable symbol.
   * 
   * @param currencyCode ISO 4217 currency code
   * @param currencySymbol symbol to use for this currency
   * @param fractionDigits default number of fraction digits
   */
  public DefaultCurrencyData(String currencyCode, String currencySymbol,
      int fractionDigits) {
    this.currencyCode = currencyCode;
    this.currencySymbol = currencySymbol;
    this.fractionDigits = fractionDigits;
  }

  @Override
  public String getCurrencyCode() {
    return currencyCode;
  }

  @Override
  public String getCurrencySymbol() {
    return currencySymbol;
  }

  @Override
  public int getDefaultFractionDigits() {
    return fractionDigits;
  }

  @Override
  public String getPortableCurrencySymbol() {
    return getCurrencySymbol();
  }

  @Override
  public String getSimpleCurrencySymbol() {
    return getCurrencySymbol();
  }

  @Override
  public boolean isDeprecated() {
    return false;
  }

  @Override
  public boolean isSpaceForced() {
    return false;
  }

  @Override
  public boolean isSpacingFixed() {
    return false;
  }

  @Override
  public boolean isSymbolPositionFixed() {
    return false;
  }

  @Override
  public boolean isSymbolPrefix() {
    return false;
  }
}
