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

import org.apache.tapestry.util.text.LocalizedProperties;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

/**
 * Generator used to generate an implementation of the LocaleInfoImpl class,
 * which is used by the LocaleInfo class.
 */
public class LocaleInfoGenerator extends Generator {

  /**
   * Properties file containing machine-generated locale display names, in
   * their native locales (if possible).
   */
  private static final String GENERATED_LOCALE_NATIVE_DISPLAY_NAMES =
      "com/google/gwt/i18n/client/impl/cldr/LocaleNativeDisplayNames-generated.properties";
  
  /**
   * Properties file containing hand-made corrections to the machine-generated
   * locale display names above.
   */
  private static final String MANUAL_LOCALE_NATIVE_DISPLAY_NAMES =
      "com/google/gwt/i18n/client/impl/cldr/LocaleNativeDisplayNames-manual.properties";

  /**
   * Properties file containing hand-made overrides of locale display names,
   * in their native locales (if possible).
   */
  private static final String OVERRIDE_LOCALE_NATIVE_DISPLAY_NAMES =
      "com/google/gwt/i18n/client/impl/cldr/LocaleNativeDisplayNames-override.properties";
  
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
    String locale = null;
    String[] localeValues = null;
    try {
      locale = propertyOracle.getPropertyValue(logger, PROP_LOCALE);
      localeValues = propertyOracle.getPropertyValueSet(logger, PROP_LOCALE);
    } catch (BadPropertyValueException e) {
      logger.log(TreeLogger.TRACE, "LocaleInfo used without I18N module, using defaults", e);
      return LocaleInfoImpl.class.getName();
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
    String qualName = packageName + "." + className;
    
    PrintWriter pw = context.tryCreate(logger, packageName, className);
    if (pw != null) {
      ClassSourceFileComposerFactory factory = new ClassSourceFileComposerFactory(
          packageName, className);
      factory.setSuperclass(targetClass.getQualifiedSourceName());
      factory.addImport("com.google.gwt.core.client.JavaScriptObject");
      SourceWriter writer = factory.createSourceWriter(context, pw);
      writer.println("private JavaScriptObject nativeDisplayNames;");
      writer.println();
      writer.println("public String[] getAvailableLocaleNames() {");
      writer.println("  return new String[] {");
      for (String propval : localeValues) {
        writer.println("    \"" + propval.replaceAll("\"", "\\\"") + "\",");
      }
      writer.println("  };");
      writer.println("}");
      writer.println();
      writer.println("public String getLocaleName() {");
      writer.println("  return \"" + locale + "\";");
      writer.println("}");
      writer.println();
      writer.println("public native String getLocaleNativeDisplayName(String localeName) /*-{");
      writer.println("  this.@" + qualName + "::ensureNativeDisplayNames()();");
      writer.println("  return this.@" + qualName + "::nativeDisplayNames[localeName] || null;");
      writer.println("}-*/;");
      writer.println();
      writer.println("private native void ensureNativeDisplayNames() /*-{");
      writer.println("  if (this.@" + qualName + "::nativeDisplayNames != null) {");
      writer.println("    return;");
      writer.println("  }");
      writer.println("  this.@" + qualName + "::nativeDisplayNames = {");
      LocalizedProperties displayNames = new LocalizedProperties();
      LocalizedProperties displayNamesManual = new LocalizedProperties();
      LocalizedProperties displayNamesOverride = new LocalizedProperties();
      ClassLoader classLoader = getClass().getClassLoader();
      try {
        InputStream str = classLoader.getResourceAsStream(GENERATED_LOCALE_NATIVE_DISPLAY_NAMES);
        if (str != null) {
          displayNames.load(str, "UTF-8");
        }
        str = classLoader.getResourceAsStream(MANUAL_LOCALE_NATIVE_DISPLAY_NAMES);
        if (str != null) {
          displayNamesManual.load(str, "UTF-8");
        }
        str = classLoader.getResourceAsStream(OVERRIDE_LOCALE_NATIVE_DISPLAY_NAMES);
        if (str != null) {
          displayNamesOverride.load(str, "UTF-8");
        }
      } catch (UnsupportedEncodingException e) {
        // UTF-8 should always be defined
        logger.log(TreeLogger.ERROR, "UTF-8 encoding is not defined", e);
        throw new UnableToCompleteException();
      } catch (IOException e) {
        logger.log(TreeLogger.ERROR, "Exception reading locale display names", e);
        throw new UnableToCompleteException();
      }
      boolean needComma = false;
      for (String propval : localeValues) {
        String displayName = displayNamesOverride.getProperty(propval);
        if (displayName == null) {
          displayName = displayNamesManual.getProperty(propval);
        }
        if (displayName == null) {
          displayName = displayNames.getProperty(propval);
        }
        if (displayName != null && displayName.length() != 0) {
          propval.replace("\"", "\\\"");
          displayName.replace("\"", "\\\"");
          if (needComma) {
            writer.println(",");
          }
          writer.print("    \"" + propval + "\": \"" + displayName + "\"");
          needComma = true;
        }
      }
      if (needComma) {
        writer.println();
      }
      writer.println("  };");
      writer.println("}-*/;");
      writer.commit(logger);
    }
    return packageName + "." + className;
  }
}
