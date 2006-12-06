// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.util.xml;

import com.google.gwt.core.ext.UnableToCompleteException;

/**
 * Converts from a string to the type needed.
 */
public abstract class AttributeConverter {
  /**
   * Converts from a string to the type needed.
   * Does not throw conversion-related exceptions.
   * @param attrValue
   *    the value to convert
   * @param schema
   *    used to report conversion problems
   * @return
   *    the argument converted to a form that is expected to compatible with
   *    the associated parameter and will work for a reflection "invoke()" call;
   *    <code>null</code> if the conversion failed.
   */
  public abstract Object convertToArg(Schema schema, int line,
      String elem, String attr, String value) throws UnableToCompleteException;
}
