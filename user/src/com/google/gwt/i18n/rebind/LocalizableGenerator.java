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
import com.google.gwt.core.ext.SelectionProperty;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.i18n.client.ConstantsWithLookup;
import com.google.gwt.i18n.client.Messages;
import com.google.gwt.i18n.shared.GwtLocale;

/**
 * Generator used to bind classes extending the <code>Localizable</code> and
 * <code>Constants</code> interfaces.
 */
public class LocalizableGenerator extends Generator {
  /**
   * GWT method to override default use of method name as resource key.
   */
  public static final String GWT_KEY = "gwt.key";

  public static final String CONSTANTS_NAME = Constants.class.getName();

  public static final String CONSTANTS_WITH_LOOKUP_NAME = ConstantsWithLookup.class.getName();

  public static final String MESSAGES_NAME = Messages.class.getName();

  /**
   * The token representing the locale property controlling Localization.
   */
  private static final String PROP_LOCALE = "locale";

  private LocalizableLinkageCreator linkageCreator = new LocalizableLinkageCreator();

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
    // Get the current locale
    PropertyOracle propertyOracle = context.getPropertyOracle();
    String locale;
    try {
      // Look at the code for the "locale" property provider in
      // I18N.gwt.xml to see the possible values for the locale
      // property. Basically,
      //
      // 1) If the locale is specified by the user using a request parameter
      // or a meta tag, AND
      // 2) The locale matches or is a parent of one of the locales
      // exposed in the application's gwt.xml file, THEN
      //
      // the value returned by getPropertyValue() will be:
      //
      // a) the locale specified by the user, OR
      // b) the parent locale, if an exact match between the user-specified
      // locale and the exposed locales cannot be found
      //
      // If the locale is not specified by the user as a request parameter
      // or via a meta tag, or if the locale is formatted incorrectly,
      // getPropertyValue() will return "default".
      SelectionProperty localeProp
          = propertyOracle.getSelectionProperty(logger, PROP_LOCALE);
      locale = localeProp.getCurrentValue();
    } catch (BadPropertyValueException e) {
      logger.log(TreeLogger.ERROR, "Could not parse specified locale", e);
      throw new UnableToCompleteException();
    }
    return generate(logger, context, typeName, locale);
  }

  public final String generate(TreeLogger logger, GeneratorContext context,
      String typeName, String localeName) throws UnableToCompleteException {
    TypeOracle typeOracle = context.getTypeOracle();
    JClassType targetClass;
    try {
      targetClass = typeOracle.getType(typeName);
    } catch (NotFoundException e) {
      logger.log(TreeLogger.ERROR, "No such type", e);
      throw new UnableToCompleteException();
    }

    GwtLocale locale = LocaleUtils.getLocaleFactory().fromString(localeName);
    
    // Link current locale and interface type to correct implementation class.
    String generatedClass = AbstractLocalizableImplCreator.generateConstantOrMessageClass(
        logger, context, locale, targetClass);
    if (generatedClass != null) {
      return generatedClass;
    }
    return linkageCreator.linkWithImplClass(logger, targetClass, locale);
  }
}
