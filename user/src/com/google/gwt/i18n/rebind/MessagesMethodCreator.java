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
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.i18n.client.PluralRule;
import com.google.gwt.i18n.client.Messages.Optional;
import com.google.gwt.i18n.client.Messages.PluralCount;
import com.google.gwt.i18n.client.Messages.PluralText;
import com.google.gwt.i18n.client.PluralRule.PluralForm;
import com.google.gwt.i18n.client.impl.plurals.DefaultRule;
import com.google.gwt.i18n.rebind.AbstractResource.ResourceList;
import com.google.gwt.i18n.rebind.MessageFormatParser.ArgumentChunk;
import com.google.gwt.i18n.rebind.MessageFormatParser.TemplateChunk;
import com.google.gwt.i18n.shared.GwtLocale;
import com.google.gwt.user.rebind.AbstractGeneratorClassCreator;
import com.google.gwt.user.rebind.AbstractMethodCreator;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

/**
 * Creator for methods of the form String getX(arg0,...,argN).
 */
class MessagesMethodCreator extends AbstractMethodCreator {

  /**
   * Implements {x,date...} references in MessageFormat.
   */
  private static class DateFormatter implements ValueFormatter {
    public String format(StringGenerator out, String subformat, String argName,
        JType argType) {
      if (!"java.util.Date".equals(argType.getQualifiedSourceName())) {
        return "Only java.util.Date acceptable for date format";
      }
      if (subformat == null || "medium".equals(subformat)) {
        out.appendExpression(dtFormatClassName + ".getMediumDateFormat()"
            + ".format(" + argName + ")", true);
      } else if ("full".equals(subformat)) {
        out.appendExpression(dtFormatClassName + ".getFullDateFormat().format("
            + argName + ")", true);
      } else if ("long".equals(subformat)) {
        out.appendExpression(dtFormatClassName + ".getLongDateFormat().format("
            + argName + ")", true);
      } else if ("short".equals(subformat)) {
        out.appendExpression(dtFormatClassName + ".getShortDateFormat()"
            + ".format(" + argName + ")", true);
      } else {
        out.appendExpression(dtFormatClassName + ".getFormat("
            + wrap(subformat) + ").format(" + argName + ")", true);
      }
      return null;
    }
  }

  /**
   * Implements {x,number...} references in MessageFormat.
   */
  private static class NumberFormatter implements ValueFormatter {
    public String format(StringGenerator out, String subformat, String argName,
        JType argType) {
      JPrimitiveType argPrimType = argType.isPrimitive();
      if (argPrimType != null) {
        if (argPrimType == JPrimitiveType.BOOLEAN
            || argPrimType == JPrimitiveType.VOID) {
          return "Illegal argument type for number format";
        }
      } else {
        JClassType classType = argType.isClass();
        if (classType == null) {
          return "Unexpected argument type for number format";
        }
        TypeOracle oracle = classType.getOracle();
        JClassType numberType = oracle.findType("java.lang.Number");
        if (!classType.isAssignableTo(numberType)) {
          return "Only Number subclasses may be formatted as a number";
        }
      }
      if (argPrimType == JPrimitiveType.BOOLEAN
          || argPrimType == JPrimitiveType.VOID) {
        return "Illegal argument type for number format";
      }
      if (subformat == null) {
        out.appendExpression(numFormatClassName + ".getDecimalFormat().format("
            + argName + ")", true);
      } else if ("integer".equals(subformat)) {
        out.appendExpression(numFormatClassName + ".getIntegerFormat().format("
            + argName + ")", true);
      } else if ("currency".equals(subformat)) {
        out.appendExpression(numFormatClassName
            + ".getCurrencyFormat().format(" + argName + ")", true);
      } else if ("percent".equals(subformat)) {
        out.appendExpression(numFormatClassName + ".getPercentFormat().format("
            + argName + ")", true);
      } else {
        out.appendExpression(numFormatClassName + ".getFormat("
            + wrap(subformat) + ").format(" + argName + ")", true);
      }
      return null;
    }
  }

  /**
   * Helper class to produce string expressions consisting of literals and
   * computed values.
   */
  private static class StringGenerator {

    /**
     * Output string buffer.
     */
    private StringBuffer buf;

    /**
     * True if we are in the middle of a string literal.
     */
    private boolean inString;

    /**
     * True if we have produced any output.
     */
    private boolean producedOutput;

    /**
     * Initialize the StringGenerator with an output buffer.
     * 
     * @param buf output buffer
     */
    public StringGenerator(StringBuffer buf) {
      this.buf = buf;
      producedOutput = false;
      inString = false;
    }

    /**
     * Append an expression to this string expression.
     * 
     * @param expression to add
     */
    public void appendExpression(String expression) {
      appendExpression(expression, false);
    }

    /**
     * Append an expression to this string expression.
     * 
     * @param expression to add
     * @param knownToBeString true if the expression is known to be of string
     *          type; otherwise ""+ will be prepended to ensure it.
     */
    public void appendExpression(String expression, boolean knownToBeString) {
      if (inString) {
        buf.append('"');
        inString = false;
        producedOutput = true;
      }
      if (producedOutput) {
        buf.append(" + ");
      } else if (!knownToBeString) {
        buf.append("\"\" + ");
      }
      buf.append(expression);
      producedOutput = true;
    }

    /**
     * Append part of a string literal.
     * 
     * @param ch part of string literal
     */
    public void appendStringLiteral(char ch) {
      if (!inString) {
        if (producedOutput) {
          buf.append(" + ");
        } else {
          producedOutput = true;
        }
        buf.append('"');
        inString = true;
      }
      buf.append(ch);
    }

    /**
     * Append part of a string literal.
     * 
     * @param str part of string literal
     */
    public void appendStringLiteral(String str) {
      if (!inString) {
        if (producedOutput) {
          buf.append(" + ");
        } else {
          producedOutput = true;
        }
        buf.append('"');
        inString = true;
      }
      buf.append(str);
    }

    /**
     * Complete the string, closing an open quote and handling empty strings.
     */
    public void completeString() {
      if (inString) {
        buf.append('\"');
      } else if (!producedOutput) {
        buf.append("\"\"");
      }
    }
  }

  /**
   * Implements {x,time...} references in MessageFormat.
   */
  private static class TimeFormatter implements ValueFormatter {
    public String format(StringGenerator out, String subformat, String argName,
        JType argType) {
      if (!"java.util.Date".equals(argType.getQualifiedSourceName())) {
        return "Only java.util.Date acceptable for date format";
      }
      if (subformat == null || "medium".equals(subformat)) {
        out.appendExpression(dtFormatClassName
            + ".getMediumTimeFormat().format(" + argName + ")", true);
      } else if ("full".equals(subformat)) {
        out.appendExpression(dtFormatClassName + ".getFullTimeFormat().format("
            + argName + ")", true);
      } else if ("long".equals(subformat)) {
        out.appendExpression(dtFormatClassName + ".getLongTimeFormat().format("
            + argName + ")", true);
      } else if ("short".equals(subformat)) {
        out.appendExpression(dtFormatClassName
            + ".getShortTimeFormat().format(" + argName + ")", true);
      } else {
        out.appendExpression(dtFormatClassName + ".getFormat("
            + wrap(subformat) + ").format(" + argName + ")", true);
      }
      return null;
    }
  }

  private interface ValueFormatter {
    /**
     * Creates code to format a value according to a format string.
     * 
     * @param out StringBuffer to append to
     * @param subformat the remainder of the format string
     * @param argName the name of the argument to use in the generated code
     * @param argType the type of the argument
     * @return null if no error or an appropriate error message
     */
    String format(StringGenerator out, String subformat, String argName,
        JType argType);
  }

  /**
   * Class names, in a refactor-friendly manner.
   */
  private static final String dtFormatClassName = DateTimeFormat.class.getCanonicalName();

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
    // TODO: implement ChoiceFormat and PluralFormat
  }

  /**
   * Constructor for <code>MessagesMethodCreator</code>.
   * 
   * @param classCreator associated class creator
   */
  public MessagesMethodCreator(AbstractGeneratorClassCreator classCreator) {
    super(classCreator);
  }

  @Override
  public void createMethodFor(TreeLogger logger, JMethod m, String key,
      ResourceList resourceList, GwtLocale locale)
      throws UnableToCompleteException {
    JParameter[] params = m.getParameters();
    int pluralParamIndex = -1;
    Class<? extends PluralRule> ruleClass = null;
    int numParams = params.length;
    boolean[] seenFlags = new boolean[numParams];

    // See if any parameter is tagged as a PluralCount parameter.
    for (int i = 0; i < numParams; ++i) {
      PluralCount pluralCount = params[i].getAnnotation(PluralCount.class);
      if (pluralCount != null) {
        if (pluralParamIndex >= 0) {
          throw error(logger, m.getName()
              + ": there can only be one PluralCount parameter");
        }
        JPrimitiveType primType = params[i].getType().isPrimitive();
        if (primType != JPrimitiveType.INT && primType != JPrimitiveType.SHORT) {
          throw error(logger, m.getName()
              + ": PluralCount parameter must be int or short");
        }
        pluralParamIndex = i;
        ruleClass = pluralCount.value();
      }
    }

    StringBuffer generated = new StringBuffer();
    if (ruleClass == null) {
      if (m.getAnnotation(PluralText.class) != null) {
        logger.log(TreeLogger.WARN, "Unused @PluralText on "
            + m.getEnclosingType().getSimpleSourceName() + "." + m.getName()
            + "; did you intend to mark a @PluralCount parameter?", null);
      }
    } else {
      if (ruleClass == PluralRule.class) {
        ruleClass = DefaultRule.class;
      }
      PluralRule rule = createLocalizedPluralRule(logger,
          m.getEnclosingType().getOracle(), ruleClass, locale);
      logger.log(TreeLogger.TRACE, "Using plural rule " + rule.getClass()
          + " for locale '" + locale + "'", null);
      generated.append(PluralRule.class.getCanonicalName());
      generated.append(" rule = new " + rule.getClass().getCanonicalName()
          + "();\n");
      generated.append("switch (rule.select(arg" + pluralParamIndex + ")) {\n");
      PluralForm[] pluralForms = rule.pluralForms();
      resourceList.setPluralForms(key, pluralForms);
      // Skip default plural form (index 0); the fall-through case will handle
      // it.
      for (int i = 1; i < pluralForms.length; ++i) {
        String template = resourceList.getStringExt(key,
            pluralForms[i].getName());
        if (template != null) {
          generated.append("  // " + pluralForms[i].getName() + " - "
              + pluralForms[i].getDescription() + "\n");
          generated.append("  case " + i + ": return ");
          generateString(logger, template, params, seenFlags, generated);
          generated.append(";\n");
        } else if (pluralForms[i].getWarnIfMissing()) {
          logger.log(TreeLogger.WARN, "No plural form '"
              + pluralForms[i].getName() + "' defined for method '"
              + m.getName() + "' in " + m.getEnclosingType() + " for locale "
              + locale, null);
        }
      }
      generated.append("}\n");
    }
    generated.append("return ");
    String template = resourceList.getRequiredStringExt(key, null);
    generateString(logger, template, params, seenFlags, generated);

    // Generate an error if any required parameter was not used somewhere.
    for (int i = 0; i < numParams; ++i) {
      if (!seenFlags[i]) {
        Optional optional = params[i].getAnnotation(Optional.class);
        if (optional == null) {
          throw error(logger, "Required argument " + i + " not present: "
              + template);
        }
      }
    }

    generated.append(';');
    println(generated.toString());
  }

  /**
   * Creates an instance of a locale-specific plural rule implementation.
   * 
   * Note that this uses TypeOracle's ability to find all subclasses of the
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
   * TODO: consider impact of possibly having multiple TypeOracles
   */
  private PluralRule createLocalizedPluralRule(TreeLogger logger,
      TypeOracle oracle, Class<? extends PluralRule> ruleClass,
      GwtLocale locale)
      throws UnableToCompleteException {
    String baseName = ruleClass.getCanonicalName();
    JClassType ruleJClassType = oracle.findType(baseName);
    Map<String, JClassType> matchingClasses = LocalizableLinkageCreator.findDerivedClasses(
        logger, ruleJClassType);
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

  /**
   * Generate a Java string for a given MessageFormat string.
   * 
   * @param logger
   * @param template
   * @param params
   * @param seenFlag
   * @param outputBuf
   * @throws UnableToCompleteException
   */
  @SuppressWarnings("fallthrough")
  private void generateString(TreeLogger logger, String template,
      JParameter[] params, boolean[] seenFlag, StringBuffer outputBuf)
      throws UnableToCompleteException {
    StringGenerator buf = new StringGenerator(outputBuf);
    try {
      for (TemplateChunk chunk : MessageFormatParser.parse(template)) {
        if (chunk.isLiteral()) {
          buf.appendStringLiteral(chunk.getString());
        } else if (chunk instanceof ArgumentChunk) {
          ArgumentChunk argChunk = (ArgumentChunk) chunk;
          int argNumber = argChunk.getArgumentNumber();
          if (argNumber >= params.length) {
            throw error(logger, "Argument " + argNumber
                + " beyond range of arguments: " + template);
          }
          seenFlag[argNumber] = true;
          String arg = "arg" + argNumber;
          String format = argChunk.getFormat();
          if (format != null) {
            String subformat = argChunk.getSubFormat();
            ValueFormatter formatter = formatters.get(format);
            if (formatter != null) {
              String err = formatter.format(buf, subformat, arg,
                  params[argNumber].getType());
              if (err != null) {
                throw error(logger, err);
              }
              continue;
            }
          }
          // no format specified or unknown format
          // have to ensure that the result is stringified if necessary
          buf.appendExpression(
              arg,
              "java.lang.String".equals(params[argNumber].getType().getQualifiedSourceName()));
        } else {
          throw error(logger, "Unexpected result from parsing template "
              + template);
        }
      }
    } catch (ParseException e) {
      throw error(logger, e);
    }
    buf.completeString();
  }
}
