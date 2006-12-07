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
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.i18n.client.ConstantsWithLookup;
import com.google.gwt.i18n.client.Messages;
import com.google.gwt.i18n.rebind.util.ResourceFactory;

import java.util.Locale;

/**
 * Generator used to bind classes extending the <code>Localizable</code> and
 * <code>Constants</code> interfaces.
 */
public class LocalizableGenerator extends Generator {
  /**
   * GWT method to override default use of method name as resource key.
   */
  public static final String GWT_KEY = "gwt.key";

  static final String CONSTANTS_NAME = Constants.class.getName();

  static final String CONSTANTS_WITH_LOOKUP_NAME = ConstantsWithLookup.class.getName();

  /**
   * Represents default locale.
   */
  static final String DEFAULT_TOKEN = "default";
  static final String MESSAGES_NAME = Messages.class.getName();
  private static long lastReloadCount = -1;
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
  public final String generate(TreeLogger logger, GeneratorContext context,
      String typeName) throws UnableToCompleteException {
    // Clear cache if reset was done.
    TypeOracle typeOracle = context.getTypeOracle();
    if (lastReloadCount != typeOracle.getReloadCount()) {
      ResourceFactory.clearCache();
      lastReloadCount = typeOracle.getReloadCount();
    }

    // Get the current locale and interface type.
    PropertyOracle propertyOracle = context.getPropertyOracle();
    Locale locale;
    try {
      String localeID = propertyOracle.getPropertyValue(logger, PROP_LOCALE);
      if ("default".equals(localeID)) {
        locale = null;
      } else {
        String[] localeChunks = localeID.split("_");
        if (localeChunks.length > 0) {
          if (!localeChunks[0].equals(localeChunks[0].toLowerCase())) {
            logger.log(TreeLogger.ERROR, localeID
                + "'s language code should be lower case", null);
            throw new UnableToCompleteException();
          }
        }
        if (localeChunks.length == 1) {
          locale = new Locale(localeChunks[0]);
        } else if (localeChunks.length == 2) {
          // Ignore the localized locale string if present, just use language
          // and country.
          locale = new Locale(localeChunks[0], localeChunks[1]);
        } else if (localeChunks.length == 3) {
          locale = new Locale(localeChunks[0], localeChunks[1], localeChunks[2]);
        } else {
          logger.log(TreeLogger.ERROR, localeID
              + " is not a correctly formatted locale", null);
          throw new UnableToCompleteException();
        }
      }
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

    // Link current locale and interface type to correct implementation class.
    String generatedClass = AbstractLocalizableImplCreator.generateConstantOrMessageClass(
        logger, context, locale, targetClass);
    if (generatedClass != null) {
      return generatedClass;
    }
    return linkageCreator.linkWithImplClass(logger, targetClass, locale);
  }
}
