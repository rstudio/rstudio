/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.codegen.server;

/**
 * Helper class to produce string expressions consisting of literals and
 * computed values.
 */
public abstract class StringGenerator {

  /**
   * Type of expression being processed.
   */
  protected enum Type {
    LITERAL,
    PRIMITIVE,
    SAFE,
    OTHER,
  }

  /**
   * Create a {@link StringGenerator} instance.
   * 
   * @param buf
   * @param returnsSafeHtml
   * @return {@link StringGenerator} instance
   */
  public static StringGenerator create(StringBuilder buf, boolean returnsSafeHtml) {
    if (returnsSafeHtml) {
      return new SafeHtmlStringGenerator(buf);
    } else {
      return new PlainStringGenerator(buf);
    }
  }

  /**
   * Output string buffer.
   */
  protected final StringBuilder buf;

  /**
   * True if we are in the middle of a string literal.
   */
  protected boolean inString;

  /**
   * Initialize the StringGenerator with an output buffer.
   *
   * @param buf output buffer
   */
  protected StringGenerator(StringBuilder buf) {
    this.buf = buf;
    inString = false;
  }
  /**
   * Append an expression to this string expression.
   *
   * @param expression to add
   * @param isSafeHtmlTyped true if the expression is known to be of type
   *     {@link com.google.gwt.safehtml.shared.SafeHtml SafeHtml}; only relevant
   *     if this generator has been initialized to generate a
   *     {@link com.google.gwt.safehtml.shared.SafeHtml SafeHtml}-valued
   *     expression
   * @param isPrimitiveTyped true if the expression is of a primitive type;
   *     only relevant if this generator has been initialized to generate a
   *     {@link com.google.gwt.safehtml.shared.SafeHtml SafeHtml}-valued
   *     expression
   * @param needsConversionToString true if the expression is not known to be
   *     of type String and needs to be converted
   */
  public void appendExpression(String expression, boolean isSafeHtmlTyped,
      boolean isPrimitiveTyped, boolean needsConversionToString) {
    if (inString) {
      buf.append('"');
      afterExpression(Type.LITERAL);
      inString = false;
    }
    Type type;
    if (isPrimitiveTyped) {
      type = Type.PRIMITIVE;
    } else if (isSafeHtmlTyped) {
      type = Type.SAFE;
    } else {
      type = Type.OTHER;
    }
    beforeExpression(type);
    if (type == Type.OTHER && needsConversionToString) {
      forceStringPrefix();
    }
    buf.append(expression);
    if (type == Type.OTHER && needsConversionToString) {
      forceStringSuffix();
    }
    afterExpression(type);
  }

  /**
   * Append part of a string literal.
   *
   * @param str part of string literal
   */
  public void appendStringLiteral(String str) {
    if (!inString) {
      beforeExpression(Type.LITERAL);
      buf.append('"');
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
      buf.append('"');
      afterExpression(Type.LITERAL);
    }
    finishOutput();
  }

  protected abstract void afterExpression(Type type);

  protected abstract void beforeExpression(Type type);

  protected abstract void finishOutput();

  protected abstract void forceStringPrefix();

  protected abstract void forceStringSuffix();
}
