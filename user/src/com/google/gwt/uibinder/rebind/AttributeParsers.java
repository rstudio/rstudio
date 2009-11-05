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
package com.google.gwt.uibinder.rebind;

import com.google.gwt.core.ext.typeinfo.JEnumType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.uibinder.parsers.AttributeParser;
import com.google.gwt.uibinder.parsers.EnumAttributeParser;
import com.google.gwt.uibinder.parsers.StrictAttributeParser;

import java.util.HashMap;
import java.util.Map;

/**
 * Managers access to all implementations of {@link AttributeParser}
 */
public class AttributeParsers {
  private static final String DOUBLE = "double";
  private static final String BOOLEAN = "boolean";

  private static AttributeParser getAttributeParserByClassName(
      String parserClassName) {
    try {
      Class<? extends AttributeParser> parserClass = Class.forName(
          parserClassName).asSubclass(AttributeParser.class);
      return parserClass.newInstance();
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Unable to instantiate parser", e);
    } catch (ClassCastException e) {
      throw new RuntimeException(parserClassName
          + " must extend AttributeParser");
    } catch (InstantiationException e) {
      throw new RuntimeException("Unable to instantiate parser", e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException("Unable to instantiate parser", e);
    }
  }

  /**
   * Class names of parsers for values of attributes with no namespace prefix,
   * keyed by method parameter signatures.
   */
  private final Map<String, String> parsers = new HashMap<String, String>();

  public AttributeParsers() {
    
    addAttributeParser(BOOLEAN,
        "com.google.gwt.uibinder.parsers.BooleanAttributeParser");

    addAttributeParser("java.lang.String",
        "com.google.gwt.uibinder.parsers.StringAttributeParser");

    addAttributeParser("int",
        "com.google.gwt.uibinder.parsers.IntAttributeParser");

    addAttributeParser(DOUBLE,
        "com.google.gwt.uibinder.parsers.DoubleAttributeParser");

    addAttributeParser("int,int",
        "com.google.gwt.uibinder.parsers.IntPairParser");

    addAttributeParser("com.google.gwt.user.client.ui.HasHorizontalAlignment."
        + "HorizontalAlignmentConstant",
        "com.google.gwt.uibinder.parsers.HorizontalAlignmentConstantParser");

    addAttributeParser("com.google.gwt.user.client.ui.HasVerticalAlignment."
        + "VerticalAlignmentConstant",
        "com.google.gwt.uibinder.parsers.VerticalAlignmentConstantParser");
  }

  public AttributeParser get(JType... types) {
    AttributeParser rtn = getForKey(getParametersKey(types));
    if (rtn != null || types.length > 1) {
      return rtn;
    }

    if (types.length == 1) {
      /* Maybe it's an enum */
      JEnumType enumType = types[0].isEnum();
      if (enumType != null) {
        return new EnumAttributeParser(enumType);
      }
    }

    /*
     * Dunno what it is, so let a StrictAttributeParser look for a
     * {field.reference}
     */
    return new StrictAttributeParser();
  }

  private void addAttributeParser(String signature, String className) {
    parsers.put(signature, className);
  }

  private AttributeParser getForKey(String key) {
    String parserClassName = parsers.get(key);
    if (parserClassName != null) {
      return getAttributeParserByClassName(parserClassName);
    }

    return null;
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
