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

import com.google.gwt.core.ext.typeinfo.JEnumType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.core.ext.typeinfo.TypeOracleException;
import com.google.gwt.uibinder.rebind.FieldManager;
import com.google.gwt.uibinder.rebind.MortalLogger;

import java.util.HashMap;
import java.util.Map;

/**
 * Managers access to all implementations of {@link AttributeParser}
 */
public class AttributeParsers {
  private static final String VERT_CONSTANT = "com.google.gwt.user.client.ui.HasVerticalAlignment."
      + "VerticalAlignmentConstant";
  private static final String HORIZ_CONSTANT = "com.google.gwt.user.client.ui.HasHorizontalAlignment."
      + "HorizontalAlignmentConstant";
  private static final String INT = "int";
  private static final String STRING = String.class.getCanonicalName();
  private static final String DOUBLE = "double";
  private static final String BOOLEAN = "boolean";

  private final MortalLogger logger;
  private final FieldReferenceConverter converter;

  /**
   * Class names of parsers for values of attributes with no namespace prefix,
   * keyed by method parameter signatures.
   */
  private final Map<String, AttributeParser> parsers = new HashMap<String, AttributeParser>();

  public AttributeParsers(TypeOracle types, FieldManager fieldManager,
      MortalLogger logger) {
    this.logger = logger;
    converter = new FieldReferenceConverter(fieldManager);

    try {
      BooleanAttributeParser boolParser = new BooleanAttributeParser(converter,
          types.parse(BOOLEAN), logger);
      addAttributeParser(BOOLEAN, boolParser);
      addAttributeParser(Boolean.class.getCanonicalName(), boolParser);
      
      IntAttributeParser intParser = new IntAttributeParser(converter,
          types.parse(INT), logger);
      addAttributeParser(INT, intParser);
      addAttributeParser(Integer.class.getCanonicalName(), intParser);
      
      DoubleAttributeParser doubleParser = new DoubleAttributeParser(converter,
          types.parse(DOUBLE), logger);
      addAttributeParser(DOUBLE, doubleParser);
      addAttributeParser(Double.class.getCanonicalName(), doubleParser);
      
      addAttributeParser("int,int", new IntPairParser());
      
      addAttributeParser(HORIZ_CONSTANT, new HorizontalAlignmentConstantParser(
          converter, types.parse(HORIZ_CONSTANT), logger));
      addAttributeParser(VERT_CONSTANT, new VerticalAlignmentConstantParser(
          converter, types.parse(VERT_CONSTANT), logger));
      
      addAttributeParser(STRING, new StringAttributeParser(converter,
          types.parse(STRING)));
    } catch (TypeOracleException e) {
      throw new RuntimeException(e);
    }
  }

  public AttributeParser get(JType... types) {
    if (types.length == 0) {
      throw new RuntimeException("Asked for attribute parser of no type");
    }

    AttributeParser rtn = getForKey(getParametersKey(types));
    if (rtn != null || types.length > 1) {
      return rtn;
    }

    /* Maybe it's an enum */
    JEnumType enumType = types[0].isEnum();
    if (enumType != null) {
      return new EnumAttributeParser(converter, enumType, logger);
    }

    /*
     * Dunno what it is, so let a StrictAttributeParser look for a
     * {field.reference}
     */
    return new StrictAttributeParser(converter, types[0], logger);
  }

  private void addAttributeParser(String signature,
      AttributeParser attributeParser) {
    parsers.put(signature, attributeParser);
  }

  private AttributeParser getForKey(String key) {
    return parsers.get(key);
  }

  /**
   * Given a types array, return a key for the attributeParsers table.
   */
  private String getParametersKey(JType[] types) {
    StringBuffer b = new StringBuffer();
    for (JType t : types) {
      if (b.length() > 0) {
        b.append(',');
      }
      b.append(t.getParameterizedQualifiedSourceName());
    }
    return b.toString();
  }
}
