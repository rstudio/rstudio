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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JArrayType;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.i18n.client.Messages.Offset;
import com.google.gwt.i18n.client.Messages.Optional;
import com.google.gwt.i18n.client.Messages.PluralCount;
import com.google.gwt.i18n.client.Messages.PluralText;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.i18n.client.PluralRule;
import com.google.gwt.i18n.client.PluralRule.PluralForm;
import com.google.gwt.i18n.client.impl.plurals.DefaultRule;
import com.google.gwt.i18n.rebind.AbstractResource.MissingResourceException;
import com.google.gwt.i18n.rebind.AbstractResource.ResourceEntry;
import com.google.gwt.i18n.rebind.AbstractResource.ResourceList;
import com.google.gwt.i18n.rebind.MessageFormatParser.ArgumentChunk;
import com.google.gwt.i18n.rebind.MessageFormatParser.DefaultTemplateChunkVisitor;
import com.google.gwt.i18n.rebind.MessageFormatParser.StaticArgChunk;
import com.google.gwt.i18n.rebind.MessageFormatParser.StringChunk;
import com.google.gwt.i18n.rebind.MessageFormatParser.TemplateChunk;
import com.google.gwt.i18n.shared.GwtLocale;
import com.google.gwt.safehtml.shared.OnlyToBeUsedInGeneratedCodeStringBlessedAsSafeHtml;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.rebind.AbstractGeneratorClassCreator;
import com.google.gwt.user.rebind.AbstractMethodCreator;

import org.apache.tapestry.util.text.LocalizedPropertiesLoader;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Creator for methods of the Messages interface.
 */
class MessagesMethodCreator extends AbstractMethodCreator {

  /**
   * Implements {x,date...} references in MessageFormat.
   */
  private static class DateFormatter implements ValueFormatter {
    public boolean format(TreeLogger logger, GwtLocale locale,
        StringGenerator out, Map<String, String> formatArgs, String subformat,
        String argName, JType argType, Parameters params) {
      if (!"java.util.Date".equals(argType.getQualifiedSourceName())) {
        logger.log(
            TreeLogger.ERROR, "Only java.util.Date acceptable for date format");
        return true;
      }
      String tzParam = "";
      String tzArg = formatArgs.get("tz");
      if (tzArg != null) {
        if (tzArg.startsWith("$")) {
          int paramNum = params.getParameterIndex(tzArg.substring(1));
          if (paramNum < 0) {
            logger.log(
                TreeLogger.ERROR, "Unable to resolve tz argument " + tzArg);
            return true;
          } else if (!"com.google.gwt.i18n.client.TimeZone".equals(
              params.getParameter(paramNum).getType().getQualifiedSourceName())) {
            logger.log(
                TreeLogger.ERROR, "Currency code parameter must be TimeZone");
            return true;
          } else {
            tzParam = ", arg" + paramNum;
          }
        } else {
          tzParam = ", com.google.gwt.i18n.client.TimeZone.createTimeZone("
              + tzArg + ")";
        }
      }
      if (subformat == null || "medium".equals(subformat)) {
        out.appendStringValuedExpression(
            dtFormatClassName + ".getMediumDateFormat()" + ".format(" + argName
                + tzParam + ")");
      } else if ("full".equals(subformat)) {
        out.appendStringValuedExpression(
            dtFormatClassName + ".getFullDateFormat().format(" + argName
                + tzParam + ")");
      } else if ("long".equals(subformat)) {
        out.appendStringValuedExpression(
            dtFormatClassName + ".getLongDateFormat().format(" + argName
                + tzParam + ")");
      } else if ("short".equals(subformat)) {
        out.appendStringValuedExpression(
            dtFormatClassName + ".getShortDateFormat()" + ".format(" + argName
                + tzParam + ")");
      } else {
        logger.log(TreeLogger.WARN, "Use localdatetime format instead");
        out.appendStringValuedExpression(
            dtFormatClassName + ".getFormat(" + wrap(subformat) + ").format("
                + argName + tzParam + ")");
      }
      return false;
    }
  }

  /**
   * Interface used to abstract away differences between accessing an array and
   * a list.
   */
  private interface ListAccessor {

    String getElement(String element);

    String getSize();
  }

  /**
   * Implementation of ListAccessor for an array.
   */
  private static class ListAccessorArray implements ListAccessor {

    private final int listArgNum;

    public ListAccessorArray(int listArgNum) {
      this.listArgNum = listArgNum;
    }

    public String getElement(String element) {
      return "arg" + listArgNum + "[" + element + "]";
    }

    public String getSize() {
      return "arg" + listArgNum + ".length";
    }
  }

  /**
   * Implementation of ListAccessor for a List.
   */
  private static class ListAccessorList implements ListAccessor {

    private final int listArgNum;

    public ListAccessorList(int listArgNum) {
      this.listArgNum = listArgNum;
    }

    public String getElement(String element) {
      return "arg" + listArgNum + ".get(" + element + ")";
    }

    public String getSize() {
      return "arg" + listArgNum + ".size()";
    }
  }

  /**
   * Implements {x,localdatetime,skeleton} references in MessageFormat.
   */
  private static class LocalDateTimeFormatter implements ValueFormatter {
    private static final String PREDEF = "predef:";

    public boolean format(TreeLogger logger, GwtLocale locale,
        StringGenerator out, Map<String, String> formatArgs, String subformat,
        String argName, JType argType, Parameters params) {
      if (!"java.util.Date".equals(argType.getQualifiedSourceName())) {
        logger.log(TreeLogger.ERROR,
            "Only java.util.Date acceptable for localdatetime format");
        return true;
      }
      if (subformat == null || subformat.length() == 0) {
        logger.log(TreeLogger.ERROR,
            "localdatetime format requires a skeleton pattern");
        return true;
      }
      String tzParam = "";
      String tzArg = formatArgs.get("tz");
      if (tzArg != null) {
        if (tzArg.startsWith("$")) {
          int paramNum = params.getParameterIndex(tzArg.substring(1));
          if (paramNum < 0) {
            logger.log(
                TreeLogger.ERROR, "Unable to resolve tz argument " + tzArg);
            return true;
          } else if (!"com.google.gwt.i18n.client.TimeZone".equals(
              params.getParameter(paramNum).getType().getQualifiedSourceName())) {
            logger.log(
                TreeLogger.ERROR, "tz parameter must be of type TimeZone");
            return true;
          } else {
            tzParam = ", arg" + paramNum;
          }
        } else {
          tzParam = ", com.google.gwt.i18n.client.TimeZone.createTimeZone("
              + tzArg + ")";
        }
      }
      if (subformat.startsWith(PREDEF)) {
        // TODO(jat): error checking/logging
        PredefinedFormat predef;
        try {
          predef = PredefinedFormat.valueOf(
              subformat.substring(PREDEF.length()));
        } catch (IllegalArgumentException e) {
          logger.log(TreeLogger.ERROR,
              "Unrecognized predefined format '" + subformat + "'");
          return true;
        }
        out.appendStringValuedExpression(
            dtFormatClassName + ".getFormat(" + PredefinedFormat.class.getName()
                + "." + predef.toString() + ").format(" + argName + tzParam
                + ")");
        return false;
      }
      DateTimePatternGenerator dtpg = new DateTimePatternGenerator(locale);
      try {
        String pattern = dtpg.getBestPattern(subformat);
        if (pattern == null) {
          logger.log(TreeLogger.ERROR,
              "Invalid localdatetime skeleton pattern \"" + subformat + "\"");
          return true;
        }
        out.appendStringValuedExpression(
            dtFormatClassName + ".getFormat(" + wrap(pattern) + ").format("
                + argName + tzParam + ")");
      } catch (IllegalArgumentException e) {
        logger.log(TreeLogger.ERROR,
            "Unable to parse '" + subformat + ": " + e.getMessage());
        return true;
      }
      return false;
    }
  }

  /**
   * Implements {x,number...} references in MessageFormat.
   */
  private static class NumberFormatter implements ValueFormatter {

    public boolean format(TreeLogger logger, GwtLocale locale,
        StringGenerator out, Map<String, String> formatArgs, String subformat,
        String argName, JType argType, Parameters params) {
      JPrimitiveType argPrimType = argType.isPrimitive();
      if (argPrimType != null) {
        if (argPrimType == JPrimitiveType.BOOLEAN
            || argPrimType == JPrimitiveType.VOID) {
          logger.log(
              TreeLogger.ERROR, "Illegal argument type for number format");
          return true;
        }
      } else {
        JClassType classType = argType.isClass();
        if (classType == null) {
          logger.log(
              TreeLogger.ERROR, "Unexpected argument type for number format");
          return true;
        }
        TypeOracle oracle = classType.getOracle();
        JClassType numberType = oracle.findType("java.lang.Number");
        if (!classType.isAssignableTo(numberType)) {
          logger.log(TreeLogger.ERROR,
              "Only Number subclasses may be formatted as a number");
          return true;
        }
      }
      String curCodeParam = "";
      String curCode = formatArgs.get("curcode");
      if (curCode != null) {
        if (curCode.startsWith("$")) {
          int paramNum = params.getParameterIndex(curCode.substring(1));
          if (paramNum < 0) {
            logger.log(TreeLogger.ERROR,
                "Unable to resolve curcode argument " + curCode);
            return true;
          } else if (!"java.lang.String".equals(
              params.getParameter(paramNum).getType().getQualifiedSourceName())) {
            logger.log(
                TreeLogger.ERROR, "Currency code parameter must be String");
            return true;
          } else {
            curCodeParam = "arg" + paramNum;
          }
        } else {
          curCodeParam = '"' + curCode + '"';
        }
      }
      if (subformat == null) {
        out.appendStringValuedExpression(
            numFormatClassName + ".getDecimalFormat().format(" + argName + ")");
      } else if ("integer".equals(subformat)) {
        out.appendStringValuedExpression(
            numFormatClassName + ".getIntegerFormat().format(" + argName + ")");
      } else if ("currency".equals(subformat)) {
        out.appendStringValuedExpression(
            numFormatClassName + ".getCurrencyFormat(" + curCodeParam
                + ").format(" + argName + ")");
      } else if ("percent".equals(subformat)) {
        out.appendStringValuedExpression(
            numFormatClassName + ".getPercentFormat().format(" + argName + ")");
      } else {
        if (curCodeParam.length() > 0) {
          curCodeParam = ", " + curCodeParam;
        }
        out.appendStringValuedExpression(
            numFormatClassName + ".getFormat(" + wrap(subformat) + curCodeParam
                + ").format(" + argName + ")");
      }
      return false;
    }
  }

  /**
   * An interface for accessing parameters, giving the ability to record
   * accesses.
   */
  private interface Parameters {

    /**
     * Returns the count of parameters.
     */
    int getCount();

    /**
     * Returns the given parameter.
     *
     * @param i index of the parameter to return, 0 .. getCount() - 1
     * @return parameter or null if i is out of range
     */
    JParameter getParameter(int i);

    /**
     * Returns the given parameter.
     *
     * @param name the name of the parameter to return
     * @return parameter or null if the named parameter doesn't exist
     */
    JParameter getParameter(String name);

    /**
     * Find the index of a parameter by name.
     *
     * @param name
     * @return index of requested parameter or -1 if not found
     */
    int getParameterIndex(String name);
  }

  private static class ParametersImpl implements Parameters {

    private JParameter[] params;
    private boolean[] seenFlag;

    public ParametersImpl(JParameter[] params, boolean[] seenFlag) {
      this.params = params;
      this.seenFlag = seenFlag;
    }

    public int getCount() {
      return params.length;
    }

    public JParameter getParameter(int i) {
      if (i < 0 || i >= params.length) {
        return null;
      }
      seenFlag[i] = true;
      return params[i];
    }

    public JParameter getParameter(String name) {
      return getParameter(getParameterIndex(name));
    }

    public int getParameterIndex(String name) {
      for (int i = 0; i < params.length; ++i) {
        if (params[i].getName().equals(name)) {
          return i;
        }
      }
      return -1;
    }
  }

  /**
   * Implements {x,time...} references in MessageFormat.
   */
  private static class TimeFormatter implements ValueFormatter {

    public boolean format(TreeLogger logger, GwtLocale locale,
        StringGenerator out, Map<String, String> formatArgs, String subformat,
        String argName, JType argType, Parameters params) {
      if (!"java.util.Date".equals(argType.getQualifiedSourceName())) {
        logger.log(
            TreeLogger.ERROR, "Only java.util.Date acceptable for date format");
        return true;
      }
      String tzParam = "";
      String tzArg = formatArgs.get("tz");
      if (tzArg != null) {
        if (tzArg.startsWith("$")) {
          int paramNum = params.getParameterIndex(tzArg.substring(1));
          if (paramNum < 0) {
            logger.log(
                TreeLogger.ERROR, "Unable to resolve tz argument " + tzArg);
            return true;
          } else if (!"com.google.gwt.i18n.client.TimeZone".equals(
              params.getParameter(paramNum).getType().getQualifiedSourceName())) {
            logger.log(
                TreeLogger.ERROR, "Currency code parameter must be TimeZone");
            return true;
          } else {
            tzParam = ", arg" + paramNum;
          }
        } else {
          tzParam = ", com.google.gwt.i18n.client.TimeZone.createTimeZone("
              + tzArg + ")";
        }
      }
      if (subformat == null || "medium".equals(subformat)) {
        out.appendStringValuedExpression(
            dtFormatClassName + ".getMediumTimeFormat().format(" + argName
                + tzParam + ")");
      } else if ("full".equals(subformat)) {
        out.appendStringValuedExpression(
            dtFormatClassName + ".getFullTimeFormat().format(" + argName
                + tzParam + ")");
      } else if ("long".equals(subformat)) {
        out.appendStringValuedExpression(
            dtFormatClassName + ".getLongTimeFormat().format(" + argName
                + tzParam + ")");
      } else if ("short".equals(subformat)) {
        out.appendStringValuedExpression(
            dtFormatClassName + ".getShortTimeFormat().format(" + argName
                + tzParam + ")");
      } else {
        logger.log(TreeLogger.WARN, "Use localdatetime format instead");
        out.appendStringValuedExpression(
            dtFormatClassName + ".getFormat(" + wrap(subformat) + ").format("
                + argName + tzParam + ")");
      }
      return false;
    }
  }

  private interface ValueFormatter {

    /**
     * Creates code to format a value according to a format string.
     *
     * @param logger
     * @param locale current locale
     * @param out StringBuffer to append to
     * @param formatArgs format-specific arguments
     * @param subformat the remainder of the format string
     * @param argName the name of the argument to use in the generated code
     * @param argType the type of the argument
     * @param params argument list or null
     * @return true if a fatal error occurred (which will already be logged)
     */
    boolean format(TreeLogger logger, GwtLocale locale, StringGenerator out,
        Map<String, String> formatArgs, String subformat, String argName,
        JType argType, Parameters params);
  }

  /**
   * Class names, in a refactor-friendly manner.
   */
  private static final String dtFormatClassName =
      DateTimeFormat.class.getCanonicalName();

  /**
   * Fully-qualified class name of the SafeHtml interface.
   */
  public static final String SAFE_HTML_FQCN = SafeHtml.class.getCanonicalName();

  /**
   * Fully-qualified class name of the SafeHtmlBuilder class.
   */
  public static final String SAFE_HTML_BUILDER_FQCN =
      SafeHtmlBuilder.class.getCanonicalName();

  /**
   * Map of supported formats.
   */
  private static Map<String, ValueFormatter> formatters = new HashMap<
      String, ValueFormatter>();

  private static final String numFormatClassName =
      NumberFormat.class.getCanonicalName();

  /*
   * Register supported formats.
   */
  static {
    formatters.put("date", new DateFormatter());
    formatters.put("number", new NumberFormatter());
    formatters.put("time", new TimeFormatter());
    formatters.put("localdatetime", new LocalDateTimeFormatter());
  }

  private final Map<GwtLocale, Map<String, String>> listPatternCache;

  /**
   * Constructor for <code>MessagesMethodCreator</code>.
   *
   * @param classCreator associated class creator
   */
  public MessagesMethodCreator(AbstractGeneratorClassCreator classCreator) {
    super(classCreator);
    listPatternCache = new HashMap<GwtLocale, Map<String, String>>();
  }

  @Override
  public void createMethodFor(TreeLogger logger, JMethod m, String key,
      ResourceList resourceList, GwtLocale locale)
      throws UnableToCompleteException {
    ResourceEntry resourceEntry = resourceList.getEntry(key);
    if (resourceEntry == null) {
      throw new MissingResourceException(key, resourceList);
    }
    JParameter[] params = m.getParameters();
    int pluralParamIndex = -1;
    Class<? extends PluralRule> ruleClass = null;
    int numParams = params.length;
    boolean[] seenFlags = new boolean[numParams];
    final Parameters paramsAccessor = new ParametersImpl(params, seenFlags);

    int pluralOffset = 0;
    String pluralSuffix = "";
    // See if any parameter is tagged as a PluralCount parameter.
    for (int i = 0; i < numParams; ++i) {
      PluralCount pluralCount = params[i].getAnnotation(PluralCount.class);
      if (pluralCount != null) {
        if (pluralParamIndex >= 0) {
          throw error(logger,
              m.getName() + ": there can only be one PluralCount parameter");
        }
        JType paramType = params[i].getType();
        boolean isArray = false;
        boolean isList = false;
        JPrimitiveType primType = paramType.isPrimitive();
        JClassType classType = paramType.isInterface();
        if (classType != null) {
          classType = classType.getErasedType();
          if ("java.util.List".equals(classType.getQualifiedSourceName())) {
            isList = true;
          } else {
            classType = null;
          }
        }

        JArrayType arrayType = paramType.isArray();
        if (arrayType != null) {
          isArray = true;
        }
        if (!isList && !isArray
            && (primType == null || (primType != JPrimitiveType.INT
                && primType != JPrimitiveType.SHORT))) {
          throw error(logger, m.getName()
              + ": PluralCount parameter must be int, short, array, or List");
        }
        if (isList) {
          pluralSuffix = ".size()";
        } else if (isArray) {
          pluralSuffix = ".length";
        }
        pluralParamIndex = i;
        ruleClass = pluralCount.value();
        Offset offset = params[i].getAnnotation(Offset.class);
        if (offset != null) {
          pluralOffset = offset.value();
        }
      }
    }

    boolean isSafeHtml = m.getReturnType().getQualifiedSourceName().equals(
        SAFE_HTML_FQCN);

    String template = resourceEntry.getForm(null);
    if (template == null) {
      logger.log(TreeLogger.ERROR,
          "No default form for method " + m.getName() + "' in "
              + m.getEnclosingType() + " for locale " + locale, null);
      throw new UnableToCompleteException();
    }
    StringBuffer generated = new StringBuffer();
    ArgumentChunk listArg = null;
    JType elemType = null;
    ListAccessor listAccessor = null;
    try {
      for (TemplateChunk chunk : MessageFormatParser.parse(template)) {
        if (chunk instanceof ArgumentChunk) {
          ArgumentChunk argChunk = (ArgumentChunk) chunk;
          if (argChunk.isList()) {
            if (listArg != null) {
              logger.log(TreeLogger.ERROR,
                  "Only one list parameter supported in "
                      + m.getEnclosingType().getSimpleSourceName() + "."
                      + m.getName());
              throw new UnableToCompleteException();
            } else {
              listArg = argChunk;
              int listArgNum = argChunk.getArgumentNumber();
              JType listType = params[listArgNum].getType();
              JClassType classType = listType.isInterface();
              if (classType != null) {
                if ("java.util.List".equals(
                    classType.getErasedType().getQualifiedSourceName())) {
                  listAccessor = new ListAccessorList(listArgNum);
                } else {
                  logger.log(TreeLogger.ERROR,
                          "Parameters formatted as lists must be declared as java.util.List or arrays in " + m.getEnclosingType().getSimpleSourceName() + "." + m.getName());
                  throw new UnableToCompleteException();
                }
                JParameterizedType paramType = classType.isParameterized();
                if (paramType != null) {
                  elemType = paramType.getTypeArgs()[0];
                } else {
                  elemType = classType.getOracle().getJavaLangObject();
                }
              } else {
                JArrayType arrayType = listType.isArray();
                if (arrayType != null) {
                  elemType = arrayType.getComponentType();
                  listAccessor = new ListAccessorArray(listArgNum);
                }
              }
            }
          }
        }
      }
    } catch (ParseException pe) {
      logger.log(TreeLogger.ERROR, "Error parsing '" + template + "'", pe);
      throw new UnableToCompleteException();
    }

    if (listArg != null) {
      generateListFormattingCode(logger, locale, generated, listArg, elemType,
          isSafeHtml, listAccessor, paramsAccessor);
    }
    if (ruleClass == null) {
      if (m.getAnnotation(PluralText.class) != null) {
        logger.log(TreeLogger.WARN,
            "Unused @PluralText on "
                + m.getEnclosingType().getSimpleSourceName() + "." + m.getName()
                + "; did you intend to mark a @PluralCount parameter?", null);
      }
    } else {
      if (ruleClass == PluralRule.class) {
        ruleClass = DefaultRule.class;
      }
      PluralRule rule = createLocalizedPluralRule(
          logger, m.getEnclosingType().getOracle(), ruleClass, locale);
      logger.log(TreeLogger.TRACE,
          "Using plural rule " + rule.getClass() + " for locale '" + locale
              + "'", null);
      boolean seenEqualsValue = false;
      for (String form : resourceEntry.getForms()) {
        if (form.startsWith("=")) {
          int value = 0;
          try {
            value = Integer.parseInt(form.substring(1));
          } catch (NumberFormatException e) {
            logger.log(TreeLogger.WARN,
                "Ignoring invalid value in plural form '" + form + "'", e);
            continue;
          }
          if (!seenEqualsValue) {
            generated.append(
                "switch (arg" + pluralParamIndex + pluralSuffix + ") {\n");
            seenEqualsValue = true;
          }
          generated.append("  case " + value + ": return ");
          String pluralTemplate = resourceEntry.getForm(form);
          generateString(logger, locale, pluralTemplate, paramsAccessor,
              generated, isSafeHtml);
          generated.append(";\n");
        }
      }
      if (seenEqualsValue) {
        generated.append("}\n");
      }
      boolean seenPluralForm = false;
      StringBuilder pluralHeader = new StringBuilder();
      pluralHeader.append(PluralRule.class.getCanonicalName());
      pluralHeader.append(
          " rule = new " + rule.getClass().getCanonicalName() + "();\n");
      if (pluralOffset != 0) {
        pluralHeader.append(
            "arg" + pluralParamIndex + " -= " + pluralOffset + ";\n");
      }
      pluralHeader.append(
          "switch (rule.select(arg" + pluralParamIndex + pluralSuffix
              + ")) {\n");
      PluralForm[] pluralForms = rule.pluralForms();
      resourceList.setPluralForms(key, pluralForms);
      // Skip default plural form (index 0); the fall-through case will handle
      // it.
      for (int i = 1; i < pluralForms.length; ++i) {
        String pluralTemplate = resourceEntry.getForm(pluralForms[i].getName());
        if (pluralTemplate != null) {
          if (!seenPluralForm) {
            generated.append(pluralHeader);
            seenPluralForm = true;
          }
          generated.append("  // " + pluralForms[i].getName() + " - "
              + pluralForms[i].getDescription() + "\n");
          generated.append("  case " + i + ": return ");
          generateString(logger, locale, pluralTemplate, paramsAccessor,
              generated, isSafeHtml);
          generated.append(";\n");
        } else if (pluralForms[i].getWarnIfMissing()) {
          if (!seenEqualsValue) {
            // If we have seen a form "=n", assume the developer knows what
            // they are doing and don't warn about plural forms that aren't
            // used.
            logger.log(TreeLogger.WARN,
                "No plural form '" + pluralForms[i].getName()
                    + "' defined for method '" + m.getName() + "' in "
                    + m.getEnclosingType() + " for locale " + locale, null);
          }
        }
      }
      if (seenPluralForm) {
        generated.append("}\n");
      }
    }
    generated.append("return ");
    generateString(
        logger, locale, template, paramsAccessor, generated, isSafeHtml);

    // Generate an error if any required parameter was not used somewhere.
    for (int i = 0; i < numParams; ++i) {
      if (!seenFlags[i]) {
        Optional optional = params[i].getAnnotation(Optional.class);
        if (optional == null) {
          throw error(
              logger, "Required argument " + i + " not present: " + template);
        }
      }
    }

    generated.append(';');
    println(generated.toString());
  }

  /**
   * Creates an instance of a locale-specific plural rule implementation.
   *
   *  Note that this uses TypeOracle's ability to find all subclasses of the
   * supplied parent class, then uses reflection to actually load the class.
   * This works because PluralRule instances are required to be translatable,
   * since part of them is executed at runtime and part at compile time.
   *
   * @param logger TreeLogger instance
   * @param oracle TypeOracle instance to use
   * @param ruleClass PluralRule implementation to localize
   * @param locale current locale we are compiling for
   * @return an instance of a PluralRule implementation. If an appropriate
   *         implementation of the requested class cannot be found, an instance
   *         of DefaultRule is used instead as a default of last resort.
   * @throws UnableToCompleteException if findDerivedClasses fails
   *
   *           TODO: consider impact of possibly having multiple TypeOracles
   */
  private PluralRule createLocalizedPluralRule(TreeLogger logger,
      TypeOracle oracle, Class<? extends PluralRule> ruleClass,
      GwtLocale locale) throws UnableToCompleteException {
    String baseName = ruleClass.getCanonicalName();
    JClassType ruleJClassType = oracle.findType(baseName);
        Map<String, JClassType> matchingClasses = LocalizableLinkageCreator.findDerivedClasses(logger, ruleJClassType);
    for (GwtLocale search : locale.getCompleteSearchList()) {
      JClassType localizedType = matchingClasses.get(search.toString());
      if (localizedType != null) {
        try {
          Class<?> testClass = Class.forName(
              localizedType.getQualifiedBinaryName(), false,
              PluralRule.class.getClassLoader());
          if (PluralRule.class.isAssignableFrom(testClass)) {
            return (PluralRule) testClass.newInstance();
          }
        } catch (ClassCastException e) {
          // ignore classes of the wrong type
        } catch (ClassNotFoundException e) {
          // ignore missing classes
        } catch (InstantiationException e) {
          // skip classes we can't instantiate
        } catch (IllegalAccessException e) {
          // ignore inaccessible classes
        }
      }
    }
    // default of last resort
    return new DefaultRule();
  }

  private void formatArg(TreeLogger logger, GwtLocale locale,
      StringGenerator buf, ArgumentChunk argChunk, String argExpr,
      JType paramType, Parameters params) throws UnableToCompleteException {
    String format = argChunk.getFormat();
    if (format != null) {
      String subformat = argChunk.getSubFormat();
      ValueFormatter formatter = formatters.get(format);
      if (formatter != null) {
        if (formatter.format(logger, locale, buf, argChunk.getFormatArgs(),
            subformat, argExpr, paramType, params)) {
          throw new UnableToCompleteException();
        }
        return;
      }
    }
    // no format specified or unknown format
    // have to ensure that the result is stringified if necessary
    boolean isSafeHtmlTyped = SAFE_HTML_FQCN.equals(
        paramType.getQualifiedSourceName());
    boolean isPrimitiveType = (paramType.isPrimitive() != null);
    boolean needsConversionToString = !("java.lang.String".equals(
        paramType.getQualifiedSourceName()));
    buf.appendExpression(
        argExpr, isSafeHtmlTyped, isPrimitiveType, needsConversionToString);
  }

  /**
   * Generate code for one list pattern.
   *
   * @param logger logger to use for error/warning messages
   * @param locale locale we are generating code for
   * @param listArg the {n,list,...} argument in the original format pattern
   * @param val0 the expression defining the {0} argument in the list pattern
   * @param val1 the expression defining the {1} argument in the list pattern
   * @param elemType the element type of the list/array being rendered as a list   * @param isSafeHtml true if the resulting string is SafeHtml
   * @param listPattern the list pattern to generate code for, ie "{0}, {1}"
   * @param formatSecond true if the {1} parameter needs to be formatted
   * @param params parameters passed to the Messages method call
   * @return a constructed string containing the code to implement the given
   *     list pattern
   * @throws UnableToCompleteException
   */
  private CharSequence formatListPattern(final TreeLogger logger,
      final GwtLocale locale, final ArgumentChunk listArg, final String val0,
      final String val1, final JType elemType, final boolean isSafeHtml,
      String listPattern, final boolean formatSecond, final Parameters params)
      throws UnableToCompleteException {
    final StringBuffer buf = new StringBuffer();
    final StringGenerator gen = new StringGenerator(buf, isSafeHtml);
    try {
      List<TemplateChunk> chunks = MessageFormatParser.parse(listPattern);
      for (TemplateChunk chunk : chunks) {
        chunk.accept(new DefaultTemplateChunkVisitor() {
          @Override
          public void visit(ArgumentChunk argChunk)
              throws UnableToCompleteException {
            // The {0} argument in the list pattern always needs formatting,
            // but the {1} argument is the part of the list already rendered
            // (in either String of SafeHtml form) unless formatSecond is true.
            if (argChunk.getArgumentNumber() == 0 || formatSecond) {
              formatArg(logger, locale, gen, listArg,
                  argChunk.getArgumentNumber() == 0 ? val0 : val1, elemType,
                  params);
            } else {
              gen.appendExpression(val1, isSafeHtml, false, false);
            }
          }

          @Override
          public void visit(StringChunk stringChunk)
              throws UnableToCompleteException {
            gen.appendStringLiteral(stringChunk.getString());
          }
        });
      }
    } catch (ParseException e) {
      logger.log(TreeLogger.ERROR,
          "Internal error: can't parse list pattern '" + listPattern
              + "' for locale " + locale, e);
      throw new UnableToCompleteException();
    }
    gen.completeString();
    return buf;
  }

  /**
   * Generates code to format a list in a format pattern.
   *
   * @param logger logger to use for error/warning messages
   * @param locale locale we are generating code for
   * @param generated a StringBuffer holding the generated code
   * @param listArg the {n,list,...} argument in the original format pattern
   * @param elemType the element type of the list/array being rendered as a list
   * @param isSafeHtml true if the resulting string is SafeHtml
   * @param listAccessor a way to access elements of the list type supplied by
   *     the user
   * @param params parameters passed to the Messages method call
   * @throws UnableToCompleteException
   */
  private void generateListFormattingCode(TreeLogger logger, GwtLocale locale,
      StringBuffer generated, ArgumentChunk listArg, JType elemType,
      boolean isSafeHtml, ListAccessor listAccessor, Parameters params)
      throws UnableToCompleteException {
    Map<String, String> listPatternParts = getListPatternParts(logger, locale);
    int listArgNum = listArg.getArgumentNumber();
    generated.append(
        "int arg" + listArgNum + "_size = " + listAccessor.getSize() + ";\n");
    if (isSafeHtml) {
      generated.append(SafeHtml.class.getCanonicalName()).append(" arg").append(
          listArgNum).append("_list = new ").append(
          OnlyToBeUsedInGeneratedCodeStringBlessedAsSafeHtml.class.getCanonicalName(
          )).append("(\"\");\n");
    } else {
      generated.append("String").append(" arg" + listArgNum
          + "_list = \"\";\n");
    }
    generated.append("switch (arg" + listArgNum + "_size) {\n");
    // TODO(jat): add support for special-cases besides 2 if CLDR ever adds them
    String pairPattern = listPatternParts.get("2");
    if (pairPattern != null) {
      generated.append("case 2:\n");
      generated.append("  arg" + listArgNum + "_list = ");
      generated.append(
          formatListPattern(logger, locale, listArg,
              listAccessor.getElement("0"), listAccessor.getElement("1"),
              elemType, isSafeHtml, pairPattern, true, params));
      generated.append(";\n");
      generated.append("  break;\n");
    }
    generated.append("default:\n");
    generated.append("  int i = arg" + listArgNum + "_size;\n");
    generated.append("  if (i > 0) {\n");
    generated.append("    arg" + listArgNum + "_list = ");
    StringGenerator buf = new StringGenerator(generated, isSafeHtml);
    formatArg(logger, locale, buf, listArg, listAccessor.getElement("--i"),
        elemType, params);
    buf.completeString();
    generated.append(";\n");
    generated.append("  }\n");
    generated.append("  if (i > 0) {\n");
    generated.append("    arg" + listArgNum + "_list = ");
    generated.append(
        formatListPattern(logger, locale, listArg,
            listAccessor.getElement("--i"), "arg" + listArgNum + "_list",
            elemType, isSafeHtml, listPatternParts.get("end"), false, params));
    generated.append(";\n");
    generated.append("  }\n");
    generated.append("  while (i > 1) {\n");
    generated.append("    arg" + listArgNum + "_list = ");
    generated.append(formatListPattern(logger, locale, listArg,
        listAccessor.getElement("--i"), "arg" + listArgNum + "_list",
        elemType, isSafeHtml, listPatternParts.get("middle"), false, params));
    generated.append("  ;\n");
    generated.append("  }\n");
    generated.append("  if (i > 0) {\n");
    generated.append("    arg" + listArgNum + "_list = ");
    generated.append(formatListPattern(logger, locale, listArg,
        listAccessor.getElement("--i"), "arg" + listArgNum + "_list",
        elemType, isSafeHtml, listPatternParts.get("start"), false, params));
    generated.append(";\n");
    generated.append("  }\n");
    generated.append("  break;\n");
    generated.append("}\n");
  }

  /**
   * Generate a Java string for a given MessageFormat string.
   *
   * @param logger
   * @param template
   * @param paramsAccessor
   * @param outputBuf
   * @throws UnableToCompleteException
   */
  @SuppressWarnings("fallthrough")
  private void generateString(final TreeLogger logger, final GwtLocale locale,
      final String template, final Parameters paramsAccessor,
      StringBuffer outputBuf, final boolean isSafeHtml)
      throws UnableToCompleteException {
    final StringGenerator buf = new StringGenerator(outputBuf, isSafeHtml);
    final int n = paramsAccessor.getCount();
    try {
      for (TemplateChunk chunk : MessageFormatParser.parse(template)) {
        chunk.accept(new DefaultTemplateChunkVisitor() {
          @Override
          public void visit(ArgumentChunk argChunk)
              throws UnableToCompleteException {
            int argNumber = argChunk.getArgumentNumber();
            if (argNumber >= n) {
              throw error(logger,
                  "Argument " + argNumber + " beyond range of arguments: "
                      + template);
            }
            JParameter param = paramsAccessor.getParameter(argNumber);
            String arg = "arg" + argNumber;
            if (argChunk.isList()) {
              buf.appendExpression(arg + "_list", isSafeHtml, false,
                  false);
            } else {
              JType paramType = param.getType();
              formatArg(logger, locale, buf, argChunk, arg, paramType,
                  paramsAccessor);
            }
          }

          @Override
          public void visit(StaticArgChunk staticArgChunk)
              throws UnableToCompleteException {
            buf.appendStringLiteral(staticArgChunk.getReplacement());
          }

          @Override
          public void visit(StringChunk stringChunk) {
            buf.appendStringLiteral(stringChunk.getString());
          }
        });
      }
    } catch (ParseException e) {
      throw error(logger, e);
    }
    buf.completeString();
  }

  private Map<String, String> getListPatternParts(
      TreeLogger logger, GwtLocale locale) {
    Map<String, String> map = listPatternCache.get(locale);
    if (map == null) {
      // TODO(jat): get these from ResourceOracle instead
      String baseName =
          MessagesMethodCreator.class.getPackage().getName().replace('.', '/')
          + "/cldr/ListPatterns_";
      ClassLoader cl = MessagesMethodCreator.class.getClassLoader();
      for (GwtLocale search : locale.getCompleteSearchList()) {
        String propFile = baseName + search.getAsString() + ".properties";
        InputStream stream = cl.getResourceAsStream(propFile);
        if (stream != null) {
          try {
            LocalizedPropertiesLoader loader = new LocalizedPropertiesLoader(
                stream, "UTF-8");
            map = new HashMap<String, String>();
            loader.load(map);
            break;
          } catch (IOException e) {
            logger.log(
                TreeLogger.WARN, "Ignoring error reading file " + propFile, e);
          } finally {
            try {
              stream.close();
            } catch (IOException e) {
            }
          }
        }
      }
      listPatternCache.put(locale, map);
    }
    return map;
  }
}
