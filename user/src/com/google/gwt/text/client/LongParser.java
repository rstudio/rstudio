/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.text.client;

import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.text.shared.Parser;

import java.text.ParseException;

/**
 * A localized parser based on {@link NumberFormat#getDecimalFormat}.
 */
public class LongParser implements Parser<Long> {

  private static LongParser INSTANCE;

  /**
   * Returns the instance of the no-op renderer.
   */
  public static Parser<Long> instance() {
    if (INSTANCE == null) {
      INSTANCE = new LongParser();
    }
    return INSTANCE;
  }

  protected LongParser() {
  }

  public Long parse(CharSequence object) throws ParseException {
    if ("".equals(object.toString())) {
      return null;
    }

    try {
      return (long) NumberFormat.getDecimalFormat().parse(object.toString());
    } catch (NumberFormatException e) {
      throw new ParseException(e.getMessage(), 0);
    }
  }
}
