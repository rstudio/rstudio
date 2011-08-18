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
package com.google.gwt.uibinder.attributeparsers;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.uibinder.rebind.MortalLogger;
import com.google.gwt.uibinder.rebind.XMLElement;

/**
 * Parses a single boolean attribute.
 */
class BooleanAttributeParser extends StrictAttributeParser {

  BooleanAttributeParser(FieldReferenceConverter converter,
      JType booleanType, MortalLogger logger) {
    super(converter, logger, booleanType);
  }

  @Override
  public String parse(XMLElement source, String value) throws UnableToCompleteException {
    if (value.equals("true") || value.equals("false")) {
      return value;
    }

    return super.parse(source, value);
  }
}
