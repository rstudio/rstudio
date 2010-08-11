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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;

import java.lang.reflect.Method;

/**
 * A base class for parsing XML that registers standard converters and throws
 * exceptions.
 */
public class DefaultSchema extends Schema {

  private final TreeLogger logger;

  public DefaultSchema(TreeLogger logger) {
    this.logger = logger;

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

  @Override
  public void onBadAttributeValue(int line, String elem, String attr,
      String value, Class<?> paramType) throws UnableToCompleteException {
    Messages.XML_ATTRIBUTE_CONVERSION_ERROR.log(logger, line, attr, paramType,
        null);
    throw new UnableToCompleteException();
  }

  @Override
  public void onHandlerException(int line, String elem, Method method,
      Throwable e) throws UnableToCompleteException {
    Messages.XML_ELEMENT_HANDLER_EXCEPTION.log(logger, line, elem, e);
    throw new UnableToCompleteException();
  }

  @Override
  public void onMissingAttribute(int line, String elem, String attr)
      throws UnableToCompleteException {
    Messages.XML_REQUIRED_ATTRIBUTE_MISSING.log(logger, elem, line, attr, null);
    throw new UnableToCompleteException();
  }

  @Override
  public void onUnexpectedAttribute(int line, String elem, String attr,
      String value) throws UnableToCompleteException {
    Messages.XML_ATTRIBUTE_UNEXPECTED.log(logger, elem, line, attr, null);
    throw new UnableToCompleteException();
  }

  @Override
  public void onUnexpectedChild(int line, String childElem)
      throws UnableToCompleteException {
    Messages.XML_CHILDREN_NOT_ALLOWED.log(logger, childElem, line, null);
    throw new UnableToCompleteException();
  }

  @Override
  public void onUnexpectedElement(int line, String elem)
      throws UnableToCompleteException {
    Messages.XML_ELEMENT_UNEXPECTED.log(logger, line, elem, null);
    throw new UnableToCompleteException();
  }
}
