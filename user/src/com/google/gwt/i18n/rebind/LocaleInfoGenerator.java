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
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.i18n.client.impl.LocaleInfoImpl;
import com.google.gwt.i18n.server.GwtLocaleImpl;
import com.google.gwt.i18n.shared.GwtLocale;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

import org.apache.tapestry.util.text.LocalizedProperties;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * Generator used to generate an implementation of the LocaleInfoImpl class,
 * which is used by the LocaleInfo class.
 */
public class LocaleInfoGenerator extends Generator {

  /**
   * Properties file containing machine-generated locale display names, in their
   * native locales (if possible).
   */
  private static final String GENERATED_LOCALE_NATIVE_DISPLAY_NAMES = "com/google/gwt/i18n/client/impl/cldr/LocaleNativeDisplayNames-generated.properties";

  /**
   * Properties file containing hand-made corrections to the machine-generated
   * locale display names above.
   */
  private static final String MANUAL_LOCALE_NATIVE_DISPLAY_NAMES = "com/google/gwt/i18n/client/impl/cldr/LocaleNativeDisplayNames-manual.properties";

  /**
   * Properties file containing hand-made overrides of locale display names, in
   * their native locales (if possible).
   */
  private static final String OVERRIDE_LOCALE_NATIVE_DISPLAY_NAMES = "com/google/gwt/i18n/client/impl/cldr/LocaleNativeDisplayNames-override.properties";

  /**
   * Set of canonical language codes which are RTL.
   */
  private static final Set<String> RTL_LOCALES = new HashSet<String>();

  static {
    // TODO(jat): get this from CLDR data.
    RTL_LOCALES.add("ar");
    RTL_LOCALES.add("fa");
    RTL_LOCALES.add("he");
    RTL_LOCALES.add("ps");
    RTL_LOCALES.add("ur");
  }

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
  public final String generate(TreeLogger logger, final GeneratorContext context,
      String typeName) throws UnableToCompleteException {
    TypeOracle typeOracle = context.getTypeOracle();
    // Get the current locale and interface type.
    PropertyOracle propertyOracle = context.getPropertyOracle();
    LocaleUtils localeUtils = LocaleUtils.getInstance(logger,
        propertyOracle);

    JClassType targetClass;
    try {
      targetClass = typeOracle.getType(typeName);
    } catch (NotFoundException e) {
      logger.log(TreeLogger.ERROR, "No such type " + typeName, e);
      throw new UnableToCompleteException();
    }
    assert (LocaleInfoImpl.class.getName().equals(
        targetClass.getQualifiedSourceName()));

    String packageName = targetClass.getPackage().getName();
    String superClassName = targetClass.getName().replace('.', '_') + "_shared";
    Set<GwtLocale> localeSet = localeUtils.getAllLocales();
    GwtLocaleImpl[] allLocales = localeSet.toArray(
        new GwtLocaleImpl[localeSet.size()]);
    // sort for deterministic output
    Arrays.sort(allLocales);
    PrintWriter pw = context.tryCreate(logger, packageName, superClassName);
    if (pw != null) {
      String qualName = packageName + "." + superClassName;
      ClassSourceFileComposerFactory factory = new ClassSourceFileComposerFactory(
          packageName, superClassName);
      factory.setSuperclass(targetClass.getQualifiedSourceName());
      factory.addImport("com.google.gwt.core.client.JavaScriptObject");
      SourceWriter writer = factory.createSourceWriter(context, pw);
      writer.println("private JavaScriptObject nativeDisplayNames;");
      writer.println();
      writer.println("@Override");
      writer.println("public String[] getAvailableLocaleNames() {");
      writer.println("  return new String[] {");
      boolean hasAnyRtl = false;
      for (GwtLocaleImpl possibleLocale : allLocales) {
        writer.println("    \""
            + possibleLocale.toString().replaceAll("\"", "\\\"") + "\",");
        if (RTL_LOCALES.contains(
            possibleLocale.getCanonicalForm().getLanguage())) {
          hasAnyRtl = true;
        }
      }
      writer.println("  };");
      writer.println("}");
      writer.println();
      writer.println("@Override");
      writer.println("public native String getLocaleNativeDisplayName(String localeName) /*-{");
      writer.println("  this.@" + qualName + "::ensureNativeDisplayNames()();");
      writer.println("  return this.@" + qualName
          + "::nativeDisplayNames[localeName];");
      writer.println("}-*/;");
      writer.println();
      writer.println("@Override");
      writer.println("public boolean hasAnyRTL() {");
      writer.println("  return " + hasAnyRtl + ";");
      writer.println("}");
      writer.println();
      writer.println("private native void ensureNativeDisplayNames() /*-{");
      writer.println("  if (this.@" + qualName
          + "::nativeDisplayNames != null) {");
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
        logger.log(TreeLogger.ERROR, "Exception reading locale display names",
            e);
        throw new UnableToCompleteException();
      }
      boolean needComma = false;
      for (GwtLocaleImpl possibleLocale : allLocales) {
        String localeName = possibleLocale.toString();
        String displayName = displayNamesOverride.getProperty(localeName);
        if (displayName == null) {
          displayName = displayNamesManual.getProperty(localeName);
        }
        if (displayName == null) {
          displayName = displayNames.getProperty(localeName);
        }
        if (displayName != null && displayName.length() != 0) {
          localeName = quoteQuotes(localeName);
          displayName = quoteQuotes(displayName);
          if (needComma) {
            writer.println(",");
          }
          writer.print("    \"" + localeName + "\": \"" + displayName + "\"");
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
    GwtLocale locale = localeUtils.getCompileLocale();
    String className = targetClass.getName().replace('.', '_') + "_"
        + locale.getAsString();
    Set<GwtLocale> runtimeLocales = localeUtils.getRuntimeLocales();
    if (!runtimeLocales.isEmpty()) {
      className += "_runtimeSelection";
    }

    pw = context.tryCreate(logger, packageName, className);
    if (pw != null) {
      ClassSourceFileComposerFactory factory = new ClassSourceFileComposerFactory(
          packageName, className);
      factory.setSuperclass(superClassName);
      factory.addImport("com.google.gwt.core.client.GWT");
      factory.addImport("com.google.gwt.i18n.client.LocaleInfo");
      factory.addImport("com.google.gwt.i18n.client.constants.NumberConstants");
      factory.addImport("com.google.gwt.i18n.client.constants.NumberConstantsImpl");
      factory.addImport("com.google.gwt.i18n.client.constants.DateTimeConstants");
      factory.addImport("com.google.gwt.i18n.client.constants.DateTimeConstantsImpl");
      SourceWriter writer = factory.createSourceWriter(context, pw);
      writer.println("@Override");
      writer.println("public String getLocaleName() {");
      if (runtimeLocales.isEmpty()) {
        writer.println("  return \"" + locale + "\";");
      } else {
        writer.println("  String rtLocale = getRuntimeLocale();");
        writer.println("  return rtLocale != null ? rtLocale : \"" + locale
            + "\";");
      }
      writer.println("}");
      writer.println();
      writer.println("@Override");
      writer.println("public DateTimeConstants getDateTimeConstants() {");
      LocalizableGenerator localizableGenerator = new LocalizableGenerator();
      // Avoid warnings for trying to create the same type multiple times
      GeneratorContext subContext = new CachedGeneratorContext(context);
      generateConstantsLookup(logger, subContext, writer, localizableGenerator,
          runtimeLocales, locale,
          "com.google.gwt.i18n.client.constants.DateTimeConstantsImpl");
      writer.println("}");
      writer.println();
      writer.println("@Override");
      writer.println("public NumberConstants getNumberConstants() {");
      generateConstantsLookup(logger, subContext, writer, localizableGenerator,
          runtimeLocales, locale,
          "com.google.gwt.i18n.client.constants.NumberConstantsImpl");
      writer.println("}");
      writer.commit(logger);
    }
    return packageName + "." + className;
  }

  /**
   * @param logger
   * @param context
   * @param writer
   * @param localizableGenerator 
   * @param runtimeLocales
   * @param locale 
   * @throws UnableToCompleteException
   */
  private void generateConstantsLookup(TreeLogger logger,
      GeneratorContext context, SourceWriter writer,
      LocalizableGenerator localizableGenerator, Set<GwtLocale> runtimeLocales,
      GwtLocale locale, String typeName)
      throws UnableToCompleteException {
    writer.indent();
    boolean fetchedRuntimeLocale = false;
    Map<String, Set<GwtLocale>> localeMap = new HashMap<String, Set<GwtLocale>>();
    generateOneLocale(logger, context, localizableGenerator, typeName,
        localeMap, locale);
    for (GwtLocale runtimeLocale : runtimeLocales) {
      generateOneLocale(logger, context, localizableGenerator, typeName,
          localeMap, runtimeLocale);
    }
    if (localeMap.size() > 1) {
      for (Entry<String, Set<GwtLocale>> entry : localeMap.entrySet()) {
        if (!fetchedRuntimeLocale) {
          writer.println("String runtimeLocale = getLocaleName();");
          fetchedRuntimeLocale = true;
        }
        writer.print("if (");
        boolean firstLocale = true;
        String generatedClass = entry.getKey();
        for (GwtLocale runtimeLocale : entry.getValue()) {
          if (firstLocale) {
            firstLocale = false;
          } else {
            writer.println();
            writer.print("    || ");
          }
          writer.print("\"" + runtimeLocale.toString()
              + "\".equals(runtimeLocale)");
        }
        writer.println(") {");
        writer.println("  return new " + generatedClass + "();");
        writer.println("}");
      }
      // TODO: if we get here, there was an unexpected runtime locale --
      //    should we have an assert or throw an exception?  Currently it
      //    just falls through to the default implementation.
    }
    writer.println("return GWT.create(" + typeName + ".class);");
    writer.outdent();
  }

  /**
   * @param logger
   * @param context
   * @param localizableGenerator
   * @param typeName
   * @param localeMap
   * @param locale
   * @throws UnableToCompleteException
   */
  private void generateOneLocale(TreeLogger logger, GeneratorContext context,
      LocalizableGenerator localizableGenerator, String typeName,
      Map<String, Set<GwtLocale>> localeMap, GwtLocale locale)
      throws UnableToCompleteException {
    String generatedClass = localizableGenerator.generate(logger, context,
        typeName, locale.toString());
    if (generatedClass == null) {
      logger.log(TreeLogger.ERROR, "Failed to generate " + typeName
          + " in locale " + locale.toString());
      // skip failed locale
      return;
    }
    Set<GwtLocale> locales = localeMap.get(generatedClass);
    if (locales == null) {
      locales = new HashSet<GwtLocale>();
      localeMap.put(generatedClass, locales);
    }
    locales.add(locale);
  }
  
  private String quoteQuotes(String val) {
    return val.replace("\"", "\\\"");
  }
}
