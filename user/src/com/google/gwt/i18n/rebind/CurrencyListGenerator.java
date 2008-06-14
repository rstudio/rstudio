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
import com.google.gwt.i18n.client.impl.CurrencyData;
import com.google.gwt.i18n.client.impl.CurrencyList;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

import org.apache.tapestry.util.text.LocalizedProperties;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Generator used to generate a localized version of CurrencyList, which
 * contains the list of currencies (with names, symbols, and other information)
 * localized to the current locale.  
 */
public class CurrencyListGenerator extends Generator {

  private static final String CURRENCY_DATA = CurrencyData.class.getCanonicalName();

  private static final String CURRENCY_LIST = CurrencyList.class.getCanonicalName();

  /**
   * Prefix for properties files containing CLDR-derived currency data for
   * each locale.
   */
  private static final String CURRENCY_DATA_PREFIX =
      "com/google/gwt/i18n/client/impl/cldr/CurrencyData";
  
  /**
   * Prefix for properties files containing additional flags about currencies
   * each locale, which are not present in CLDR.
   */
  private static final String CURRENCY_EXTRA_PREFIX =
      "com/google/gwt/i18n/client/constants/CurrencyExtra";
  
  /**
   * Prefix for properties files containing number formatting constants for
   * each locale.  We use this only to get the default currency for our
   * current locale.
   */
  private static final String NUMBER_CONSTANTS_PREFIX =
      "com/google/gwt/i18n/client/constants/NumberConstants";
  
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
    assert CURRENCY_LIST.equals(typeName);
    TypeOracle typeOracle = context.getTypeOracle();

    // Get the current locale.
    PropertyOracle propertyOracle = context.getPropertyOracle();
    String locale = "default";
    try {
      locale = propertyOracle.getPropertyValue(logger, PROP_LOCALE);
    } catch (BadPropertyValueException e) {
      logger.log(TreeLogger.WARN, "locale property not defined, using defaults", e);
    }

    JClassType targetClass;
    try {
      targetClass = typeOracle.getType(typeName);
    } catch (NotFoundException e) {
      logger.log(TreeLogger.ERROR, "No such type", e);
      throw new UnableToCompleteException();
    }
    
    String packageName = targetClass.getPackage().getName();
    String className = targetClass.getName().replace('.', '_') + "_";
    if (!locale.equals("default")) {
      className += locale;
    }
    PrintWriter pw = context.tryCreate(logger, packageName, className);
    if (pw != null) {
      ClassSourceFileComposerFactory factory = new ClassSourceFileComposerFactory(
          packageName, className);
      factory.setSuperclass(targetClass.getQualifiedSourceName());
      factory.addImport(CURRENCY_LIST);
      factory.addImport(CURRENCY_DATA);
      SourceWriter writer = factory.createSourceWriter(context, pw);
      
      // Load property files for this locale, handling inheritance properly.
      LocalizedProperties currencyData = readProperties(logger, CURRENCY_DATA_PREFIX, locale);
      LocalizedProperties currencyExtra = readProperties(logger, CURRENCY_EXTRA_PREFIX, locale);
      LocalizedProperties numberConstants = readProperties(logger, NUMBER_CONSTANTS_PREFIX, locale);
      
      // Get default currency code, set defaults in case it isn't found.
      String defCurrencyCode = numberConstants.getProperty("defCurrencyCode");
      if (defCurrencyCode == null) {
        defCurrencyCode = "USD";
      }
      
      // Sort for deterministic output.
      Set<Object> keySet = currencyData.getPropertyMap().keySet();
      String[] currencies = new String[keySet.size()];
      keySet.toArray(currencies);
      Arrays.sort(currencies);
      Map<String, String> nameMap = new HashMap<String, String>();

      writer.println("@Override");
      writer.println("protected native void loadCurrencyMap() /*-{");
      writer.indent();
      writer.println("this.@com.google.gwt.i18n.client.impl.CurrencyList::dataMap = {");
      writer.indent();
      String defCurrencyObject = "[ \"" + quote(defCurrencyCode) + "\", \""
          + quote(defCurrencyCode) + "\", 2 ]";
      for (String currencyCode : currencies) {
        String currencyEntry = currencyData.getProperty(currencyCode);
        String[] currencySplit = currencyEntry.split("\\|");
        String currencyDisplay = currencySplit[0];
        String currencySymbol = null;
        if (currencySplit.length > 1 && currencySplit[1].length() > 0) {
          currencySymbol = currencySplit[1];
        }
        int currencyFractionDigits = 2;
        if (currencySplit.length > 2 && currencySplit[2].length() > 0) {
          try {
            currencyFractionDigits = Integer.valueOf(currencySplit[2]);
          } catch (NumberFormatException e) {
            // Ignore bad values
            logger.log(TreeLogger.WARN, "Parse of \"" + currencySplit[2] + "\" failed", e);
          }
        }
        boolean currencyObsolete = false;
        if (currencySplit.length > 3 && currencySplit[3].length() > 0) {
          try {
            currencyObsolete = Integer.valueOf(currencySplit[3]) != 0;
          } catch (NumberFormatException e) {
            // Ignore bad values
            logger.log(TreeLogger.WARN, "Parse of \"" + currencySplit[3] + "\" failed", e);
          }
        }
        int currencyFlags = currencyFractionDigits;
        String extraData = currencyExtra.getProperty(currencyCode);
        String portableSymbol = "";
        if (extraData != null) {
          // CurrencyExtra contains up to 3 fields separated by |
          //   0 - portable currency symbol
          //   1 - space-separated flags regarding currency symbol positioning/spacing
          //   2 - override of CLDR-derived currency symbol
          String[] extraSplit = extraData.split("\\|");
          portableSymbol = extraSplit[0];
          if (extraSplit.length > 1) {
            if (extraSplit[1].contains("SymPrefix")) {
              currencyFlags |= CurrencyData.POS_FIXED_FLAG; 
            } else if (extraSplit[1].contains("SymSuffix")) {
              currencyFlags |= CurrencyData.POS_FIXED_FLAG | CurrencyData.POS_SUFFIX_FLAG;
            } 
            if (extraSplit[1].contains("ForceSpace")) {
              currencyFlags |= CurrencyData.SPACING_FIXED_FLAG | CurrencyData.SPACE_FORCED_FLAG;
            } else if (extraSplit[1].contains("ForceNoSpace")) {
              currencyFlags |= CurrencyData.SPACING_FIXED_FLAG;
            }
          }
          // If a non-empty override is supplied, use it for the currency symbol.
          if (extraSplit.length > 2 && extraSplit[2].length() > 0) {
            currencySymbol = extraSplit[2];
          }
          // If we don't have a currency symbol yet, use the portable symbol if supplied.
          if (currencySymbol == null && portableSymbol.length() > 0) {
            currencySymbol = portableSymbol;
          }
        }
        // If all else fails, use the currency code as the symbol.
        if (currencySymbol == null) {
          currencySymbol = currencyCode;
        }
        String currencyObject = "[ \"" + quote(currencyCode) + "\", \"" + quote(currencySymbol)
            + "\", " + currencyFlags;
        if (portableSymbol.length() > 0) {
          currencyObject += ", \"" + quote(portableSymbol) + "\"";
        }
        currencyObject += "]";
        if (!currencyObsolete) {
          nameMap.put(currencyCode, currencyDisplay);
          writer.println("// " + currencyDisplay);
          writer.println("\":" + quote(currencyCode) + "\": " + currencyObject + ",");
        }
        if (currencyCode.equals(defCurrencyCode)) {
          defCurrencyObject  = currencyObject;
        }
      }
      writer.outdent();
      writer.println("};");
      writer.outdent();
      writer.println("}-*/;");
      writer.println();
      writer.println("@Override");
      writer.println("public native void loadNamesMap() /*-{");
      writer.indent();
      writer.println("this.@com.google.gwt.i18n.client.impl.CurrencyList::namesMap = {");
      writer.indent();
      for (String currencyCode : currencies) {
        String displayName = nameMap.get(currencyCode);
        if (displayName != null && !currencyCode.equals(displayName)) {
          writer.println("\"" + quote(currencyCode) + "\": \"" + quote(displayName) + "\",");
        }
      }
      writer.outdent();
      writer.println("};");
      writer.outdent();
      writer.println("}-*/;");
      writer.println();
      writer.println("@Override");
      writer.println("public native CurrencyData getDefault() /*-{");
      writer.println("  return " + defCurrencyObject + ";");
      writer.println("}-*/;");
      writer.commit(logger);
    }
    return packageName + "." + className;
  }
      
  /**
   * Backslash-escape any double quotes in the supplied string.
   * 
   * @param str string to quote
   * @return string with double quotes backslash-escaped.
   */
  private String quote(String str) {
    return str.replace("\"", "\\\"");
  }

  /**
   * Load a chain of localized properties files, starting with the default and
   * adding locale components so inheritance is properly recognized.
   * 
   * @param logger TreeLogger instance
   * @param propFilePrefix property file name prefix; locale is added to it
   * @return a LocalizedProperties object containing all properties
   * @throws UnableToCompleteException if an error occurs reading the file
   */
  private LocalizedProperties readProperties(TreeLogger logger, String propFilePrefix,
      String locale) throws UnableToCompleteException {
    LocalizedProperties props = new LocalizedProperties();
    ClassLoader classLoader = getClass().getClassLoader();
    readProperties(logger, classLoader, propFilePrefix, props);
    if (locale.equals("default")) {
      return props;
    }
    int idx = -1;
    while ((idx = locale.indexOf('_', idx + 1)) >= 0) {
      readProperties(logger, classLoader, propFilePrefix + "_" + locale.substring(0, idx), props);
    }
    if (locale.length() > 0) {
      readProperties(logger, classLoader, propFilePrefix + "_" + locale, props);
    }
    return props;
  }
  
  /**
   * Load a single localized properties file, adding to an existing LocalizedProperties object.
   * 
   * @param logger TreeLogger instance
   * @param classLoader class loader to use to find property file
   * @param propFile property file name
   * @param props existing LocalizedProperties object to add to
   * @throws UnableToCompleteException if an error occurs reading the file
   */
  private void readProperties(TreeLogger logger, ClassLoader classLoader,
      String propFile, LocalizedProperties props) throws UnableToCompleteException {
    propFile += ".properties";
    InputStream str = null;
    try {
      str = classLoader.getResourceAsStream(propFile);
      if (str != null) {
        props.load(str, "UTF-8");
      }
    } catch (UnsupportedEncodingException e) {
      // UTF-8 should always be defined
      logger.log(TreeLogger.ERROR, "UTF-8 encoding is not defined", e);
      throw new UnableToCompleteException();
    } catch (IOException e) {
      logger.log(TreeLogger.ERROR, "Exception reading " + propFile, e);
      throw new UnableToCompleteException();
    } finally {
      if (str != null) {
        try {
          str.close();
        } catch (IOException e) {
          logger.log(TreeLogger.ERROR, "Exception closing " + propFile, e);
          throw new UnableToCompleteException();
        }
      }
    }
  }
}
