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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.user.rebind.AbstractSourceCreator;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Links classes with their localized counterparts.
 */
class LocalizableLinkageCreator extends AbstractSourceCreator {
  private static Map findDerivedClasses(TreeLogger logger, JClassType baseClass)
      throws UnableToCompleteException {
    // Construct valid set of candidates for this type.
    Map matchingClasses = new HashMap();
    // Add base class if possible.
    if (baseClass.isInterface() == null && baseClass.isAbstract() == false) {
      matchingClasses.put(LocalizableGenerator.DEFAULT_TOKEN, baseClass);
    }
    String baseName = baseClass.getSimpleSourceName();

    // Find matching sub types.
    JClassType[] x = baseClass.getSubtypes();
    for (int i = 0; i < x.length; i++) {
      JClassType subType = x[i];
      if ((subType.isInterface() == null) && (subType.isAbstract() == false)) {
        String name = subType.getSimpleSourceName();
        // Strip locale from type,
        int localeIndex = name.indexOf("_");
        String subTypeBaseName = name;
        if (localeIndex != -1) {
          subTypeBaseName = name.substring(0, localeIndex);
        }
        boolean matches = subTypeBaseName.equals(baseName);
        if (matches) {
          boolean isDefault = localeIndex == -1
              || localeIndex == name.length() - 1;
          if (isDefault) {
            // Don't override base as default if present.
            JClassType defaultClass = (JClassType) matchingClasses.get(LocalizableGenerator.DEFAULT_TOKEN);
            if (defaultClass != null) {
              throw error(logger, defaultClass + " and " + baseName
                  + " are both potencial default classes for " + baseClass);
            } else {
              matchingClasses.put(LocalizableGenerator.DEFAULT_TOKEN, subType);
            }
          } else {
            // Don't allow a locale to be ambiguous. Very similar to default
            // case, different error message.
            String localeSubString = name.substring(localeIndex + 1);
            JClassType dopClass = (JClassType) matchingClasses.get(localeSubString);
            if (dopClass != null) {
              throw error(logger, dopClass.getQualifiedSourceName() + " and "
                  + subType.getQualifiedSourceName()
                  + " are both potential matches to " + baseClass
                  + " in locale" + localeSubString);
            }
            matchingClasses.put(localeSubString, subType);
          }
        }
      }
    }
    return matchingClasses;
  }

  /**
   * Map to cache linkages of implementation classes and interfaces.
   */
  // Change back to ReferenceMap once apache collections is in.
  private final Map implCache = new HashMap();

  /**
   * * Finds associated implementation in the current locale. Here are the rules
   * <p>
   * </p>
   * <p>
   * If class name is X, and locale is z_y, look for X_z_y, then X_z, then X
   * </p>
   * 
   * @param baseClass
   * @return class name to link with
   * @throws UnableToCompleteException
   */
  String linkWithImplClass(TreeLogger logger, JClassType baseClass,
      Locale locale) throws UnableToCompleteException {

    String baseName = baseClass.getQualifiedSourceName();
    /**
     * Try to find implementation class, as the current class is not a Constant
     * or Message.
     */
    String className = (String) implCache.get(baseName + locale);
    if (className != null) {
      return className;
    }

    if (baseClass.getName().indexOf("_") == 0) {
      throw error(logger, "Cannot have a '_' in the base localizable class "
          + baseClass);
    }
    Map matchingClasses = findDerivedClasses(logger, baseClass);
    // Now that we have all matches, find best class
    String localeSuffix;
    JClassType result = null;
    if (locale == null) {
      localeSuffix = LocalizableGenerator.DEFAULT_TOKEN;
    } else {
      localeSuffix = locale.toString();
    }
    while (true) {
      // Check for current result.
      result = (JClassType) matchingClasses.get(localeSuffix);
      if (result != null) {
        break;
      }
      // Now set up next option.
      int strip = localeSuffix.lastIndexOf("_");
      if (localeSuffix == LocalizableGenerator.DEFAULT_TOKEN) {
        // We already shot our wad, no classes matched.
        throw error(logger, "Cannot find a class to bind to argument type "
            + baseClass.getQualifiedSourceName());
      } else if (strip == -1) {
        // Try default.
        localeSuffix = LocalizableGenerator.DEFAULT_TOKEN;
      } else {
        // Try language specific locale.
        localeSuffix = localeSuffix.substring(0, strip);
      }
    }
    implCache.put(baseName + locale, className);
    return result.getQualifiedSourceName();
  }
}
