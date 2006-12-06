// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.util.xml;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class HandlerParam {

  /**
   * Looks for a field of the form [methodName]_[paramIndex]_[attrName].
   */
  public static HandlerParam create(Method method, String normalizedTagName,
      int paramIndex) {
    Class paramType = method.getParameterTypes()[paramIndex];
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

  private HandlerParam(Class paramType, Field metaField,
      String normalizedAttrName) {
    fParamType = paramType;
    fMetaField = metaField;
    fNormalizedAttrName = normalizedAttrName;
  }

  /**
   * Called while parsing to get the default value for an attribute. 
   */
  public String getDefaultValue(Schema schema) {
    Throwable caught = null;
    try {
      return (String) fMetaField.get(schema);
    } catch (IllegalArgumentException e) {
      caught = e;
    } catch (IllegalAccessException e) {
      caught = e;
    }

    // IllegalStateException has no (String, Throwable) constructor in 1.4,
    // which forces us to use this incantation. See the top of
    // {@link java.lang.Throwable} for details.
    //
    throw (IllegalStateException)new IllegalStateException(
      "Unable to get attribute default value from meta field '"
        + fMetaField.getName() + "'").initCause(caught);
  }

  public String getNormalizedName() {
    return fNormalizedAttrName;
  }

  public Class getParamType() {
    return fParamType;
  }

  private final Class fParamType;
  private final Field fMetaField;
  private final String fNormalizedAttrName;
}
