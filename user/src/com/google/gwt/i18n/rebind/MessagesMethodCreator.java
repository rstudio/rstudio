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

import com.google.gwt.codegen.server.StringGenerator;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JArrayType;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JEnumConstant;
import com.google.gwt.core.ext.typeinfo.JEnumType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.i18n.client.Messages.AlternateMessage;
import com.google.gwt.i18n.client.Messages.Offset;
import com.google.gwt.i18n.client.Messages.Optional;
import com.google.gwt.i18n.client.Messages.PluralCount;
import com.google.gwt.i18n.client.Messages.PluralText;
import com.google.gwt.i18n.client.Messages.Select;
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
import com.google.gwt.i18n.shared.AlternateMessageSelector;
import com.google.gwt.i18n.shared.GwtLocale;
import com.google.gwt.safehtml.shared.OnlyToBeUsedInGeneratedCodeStringBlessedAsSafeHtml;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.rebind.AbstractGeneratorClassCreator;
import com.google.gwt.user.rebind.AbstractMethodCreator;
import com.google.gwt.user.rebind.SourceWriter;

import org.apache.tapestry.util.text.LocalizedPropertiesLoader;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Creator for methods of the Messages interface.
 */
@SuppressWarnings("deprecation") // for @PluralText
class MessagesMethodCreator extends AbstractMethodCreator {

  private abstract static class AlternateFormSelector {
    protected final int argNumber;
    protected final JType argType;
    
    public AlternateFormSelector(TreeLogger logger, int argNumber, JParameter[] params) {
      this.argNumber = argNumber;
      this.argType = params[argNumber].getType();
    }

    public abstract void generatePrepCode(SourceWriter out);

    public abstract void generateSelectEnd(SourceWriter out);

    public abstract void generateSelectMatchEnd(SourceWriter out, String value);

    /**
     * @param out
     * @param logger
     * @param value
     * @throws UnableToCompleteException
     */
    public abstract void generateSelectMatchStart(SourceWriter out,
        TreeLogger logger, String value) throws UnableToCompleteException;
    
    public abstract void generateSelectStart(SourceWriter out,
        boolean exactMatches);

    public abstract void issueWarnings(TreeLogger logger, JMethod m,
        GwtLocale locale);
  }

  /**
   * Implements {x,date...} references in MessageFormat.
   */
  private static class DateFormatter implements ValueFormatter {
    public boolean format(TreeLogger logger, GwtLocale locale,
        StringGenerator out, Map<String, String> formatArgs, String subformat,
        String argName, JType argType, Parameters params) {
      if (!"java.util.Date".equals(argType.getQualifiedSourceName())) {
        logger.log(TreeLogger.ERROR, "Only java.util.Date acceptable for date format");
        return true;
      }
      String tzParam = "";
      String tzArg = formatArgs.get("tz");
      if (tzArg != null) {
        if (tzArg.startsWith("$")) {
          int paramNum = params.getParameterIndex(tzArg.substring(1));
          if (paramNum < 0) {
            logger.log(TreeLogger.ERROR, "Unable to resolve tz argument " + tzArg);
            return true;
          } else if (!"com.google.gwt.i18n.client.TimeZone".equals(
              params.getParameter(paramNum).getType().getQualifiedSourceName())) {
            logger.log(TreeLogger.ERROR, "Currency code parameter must be TimeZone");
            return true;
          } else {
            tzParam = ", arg" + paramNum;
          }
        } else {
          tzParam = ", com.google.gwt.i18n.client.TimeZone.createTimeZone(" + tzArg + ")";
        }
      }
      if (subformat == null || "medium".equals(subformat)) {
        out.appendStringValuedExpression(
            dtFormatClassName + ".getMediumDateFormat()" + ".format(" + argName + tzParam + ")");
      } else if ("full".equals(subformat)) {
        out.appendStringValuedExpression(
            dtFormatClassName + ".getFullDateFormat().format(" + argName + tzParam + ")");
      } else if ("long".equals(subformat)) {
        out.appendStringValuedExpression(
            dtFormatClassName + ".getLongDateFormat().format(" + argName + tzParam + ")");
      } else if ("short".equals(subformat)) {
        out.appendStringValuedExpression(
            dtFormatClassName + ".getShortDateFormat()" + ".format(" + argName + tzParam + ")");
      } else {
        logger.log(TreeLogger.WARN, "Use localdatetime format instead");
        out.appendStringValuedExpression(
            dtFormatClassName + ".getFormat(" + wrap(subformat) + ").format(" + argName + tzParam
                + ")");
      }
      return false;
    }
  }

  /**
   * Comparator that ensures all exact value matches (=N) strings come before
   * all non-exact matches.
   */
  private static class ExactValueComparator implements Comparator<String> {
    
    private static int compareOne(String a, String b) {
      boolean aExact = a.startsWith("=");
      boolean bExact = a.startsWith("=");
      if (aExact != bExact) {
        return aExact ? -1 : 1;
      }
      if (aExact) {
        return a.substring(1).compareTo(b.substring(1));
      } else {
        return a.compareTo(b);
      }
    }

    public int compare(String a, String b) {
      String[] aSplit = a.split("\\|");
      String[] bSplit = b.split("\\|");
      int c = 0;
      for (int i = 0; c == 0 && i < aSplit.length && i < bSplit.length; ++i) {
        c = compareOne(aSplit[i], bSplit[i]);
      }
      if (c == 0 && aSplit.length != bSplit.length) {
        c = aSplit.length < bSplit.length ? -1 : 1;
      }
      return c;
    }
  }

  /**
   * An {@link AlternateFormSelector} used with {@link Select}.
   */
  private static class GenericSelector extends AlternateFormSelector {

    private final JEnumType enumType;
    private final boolean isBoolean;
    private final boolean isString;
    private final boolean needsIf;
    private boolean startedIfChain;

    /**
     * @param logger
     * @param m
     * @param i
     * @param params
     * @throws UnableToCompleteException 
     */
    public GenericSelector(TreeLogger logger, JMethod m, int i,
        JParameter[] params) throws UnableToCompleteException {
      super(logger, i, params);
      JPrimitiveType primType = argType.isPrimitive();
      JClassType classType = argType.isClass();
      JEnumType tempEnumType = null;
      boolean tempIsBoolean = false;
      boolean tempIsString = false;
      boolean tempNeedsIf = false;
      if (primType != null) {
        if (primType == JPrimitiveType.DOUBLE
            || primType == JPrimitiveType.FLOAT) {
          throw error(logger, m.getName() + ": @Select arguments may only be"
              + " integral primitives, boolean, enums, or String");
        }
        tempIsBoolean = (primType == JPrimitiveType.BOOLEAN);
        tempNeedsIf = tempIsBoolean || (primType == JPrimitiveType.LONG);
      } else if (classType != null) {
        tempEnumType = classType.isEnum();
        tempIsString = "java.lang.String".equals(classType.getQualifiedSourceName());
        if (tempEnumType == null && !tempIsString) {
          throw error(logger, m.getName() + ": @Select arguments may only be"
              + " integral primitives, boolean, enums, or String");
        }
      } else {
        throw error(logger, m.getName() + ": @Select arguments may only be"
            + " integral primitives, boolean, enums, or String");
      }
      tempNeedsIf |= tempIsString;
      enumType = tempEnumType;
      isBoolean = tempIsBoolean;
      isString = tempIsString;
      needsIf = tempNeedsIf;
    }

    @Override
    public void generatePrepCode(SourceWriter out) {
      if (enumType != null) {
        out.println("int arg" + argNumber + "_ordinal = -1;");
        out.println("if (arg" + argNumber + " != null) {");
        out.indent();
        out.println("arg" + argNumber + "_ordinal = arg" + argNumber
            + ".ordinal();");
        out.outdent();
        out.println("}");
      }
    }

    @Override
    public void generateSelectEnd(SourceWriter out) {
      if (!startedIfChain) {
        out.outdent();
      }
      out.println("}");
    }
    
    @Override
    public void generateSelectMatchEnd(SourceWriter out, String value) {
      if (!startedIfChain) {
        out.println("break;");
      }
      out.outdent();
    }

    @Override
    public void generateSelectMatchStart(SourceWriter out, TreeLogger logger,
        String value) throws UnableToCompleteException {
      if (needsIf) {
        if (startedIfChain) {
          out.print("} else ");
        } else {
          startedIfChain = true;
        }
        if (AlternateMessageSelector.OTHER_FORM_NAME.equals(value)) {
          out.println("{  // other");
        } else {
          if (isString) {
            value = value.replace("\"", "\\\"");
            out.println("if (\"" + value + "\".equals(arg" + argNumber + ")) {");
          } else if (isBoolean) {
            boolean isTrue = Boolean.parseBoolean(value);
            out.println("if (" + (isTrue ? "" : "!") + "arg" + argNumber + ") {");
          } else {
            long longVal;
            try {
              longVal = Long.parseLong(value);
            } catch (NumberFormatException e) {
              throw error(logger, "'" + value + "' is not a valid long value",
                  e);
            }
            out.println("if (" + longVal + " == arg" + argNumber + ") {");
          }
        }
      } else {
        if (AlternateMessageSelector.OTHER_FORM_NAME.equals(value)) {
          out.println("default:  // other");
        } else if (enumType != null) {
          JField field = enumType.findField(value);
          JEnumConstant enumConstant = null;
          if (field != null) {
            enumConstant = field.isEnumConstant();
          }
          if (field == null || enumConstant == null) {
            throw error(logger, "'" + value + "' is not a valid value of "
                + enumType.getQualifiedSourceName() + " or 'other'");
          }
          out.println("case " + enumConstant.getOrdinal() + ":  // " + value);
        } else {
          int intVal;
          try {
            intVal = Integer.parseInt(value);
          } catch (NumberFormatException e) {
            throw error(logger, "'" + value + "' is not a valid integral value",
                e);
          }
          out.println("case " + intVal + ":");
        }
      }
      out.indent();
    }

    @Override
    public void generateSelectStart(SourceWriter out, boolean exactMatches) {
      // ignore exactMatches, so "=VALUE" is the same as "VALUE"
      if (needsIf) {
        startedIfChain = false;
        return;
      }
      String suffix = "";
      if (enumType != null) {
        suffix = "_ordinal";
      }
      out.println("switch (arg" + argNumber + suffix + ") {");
      out.indent();
    }

    @Override
    public void issueWarnings(TreeLogger logger, JMethod m, GwtLocale locale) {
      // nothing to warn about
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

    public boolean format(TreeLogger logger,
        GwtLocale locale,
        StringGenerator out,
        Map<String, String> formatArgs,
        String subformat,
        String argName,
        JType argType,
        Parameters params) {
      if (!"java.util.Date".equals(argType.getQualifiedSourceName())) {
        logger.log(TreeLogger.ERROR, "Only java.util.Date acceptable for localdatetime format");
        return true;
      }
      if (subformat == null || subformat.length() == 0) {
        logger.log(TreeLogger.ERROR, "localdatetime format requires a skeleton pattern");
        return true;
      }
      String tzParam = "";
      String tzArg = formatArgs.get("tz");
      if (tzArg != null) {
        if (tzArg.startsWith("$")) {
          int paramNum = params.getParameterIndex(tzArg.substring(1));
          if (paramNum < 0) {
            logger.log(TreeLogger.ERROR, "Unable to resolve tz argument " + tzArg);
            return true;
          } else if (!"com.google.gwt.i18n.client.TimeZone".equals(
              params.getParameter(paramNum).getType().getQualifiedSourceName())) {
            logger.log(TreeLogger.ERROR, "tz parameter must be of type TimeZone");
            return true;
          } else {
            tzParam = ", arg" + paramNum;
          }
        } else {
          tzParam = ", com.google.gwt.i18n.client.TimeZone.createTimeZone(" + tzArg + ")";
        }
      }
      if (subformat.startsWith(PREDEF)) {
        // TODO(jat): error checking/logging
        PredefinedFormat predef;
        try {
          predef = PredefinedFormat.valueOf(subformat.substring(PREDEF.length()));
        } catch (IllegalArgumentException e) {
          logger.log(TreeLogger.ERROR, "Unrecognized predefined format '" + subformat + "'");
          return true;
        }
        out.appendStringValuedExpression(
            dtFormatClassName + ".getFormat("
                + PredefinedFormat.class.getCanonicalName() + "."
                + predef.toString() + ").format(" + argName + tzParam + ")");
        return false;
      }
      DateTimePatternGenerator dtpg = new DateTimePatternGenerator(locale);
      try {
        String pattern = dtpg.getBestPattern(subformat);
        if (pattern == null) {
          logger.log(
              TreeLogger.ERROR, "Invalid localdatetime skeleton pattern \"" + subformat + "\"");
          return true;
        }
        out.appendStringValuedExpression(
            dtFormatClassName + ".getFormat(" + wrap(pattern) + ").format(" + argName + tzParam
                + ")");
      } catch (IllegalArgumentException e) {
        logger.log(TreeLogger.ERROR, "Unable to parse '" + subformat + ": " + e.getMessage());
        return true;
      }
      return false;
    }
  }

  /**
   * Implements {x,number...} references in MessageFormat.
   */
  private static class NumberFormatter implements ValueFormatter {

    public boolean format(TreeLogger logger,
        GwtLocale locale,
        StringGenerator out,
        Map<String, String> formatArgs,
        String subformat,
        String argName,
        JType argType,
        Parameters params) {
      JPrimitiveType argPrimType = argType.isPrimitive();
      if (argPrimType != null) {
        if (argPrimType == JPrimitiveType.BOOLEAN || argPrimType == JPrimitiveType.VOID) {
          logger.log(TreeLogger.ERROR, "Illegal argument type for number format");
          return true;
        }
      } else {
        JClassType classType = argType.isClass();
        if (classType == null) {
          logger.log(TreeLogger.ERROR, "Unexpected argument type for number format");
          return true;
        }
        TypeOracle oracle = classType.getOracle();
        JClassType numberType = oracle.findType("java.lang.Number");
        if (!classType.isAssignableTo(numberType)) {
          logger.log(TreeLogger.ERROR, "Only Number subclasses may be formatted as a number");
          return true;
        }
      }
      String curCodeParam = "";
      String curCode = formatArgs.get("curcode");
      if (curCode != null) {
        if (curCode.startsWith("$")) {
          int paramNum = params.getParameterIndex(curCode.substring(1));
          if (paramNum < 0) {
            logger.log(TreeLogger.ERROR, "Unable to resolve curcode argument " + curCode);
            return true;
          } else if (!"java.lang.String".equals(
              params.getParameter(paramNum).getType().getQualifiedSourceName())) {
            logger.log(TreeLogger.ERROR, "Currency code parameter must be String");
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
            numFormatClassName + ".getCurrencyFormat(" + curCodeParam + ").format(" + argName
                + ")");
      } else if ("percent".equals(subformat)) {
        out.appendStringValuedExpression(
            numFormatClassName + ".getPercentFormat().format(" + argName + ")");
      } else {
        if (curCodeParam.length() > 0) {
          curCodeParam = ", " + curCodeParam;
        }
        out.appendStringValuedExpression(
            numFormatClassName + ".getFormat(" + wrap(subformat) + curCodeParam + ").format("
                + argName + ")");
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
     * Allow generated code to take advantage of plural offsets (see
     * {@link Offset}).
     */
    void enablePluralOffsets();

    /**
     * Return the count of parameters.
     * @return the count of parameters 
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
     * Return an expression to get the value of the requested parameter.  Note
     * that for arrays or lists this will return an expression giving the count
     * of items in the array or list.
     *  
     * @param i index of the paramter, 0 .. getCount() - 1
     * @return the source of code to access the parameter value
     */
    String getParameterExpression(int i);

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
    private int[] offset;
    private boolean[] isList;
    private boolean[] isArray;
    private boolean enablePluralOffsets;

    public ParametersImpl(JParameter[] params, boolean[] seenFlag) {
      this.params = params;
      this.seenFlag = seenFlag;
      int n = params.length;
      offset = new int[n];
      isList = new boolean[n];
      isArray = new boolean[n];
      for (int i = 0; i < n; ++i) {
        Offset offsetAnnot = params[i].getAnnotation(Offset.class);
        if (offsetAnnot != null) {
          offset[i] = offsetAnnot.value();
        }
        JType type = params[i].getType();
        if (type.isArray() != null) {
          isArray[i] = true;
        } else if (type.isInterface() != null) {
          JClassType rawType = type.isInterface().getErasedType();
          if ("java.util.List".equals(rawType.getQualifiedSourceName())) {
            isList[i] = true;
          }
        }
      }
    }

    public void enablePluralOffsets() {
      enablePluralOffsets = true;
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

    public String getParameterExpression(int i) {
      if (i < 0 || i >= params.length) {
        return null;
      }
      String argName = "arg" + i;
      seenFlag[i] = true;
      if (enablePluralOffsets && offset[i] != 0) {
        return argName + "_count";
      }
      if (isArray[i]) {
        return argName + ".length"; 
      }
      if (isList[i]) {
        return argName + ".size()";
      }
      return argName;
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
   * An {@link AlternateFormSelector} used with {@link PluralCount}.
   */
  private static class PluralFormSelector extends AlternateFormSelector {
    protected final String countSuffix;
    protected final String listSuffix;
    protected final Set<String> missingPluralForms;
    protected final int pluralOffset;
    protected final PluralRule pluralRule;
    private boolean hasExactMatches;
    private boolean inExactMatches;
    
    // used to generate unique case values for bogus plural forms
    private int bogusCaseValue = 1000;
    
    public PluralFormSelector(TreeLogger logger, JMethod method, int argNumber,
        JParameter[] params, GwtLocale locale)
        throws UnableToCompleteException {
      super(logger, argNumber, params);
      PluralCount pluralCount = params[argNumber].getAnnotation(
          PluralCount.class);
      Class<? extends PluralRule> ruleClass = pluralCount.value();
      if (ruleClass == PluralRule.class) {
        ruleClass = DefaultRule.class;
      }
      pluralRule = createLocalizedPluralRule(logger,
          method.getEnclosingType().getOracle(), ruleClass, locale);
      missingPluralForms = new HashSet<String>();
      for (PluralForm form : pluralRule.pluralForms()) {
        if (form.getWarnIfMissing() && !AlternateMessageSelector.OTHER_FORM_NAME.equals(form.getName())) {
          missingPluralForms.add(form.getName());
        }
      }
      
      Offset offsetAnnot = params[argNumber].getAnnotation(Offset.class);
      int offset = 0;
      if (offsetAnnot != null) {
        offset = offsetAnnot.value();
      }
      this.pluralOffset = offset;
      boolean isArray = false;
      boolean isList = false;
      JPrimitiveType primType = argType.isPrimitive();
      JClassType classType = argType.isInterface();
      if (classType != null) {
        classType = classType.getErasedType();
        if ("java.util.List".equals(classType.getQualifiedSourceName())) {
          isList = true;
        } else {
          classType = null;
        }
      }

      JArrayType arrayType = argType.isArray();
      if (arrayType != null) {
        isArray = true;
      }
      if (!isList && !isArray && (primType == null
          || (primType != JPrimitiveType.INT
              && primType != JPrimitiveType.SHORT))) {
        throw error(logger, method.getName()
            + ": PluralCount parameter must be int, short, array, or List");
      }
      String tempListSuffix = "";
      if (isList) {
        tempListSuffix = ".size()";
      } else if (isArray) {
        tempListSuffix = ".length";
      }
      String tempCountSuffix = tempListSuffix;
      if (isList || isArray || offset != 0) {
        tempCountSuffix = "_count";
      }
      listSuffix = tempListSuffix;
      countSuffix = tempCountSuffix;
    }

    @Override
    public void generatePrepCode(SourceWriter out) {
      // save a value with the count value, applying an offset if present
      if (countSuffix.length() > 0) {
        out.print("int arg" + argNumber + countSuffix + " = arg" + argNumber
            + listSuffix);
        if (pluralOffset != 0) {
          out.print(" - " + pluralOffset);
        }
        out.println(";");
      }
      // save the selected plural form
      // TODO(jat): cache instances of the same plural rule?
      out.println("int arg" + argNumber + "_form = new "
          + pluralRule.getClass().getCanonicalName()
          + "().select(arg" + argNumber + countSuffix + ");");
    }

    @Override
    public void generateSelectEnd(SourceWriter out) {
      if (hasExactMatches && !inExactMatches) {
        // undo extra nesting level
        out.outdent();
        out.println("}");
        out.println("break;");
        out.outdent();
      }
      out.outdent();
      out.println("}");
    }
    
    @Override
    public void generateSelectMatchEnd(SourceWriter out, String value) {
      out.println("break;");
      out.outdent();
    }

    @Override
    public void generateSelectMatchStart(SourceWriter out, TreeLogger logger,
        String value) throws UnableToCompleteException {
      missingPluralForms.remove(value);
      if (value.startsWith("=")) {
        try {
          long val = Long.parseLong(value.substring(1));
          out.println("case " + val + ":  // " + value);
        } catch (NumberFormatException e) {
          throw error(logger, "Exact match value '" + value
              + "' must be integral", e);
        }
        out.indent();
        return;
      }
      if (inExactMatches) {
        /*
         * If this is the first non-exact value, create a nested select that
         * chooses the message based on the plural form only if no exact values
         * matched.
         */
        inExactMatches = false;
        out.println("default: // non-exact matches");
        out.indent();
        out.println("switch (arg" + argNumber + "_form) {");
        out.indent();
      }
      if (AlternateMessageSelector.OTHER_FORM_NAME.equals(value)) {
        out.println("default: // other");
        out.indent();
        return;
      }
      PluralForm[] pluralForms = pluralRule.pluralForms();
      for (int i = 0; i < pluralForms.length; ++i) {
        if (pluralForms[i].getName().equals(value)) {
          out.println("case " + i + ":  // " + value);
          out.indent();
          return;
        }
      }
      logger.log(TreeLogger.WARN, "Plural form '" + value + "' unknown in "
          + pluralRule.getClass().getCanonicalName() + ": ignoring");
      // TODO(jat): perhaps return a failure instead, and let the called skip
      // the nested selector code?  It gets complicated really quick though.
      out.println("case " + (bogusCaseValue++) + ": // unknown plural form '"
          + value + "'");
      out.indent();
    }

    @Override
    public void generateSelectStart(SourceWriter out, boolean hasExactMatches) {
      this.hasExactMatches = hasExactMatches;
      inExactMatches = hasExactMatches;
      String suffix = hasExactMatches ? listSuffix : "_form";
      out.println("switch (arg" + argNumber + suffix + ") {");
      out.indent();
    }

    public PluralForm[] getPluralForms() {
      return pluralRule.pluralForms();
    }
    
    @Override
    public void issueWarnings(TreeLogger logger, JMethod m, GwtLocale locale) {
      if (!missingPluralForms.isEmpty()) {
        // TODO(jat): avoid giving warnings for values that are not necessary
        // due to exact value matches.  For example, in English there is no need
        // for ONE if the =1 value was given, and it may be important to have
        // the =1 value across all locales.
        logger.log(TreeLogger.WARN, "In locale '" + locale
            + "', required plural forms are missing: " + missingPluralForms);
      }
    }
  }

  /**
   * Implements {x,time...} references in MessageFormat.
   */
  private static class TimeFormatter implements ValueFormatter {

    public boolean format(TreeLogger logger,
        GwtLocale locale,
        StringGenerator out,
        Map<String, String> formatArgs,
        String subformat,
        String argName,
        JType argType,
        Parameters params) {
      if (!"java.util.Date".equals(argType.getQualifiedSourceName())) {
        logger.log(TreeLogger.ERROR, "Only java.util.Date acceptable for date format");
        return true;
      }
      String tzParam = "";
      String tzArg = formatArgs.get("tz");
      if (tzArg != null) {
        if (tzArg.startsWith("$")) {
          int paramNum = params.getParameterIndex(tzArg.substring(1));
          if (paramNum < 0) {
            logger.log(TreeLogger.ERROR, "Unable to resolve tz argument " + tzArg);
            return true;
          } else if (!"com.google.gwt.i18n.client.TimeZone".equals(
              params.getParameter(paramNum).getType().getQualifiedSourceName())) {
            logger.log(TreeLogger.ERROR, "Currency code parameter must be TimeZone");
            return true;
          } else {
            tzParam = ", arg" + paramNum;
          }
        } else {
          tzParam = ", com.google.gwt.i18n.client.TimeZone.createTimeZone(" + tzArg + ")";
        }
      }
      if (subformat == null || "medium".equals(subformat)) {
        out.appendStringValuedExpression(
            dtFormatClassName + ".getMediumTimeFormat().format(" + argName + tzParam + ")");
      } else if ("full".equals(subformat)) {
        out.appendStringValuedExpression(
            dtFormatClassName + ".getFullTimeFormat().format(" + argName + tzParam + ")");
      } else if ("long".equals(subformat)) {
        out.appendStringValuedExpression(
            dtFormatClassName + ".getLongTimeFormat().format(" + argName + tzParam + ")");
      } else if ("short".equals(subformat)) {
        out.appendStringValuedExpression(
            dtFormatClassName + ".getShortTimeFormat().format(" + argName + tzParam + ")");
      } else {
        logger.log(TreeLogger.WARN, "Use localdatetime format instead");
        out.appendStringValuedExpression(
            dtFormatClassName + ".getFormat(" + wrap(subformat) + ").format(" + argName + tzParam
                + ")");
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
     * @param out StringBuilder to append to
     * @param formatArgs format-specific arguments
     * @param subformat the remainder of the format string
     * @param argName the name of the argument to use in the generated code
     * @param argType the type of the argument
     * @param params argument list or null
     * @return true if a fatal error occurred (which will already be logged)
     */
    boolean format(TreeLogger logger,
        GwtLocale locale,
        StringGenerator out,
        Map<String, String> formatArgs,
        String subformat,
        String argName,
        JType argType,
        Parameters params);
  }

  /**
   * Class names, in a refactor-friendly manner.
   */
  private static final String dtFormatClassName = DateTimeFormat.class.getCanonicalName();

  /**
   * Fully-qualified class name of the SafeHtml interface.
   */
  public static final String SAFE_HTML_FQCN = SafeHtml.class.getCanonicalName();

  /**
   * Fully-qualified class name of the SafeHtmlBuilder class.
   */
  public static final String SAFE_HTML_BUILDER_FQCN = SafeHtmlBuilder.class.getCanonicalName();

  /**
   * Map of supported formats.
   */
  private static Map<String, ValueFormatter> formatters = new HashMap<String, ValueFormatter>();

  private static final String numFormatClassName = NumberFormat.class.getCanonicalName();

  /*
   * Register supported formats.
   */
  static {
    formatters.put("date", new DateFormatter());
    formatters.put("number", new NumberFormatter());
    formatters.put("time", new TimeFormatter());
    formatters.put("localdatetime", new LocalDateTimeFormatter());
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
   *         TODO: consider impact of possibly having multiple TypeOracles
   */
  private static PluralRule createLocalizedPluralRule(
      TreeLogger logger, TypeOracle oracle, Class<? extends PluralRule> ruleClass, GwtLocale locale)
      throws UnableToCompleteException {
    String baseName = ruleClass.getCanonicalName();
    JClassType ruleJClassType = oracle.findType(baseName);
    Map<String, JClassType> matchingClasses =
        LocalizableLinkageCreator.findDerivedClasses(logger, ruleJClassType);
    for (GwtLocale search : locale.getCompleteSearchList()) {
      JClassType localizedType = matchingClasses.get(search.toString());
      if (localizedType != null) {
        try {
          Class<?> testClass = Class.forName(
              localizedType.getQualifiedBinaryName(), false, PluralRule.class.getClassLoader());
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

  private final Map<GwtLocale, Map<String, String>> listPatternCache;

  private SourceWriter writer;

  /**
   * Constructor for <code>MessagesMethodCreator</code>.
   *
   * @param classCreator associated class creator
   * @param writer 
   */
  public MessagesMethodCreator(AbstractGeneratorClassCreator classCreator,
      SourceWriter writer) {
    super(classCreator);
    listPatternCache = new HashMap<GwtLocale, Map<String, String>>();
    this.writer = writer;
  }

  /**
   * Append an argument to the output without doing any formatting.
   * 
   * @param buf
   * @param argExpr Java source for expression to produce argument value
   * @param argType type of the argument being appended
   */
  private void appendUnformattedArg(StringGenerator buf, String argExpr, JType argType) {
    boolean isSafeHtmlTyped = SAFE_HTML_FQCN.equals(argType.getQualifiedSourceName());
    boolean isPrimitiveType = (argType.isPrimitive() != null);
    boolean needsConversionToString =
        !("java.lang.String".equals(argType.getQualifiedSourceName()));
    buf.appendExpression(argExpr, isSafeHtmlTyped, isPrimitiveType, needsConversionToString);
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
    boolean seenPluralCount = false;
    boolean seenSelect = false;

    int numParams = params.length;
    int lastPluralArgNumber = -1;
    List<AlternateFormSelector> selectors = new ArrayList<AlternateFormSelector>();
    // See if any parameter is tagged as a PluralCount or Select parameter.
    for (int i = 0; i < numParams; ++i) {
      PluralCount pluralCount = params[i].getAnnotation(PluralCount.class);
      Select select = params[i].getAnnotation(Select.class);
      if (pluralCount != null && select != null) {
        throw error(logger, params[i].getName() + " cannot be both @PluralCount"
            + " and @Select");
      }
      AlternateFormSelector selector = null;
      if (select != null) {
        selector = new GenericSelector(logger, m, i, params);
        seenSelect = true;
      } else if (pluralCount != null) {
        PluralFormSelector pluralSelector = new PluralFormSelector(logger, m, i,
            params, locale);
        selector = pluralSelector;
        if (!seenPluralCount) {
          // TODO(jat): what if we have different plural rules on the different
          // forms?
          resourceList.setPluralForms(key, pluralSelector.getPluralForms());
        }
        seenPluralCount = true;
        lastPluralArgNumber = i;
      }
      if (selector != null) {
        selectors.add(selector);
      }
    }

    boolean[] seenFlags = new boolean[numParams];
    final Parameters paramsAccessor = new ParametersImpl(params, seenFlags);
    boolean isSafeHtml = m.getReturnType().getQualifiedSourceName().equals(
        SAFE_HTML_FQCN);

    String template = resourceEntry.getForm(null);
    if (template == null) {
      logger.log(TreeLogger.ERROR,"No default form for method " + m.getName()
          + "' in " + m.getEnclosingType() + " for locale " + locale, null);
      throw new UnableToCompleteException();
    }

    // Generate code to format any lists
    // TODO(jat): handle messages with different list formats in alternate forms 
    try {
      for (TemplateChunk chunk : MessageFormatParser.parse(template)) {
        if (chunk instanceof ArgumentChunk) {
          ArgumentChunk argChunk = (ArgumentChunk) chunk;
          if (argChunk.isList()) {
            ListAccessor listAccessor = null;
            int listArgNum = argChunk.getArgumentNumber();
            JType listType = params[listArgNum].getType();
            JClassType classType = listType.isInterface();
            JType elemType = null;
            if (classType != null) {
              if ("java.util.List".equals(
                  classType.getErasedType().getQualifiedSourceName())) {
                listAccessor = new ListAccessorList(listArgNum);
              } else {
                logger.log(TreeLogger.ERROR, "Parameters formatted as lists "
                    + "must be declared as java.util.List or arrays in "
                    + m.getEnclosingType().getSimpleSourceName() + "."
                    + m.getName());
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
            generateListFormattingCode(logger, locale, argChunk,
                elemType, isSafeHtml, listAccessor, paramsAccessor);
          }
        }
      }
    } catch (ParseException pe) {
      throw error(logger, "Error parsing '" + template + "'", pe);
    }

    if (!seenPluralCount && !seenSelect 
        && (m.getAnnotation(AlternateMessage.class) != null
        || m.getAnnotation(PluralText.class) != null)) {
      logger.log(TreeLogger.WARN, "Unused @AlternateMessage or @PluralText on "
          + m.getEnclosingType().getSimpleSourceName() + "." + m.getName()
          + "; did you intend to mark a @Select or @PluralCount parameter?",
          null);
    }
    Collection<String> resourceForms = resourceEntry.getForms();
    if (seenPluralCount || seenSelect) {
      paramsAccessor.enablePluralOffsets();
      writer.println(m.getReturnType().getParameterizedQualifiedSourceName()
          + " returnVal = null;");
      for (AlternateFormSelector selector : selectors) {
        selector.generatePrepCode(writer);
      }
      
      // sort forms so that all exact-value forms come first
      String[] forms = resourceForms.toArray(new String[resourceForms.size()]);
      Arrays.sort(forms, new ExactValueComparator());

      generateMessageSelectors(logger, m, locale, resourceEntry, selectors, paramsAccessor,
          isSafeHtml, forms, lastPluralArgNumber);
      for (AlternateFormSelector selector : selectors) {
        selector.issueWarnings(logger, m, locale);
      }
      writer.println("if (returnVal != null) {");
      writer.indent();
      writer.println("return returnVal;");
      writer.outdent();
      writer.println("}");
    }
    writer.print("return ");
    generateString(logger, locale, template, paramsAccessor, writer,
        isSafeHtml, lastPluralArgNumber);
    writer.println(";");

    // Generate an error if any required parameter was not used somewhere.
    for (int i = 0; i < numParams; ++i) {
      if (!seenFlags[i]) {
        Optional optional = params[i].getAnnotation(Optional.class);
        Select select = params[i].getAnnotation(Select.class);
        if (optional == null && select == null) {
          throw error(logger, "Required argument " + i + " not present: "
              + template);
        }
      }
    }
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
    appendUnformattedArg(buf, argExpr, paramType);
  }

  /**
   * Generate code for one list pattern.
   *
   * @param logger logger to use for error/warning messages
   * @param locale locale we are generating code for
   * @param listArg the {n,list,...} argument in the original format pattern
   * @param val0 the expression defining the {0} argument in the list pattern
   * @param val1 the expression defining the {1} argument in the list pattern
   * @param elemType the element type of the list/array being rendered as a list
   *        * @param isSafeHtml true if the resulting string is SafeHtml
   * @param listPattern the list pattern to generate code for, ie "{0}, {1}"
   * @param formatSecond true if the {1} parameter needs to be formatted
   * @param params parameters passed to the Messages method call
   * @return a constructed string containing the code to implement the given
   *         list pattern
   * @throws UnableToCompleteException
   */
  private CharSequence formatListPattern(final TreeLogger logger,
      final GwtLocale locale, final ArgumentChunk listArg, final String val0,
      final String val1, final JType elemType, final boolean isSafeHtml,
      String listPattern, final boolean formatSecond, final Parameters params)
      throws UnableToCompleteException {
    final StringBuilder buf = new StringBuilder();
    final StringGenerator gen = StringGenerator.create(buf, isSafeHtml);
    try {
      List<TemplateChunk> chunks = MessageFormatParser.parse(listPattern);
      for (TemplateChunk chunk : chunks) {
        chunk.accept(new DefaultTemplateChunkVisitor() {
          @Override
          public void visit(ArgumentChunk argChunk) throws UnableToCompleteException {
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
          public void visit(StringChunk stringChunk) {
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
   * @param listArg the {n,list,...} argument in the original format pattern
   * @param elemType the element type of the list/array being rendered as a list
   * @param isSafeHtml true if the resulting string is SafeHtml
   * @param listAccessor a way to access elements of the list type supplied by
   *        the user
   * @param params parameters passed to the Messages method call
   * @throws UnableToCompleteException
   */
  private void generateListFormattingCode(TreeLogger logger, GwtLocale locale,
      ArgumentChunk listArg, JType elemType,
      boolean isSafeHtml, ListAccessor listAccessor, Parameters params)
      throws UnableToCompleteException {
    Map<String, String> listPatternParts = getListPatternParts(logger, locale);
    int listArgNum = listArg.getArgumentNumber();
    writer.println("int arg" + listArgNum + "_size = " + listAccessor.getSize()
        + ";");
    if (isSafeHtml) {
      writer.println(SafeHtml.class.getCanonicalName() + " arg" + listArgNum
          + "_list = new "
          + OnlyToBeUsedInGeneratedCodeStringBlessedAsSafeHtml.class.getCanonicalName()
          + "(\"\");");
    } else {
      writer.println("String arg" + listArgNum + "_list = \"\";");
    }
    writer.println("switch (arg" + listArgNum + "_size) {");
    writer.indent();
    // TODO(jat): add support for special-cases besides 2 if CLDR ever adds them
    String pairPattern = listPatternParts.get("2");
    if (pairPattern != null) {
      writer.println("case 2:");
      writer.indent();
      writer.println("  arg" + listArgNum + "_list = "
          + formatListPattern(logger, locale, listArg,
          listAccessor.getElement("0"), listAccessor.getElement("1"), elemType,
          isSafeHtml, pairPattern, true, params) + ";");
      writer.println("break;");
      writer.outdent();
    }
    writer.println("default:");
    writer.indent();
    writer.println("int i = arg" + listArgNum + "_size;");
    writer.println("if (i > 0) {");
    writer.indent();
    StringBuilder outbuf = new StringBuilder();
    StringGenerator buf = StringGenerator.create(outbuf, isSafeHtml);
    formatArg(logger, locale, buf, listArg, listAccessor.getElement("--i"),
        elemType, params);
    buf.completeString();
    writer.println("arg" + listArgNum + "_list = " + outbuf + ";");
    writer.outdent();
    writer.println("}");
    writer.println("if (i > 0) {");
    writer.indent();
    writer.println("arg" + listArgNum + "_list = "
        + formatListPattern(logger, locale, listArg,
        listAccessor.getElement("--i"), "arg" + listArgNum + "_list", elemType,
        isSafeHtml, listPatternParts.get("end"), false, params) + ";");
    writer.outdent();
    writer.println("}");
    writer.println("while (i > 1) {");
    writer.indent();
    writer.println("arg" + listArgNum + "_list = "
        + formatListPattern(logger, locale, listArg,
        listAccessor.getElement("--i"), "arg" + listArgNum + "_list", elemType,
        isSafeHtml, listPatternParts.get("middle"), false, params) + ";");
    writer.outdent();
    writer.println("}");
    writer.println("if (i > 0) {");
    writer.indent();
    writer.println("arg" + listArgNum + "_list = "
        + formatListPattern(logger, locale, listArg,
        listAccessor.getElement("--i"), "arg" + listArgNum + "_list", elemType,
        isSafeHtml, listPatternParts.get("start"), false, params) + ";");
    writer.outdent();
    writer.println("}");
    writer.println("break;");
    writer.outdent();
    writer.outdent();
    writer.println("}");
  }

  /**
   * @param logger
   * @param m
   * @param locale
   * @param resourceEntry
   * @param selectors
   * @param paramsAccessor
   * @param isSafeHtml
   * @param forms
   * @param lastPluralArgNumber index of most recent plural argument, used for
   *     processing inner-plural arguments ({#})
   * @throws UnableToCompleteException
   */
  private void generateMessageSelectors(TreeLogger logger, JMethod m,
      GwtLocale locale, ResourceEntry resourceEntry,
      List<AlternateFormSelector> selectors, Parameters paramsAccessor,
      boolean isSafeHtml, String[] forms, int lastPluralArgNumber)
      throws UnableToCompleteException {
    int numSelectors = selectors.size();
    String[] lastForm = new String[numSelectors];
    for (String form : forms) {
      String[] splitForms = form.split("\\|");
      if (splitForms.length != numSelectors) {
        throw error(logger, "Incorrect number of selector forms for "
            + m.getName() + " - '" + form + "'");
      }
      boolean allOther = true;
      for (String splitForm : splitForms) {
        if (splitForm.startsWith("=")) {
          allOther = false;
        } else if (!AlternateMessageSelector.OTHER_FORM_NAME.equals(splitForm)) {
          allOther = false;
        }
      }
      if (allOther) {
        // don't process the all-other case, that is the default return value
        logger.log(TreeLogger.WARN, "Ignoring supplied alternate form with all"
            + " 'other' values, @DefaultMessage will be used");
        continue;
      }
      
      // find where the changes are
      int firstDifferent = 0;
      while (firstDifferent < numSelectors
          && splitForms[firstDifferent].equals(lastForm[firstDifferent])) {
        firstDifferent++;
      }

      // close nested selects deeper than where the change was
      for (int i = numSelectors; i-- > firstDifferent; ) {
        if (lastForm[i] != null) {
          selectors.get(i).generateSelectMatchEnd(writer, lastForm[i]);
          if (i > firstDifferent) {
            selectors.get(i).generateSelectEnd(writer);
          }
        }
      }

      // open all the nested selects from here
      for (int i = firstDifferent; i < numSelectors; ++i) {
        if (i > firstDifferent || lastForm[i] == null) {
          selectors.get(i).generateSelectStart(writer,
              splitForms[i].startsWith("="));
        }
        selectors.get(i).generateSelectMatchStart(writer, logger,
            splitForms[i]);
        lastForm[i] = splitForms[i];
      }
      writer.print("returnVal = ");
      generateString(logger, locale, resourceEntry.getForm(form),
          paramsAccessor, writer, isSafeHtml, lastPluralArgNumber);
      writer.println(";");
    }
    for (int i = numSelectors; i-- > 0; ) {
      if (lastForm[i] != null) {
        selectors.get(i).generateSelectMatchEnd(writer, lastForm[i]);
        selectors.get(i).generateSelectEnd(writer);
      }
    }
  }

  /**
   * Generate a Java string for a given MessageFormat string.
   *
   * @param logger
   * @param template
   * @param paramsAccessor
   * @param writer
   * @param lastPluralArgNumber index of most recent plural argument, used for
   *     processing inner-plural arguments ({#})
   * @throws UnableToCompleteException
   */
  private void generateString(final TreeLogger logger, final GwtLocale locale,
      final String template, final Parameters paramsAccessor, SourceWriter writer,
      final boolean isSafeHtml, final int lastPluralArgNumber) throws UnableToCompleteException {
    StringBuilder outputBuf = new StringBuilder();
    final StringGenerator buf = StringGenerator.create(outputBuf, isSafeHtml);
    final int n = paramsAccessor.getCount();
    try {
      for (TemplateChunk chunk : MessageFormatParser.parse(template)) {
        chunk.accept(new DefaultTemplateChunkVisitor() {
          @Override
          public void visit(ArgumentChunk argChunk) throws UnableToCompleteException {
            int argNumber = argChunk.getArgumentNumber();
            if (argNumber >= n) {
              throw error(logger, "Argument " + argNumber + " beyond range of arguments: " + template);
            }
            if (argNumber < 0) {
              if (lastPluralArgNumber < 0) {
                throw error(logger, "Inner-plural notation {#} used outside in non-plural message");
              }
              argNumber = lastPluralArgNumber;
            }
            JParameter param = paramsAccessor.getParameter(argNumber);
            String arg = "arg" + argNumber;
            if (argChunk.isList()) {
              buf.appendExpression(arg + "_list", isSafeHtml, false, false);
            } else {
              JType paramType = param.getType();
              formatArg(logger, locale, buf, argChunk,
                  paramsAccessor.getParameterExpression(argNumber), paramType,
                  paramsAccessor);
            }
          }

          @Override
          public void visit(StaticArgChunk staticArgChunk) {
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
    writer.print(outputBuf.toString());
  }

  private Map<String, String> getListPatternParts(TreeLogger logger, GwtLocale locale) {
    Map<String, String> map = listPatternCache.get(locale);
    if (map == null) {
      // TODO(jat): get these from ResourceOracle instead
      String baseName = MessagesMethodCreator.class.getPackage().getName().replace('.', '/')
          + "/cldr/ListPatterns_";
      ClassLoader cl = MessagesMethodCreator.class.getClassLoader();
      for (GwtLocale search : locale.getCompleteSearchList()) {
        String propFile = baseName + search.getAsString() + ".properties";
        InputStream stream = cl.getResourceAsStream(propFile);
        if (stream != null) {
          try {
            LocalizedPropertiesLoader loader = new LocalizedPropertiesLoader(stream, "UTF-8");
            map = new HashMap<String, String>();
            loader.load(map);
            break;
          } catch (IOException e) {
            logger.log(TreeLogger.WARN, "Ignoring error reading file " + propFile, e);
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
