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
package com.google.gwt.uibinder.parsers;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.uibinder.rebind.MortalLogger;

/**
 * Parses a single double attribute.
 */
public class DoubleAttributeParser extends StrictAttributeParser {

  public String parse(String value, MortalLogger logger)
      throws UnableToCompleteException {
    try {
      Double.parseDouble(value);
      // Happy double
      return value;
    } catch (NumberFormatException e) {
      // Not a double, maybe super sees a field ref
    }
    return super.parse(value, logger);
  }
}
