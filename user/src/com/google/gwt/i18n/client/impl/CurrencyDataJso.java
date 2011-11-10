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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.i18n.client.CurrencyData;

/**
 * JSO Overlay type that wraps currency data.
 * 
 * The JSO is an array with three elements:
 * <pre>
 *   0 - ISO4217 currency code
 *   1 - currency symbol to use for this locale
 *   2 - flags and # of decimal digits:
 *       d0-d2: # of decimal digits for this currency, 0-7
 *       d3:    currency symbol goes after number, 0=before
 *       d4:    currency symbol position is based on d3
 *       d5:    space is forced, 0=no space present
 *       d6:    spacing around currency symbol is based on d5
 *   3 - portable currency symbol (optional)
 *   4 - simple currency symbol (optional)
 * </pre>
 */
public final class CurrencyDataJso extends JavaScriptObject implements CurrencyData {
  
  protected CurrencyDataJso() {
  }

  @Override
  public native String getCurrencyCode() /*-{
    return this[0];
  }-*/;

  @Override
  public native String getCurrencySymbol() /*-{
    return this[1];
  }-*/;

  @Override
  public int getDefaultFractionDigits() {
    return CurrencyDataImpl.getDefaultFractionDigits(getFlagsAndPrecision());
  }

  @Override
  public native String getPortableCurrencySymbol() /*-{
    return this[3] || this[1];
  }-*/;

  @Override
  public native String getSimpleCurrencySymbol() /*-{
    return this[4] || this[1];
  }-*/;

  @Override
  public boolean isDeprecated() {
    return CurrencyDataImpl.isDeprecated(getFlagsAndPrecision());
  }

  @Override
  public boolean isSpaceForced() {
    return CurrencyDataImpl.isSpaceForced(getFlagsAndPrecision());
  }

  @Override
  public boolean isSpacingFixed() {
    return CurrencyDataImpl.isSpacingFixed(getFlagsAndPrecision());
  }

  @Override
  public boolean isSymbolPositionFixed() {
    return CurrencyDataImpl.isSymbolPositionFixed(getFlagsAndPrecision());
  }

  @Override
  public boolean isSymbolPrefix() {
    return CurrencyDataImpl.isSymbolPrefix(getFlagsAndPrecision());
  }

  private native int getFlagsAndPrecision() /*-{
    return this[2];
  }-*/;
}
