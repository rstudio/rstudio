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
package com.google.gwt.i18n.rebind;

/**
 * Helper class to produce string expressions consisting of literals and
 * computed values.
 */
public class StringGenerator {

  /**
   * Output string buffer.
   */
  private StringBuffer buf;

  /**
   * True if we are in the middle of a string literal.
   */
  private boolean inString;

  /**
   * True if the method's return type is SafeHtml (and SafeHtmlBuilder is to
   * be used to generate the expression); otherwise a String expression is
   * generated.
   */
  private final boolean returnsSafeHtml;

  /**
   * Initialize the StringGenerator with an output buffer.
   *
   * @param buf output buffer
   * @param returnsSafeHtml if true, an expression of type {@link SafeHtml} is
   *          being generated, otherwise a {@link String}-valued expression is
   *          generated
   */
  public StringGenerator(StringBuffer buf, boolean returnsSafeHtml) {
    this.buf = buf;
    inString = false;
    this.returnsSafeHtml = returnsSafeHtml;
    if (returnsSafeHtml) {
      buf.append("new " + MessagesMethodCreator.SAFE_HTML_BUILDER_FQCN + "()");
    } else {
      buf.append("new java.lang.StringBuffer()");
    }
  }

  /**
   * Append an expression to this string expression.
   *
   * @param expression to add
   * @param isSafeHtmlTyped true if the expression is known to be of type
   *          {@link SafeHtml}; only relevant if this generator has been
   *          initialized to generate a {@link SafeHtml}-valued expression
   * @param isPrimititiveTyped true if the expression is of a primitive type;
   *          only relevant if this generator has been initialized to generate
   *          a {@link SafeHtml}-valued expression
   * @param needsConversionToString true if the expression is not known to be
   *          of type String and needs to be converted
   */
  public void appendExpression(String expression, boolean isSafeHtmlTyped,
      boolean isPrimititiveTyped, boolean needsConversionToString) {
    if (inString) {
      buf.append("\")");
      inString = false;
    }
    /*
     * SafeHtmlBuilder has append() methods for primitive types as well as for
     * SafeHtml-valued expressions. For all other expression types, use
     * appendEscaped(). In addition, if the expression is not known to be of
     * type String, covert to String.
     */
    if (returnsSafeHtml && !isSafeHtmlTyped && !isPrimititiveTyped) {
      buf.append(".appendEscaped(");
      if (needsConversionToString) {
        buf.append("String.valueOf(");
      }
    } else {
      buf.append(".append(");
    }
    buf.append(expression);
    buf.append(")");
    if (returnsSafeHtml && !isSafeHtmlTyped && !isPrimititiveTyped
        && needsConversionToString) {
      buf.append(")");
    }
  }

  /**
   * Append part of a string literal.
   *
   * @param str part of string literal
   */
  public void appendStringLiteral(String str) {
    if (!inString) {
      if (returnsSafeHtml) {
        buf.append(".appendHtmlConstant(\"");
      } else {
        buf.append(".append(\"");
      }
      inString = true;
    }
    buf.append(str);
  }

  /**
   * Append an expression to this string expression.
   *
   * @param expression to add, which the caller asserts is String-valued
   */
  public void appendStringValuedExpression(String expression) {
    appendExpression(expression, false, false, false);
  }

  /**
   * Complete the string, closing an open quote and handling empty strings.
   */
  public void completeString() {
    if (inString) {
      buf.append("\")");
    }
    if (returnsSafeHtml) {
      buf.append(".toSafeHtml()");
    } else {
      buf.append(".toString()");
    }
  }
}
