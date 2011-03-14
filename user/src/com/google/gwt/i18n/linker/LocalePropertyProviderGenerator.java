/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.i18n.linker;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.ConfigurationProperty;
import com.google.gwt.core.ext.linker.PropertyProviderGenerator;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.gwt.user.rebind.StringSourceWriter;

import java.util.SortedSet;
import java.util.regex.Pattern;

/**
 * Generates a property provider implementation for the "locale" property.
 */
public class LocalePropertyProviderGenerator implements PropertyProviderGenerator {

  public static final String LOCALE_QUERYPARAM = "locale.queryparam";
  
  public static final String LOCALE_COOKIE = "locale.cookie";
  
  public static final String LOCALE_SEARCHORDER = "locale.searchorder";

  public static final String LOCALE_USEMETA = "locale.usemeta";

  public static final String LOCALE_USERAGENT = "locale.useragent";

  protected static final Pattern COOKIE_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_]*$");

  protected static final Pattern QUERYPARAM_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_]*$");

  /**
   * Return true when the supplied value represents a true/yes/on value.
   * 
   * @param value
   * @return true if the string represents true/yes/on
   */
  protected static boolean isTrue(String value) {
    return value != null && ("yes".equalsIgnoreCase(value)
        || "y".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value)
        || "on".equalsIgnoreCase(value));
  }

  public String generate(TreeLogger logger, SortedSet<String> possibleValues,
      String fallback, SortedSet<ConfigurationProperty> configProperties)
      throws UnableToCompleteException {
    // get relevant config property values
    String localeQueryParam = null;
    String localeCookie = null;
    boolean localeUserAgent = false;
    boolean localeUseMeta = false;
    String localeSearchOrder = "queryparam,cookie,meta,useragent";
    for (ConfigurationProperty configProp : configProperties) {
      String name = configProp.getName();
      if (LOCALE_QUERYPARAM.equals(name)) {
        localeQueryParam = configProp.getValues().get(0);
        if (localeQueryParam != null && localeQueryParam.length() != 0
            && !validateQueryParam(localeQueryParam)) {
          logger.log(TreeLogger.WARN, "Ignoring invalid value of '"
              + localeQueryParam + "' from '" + LOCALE_QUERYPARAM
              + "', not a valid query parameter name");
          localeQueryParam = null;
        }
      } else if (LOCALE_COOKIE.equals(name)) {
          localeCookie = configProp.getValues().get(0);
          if (localeCookie != null && localeCookie.length() != 0
              && !validateCookieName(localeCookie)) {
            logger.log(TreeLogger.WARN, "Ignoring invalid value of '"
                + localeCookie + "' from '" + LOCALE_COOKIE
                + "', not a valid cookie name");
            localeCookie = null;
          }
      } else if (LOCALE_USEMETA.equals(name)) {
        localeUseMeta = isTrue(configProp.getValues().get(0));
      } else if (LOCALE_USERAGENT.equals(name)) {
        localeUserAgent = isTrue(configProp.getValues().get(0));
      } else if (LOCALE_SEARCHORDER.equals(name)) {
        localeSearchOrder = configProp.getValues().get(0);
      }
    }
    // provide a default for the search order
    localeSearchOrder = localeSearchOrder.trim();
    if (localeSearchOrder == null || localeSearchOrder.length() == 0) {
      localeSearchOrder = "queryparam,cookie,meta,useragent";
    }

    if (fallback == null) {
      // TODO(jat): define this in a common place
      fallback = "default";
    }

    // build property provider body
    StringSourceWriter body = new StringSourceWriter();
    body.println("{");
    body.indent();
    body.println("var locale = null;");
    body.println("var rtlocale = '" + fallback + "';");
    body.println("try {");
    for (String method : localeSearchOrder.split(",")) {
      if ("queryparam".equals(method)) {
        if (localeQueryParam != null && localeQueryParam.length() > 0) {
          body.println("if (!locale) {");
          body.indent();
          generateQueryParamLookup(logger, body, localeQueryParam);
          body.outdent();
          body.println("}");
        }
      } else if ("cookie".equals(method)) {
        if (localeCookie != null && localeCookie.length() > 0) {
          body.println("if (!locale) {");
          body.indent();
          generateCookieLookup(logger, body, localeCookie);
          body.outdent();
          body.println("}");
        }
      } else if ("meta".equals(method)) {
        if (localeUseMeta) {
          body.println("if (!locale) {");
          body.indent();
          generateMetaLookup(logger, body);
          body.outdent();
          body.println("}");
        }
      } else if ("useragent".equals(method)) {
        if (localeUserAgent) {
          body.println("if (!locale) {");
          body.indent();
          generateUserAgentLookup(logger, body);
          body.outdent();
          body.println("}");
        }
      } else {
        logger.log(TreeLogger.WARN, "Ignoring unknown locale lookup method \""
            + method + "\"");
        body.println("// ignoring invalid lookup method '" + method + "'");
      }
    }
    body.println("if (!locale) {");
    body.indent();
    body.println("locale = $wnd['__gwt_Locale'];");
    body.outdent();
    body.println("}");
    body.println("if (locale) {");
    body.indent();
    body.println("rtlocale = locale;");
    body.outdent();
    body.println("}");
    generateInheritanceLookup(logger, body);
    body.outdent();
    body.println("} catch (e) {");
    body.indent();
    body.println("alert(\"Unexpected exception in locale detection, using "
        + "default: \" + e);\n");
    body.outdent();
    body.println("}");
    body.println("$wnd['__gwt_Locale'] = rtlocale;");
    body.println("return locale || \"" + fallback + "\";");
    body.outdent();
    body.println("}");
    return body.toString();
  }

  /**
   * Generate JS code that looks up the locale value from a cookie.
   *
   * @param logger logger to use
   * @param body
   * @param cookieName
   * @throws UnableToCompleteException
   */
  protected void generateCookieLookup(TreeLogger logger, SourceWriter body,
      String cookieName) throws UnableToCompleteException  {
    body.println("var cookies = $doc.cookie;");
    body.println("var idx = cookies.indexOf(\"" + cookieName + "=\");");
    body.println("if (idx >= 0) {");
    body.indent();
    body.println("var end = cookies.indexOf(';', idx);");
    body.println("if (end < 0) {");
    body.indent();
    body.println("end = cookies.length;");
    body.outdent();
    body.println("}");
    body.println("locale = cookies.substring(idx + " + (cookieName.length() + 1)
        + ", end);");
    body.outdent();
    body.println("}");
  }

  /**
   * Generate JS code that takes the value of the "locale" variable and finds
   * parent locales until the value is a supported locale or the default locale.
   * 
   * @param logger logger to use
   * @param body
   * @throws UnableToCompleteException
   */
  protected void generateInheritanceLookup(TreeLogger logger, SourceWriter body)
      throws UnableToCompleteException  {
    body.println("while (locale && !__gwt_isKnownPropertyValue(\"locale\", locale)) {");
    body.indent();
    body.println("var lastIndex = locale.lastIndexOf(\"_\");");
    body.println("if (lastIndex < 0) {");
    body.indent();
    body.println("locale = null;");
    body.println("break;");
    body.outdent();
    body.println("}");
    body.println("locale = locale.substring(0, lastIndex);");
    body.outdent();
    body.println("}");
  }

  /**
   * Generate JS code to fetch the locale from a meta property.
   *
   * @param logger logger to use
   * @param body
   * @throws UnableToCompleteException
   */
  protected void generateMetaLookup(TreeLogger logger, SourceWriter body)
      throws UnableToCompleteException  {
    // TODO(jat): do we want to allow customizing the meta property name?
    body.println("locale = __gwt_getMetaProperty(\"locale\");");
  }

  /**
   * Generate JS code to get the locale from a query parameter.
   *
   * @param logger logger to use
   * @param body where to append JS output
   * @param queryParam the query parameter to use
   * @throws UnableToCompleteException
   */
  protected void generateQueryParamLookup(TreeLogger logger, SourceWriter body,
      String queryParam) throws UnableToCompleteException  {
    body.println("var queryParam = location.search;");
    body.println("var qpStart = queryParam.indexOf(\"" + queryParam + "=\");");
    body.println("if (qpStart >= 0) {");
    body.indent();
    body.println("var value = queryParam.substring(qpStart + "
        + (queryParam.length() + 1) + ");");
    body.println("var end = queryParam.indexOf(\"&\", qpStart);");
    body.println("if (end < 0) {");
    body.indent();
    body.println("end = queryParam.length;");
    body.outdent();
    body.println("}");
    body.println("locale = queryParam.substring(qpStart + "
        + (queryParam.length() + 1) + ", end);");
    body.outdent();
    body.println("}");
  }

  /**
   * Generate JS code to fetch the locale from the user agent's compile-time
   * locale.
   *
   * @param logger logger to use
   * @param body
   * @throws UnableToCompleteException
   */
  protected void generateUserAgentLookup(TreeLogger logger, SourceWriter body)
      throws UnableToCompleteException {
    body.println("var language = navigator.browserLanguage ? "
        + "navigator.browserLanguage : navigator.language;");
    body.println("if (language) {");
    body.indent();
    body.println("var parts = language.split(/[-_]/);");
    body.println("if (parts.length > 1) {");
    body.indent();
    body.println("parts[1] = parts[1].toUpperCase();");
    body.outdent();
    body.println("}");
    body.println("locale = parts.join(\"_\");");
    body.outdent();
    body.println("}");
  }

  /**
   * Validate that a name is a valid cookie name.
   * 
   * @param cookieName
   * @return true if cookieName is an acceptable cookie name
   */
  protected boolean validateCookieName(String cookieName) {
    return COOKIE_PATTERN.matcher(cookieName).matches();
  }

  /**
   * Validate that a value is a valid query parameter name.
   * 
   * @param queryParam
   * @return true if queryParam is a valid query parameter name. 
   */
  protected boolean validateQueryParam(String queryParam) {
    return QUERYPARAM_PATTERN.matcher(queryParam).matches();
  }
}
