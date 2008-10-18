// Copyright 2008 Google Inc.  All Rights Reserved.
package com.google.gwt.examples.i18n;

import com.google.gwt.i18n.client.Constants;
import com.google.gwt.i18n.client.LocalizableResource.DefaultLocale;

import java.util.Map;

@DefaultLocale("en_US")
public interface AnnotConstants extends Constants {
  int CURRENCY_DECIMALS = 2;

  @DefaultStringValue("Orange")
  @Meaning("the color")
  String orange();

  @DefaultStringValue("Red")
  String red();

  @DefaultIntValue(CURRENCY_DECIMALS)
  int currencyDecimals();
  
  @DefaultIntValue(CURRENCY_DECIMALS * 2)
  int extraCurrencyDecimals();
  
  @DefaultStringArrayValue({"zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine"})
  String[] numberNames();
  
  @DefaultDoubleValue(3.14159)
  double pi();
  
  @DefaultStringMapValue({"key1", "comma,value", "comma,key", "value2"})
  Map<String, String> map();
}
