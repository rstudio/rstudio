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

package com.google.gwt.i18n.client.constants;

/**
 * NumberConstants class encapsulate a collection of Number formatting 
 * symbols for use with Number format and parse services. This class extends
 * GWT's Constants class. The actual symbol collections are defined in a set
 * of property files named like "NumberConstants_xx.properties". GWT will 
 * will perform late binding to the property file that specific to user's 
 * locale.
 * 
 * If you previously were using GWT.create on this interface, you should
 * use LocaleInfo.getCurrentLocale().getNumberConstants() instead.
 */
public interface NumberConstants {
  String notANumber();
  String currencyPattern();
  String decimalPattern();
  String decimalSeparator();
  String defCurrencyCode();
  String exponentialSymbol();
  String globalCurrencyPattern();
  String groupingSeparator();
  String infinity();
  String minusSign();
  String monetaryGroupingSeparator();
  String monetarySeparator();
  String percent();
  String percentPattern();
  String perMill();
  String plusSign();
  String scientificPattern();
  String simpleCurrencyPattern();
  String zeroDigit();
}
