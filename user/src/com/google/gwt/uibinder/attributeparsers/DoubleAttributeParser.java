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
package com.google.gwt.uibinder.attributeparsers;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.uibinder.rebind.MortalLogger;
import com.google.gwt.uibinder.rebind.XMLElement;

/**
 * Parses a single double attribute.
 */
class DoubleAttributeParser extends StrictAttributeParser {

  DoubleAttributeParser(FieldReferenceConverter converter,
      JType doubleType, MortalLogger logger) {
    super(converter, logger, doubleType);
  }

  @Override
  public String parse(XMLElement source, String value) throws UnableToCompleteException {
    try {
      Double.parseDouble(value);
      // Happy double
      return value;
    } catch (NumberFormatException e) {
      // Not a double, maybe super sees a field ref
    }
    String fieldMaybe = super.parse(source, value);
    if ("".equals(fieldMaybe)) {
      return "";
    }
    return "(double)" + fieldMaybe;
  }
}
