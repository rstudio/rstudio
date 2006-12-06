// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.util.xml;

import com.google.gwt.core.ext.UnableToCompleteException;

/**
 * Subclass for converting strings into Integer.
 */
public class AttributeConverterForBoolean extends AttributeConverter {
  public Object convertToArg(Schema schema, int line, String elem,
      String attr, String value) throws UnableToCompleteException {
    if (value.equals("true")) {
      return Boolean.TRUE;
    } else if (value.equals("false")) {
      return Boolean.FALSE;
    } else {
      schema.onBadAttributeValue(line, elem, attr, value, Boolean.class);
    }
    return null;
  }
}
