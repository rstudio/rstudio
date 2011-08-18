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
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.uibinder.rebind.FieldManager;
import com.google.gwt.uibinder.rebind.MortalLogger;
import com.google.gwt.user.client.ui.HasHorizontalAlignment.HorizontalAlignmentConstant;
import com.google.gwt.user.client.ui.HasVerticalAlignment.VerticalAlignmentConstant;

import java.util.HashMap;
import java.util.Map;

/**
 * Managers access to all implementations of {@link AttributeParser}.
 */
public class AttributeParsers {
  private static final String HORIZ_CONSTANT = HorizontalAlignmentConstant.class.getCanonicalName();
  private static final String VERT_CONSTANT = VerticalAlignmentConstant.class.getCanonicalName();
  @SuppressWarnings("deprecation")
  private static final String TEXT_ALIGN_CONSTANT = 
    com.google.gwt.user.client.ui.TextBoxBase.TextAlignConstant.class.getCanonicalName();
  private static final String INT = "int";
  private static final String STRING = String.class.getCanonicalName();
  private static final String DOUBLE = "double";
  private static final String BOOLEAN = "boolean";
  private static final String UNIT = Unit.class.getCanonicalName();
  private static final String SAFE_URI = SafeUri.class.getCanonicalName();

  private final MortalLogger logger;
  private final FieldReferenceConverter converter;
  
  /**
   * Class names of parsers keyed by method parameter signatures.
   */
  private final Map<String, AttributeParser> parsers = new HashMap<String, AttributeParser>();
  private final SafeUriAttributeParser safeUriInHtmlParser;

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

      addAttributeParser("int,int", new IntPairAttributeParser(intParser,
          logger));

      addAttributeParser(HORIZ_CONSTANT, new HorizontalAlignmentConstantParser(
          converter, types.parse(HORIZ_CONSTANT), logger));
      addAttributeParser(VERT_CONSTANT, new VerticalAlignmentConstantParser(
          converter, types.parse(VERT_CONSTANT), logger));
      addAttributeParser(TEXT_ALIGN_CONSTANT, new TextAlignConstantParser(
          converter, types.parse(TEXT_ALIGN_CONSTANT), logger));

      StringAttributeParser stringParser = new StringAttributeParser(converter, types.parse(STRING));
      addAttributeParser(STRING, stringParser);

      EnumAttributeParser unitParser = new EnumAttributeParser(converter,
          (JEnumType) types.parse(UNIT), logger);
      addAttributeParser(DOUBLE + "," + UNIT, new LengthAttributeParser(
          doubleParser, unitParser, logger));

      SafeUriAttributeParser uriParser = new SafeUriAttributeParser(stringParser,
          converter, types.parse(SAFE_URI), logger);
      addAttributeParser(SAFE_URI, uriParser);
      
      safeUriInHtmlParser = new SafeUriAttributeParser(stringParser,
          converter, types.parse(SAFE_URI), types.parse(STRING), logger);
    } catch (TypeOracleException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns a parser for the given type(s). Accepts multiple types args to
   * allow requesting parsers for things like for pairs of ints.
   */
  public AttributeParser getParser(JType... types) {
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
    return new StrictAttributeParser(converter, logger, types[0]);
  }
  
  /**
   * Returns a parser specialized for handling URI references
   * in html contexts, like &lt;a href="{foo.bar}">.
   */
  public AttributeParser getSafeUriInHtmlParser() {
    return safeUriInHtmlParser;
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
