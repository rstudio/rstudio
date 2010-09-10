/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.i18n.rebind;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.i18n.rebind.AbstractResource.ResourceList;
import com.google.gwt.i18n.shared.GwtLocale;
import com.google.gwt.user.rebind.AbstractGeneratorClassCreator;

/**
 * Method creator to handle <code>Number</code> creation.
 */
class SimpleValueMethodCreator extends AbstractLocalizableMethodCreator {

  /**
   * Helper class to delegate to the correct Number parser for this method.
   */
  public abstract static class AbstractValueCreator {
    abstract String getValue(String stringVal);
  }

  private static class BadBooleanPropertyValue extends RuntimeException {
  }

  static final AbstractValueCreator DOUBLE = new AbstractValueCreator() {
    @Override
    String getValue(String stringVal) {
      return Double.parseDouble(stringVal) + "";
    }
  };
  static final AbstractValueCreator FLOAT = new AbstractValueCreator() {
    @Override
    String getValue(String stringVal) {
      return Float.parseFloat(stringVal) + "f";
    }
  };
  static final AbstractValueCreator INT = new AbstractValueCreator() {
    @Override
    String getValue(String stringVal) {
      return Integer.parseInt(stringVal) + "";
    }
  };
  static final AbstractValueCreator STRING = new AbstractValueCreator() {
    @Override
    String getValue(String stringVal) {
      return wrap(stringVal);
    }
  };
  static final AbstractValueCreator BOOLEAN = new AbstractValueCreator() {
    @Override
    String getValue(String stringVal) {
      if ("true".equals(stringVal)) {
        return "true";
      } else if ("false".equals(stringVal)) {
        return "false";
      } else {
        throw new BadBooleanPropertyValue();
      }
    }
  };

  private AbstractValueCreator valueCreator;

  /**
   * Constructor for <code>SimpleValueMethodCreator</code>.
   * 
   * @param currentCreator
   * @param valueCreator
   */
  SimpleValueMethodCreator(AbstractGeneratorClassCreator currentCreator,
      AbstractValueCreator valueCreator) {
    super(currentCreator);
    this.valueCreator = valueCreator;
  }

  @Override
  public void createMethodFor(TreeLogger logger, JMethod targetMethod,
      String key, ResourceList resourceList, GwtLocale locale)
      throws UnableToCompleteException {
    String value = resourceList.getRequiredStringExt(key, null);
    try {
      String translatedValue = valueCreator.getValue(value);
      println("return " + translatedValue + ";");
    } catch (NumberFormatException e) {
      throw error(logger, value + " could not be parsed as a number.");
    } catch (BadBooleanPropertyValue e) {
      throw error(
          logger,
          "'"
              + value
              + "' is not a valid boolean property value; must be 'true' or 'false'");
    }
  }
}
