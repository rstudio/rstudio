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
package com.google.gwt.app.client;

import com.google.gwt.text.shared.Parser;

import java.text.ParseException;

/**
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * <p>
 * Simple unlocalized parser of Double that wraps {@link Double#valueOf(String)}.
 */
public class DoubleParser implements Parser<Double> {

  private static DoubleParser INSTANCE;

  /**
   * @return the instance of the no-op renderer
   */
  public static Parser<Double> instance() {
    if (INSTANCE == null) {
      INSTANCE = new DoubleParser();
    }
    return INSTANCE;
  }

  protected DoubleParser() {
  }

  public Double parse(CharSequence object) throws ParseException {
    try {
      return Double.valueOf(object.toString());
    } catch (NumberFormatException e) {
      throw new ParseException(e.getMessage(), 0);
    }
  }
}
