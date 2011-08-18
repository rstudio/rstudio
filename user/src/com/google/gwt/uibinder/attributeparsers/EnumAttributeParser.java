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
import com.google.gwt.core.ext.typeinfo.JEnumConstant;
import com.google.gwt.core.ext.typeinfo.JEnumType;
import com.google.gwt.uibinder.rebind.MortalLogger;
import com.google.gwt.uibinder.rebind.XMLElement;

import java.util.HashMap;
import java.util.Map;

/**
 * Parses an enum attribute.
 */
class EnumAttributeParser extends StrictAttributeParser {
  private final Map<String, JEnumConstant> values = new HashMap<String, JEnumConstant>();

  EnumAttributeParser(FieldReferenceConverter converter, JEnumType enumType,
      MortalLogger logger) {
    super(converter, logger, enumType);
    JEnumConstant[] constants = enumType.getEnumConstants();
    for (JEnumConstant c : constants) {
      values.put(c.getName(), c);
    }
  }

  @Override
  public String parse(XMLElement source, String value) throws UnableToCompleteException {
    JEnumConstant c = values.get(value);
    if (c != null) {
      return String.format("%s.%s",
          c.getEnclosingType().getQualifiedSourceName(), value);
    }
    return super.parse(source, value);
  }
}
