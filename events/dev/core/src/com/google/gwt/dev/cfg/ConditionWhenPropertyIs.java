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
package com.google.gwt.dev.cfg;

import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;

/**
 * A deferred binding condition to determine whether a named property has a
 * particular value.
 */
public class ConditionWhenPropertyIs extends Condition {

  private final String propName;

  private final String value;

  public ConditionWhenPropertyIs(String propName, String value) {
    this.propName = propName;
    this.value = value;
  }

  public String toString() {
    return "<when-property-is name='" + propName + "' value='" + value + "'/>";
  }

  protected boolean doEval(TreeLogger logger, GeneratorContext context,
      String testType) throws UnableToCompleteException {
    String testValue;
    try {
      PropertyOracle propertyOracle = context.getPropertyOracle();
      testValue = propertyOracle.getPropertyValue(logger, propName);
      logger.log(TreeLogger.DEBUG, "Property value is '" + testValue + "'",
          null);
      if (testValue.equals(value)) {
        return true;
      } else {
        return false;
      }
    } catch (BadPropertyValueException e) {
      String msg = "Unable to get value of property '" + propName + "'";
      logger.log(TreeLogger.ERROR, msg, e);
      throw new UnableToCompleteException();
    }
  }

  protected String getEvalAfterMessage(String testType, boolean result) {
    if (result) {
      return "Yes, the property value matched";
    } else {
      return "No, the value did not match";
    }
  }

  protected String getEvalBeforeMessage(String testType) {
    return toString();
  }
}
