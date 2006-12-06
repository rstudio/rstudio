// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.util.xml;

import com.google.gwt.core.ext.UnableToCompleteException;

/**
 * Subclass for converting strings into Integer.
 */
public class AttributeConverterForInteger extends AttributeConverter {
  public Object convertToArg(Schema schema, int lineNumber, String elemName,
      String attrName, String attrValue) throws UnableToCompleteException {
    try {
      return Integer.valueOf(attrValue);
    } catch (NumberFormatException e) {
      schema.onBadAttributeValue(lineNumber, elemName, attrName, attrValue,
        Integer.class);
      return null;
    }
  }
}