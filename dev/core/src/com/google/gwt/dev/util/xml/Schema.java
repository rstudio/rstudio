// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.util.xml;

import com.google.gwt.core.ext.UnableToCompleteException;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public abstract class Schema {

  /**
   * Finds the most recent converter in the schema chain that can convert the
   * specified type.
   */
  public AttributeConverter getAttributeConverter(Class type) {
    AttributeConverter converter = (AttributeConverter) convertersByType
      .get(type);
    if (converter != null)
      return converter;
    else if (parent != null)
      return parent.getAttributeConverter(type);

    throw new IllegalStateException(
      "Unable to find an attribute converter for type " + type.getName());
  }

  public int getLineNumber() {
    return lineNumber;
  }

  public void onBadAttributeValue(int line, String elem, String attr,
      String value, Class paramType) throws UnableToCompleteException {
    if (parent != null)
      parent.onBadAttributeValue(line, elem, attr, value, paramType);
  }

  public void onHandlerException(int line, String elem, Method method,
      Throwable e) throws UnableToCompleteException {
    if (parent != null)
      parent.onHandlerException(line, elem, method, e);
  }

  public void onMissingAttribute(int line, String elem, String attr)
      throws UnableToCompleteException {
    if (parent != null)
      parent.onMissingAttribute(line, elem, attr);
  }

  public void onUnexpectedAttribute(int line, String elem, String attr,
      String value) throws UnableToCompleteException {
    if (parent != null)
      parent.onUnexpectedAttribute(line, elem, attr, value);
  }

  public void onUnexpectedChild(int line, String elem)
      throws UnableToCompleteException {
    if (parent != null)
      parent.onUnexpectedChild(line, elem);
  }

  public void onUnexpectedElement(int line, String elem)
      throws UnableToCompleteException {
    if (parent != null)
      parent.onUnexpectedElement(line, elem);
  }

  public void registerAttributeConverter(Class type,
      AttributeConverter converter) {
    convertersByType.put(type, converter);
  }

  public void setLineNumber(int lineNumber) {
    this.lineNumber = lineNumber;
  }

  public void setParent(Schema parent) {
    this.parent = parent;
  }

  private final Map convertersByType = new HashMap();
  private Schema parent;
  private int lineNumber;
}
