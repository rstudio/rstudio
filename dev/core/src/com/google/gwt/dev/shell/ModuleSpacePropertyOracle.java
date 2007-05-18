/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.dev.shell;

import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.cfg.Properties;
import com.google.gwt.dev.cfg.Property;

import java.util.HashMap;
import java.util.Map;

/**
 * Implements a {@link PropertyOracle} in terms of a module space, which makes
 * it possible to execute property providers.
 */
public class ModuleSpacePropertyOracle implements PropertyOracle {

  private final Map prevAnswers = new HashMap();

  private final Properties props;

  private final ModuleSpace space;

  public ModuleSpacePropertyOracle(Properties props, ModuleSpace space) {
    this.space = space;
    this.props = props;
  }

  /**
   * Executes JavaScript to find the property value.
   */
  public String getPropertyValue(TreeLogger logger, String propertyName)
      throws BadPropertyValueException {
    if (propertyName == null) {
      throw new NullPointerException("propertyName");
    }

    Property prop = props.find(propertyName);
    if (prop == null) {
      // Don't know this property; that's not good.
      //
      throw new BadPropertyValueException(propertyName);
    }

    // Check if this property has already been queried for; if so, return
    // the same answer. This is necessary to match web mode behavior since
    // property providers are only called once. We cache even values that
    // cause exceptions to be thrown to make sure we are consistent even
    // in throwing exceptions for the same property.
    if (prevAnswers.containsKey(propertyName)) {
      return (String) prevAnswers.get(propertyName);
    } else {
      String value = computePropertyValue(logger, propertyName, prop);
      prevAnswers.put(propertyName, value);
      return value;
    }
  }

  /**
   * Returns the value of the specified property.
   * 
   * @throws BadPropertyValueException if the property value could not be
   *           computed, or if the returned result is not a legal value for this
   *           property.
   */
  private String computePropertyValue(TreeLogger logger, String propertyName,
      Property prop) throws BadPropertyValueException {
    String value;
    // If there is an active value, use that.
    //
    value = prop.getActiveValue();

    // In case there isn't an active value...
    if (value == null) {
      // Invokes the script function.
      //
      try {
        // Invoke the property provider function in JavaScript.
        //
        value = space.invokeNativeString("__gwt_getProperty", null,
            new Class[] {String.class}, new Object[] {prop.getName()});
      } catch (Throwable e) {
        // Treat as an unknown value.
        //
        String msg = "Error while executing the JavaScript provider for property '"
            + propertyName + "'";
        logger.log(TreeLogger.ERROR, msg, e);
        throw new BadPropertyValueException(propertyName, "<failed to compute>");
      }
    }

    // value may be null if the provider returned an unknown property value.
    if (prop.isKnownValue(value)) {
      return value;
    } else {
      // Bad value due to the provider returning an unknown value.
      // The fact that the provider returned an invalid value will also
      // have been reported to the JS bad property value handler function.
      throw new BadPropertyValueException(propertyName, value);
    }
  }
}
