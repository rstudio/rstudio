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

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.i18n.shared.CustomDateTimeFormat;
import com.google.gwt.i18n.shared.CustomDateTimeFormat.Pattern;
import com.google.gwt.i18n.shared.DateTimeFormat;
import com.google.gwt.i18n.shared.GwtLocale;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

import java.io.PrintWriter;

/**
 * Generator used to generate an implementation of a
 * {@link CustomDateTimeFormat} interface, computing the best matching localized
 * format patterns at compile time.
 */
public class CustomDateTimeFormatGenerator extends Generator {

  /**
   * Generate an implementation for the given type.
   * 
   * @param logger error logger
   * @param context generator context
   * @param typeName target type name
   * @return generated class name
   * @throws UnableToCompleteException
   */
  @SuppressWarnings("deprecation")
  @Override
  public final String generate(TreeLogger logger, final GeneratorContext context,
      String typeName) throws UnableToCompleteException {
    TypeOracle typeOracle = context.getTypeOracle();
    // Get the current locale and interface type.
    PropertyOracle propertyOracle = context.getPropertyOracle();
    LocaleUtils localeUtils = LocaleUtils.getInstance(logger, propertyOracle,
        context);

    JClassType targetClass;
    try {
      targetClass = typeOracle.getType(typeName);
    } catch (NotFoundException e) {
      logger.log(TreeLogger.ERROR, "No such type " + typeName, e);
      throw new UnableToCompleteException();
    }
    JClassType cdtfClass;
    try {
      cdtfClass = typeOracle.getType(CustomDateTimeFormat.class.getName());
    } catch (NotFoundException e) {
      logger.log(TreeLogger.ERROR, "No such type "
          + CustomDateTimeFormat.class.getName(), e);
      throw new UnableToCompleteException();
    }
    if (!cdtfClass.isAssignableFrom(targetClass)) {
      logger.log(TreeLogger.ERROR, typeName + " is not assignable to "
          + CustomDateTimeFormat.class.getName());
      throw new UnableToCompleteException();
    }
    JClassType oldDateTimeFormat;
    try {
      oldDateTimeFormat = typeOracle.getType(
          com.google.gwt.i18n.client.DateTimeFormat.class.getName());
    } catch (NotFoundException e) {
      logger.log(TreeLogger.ERROR, "No client DateTimeFormat type?", e);
      throw new UnableToCompleteException();
    }
    JClassType dateTimeFormat;
    try {
      dateTimeFormat = typeOracle.getType(DateTimeFormat.class.getName());
    } catch (NotFoundException e) {
      logger.log(TreeLogger.ERROR, "No DateTimeFormat type?", e);
      throw new UnableToCompleteException();
    }
    // TODO(jat): runtime locales support
    GwtLocale gwtLocale = localeUtils.getCompileLocale();
    DateTimePatternGenerator dtpg = new DateTimePatternGenerator(gwtLocale);
    String packageName = targetClass.getPackage().getName();
    String className = targetClass.getName().replace('.', '_') + "_"
        + gwtLocale.getAsString();
    PrintWriter pw = context.tryCreate(logger, packageName, className);
    if (pw != null) {
      ClassSourceFileComposerFactory factory = new ClassSourceFileComposerFactory(
          packageName, className);
      factory.addImplementedInterface(targetClass.getQualifiedSourceName());
      factory.addImport("com.google.gwt.i18n.client.DateTimeFormat");
      SourceWriter writer = factory.createSourceWriter(context, pw);
      writer.indent();
      for (JMethod method : targetClass.getMethods()) {
        JType returnType = method.getReturnType();
        if (returnType != dateTimeFormat && returnType != oldDateTimeFormat) {
          logger.log(TreeLogger.ERROR, typeName + "." + method.getName()
              + " must return DateTimeFormat");
          throw new UnableToCompleteException();
        }
        String pattern;
        Pattern annotation = method.getAnnotation(Pattern.class);
        if (annotation == null) {
          com.google.gwt.i18n.client.CustomDateTimeFormat.Pattern oldAnnotation
              = method.getAnnotation(com.google.gwt.i18n.client.CustomDateTimeFormat.Pattern.class);
          if (oldAnnotation == null) {
            logger.log(TreeLogger.ERROR, typeName + "." + method.getName()
                + " must have an @Pattern annotation");
            throw new UnableToCompleteException();
          }
          pattern = oldAnnotation.value();
        } else {
          pattern = annotation.value();
        }
        pattern = dtpg.getBestPattern(pattern); 
        writer.println();
        String retTypeName = method.getReturnType().getQualifiedSourceName();
        writer.println("public " + retTypeName + " " + method.getName() + "() {");
        writer.println("  return " + retTypeName + ".getFormat(\"" + pattern
            + "\");");
        writer.println("}");
      }
      writer.commit(logger);
    }
    return packageName + "." + className;
  }
}
