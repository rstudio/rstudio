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

import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.i18n.client.impl.LocaleInfoImpl;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

import java.io.PrintWriter;

/**
 * Generator used to generate an implementation of the LocaleInfoImpl class,
 * which is used by the LocaleInfo class.
 */
public class LocaleInfoGenerator extends Generator {

  /**
   * The token representing the locale property controlling Localization.
   */
  private static final String PROP_LOCALE = "locale";

  /**
   * Generate an implementation for the given type.
   * 
   * @param logger error logger
   * @param context generator context
   * @param typeName target type name
   * @return generated class name
   * @throws UnableToCompleteException
   */
  @Override
  public final String generate(TreeLogger logger, GeneratorContext context,
      String typeName) throws UnableToCompleteException {
    TypeOracle typeOracle = context.getTypeOracle();
    // Get the current locale and interface type.
    PropertyOracle propertyOracle = context.getPropertyOracle();
    String locale;
    try {
      locale = propertyOracle.getPropertyValue(logger, PROP_LOCALE);
    } catch (BadPropertyValueException e) {
      logger.log(TreeLogger.ERROR, "Could not parse specified locale", e);
      throw new UnableToCompleteException();
    }

    JClassType targetClass;
    try {
      targetClass = typeOracle.getType(typeName);
    } catch (NotFoundException e) {
      logger.log(TreeLogger.ERROR, "No such type", e);
      throw new UnableToCompleteException();
    }
    assert (LocaleInfoImpl.class.getName().equals(targetClass.getQualifiedSourceName()));
    
    String packageName = targetClass.getPackage().getName();
    String className = targetClass.getName().replace('.', '_') + "_";
    if (!locale.equals("default")) {
      className += locale;
    }
    
    PrintWriter pw = context.tryCreate(logger, packageName, className);
    if (pw != null) {
      ClassSourceFileComposerFactory factory = new ClassSourceFileComposerFactory(
          packageName, className);
      factory.addImplementedInterface(targetClass.getQualifiedSourceName());
      SourceWriter writer = factory.createSourceWriter(context, pw);
      writer.println("private static final String[] availableLocales = new String[] {");
      try {
        for (String propval : propertyOracle.getPropertyValueSet(logger,
            PROP_LOCALE)) {
          writer.println("  \"" + propval.replaceAll("\"", "\\\"") + "\",");
        }
      } catch (BadPropertyValueException e) {
        logger.log(TreeLogger.ERROR,
            "No locale property defined -- did you inherit the I18N module?", e);
        throw new UnableToCompleteException();
      }
      writer.println("};");
      writer.println();
      writer.println("public String getLocaleName() {");
      writer.println("  return \"" + locale + "\";");
      writer.println("}");
      writer.println();
      writer.println("public String[] getAvailableLocaleNames() {");
      writer.println("  return availableLocales;");
      writer.println("}");
      writer.commit(logger);
    }
    return packageName + "." + className;
  }
}
