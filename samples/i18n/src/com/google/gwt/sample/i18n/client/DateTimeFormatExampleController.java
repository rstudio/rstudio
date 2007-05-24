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

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.HasText;

import java.util.Date;

/**
 * Demonstrates how to use {@link DateTimeFormat}.
 */
public class DateTimeFormatExampleController extends
    AbstractFormatExampleController {

  private static final String DEFAULT_INPUT = "13 September 1999";
  private DateTimeFormat activeFormat;
  private final DateTimeFormatExampleConstants constants;

  public DateTimeFormatExampleController(

  final DateTimeFormatExampleConstants constants) {
    super(DEFAULT_INPUT, constants.dateTimeFormatPatterns());
    this.constants = constants;
  }

  public DateTimeFormatExampleConstants getConstants() {
    return constants;
  }

  protected String doGetPattern(String patternKey) {
    // Date + Time
    if ("fullDateTime".equals(patternKey)) {
      return DateTimeFormat.getFullDateTimeFormat().getPattern();
    }

    if ("longDateTime".equals(patternKey)) {
      return DateTimeFormat.getLongDateTimeFormat().getPattern();
    }

    if ("mediumDateTime".equals(patternKey)) {
      return DateTimeFormat.getMediumDateTimeFormat().getPattern();
    }
    
    if ("shortDateTime".equals(patternKey)) {
      return DateTimeFormat.getShortDateTimeFormat().getPattern();
    }

    // Date only
    if ("fullDate".equals(patternKey)) {
      return DateTimeFormat.getFullDateFormat().getPattern();
    }

    if ("longDate".equals(patternKey)) {
      return DateTimeFormat.getLongDateFormat().getPattern();
    }

    if ("mediumDate".equals(patternKey)) {
      return DateTimeFormat.getMediumDateFormat().getPattern();
    }
    
    if ("shortDate".equals(patternKey)) {
      return DateTimeFormat.getShortDateFormat().getPattern();
    }

    // Time only
    if ("fullTime".equals(patternKey)) {
      return DateTimeFormat.getFullTimeFormat().getPattern();
    }

    if ("longTime".equals(patternKey)) {
      return DateTimeFormat.getLongTimeFormat().getPattern();
    }

    if ("mediumTime".equals(patternKey)) {
      return DateTimeFormat.getMediumTimeFormat().getPattern();
    }
    
    if ("shortTime".equals(patternKey)) {
      return DateTimeFormat.getShortTimeFormat().getPattern();
    }
    
    throw new IllegalArgumentException("Unknown pattern key '" + patternKey
        + "'");
  }

  protected void doParseAndRememberPattern(String pattern) {
    activeFormat = DateTimeFormat.getFormat(pattern);
  }

  protected void doParseInput(String toParse, HasText output, HasText error) {
    error.setText("");
    if (!"".equals(toParse)) {
      try {
        Date x = new Date(toParse);
        String s = activeFormat.format(x);
        output.setText(s);
      } catch (IllegalArgumentException e) {
        String errMsg = constants.failedToParseInput();
        error.setText(errMsg);
      }
    } else {
      output.setText("<None>");
    }
  }
}
