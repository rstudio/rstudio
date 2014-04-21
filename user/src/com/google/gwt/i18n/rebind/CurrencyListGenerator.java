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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.Generator.RunsLocal;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.i18n.client.CurrencyList;
import com.google.gwt.i18n.client.impl.CurrencyDataImpl;
import com.google.gwt.i18n.shared.GwtLocale;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

import org.apache.tapestry.util.text.LocalizedProperties;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Generator used to generate a localized version of CurrencyList, which contains the list of
 * currencies (with names, symbols, and other information) localized to the current locale.
 */
@RunsLocal(requiresProperties = {"locale.queryparam", "locale", "runtime.locales", "locale.cookie"})
public class CurrencyListGenerator extends Generator {

  /**
   * Immutable collection of data about a currency in a locale, built from the
   * CurrencyData and CurrencyExtra properties files.
   */
  private static class CurrencyInfo {

    private static final Pattern SPLIT_VERTICALBAR = Pattern.compile("\\|");

    private final String code;

    private final String displayName;

    private final int flags;

    private final boolean obsolete;

    private final String portableSymbol;

    private String simpleSymbol;

    private final String symbol;

    /**
     * Create an instance.
     *
     * currencyData format:
     *
     * <pre>
     *       display name|symbol|decimal digits|not-used-flag
     * </pre>
     *
     * <ul>
     * <li>If a symbol is not supplied, the currency code will be used
     * <li>If # of decimal digits is omitted, 2 is used
     * <li>If a currency is not generally used, not-used-flag=1
     * <li>Trailing empty fields can be omitted
     * <li>If null, use currencyCode as the display name
     * </ul>
     *
     * extraData format:
     *
     * <pre>
     *       portable symbol|flags|currency symbol override
     *     flags are space separated list of:
     *       At most one of the following:
     *         SymPrefix     The currency symbol goes before the number,
     *                       regardless of the normal position for this locale.
     *         SymSuffix     The currency symbol goes after the number,
     *                       regardless of the normal position for this locale.
     *
     *       At most one of the following:
     *         ForceSpace    Always add a space between the currency symbol
     *                       and the number.
     *         ForceNoSpace  Never add a space between the currency symbol
     *                       and the number.
     * </pre>
     *
     * @param currencyCode ISO4217 currency code
     * @param currencyData entry from a CurrencyData properties file
     * @param extraData entry from a CurrencyExtra properties file
     * @throws NumberFormatException
     */
    public CurrencyInfo(String currencyCode, String currencyData,
        String extraData) throws NumberFormatException {
      code = currencyCode;
      if (currencyData == null) {
        currencyData = currencyCode;
      }
      String[] currencySplit = SPLIT_VERTICALBAR.split(currencyData);
      String currencyDisplay = currencySplit[0];
      String currencySymbol = null;
      String simpleCurrencySymbol = null;
      if (currencySplit.length > 1 && currencySplit[1].length() > 0) {
        currencySymbol = currencySplit[1];
      }
      int currencyFractionDigits = 2;
      if (currencySplit.length > 2 && currencySplit[2].length() > 0) {
        currencyFractionDigits = Integer.valueOf(currencySplit[2]);
      }
      boolean currencyObsolete = false;
      if (currencySplit.length > 3 && currencySplit[3].length() > 0) {
        currencyObsolete = Integer.valueOf(currencySplit[3]) != 0;
      }
      int currencyFlags = currencyFractionDigits;
      if (currencyObsolete) {
        currencyFlags |= CurrencyDataImpl.DEPRECATED_FLAG;
      }
      String currencyPortableSymbol = "";
      if (extraData != null) {
        // CurrencyExtra contains up to 3 fields separated by |
        // 0 - portable currency symbol
        // 1 - space-separated flags regarding currency symbol
        // positioning/spacing
        // 2 - override of CLDR-derived currency symbol
        // 3 - simple currency symbol
        String[] extraSplit = SPLIT_VERTICALBAR.split(extraData);
        currencyPortableSymbol = extraSplit[0];
        if (extraSplit.length > 1) {
          if (extraSplit[1].contains("SymPrefix")) {
            currencyFlags |= CurrencyDataImpl.POS_FIXED_FLAG;
          } else if (extraSplit[1].contains("SymSuffix")) {
            currencyFlags |= CurrencyDataImpl.POS_FIXED_FLAG
                | CurrencyDataImpl.POS_SUFFIX_FLAG;
          }
          if (extraSplit[1].contains("ForceSpace")) {
            currencyFlags |= CurrencyDataImpl.SPACING_FIXED_FLAG
                | CurrencyDataImpl.SPACE_FORCED_FLAG;
          } else if (extraSplit[1].contains("ForceNoSpace")) {
            currencyFlags |= CurrencyDataImpl.SPACING_FIXED_FLAG;
          }
        }
        // If a non-empty override is supplied, use it for the currency
        // symbol.
        if (extraSplit.length > 2 && extraSplit[2].length() > 0) {
          currencySymbol = extraSplit[2];
        }
        // If a non-empty simple symbol is supplied, use it for the currency
        // symbol.
        if (extraSplit.length > 3 && extraSplit[3].length() > 0) {
          simpleCurrencySymbol = extraSplit[3];
        }
        // If we don't have a currency symbol yet, use the portable symbol if
        // supplied.
        if (currencySymbol == null && currencyPortableSymbol.length() > 0) {
          currencySymbol = currencyPortableSymbol;
        }
      }
      // If all else fails, use the currency code as the symbol.
      if (currencySymbol == null) {
        currencySymbol = currencyCode;
      }
      if (currencyPortableSymbol.length() == 0) {
        currencyPortableSymbol = currencySymbol;
      }
      if (simpleCurrencySymbol == null) {
        simpleCurrencySymbol = currencySymbol;
      }
      displayName = currencyDisplay;
      symbol = currencySymbol;
      flags = currencyFlags;
      portableSymbol = currencyPortableSymbol;
      simpleSymbol = simpleCurrencySymbol;
      obsolete = currencyObsolete;
    }

    public String getDisplayName() {
      return displayName;
    }

    public String getJava() {
      StringBuilder buf = new StringBuilder();
      buf.append("new CurrencyDataImpl(\"").append(quote(code)).append("\", \"");
      buf.append(quote(symbol)).append("\", ").append(flags);
      buf.append(", \"").append(quote(portableSymbol)).append('\"');
      buf.append(", \"").append(quote(simpleSymbol)).append('\"');
      return buf.append(')').toString();
    }

    public String getJson() {
      StringBuilder buf = new StringBuilder();
      buf.append("[ \"").append(quote(code)).append("\", \"");
      buf.append(quote(symbol)).append("\", ").append(flags);
      buf.append(", \"").append(quote(portableSymbol)).append('\"');
      buf.append(", \"").append(quote(simpleSymbol)).append('\"');
      return buf.append(']').toString();
    }
  }

  private static final String CURRENCY_DATA = CurrencyDataImpl.class.getCanonicalName();

  /**
   * Prefix for properties files containing CLDR-derived currency data for each
   * locale.
   */
  private static final String CURRENCY_DATA_PREFIX =
      "com/google/gwt/i18n/client/impl/cldr/CurrencyData";

  /**
   * Prefix for properties files containing additional flags about currencies
   * each locale, which are not present in CLDR.
   */
  private static final String CURRENCY_EXTRA_PREFIX =
      "com/google/gwt/i18n/client/constants/CurrencyExtra";

  private static final String CURRENCY_LIST = CurrencyList.class.getCanonicalName();

  private static final String HASHMAP = HashMap.class.getCanonicalName();

  private static final String JAVASCRIPTOBJECT = JavaScriptObject.class.getCanonicalName();

  /**
   * Prefix for properties files containing number formatting constants for each
   * locale. We use this only to get the default currency for our current
   * locale.
   */
  private static final String NUMBER_CONSTANTS_PREFIX =
      "com/google/gwt/i18n/client/constants/NumberConstantsImpl";

  /**
   * Backslash-escape any double quotes in the supplied string.
   *
   * @param str string to quote
   * @return string with double quotes backslash-escaped.
   */
  private static String quote(String str) {
    return str.replace("\"", "\\\"");
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
  public final String generate(TreeLogger logger, GeneratorContext context,
      String typeName) throws UnableToCompleteException {
    assert CURRENCY_LIST.equals(typeName);
    TypeOracle typeOracle = context.getTypeOracle();

    PropertyOracle propertyOracle = context.getPropertyOracle();
    LocaleUtils localeUtils = LocaleUtils.getInstance(logger, propertyOracle,
        context);
    GwtLocale locale = localeUtils.getCompileLocale();
    Set<GwtLocale> runtimeLocales = localeUtils.getRuntimeLocales();

    JClassType targetClass;
    try {
      targetClass = typeOracle.getType(typeName);
    } catch (NotFoundException e) {
      logger.log(TreeLogger.ERROR, "No such type", e);
      throw new UnableToCompleteException();
    }
    if (runtimeLocales.isEmpty()) {
      return generateLocaleTree(logger, context, targetClass, locale);
    }
    CachedGeneratorContext cachedContext = new CachedGeneratorContext(context);
    return generateRuntimeSelection(logger, cachedContext, targetClass, locale,
        runtimeLocales);
  }

  /**
   * Generate an implementation class for the requested locale, including all
   * parent classes along the inheritance chain. The data will be kept at the
   * location in the inheritance chain where it was defined in properties files.
   *
   * @param logger
   * @param context
   * @param targetClass
   * @param locale
   * @return generated class name for the requested locale
   */
  private String generateLocaleTree(TreeLogger logger, GeneratorContext context,
      JClassType targetClass, GwtLocale locale) {
    String superClassName = CURRENCY_LIST;
    List<GwtLocale> searchList = locale.getCompleteSearchList();

    /**
     * Map of currency code -> CurrencyInfo for that code.
     */
    Map<String, CurrencyInfo> allCurrencyData = new HashMap<String, CurrencyInfo>();

    LocalizedProperties currencyExtra = null;
    /*
     * The searchList is guaranteed to be ordered such that subclasses always
     * precede superclasses. Therefore, we iterate backwards to ensure that
     * superclasses are always generated first.
     */
    String lastDefaultCurrencyCode = null;
    for (int i = searchList.size(); i-- > 0;) {
      GwtLocale search = searchList.get(i);
      LocalizedProperties newExtra = getProperties(CURRENCY_EXTRA_PREFIX,
          search);
      if (newExtra != null) {
        currencyExtra = newExtra;
      }
      Map<String, String> currencyData = getCurrencyData(search);
      Set<String> keySet = currencyData.keySet();
      String[] currencies = new String[keySet.size()];
      keySet.toArray(currencies);
      Arrays.sort(currencies);

      // Go ahead and populate the data map.
      for (String currencyCode : currencies) {
        String extraData = currencyExtra == null ? null
            : currencyExtra.getProperty(currencyCode);
        allCurrencyData.put(currencyCode, new CurrencyInfo(currencyCode,
            currencyData.get(currencyCode), extraData));
      }

      String defCurrencyCode = getDefaultCurrency(search);
      // If this locale specifies a particular locale, or the one that is
      // inherited has been changed in this locale, re-specify the default
      // currency so the method will be generated.
      if (defCurrencyCode == null && keySet.contains(lastDefaultCurrencyCode)) {
        defCurrencyCode = lastDefaultCurrencyCode;
      }
      if (!currencyData.isEmpty() || defCurrencyCode != null) {
        String newClass =
            generateOneLocale(logger, context, targetClass, search,
                superClassName, currencies, allCurrencyData, defCurrencyCode);
        superClassName = newClass;
        lastDefaultCurrencyCode = defCurrencyCode;
      }
    }
    return superClassName;
  }

  /**
   * Generate the implementation for a single locale, overriding from its parent
   * only data that has changed in this locale.
   *
   * @param logger
   * @param context
   * @param targetClass
   * @param locale
   * @param superClassName
   * @param currencies the set of currencies defined in this locale
   * @param allCurrencyData map of currency code -> unparsed CurrencyInfo for
   *        that code
   * @param defCurrencyCode default currency code for this locale
   * @return fully-qualified class name generated
   */
  private String generateOneLocale(TreeLogger logger, GeneratorContext context,
      JClassType targetClass, GwtLocale locale, String superClassName,
      String[] currencies, Map<String, CurrencyInfo> allCurrencyData,
      String defCurrencyCode) {
    String packageName = targetClass.getPackage().getName();
    String className = targetClass.getName().replace('.', '_') + "_"
        + locale.getAsString();
    PrintWriter pw = context.tryCreate(logger, packageName, className);
    if (pw != null) {
      ClassSourceFileComposerFactory factory =
          new ClassSourceFileComposerFactory(packageName, className);
      factory.setSuperclass(superClassName);
      factory.addImport(CURRENCY_DATA);
      factory.addImport(JAVASCRIPTOBJECT);
      factory.addImport(HASHMAP);
      SourceWriter writer = factory.createSourceWriter(context, pw);
      if (defCurrencyCode != null) {
        CurrencyInfo currencyInfo = allCurrencyData.get(defCurrencyCode);
        if (currencyInfo == null) {
          // Synthesize a null info if the specified default wasn't found.
          currencyInfo = new CurrencyInfo(defCurrencyCode, null, null);
          allCurrencyData.put(defCurrencyCode, currencyInfo);
        }
        writer.println();
        writer.println("@Override");
        writer.println("protected CurrencyData getDefaultJava() {");
        writer.println("  return " + currencyInfo.getJava() + ";");
        writer.println("}");
        writer.println();
        writer.println("@Override");
        writer.println("protected native CurrencyData getDefaultNative() /*-{");
        writer.println("  return " + currencyInfo.getJson() + ";");
        writer.println("}-*/;");
      }
      if (currencies.length > 0) {
        writeCurrencyMethodJava(writer, currencies, allCurrencyData);
        writeCurrencyMethodNative(writer, currencies, allCurrencyData);
        writeNamesMethodJava(writer, currencies, allCurrencyData);
        writeNamesMethodNative(writer, currencies, allCurrencyData);
      }
      writer.commit(logger);
    }
    return packageName + "." + className;
  }

  /**
   * Generate a class which can select between alternate implementations at
   * runtime based on the runtime locale.
   *
   * @param logger TreeLogger instance for log messages
   * @param context GeneratorContext for generating source files
   * @param targetClass class to generate
   * @param compileLocale the compile-time locale we are generating for
   * @param locales set of all locales to generate
   * @return fully-qualified class name that was generated
   */
  private String generateRuntimeSelection(TreeLogger logger,
      GeneratorContext context, JClassType targetClass, GwtLocale compileLocale,
      Set<GwtLocale> locales) {
    String packageName = targetClass.getPackage().getName();
    String className =
        targetClass.getName().replace('.', '_') + "_"
            + compileLocale.getAsString() + "_runtimeSelection";
    PrintWriter pw = context.tryCreate(logger, packageName, className);
    if (pw != null) {
      ClassSourceFileComposerFactory factory =
          new ClassSourceFileComposerFactory(packageName, className);
      factory.setSuperclass(targetClass.getQualifiedSourceName());
      factory.addImport(CURRENCY_DATA);
      factory.addImport(JAVASCRIPTOBJECT);
      factory.addImport(HASHMAP);
      factory.addImport("com.google.gwt.i18n.client.LocaleInfo");
      SourceWriter writer = factory.createSourceWriter(context, pw);
      writer.println("private CurrencyList instance;");
      writer.println();
      writer.println("@Override");
      writer.println("protected CurrencyData getDefaultJava() {");
      writer.println("  ensureInstance();");
      writer.println("  return instance.getDefaultJava();");
      writer.println("}");
      writer.println();
      writer.println("@Override");
      writer.println("protected CurrencyData getDefaultNative() {");
      writer.println("  ensureInstance();");
      writer.println("  return instance.getDefaultNative();");
      writer.println("}");
      writer.println();
      writer.println("@Override");
      writer.println("protected HashMap<String, CurrencyData> loadCurrencyMapJava() {");
      writer.println("  ensureInstance();");
      writer.println("  return instance.loadCurrencyMapJava();");
      writer.println("}");
      writer.println();
      writer.println("@Override");
      writer.println("protected JavaScriptObject loadCurrencyMapNative() {");
      writer.println("  ensureInstance();");
      writer.println("  return instance.loadCurrencyMapNative();");
      writer.println("}");
      writer.println();
      writer.println("@Override");
      writer.println("protected HashMap<String, String> loadNamesMapJava() {");
      writer.println("  ensureInstance();");
      writer.println("  return instance.loadNamesMapJava();");
      writer.println("}");
      writer.println();
      writer.println("@Override");
      writer.println("protected JavaScriptObject loadNamesMapNative() {");
      writer.println("  ensureInstance();");
      writer.println("  return instance.loadNamesMapNative();");
      writer.println("}");
      writer.println();
      writer.println("private void ensureInstance() {");
      writer.indent();
      writer.println("if (instance != null) {");
      writer.println("  return;");
      writer.println("}");
      boolean fetchedLocale = false;
      Map<String, Set<GwtLocale>> localeMap = new TreeMap<String,
          Set<GwtLocale>>();
      String compileLocaleClass =
          processChildLocale(logger, context, targetClass, localeMap,
              compileLocale);
      if (compileLocaleClass == null) {
        // already gave warning, just use default implementation
        return null;
      }
      for (GwtLocale runtimeLocale : locales) {
        processChildLocale(logger, context, targetClass, localeMap,
            runtimeLocale);
      }
      for (Entry<String, Set<GwtLocale>> entry : localeMap.entrySet()) {
        if (!fetchedLocale) {
          writer.println("String runtimeLocale = LocaleInfo.getCurrentLocale().getLocaleName();");
          fetchedLocale = true;
        }
        boolean firstLocale = true;
        String generatedClass = entry.getKey();
        if (compileLocaleClass.equals(generatedClass)) {
          // The catch-all will handle this
          continue;
        }
        writer.print("if (");
        for (GwtLocale locale : entry.getValue()) {
          if (firstLocale) {
            firstLocale = false;
          } else {
            writer.println();
            writer.print("    || ");
          }
          writer.print("\"" + locale.toString() + "\".equals(runtimeLocale)");
        }
        writer.println(") {");
        writer.println("  instance = new " + generatedClass + "();");
        writer.println("  return;");
        writer.println("}");
      }
      writer.println("instance = new " + compileLocaleClass + "();");
      writer.outdent();
      writer.println("}");
      writer.commit(logger);
    }
    return packageName + "." + className;
  }

  /**
   * Return a map of currency data for the requested locale, or null if there is
   * not one (not that inheritance is not handled here).
   * <p/>
   * The keys are ISO4217 currency codes. The format of the map values is:
   *
   * <pre>
   * display name|symbol|decimal digits|not-used-flag
   * </pre>
   *
   * If a symbol is not supplied, the currency code will be used If # of decimal
   * digits is omitted, 2 is used If a currency is not generally used,
   * not-used-flag=1 Trailing empty fields can be omitted
   *
   * @param locale
   * @return currency data map
   */
  @SuppressWarnings("unchecked")
  private Map<String, String> getCurrencyData(GwtLocale locale) {
    LocalizedProperties currencyData = getProperties(CURRENCY_DATA_PREFIX,
        locale);
    if (currencyData == null) {
      return Collections.emptyMap();
    }
    return currencyData.getPropertyMap();
  }

  /**
   * Returns the default currency code for the requested locale.
   *
   * @param locale
   * @return ISO4217 currency code
   */
  private String getDefaultCurrency(GwtLocale locale) {
    String defCurrencyCode = null;
    LocalizedProperties numberConstants = getProperties(NUMBER_CONSTANTS_PREFIX, locale);
    if (numberConstants != null) {
      defCurrencyCode = numberConstants.getProperty("defCurrencyCode");
    }
    if (defCurrencyCode == null && locale.isDefault()) {
      defCurrencyCode = "USD";
    }
    return defCurrencyCode;
  }

  /**
   * Load a properties file for a given locale. Note that locale inheritance is
   * the responsibility of the caller.
   *
   * @param prefix classpath prefix of properties file
   * @param locale locale to load
   * @return LocalizedProperties instance containing properties file or null if
   *         not found.
   */
  private LocalizedProperties getProperties(String prefix, GwtLocale locale) {
    String propFile = prefix;
    if (!locale.isDefault()) {
      propFile += "_" + locale.getAsString();
    }
    propFile += ".properties";
    InputStream str = null;
    ClassLoader classLoader = getClass().getClassLoader();
    LocalizedProperties props = new LocalizedProperties();
    try {
      str = classLoader.getResourceAsStream(propFile);
      if (str != null) {
        props.load(str, "UTF-8");
        return props;
      }
    } catch (UnsupportedEncodingException e) {
      // UTF-8 should always be defined
      return null;
    } catch (IOException e) {
      return null;
    } finally {
      if (str != null) {
        try {
          str.close();
        } catch (IOException e) {
        }
      }
    }
    return null;
  }

  /**
   * Generate an implementation for a runtime locale, to be referenced from the
   * generated runtime selection code.
   *
   * @param logger
   * @param context
   * @param targetClass
   * @param localeMap
   * @param locale
   * @return class name of the generated class, or null if failed
   */
  private String processChildLocale(TreeLogger logger, GeneratorContext context,
      JClassType targetClass, Map<String, Set<GwtLocale>> localeMap,
      GwtLocale locale) {
    String generatedClass = generateLocaleTree(logger, context, targetClass,
        locale);
    if (generatedClass == null) {
      logger.log(TreeLogger.ERROR, "Failed to generate "
          + targetClass.getQualifiedSourceName() + " in locale "
          + locale.toString());
      // skip failed locale
      return null;
    }
    Set<GwtLocale> locales = localeMap.get(generatedClass);
    if (locales == null) {
      locales = new HashSet<GwtLocale>();
      localeMap.put(generatedClass, locales);
    }
    locales.add(locale);
    return generatedClass;
  }

  /**
   * Writes a loadCurrencyMapJava method for the current locale, based on its
   * currency data and its superclass (if any). As currencies are included in
   * this method, their names are added to {@code nameMap} for later use.
   *
   * If no new currency data is added for this locale over its superclass, the
   * method is omitted entirely.
   *
   * @param writer SourceWriter instance to use for writing the class
   * @param currencies array of valid currency names in the order they should be
   *          listed
   * @param allCurrencyData map of currency codes to currency data for the
   *          current locale, including all inherited currencies data
   */
  private void writeCurrencyMethodJava(SourceWriter writer,
      String[] currencies, Map<String, CurrencyInfo> allCurrencyData) {
    boolean needHeader = true;
    for (String currencyCode : currencies) {
      // TODO(jat): only emit new data where it differs from superclass!
      CurrencyInfo currencyInfo = allCurrencyData.get(currencyCode);
      if (needHeader) {
        needHeader = false;
        writer.println();
        writer.println("@Override");
        writer.println("protected HashMap<String, CurrencyData> loadCurrencyMapJava() {");
        writer.indent();
        writer.println("HashMap<String, CurrencyData> result = super.loadCurrencyMapJava();");
      }
      writer.println("// " + currencyInfo.getDisplayName());
      writer.println("result.put(\"" + quote(currencyCode) + "\", "
          + currencyInfo.getJava() + ");");
    }
    if (!needHeader) {
      writer.println("return result;");
      writer.outdent();
      writer.println("}");
    }
  }

  /**
   * Writes a loadCurrencyMapNative method for the current locale, based on its
   * currency data and its superclass (if any). As currencies are included in
   * this method, their names are added to {@code nameMap} for later use.
   *
   * If no new currency data is added for this locale over its superclass, the
   * method is omitted entirely.
   *
   * @param writer SourceWriter instance to use for writing the class
   * @param currencies array of valid currency names in the order they should be
   *          listed
   * @param allCurrencyData map of currency codes to currency data for the
   *          current locale, including all inherited currencies data
   */
  private void writeCurrencyMethodNative(SourceWriter writer,
      String[] currencies, Map<String, CurrencyInfo> allCurrencyData) {
    boolean needHeader = true;
    for (String currencyCode : currencies) {
      // TODO(jat): only emit new data where it differs from superclass!
      CurrencyInfo currencyInfo = allCurrencyData.get(currencyCode);
      if (needHeader) {
        needHeader = false;
        writer.println();
        writer.println("@Override");
        writer.println("protected JavaScriptObject loadCurrencyMapNative() {");
        writer.indent();
        writer.println("return overrideMap(super.loadCurrencyMapNative(), loadMyCurrencyMapOverridesNative());");
        writer.outdent();
        writer.println("}");
        writer.println();
        writer.println("private native JavaScriptObject loadMyCurrencyMapOverridesNative() /*-{");
        writer.indent();
        writer.println("return {");
        writer.indent();
      }
      writer.println("// " + currencyInfo.getDisplayName());
      writer.println("\"" + quote(currencyCode) + "\": "
          + currencyInfo.getJson() + ",");
    }
    if (!needHeader) {
      writer.outdent();
      writer.println("};");
      writer.outdent();
      writer.println("}-*/;");
    }
  }

  /**
   * Writes a loadNamesMapJava method for the current locale, based on its the
   * supplied names map and its superclass (if any).
   *
   * If no new names are added for this locale over its superclass, the method
   * is omitted entirely.
   *
   * @param writer SourceWriter instance to use for writing the class
   * @param currencies array of valid currency names in the order they should be
   *          listed
   * @param allCurrencyData map of currency codes to currency data for the
   *          current locale, including all inherited currencies data
   */
  private void writeNamesMethodJava(SourceWriter writer, String[] currencies,
      Map<String, CurrencyInfo> allCurrencyData) {
    boolean needHeader = true;
    for (String currencyCode : currencies) {
      // TODO(jat): only emit new data where it differs from superclass!
      CurrencyInfo currencyInfo = allCurrencyData.get(currencyCode);
      String displayName = currencyInfo.getDisplayName();
      if (displayName != null && !currencyCode.equals(displayName)) {
        if (needHeader) {
          needHeader = false;
          writer.println();
          writer.println("@Override");
          writer.println("protected HashMap<String, String> loadNamesMapJava() {");
          writer.indent();
          writer.println("HashMap<String, String> result = super.loadNamesMapJava();");
        }
        writer.println("result.put(\"" + quote(currencyCode) + "\", \""
            + quote(displayName) + "\");");
      }
    }
    if (!needHeader) {
      writer.println("return result;");
      writer.outdent();
      writer.println("}");
    }
  }

  /**
   * Writes a loadNamesMapNative method for the current locale, based on its the
   * supplied names map and its superclass (if any).
   *
   * If no new names are added for this locale over its superclass, the method
   * is omitted entirely.
   *
   * @param writer SourceWriter instance to use for writing the class
   * @param currencies array of valid currency names in the order they should be
   *          listed
   * @param allCurrencyData map of currency codes to currency data for the
   *          current locale, including all inherited currencies data
   */
  private void writeNamesMethodNative(SourceWriter writer, String[] currencies,
      Map<String, CurrencyInfo> allCurrencyData) {
    boolean needHeader = true;
    for (String currencyCode : currencies) {
      // TODO(jat): only emit new data where it differs from superclass!
      CurrencyInfo currencyInfo = allCurrencyData.get(currencyCode);
      String displayName = currencyInfo.getDisplayName();
      if (displayName != null && !currencyCode.equals(displayName)) {
        if (needHeader) {
          needHeader = false;
          writer.println();
          writer.println("@Override");
          writer.println("protected JavaScriptObject loadNamesMapNative() {");
          writer.indent();
          writer.println("return overrideMap(super.loadNamesMapNative(), loadMyNamesMapOverridesNative());");
          writer.outdent();
          writer.println("}");
          writer.println();
          writer.println("private native JavaScriptObject loadMyNamesMapOverridesNative() /*-{");
          writer.indent();
          writer.println("return {");
          writer.indent();
        }
        writer.println("\"" + quote(currencyCode) + "\": \""
            + quote(displayName) + "\",");
      }
    }
    if (!needHeader) {
      writer.outdent();
      writer.println("};");
      writer.outdent();
      writer.println("}-*/;");
    }
  }
}
