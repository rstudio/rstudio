/*
 * Copyright 2011 Google Inc.
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

package com.google.gwt.useragent.rebind;

import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.ConfigurationProperty;
import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.SelectionProperty;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

import java.io.PrintWriter;

/**
 * Generator for {@link com.google.gwt.useragent.client.UserAgentAsserter}.
 */
public class UserAgentGenerator extends Generator {
  static final String PROPERTY_USER_AGENT = "user.agent";

  static final String PROPERTY_USER_AGENT_RUNTIME_WARNING = "user.agent.runtimeWarning";

  @Override
  public String generate(TreeLogger logger, GeneratorContext context, String typeName)
      throws UnableToCompleteException {
    TypeOracle typeOracle = context.getTypeOracle();

    JClassType userType;
    try {
      userType = typeOracle.getType(typeName);
    } catch (NotFoundException e) {
      logger.log(TreeLogger.ERROR, "Unable to find metadata for type: " + typeName, e);
      throw new UnableToCompleteException();
    }
    String packageName = userType.getPackage().getName();
    String className = userType.getName();
    className = className.replace('.', '_');

    if (userType.isInterface() == null) {
      logger.log(TreeLogger.ERROR, userType.getQualifiedSourceName() + " is not an interface", null);
      throw new UnableToCompleteException();
    }

    PropertyOracle propertyOracle = context.getPropertyOracle();

    boolean userAgentRuntimeWarning = true;
    try {
      ConfigurationProperty property = propertyOracle.getConfigurationProperty(PROPERTY_USER_AGENT_RUNTIME_WARNING);
      userAgentRuntimeWarning = Boolean.valueOf(property.getValues().get(0));
    } catch (BadPropertyValueException e) {
      logger.log(TreeLogger.WARN, "Unable to find value for '"
          + PROPERTY_USER_AGENT_RUNTIME_WARNING + "'", e);
    }

    if (!userAgentRuntimeWarning) {
      return "com.google.gwt.useragent.client.UserAgentAsserter.UserAgentPropertyDisabled";
    }

    String userAgentValue;
    SelectionProperty selectionProperty;
    try {
      selectionProperty = propertyOracle.getSelectionProperty(logger, PROPERTY_USER_AGENT);
      userAgentValue = selectionProperty.getCurrentValue();
    } catch (BadPropertyValueException e) {
      logger.log(TreeLogger.ERROR, "Unable to find value for '" + PROPERTY_USER_AGENT + "'", e);
      throw new UnableToCompleteException();
    }

    String userAgentValueInitialCap = userAgentValue.substring(0, 1).toUpperCase()
        + userAgentValue.substring(1);
    className = className + "Impl" + userAgentValueInitialCap;

    ClassSourceFileComposerFactory composerFactory = new ClassSourceFileComposerFactory(
        packageName, className);
    composerFactory.addImplementedInterface(userType.getQualifiedSourceName());

    PrintWriter pw = context.tryCreate(logger, packageName, className);
    if (pw != null) {
      SourceWriter sw = composerFactory.createSourceWriter(context, pw);

      sw.println();
      sw.println("public boolean getUserAgentRuntimeWarning() {");
      sw.indent();
      sw.println("return " + userAgentRuntimeWarning + ";");
      sw.outdent();
      sw.println("}");
      sw.println();

      sw.println();
      sw.println("public native String getRuntimeValue() /*-{");
      sw.indent();
      UserAgentPropertyGenerator.writeUserAgentPropertyJavaScript(sw,
          selectionProperty.getPossibleValues());
      sw.outdent();
      sw.println("}-*/;");
      sw.println();

      sw.println();
      sw.println("public String getCompileTimeValue() {");
      sw.indent();
      sw.println("return \"" + userAgentValue.trim() + "\";");
      sw.outdent();
      sw.println("}");

      sw.commit(logger);
    }
    return composerFactory.getCreatedClassName();
  }
}
