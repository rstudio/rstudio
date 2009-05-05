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
package com.google.gwt.module.rebind;

import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.ConfigurationProperty;
import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;

import java.io.PrintWriter;

/**
 * A test generator to dump the contents of the "testProperty" configuration
 * property.
 */
public class ConfigurationPropertiesGenerator extends Generator {

  @Override
  public String generate(TreeLogger logger, GeneratorContext context,
      String typeName) throws UnableToCompleteException {
    JClassType type = context.getTypeOracle().findType(typeName);
    assert type != null;

    PrintWriter out = context.tryCreate(logger, type.getPackage().getName(),
        "TestHookImpl");
    if (out != null) {
      String propertyValue;
      try {
        ConfigurationProperty prop = context.getPropertyOracle().
            getConfigurationProperty("testProperty");
        propertyValue = prop.getValues().get(0);
      } catch (BadPropertyValueException e) {
        logger.log(TreeLogger.ERROR, "testProperty not set", e);
        throw new UnableToCompleteException();
      }

      try {
        ConfigurationProperty prop = context.getPropertyOracle().
            getConfigurationProperty("bad_property");
        logger.log(TreeLogger.ERROR,
            "Did not get an exception trying to access fake property");
        throw new UnableToCompleteException();
      } catch (BadPropertyValueException e) {
        // OK
      }

      out.println("package " + type.getPackage().getName() + ";");
      out.println("public class TestHookImpl implements ConfigurationPropertiesTest.TestHook {");
      out.println("public String getConfigProperty() {");
      out.println("return \"" + escape(propertyValue) + "\";");
      out.println("}}");

      context.commit(logger, out);
    }
    return type.getPackage().getName() + ".TestHookImpl";
  }
}
