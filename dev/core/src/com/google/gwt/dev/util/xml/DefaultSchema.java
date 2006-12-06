// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.util.xml;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;

import java.lang.reflect.Method;

public class DefaultSchema extends Schema {

  public DefaultSchema(TreeLogger logger) {
    fLogger = logger;

    // Registers converters for the typical primitive types.
    //
    registerAttributeConverter(int.class, new AttributeConverterForInteger());
    registerAttributeConverter(Integer.class,
      new AttributeConverterForInteger());
    registerAttributeConverter(String.class, new AttributeConverterForString());
    registerAttributeConverter(boolean.class,
      new AttributeConverterForBoolean());
    registerAttributeConverter(Boolean.class,
      new AttributeConverterForBoolean());
  }

  public void onBadAttributeValue(int line, String elem, String attr,
      String value, Class paramType) throws UnableToCompleteException {
    Messages.XML_ATTRIBUTE_CONVERSION_ERROR.log(fLogger, line, attr, paramType,
      null);
    throw new UnableToCompleteException();
  }

  public void onHandlerException(int line, String elem, Method method,
      Throwable e) throws UnableToCompleteException {
    Messages.XML_ELEMENT_HANDLER_EXCEPTION.log(fLogger, line, elem, e);
    throw new UnableToCompleteException();
  }

  public void onMissingAttribute(int line, String elem, String attr)
      throws UnableToCompleteException {
    Messages.XML_REQUIRED_ATTRIBUTE_MISSING
      .log(fLogger, elem, line, attr, null);
    throw new UnableToCompleteException();
  }

  public void onUnexpectedAttribute(int line, String elem, String attr,
      String value) throws UnableToCompleteException {
    Messages.XML_ATTRIBUTE_UNEXPECTED.log(fLogger, elem, line, attr, null);
    throw new UnableToCompleteException();
  }

  public void onUnexpectedChild(int line, String childElem)
      throws UnableToCompleteException {
    Messages.XML_CHILDREN_NOT_ALLOWED.log(fLogger, childElem, line, null);
    throw new UnableToCompleteException();
  }

  public void onUnexpectedElement(int line, String elem)
      throws UnableToCompleteException {
    Messages.XML_ELEMENT_UNEXPECTED.log(fLogger, line, elem, null);
    throw new UnableToCompleteException();
  }

  private final TreeLogger fLogger;
}
