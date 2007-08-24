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
package com.google.gwt.sample.i18n.client;

import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.HasText;

/**
 * Demonstrates how to use {@link NumberFormat}.
 */
public class NumberFormatExampleController extends
    AbstractFormatExampleController {

  private static final String DEFAULT_INPUT = "31415926535.897932";
  private NumberFormat activeFormat;
  private final NumberFormatExampleConstants constants;

  public NumberFormatExampleController(
      final NumberFormatExampleConstants constants) {
    super(DEFAULT_INPUT, constants.numberFormatPatterns());
    this.constants = constants;
  }

  public NumberFormatExampleConstants getConstants() {
    return constants;
  }

  @Override
  protected String doGetPattern(String patternKey) {
    if ("currency".equals(patternKey)) {
      return NumberFormat.getCurrencyFormat().getPattern();
    }

    if ("decimal".equals(patternKey)) {
      return NumberFormat.getDecimalFormat().getPattern();
    }

    if ("scientific".equals(patternKey)) {
      return NumberFormat.getScientificFormat().getPattern();
    }

    if ("percent".equals(patternKey)) {
      return NumberFormat.getPercentFormat().getPattern();
    }

    throw new IllegalArgumentException("Unknown pattern key '" + patternKey
        + "'");
  }

  @Override
  protected void doParseAndRememberPattern(String pattern) {
    activeFormat = NumberFormat.getFormat(pattern);
  }

  @Override
  protected void doParseInput(String toParse, HasText output, HasText error) {
    if (!"".equals(toParse)) {
      try {
        double x = Double.parseDouble(toParse);
        String s = activeFormat.format(x);
        output.setText(s);
      } catch (NumberFormatException e) {
        String errMsg = constants.failedToParseInput();
        error.setText(errMsg);
      }
    } else {
      output.setText("<None>");
    }
  }
}
