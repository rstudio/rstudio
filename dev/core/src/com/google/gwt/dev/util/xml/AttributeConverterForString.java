// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.util.xml;

/**
 * Subclass for converting strings into String.
 */
public class AttributeConverterForString extends AttributeConverter {
  public Object convertToArg(Schema schema, int lineNumber,
      String elemName, String attrName, String attrValue) {
    return attrValue;
  }
}

