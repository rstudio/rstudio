/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.dev.util.xml;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Represents metadata about a parameter in a handler method in a class derived
 * from {@link Schema}.
 */
public final class HandlerParam {

  /**
   * Looks for a field of the form [methodName]_[paramIndex]_[attrName].
   */
  public static HandlerParam create(Method method, String normalizedTagName,
      int paramIndex) {
    Class<?> paramType = method.getParameterTypes()[paramIndex];
    Field[] fields = method.getDeclaringClass().getDeclaredFields();
    String fieldNamePrefix = normalizedTagName + "_" + (paramIndex + 1);
    Field matchingField = null;
    String fieldName = null;
    for (int i = 0, n = fields.length; i < n; ++i) {
      Field testField = fields[i];
      fieldName = testField.getName();
      if (fieldName.startsWith(fieldNamePrefix)) {
        matchingField = testField;
        break;
      }
    }

    if (matchingField == null) {
      throw new IllegalArgumentException("Expecting a meta field with prefix '"
          + fieldNamePrefix + "'");
    }

    int under = fieldName.indexOf("_", fieldNamePrefix.length());
    if (under == -1) {
      // Not a valid signature.
      //
      throw new IllegalArgumentException(
          "Expecting a normalized attribute name suffix (e.g. \"_attr_name\") on field '"
              + fieldName + "'");
    }

    // Infer the associated attribute name.
    //
    String normalizedAttrName = fieldName.substring(under + 1);

    // GWT fields values must be of type String.
    //
    if (!String.class.equals(matchingField.getType())) {
      // Type mismatch.
      //
      throw new IllegalArgumentException("GWT field '" + fieldName
          + "' must be of type String");
    }

    // Instantiate one.
    //
    matchingField.setAccessible(true);
    HandlerParam handlerParam = new HandlerParam(paramType, matchingField,
        normalizedAttrName);
    return handlerParam;
  }

  private final Class<?> paramType;

  private final Field metaField;
  
  private final boolean isOptional;

  private final String normalizedAttrName;

  private HandlerParam(Class<?> paramType, Field metaField,
      String normalizedAttrName) {
    this.isOptional = normalizedAttrName.endsWith("$");
    if (isOptional) {
      normalizedAttrName = normalizedAttrName.substring(0, normalizedAttrName.length() - 1);
    }
    this.paramType = paramType;
    this.metaField = metaField;
    this.normalizedAttrName = normalizedAttrName;
  }

  /**
   * Called while parsing to get the default value for an attribute.
   */
  public String getDefaultValue(Schema schema) {
    Throwable caught = null;
    try {
      return (String) metaField.get(schema);
    } catch (IllegalArgumentException e) {
      caught = e;
    } catch (IllegalAccessException e) {
      caught = e;
    }

    throw new IllegalStateException(
        "Unable to get attribute default value from meta field '"
            + metaField.getName() + "'", caught);
  }

  public String getNormalizedName() {
    return normalizedAttrName;
  }

  public Class<?> getParamType() {
    return paramType;
  }
  
  public boolean isOptional() {
    return isOptional;
  }
}
