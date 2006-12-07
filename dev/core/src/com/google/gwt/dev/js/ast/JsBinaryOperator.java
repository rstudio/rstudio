/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.dev.js.ast;

/**
 * Represents the operator in a JavaScript binary operation.
 */
public final class JsBinaryOperator extends JsOperator {

  // Precedence indices from "JavaScript - The Definitive Guide" 4th Edition
  // (page 57)
  //

  // Precendence 15 is for really important things that have their own AST
  // classes.

  // Precendence 14 is for unary operators.

  private static final int LEFT_INFIX = LEFT | INFIX;
  public static final JsBinaryOperator MUL = create("*", 13, LEFT_INFIX);
  public static final JsBinaryOperator DIV = create("/", 13, LEFT_INFIX);
  public static final JsBinaryOperator MOD = create("%", 13, LEFT_INFIX);

  public static final JsBinaryOperator ADD = create("+", 12, LEFT_INFIX);
  public static final JsBinaryOperator SUB = create("-", 12, LEFT_INFIX);

  public static final JsBinaryOperator SHL = create("<<", 11, LEFT_INFIX);
  public static final JsBinaryOperator SHR = create(">>", 11, LEFT_INFIX);
  public static final JsBinaryOperator SHRU = create(">>>", 11, LEFT_INFIX);

  public static final JsBinaryOperator LT = create("<", 10, LEFT_INFIX);
  public static final JsBinaryOperator LTE = create("<=", 10, LEFT_INFIX);
  public static final JsBinaryOperator GT = create(">", 10, LEFT_INFIX);
  public static final JsBinaryOperator GTE = create(">=", 10, LEFT_INFIX);
  public static final JsBinaryOperator INSTANCEOF = create("instanceof", 10,
    LEFT_INFIX);
  public static final JsBinaryOperator INOP = create("in", 10, LEFT_INFIX);

  public static final JsBinaryOperator EQ = create("==", 9, LEFT_INFIX);
  public static final JsBinaryOperator NEQ = create("!=", 9, LEFT_INFIX);
  public static final JsBinaryOperator REF_EQ = create("===", 9, LEFT_INFIX);
  public static final JsBinaryOperator REF_NEQ = create("!==", 9, LEFT_INFIX);

  public static final JsBinaryOperator BIT_AND = create("&", 8, LEFT_INFIX);

  public static final JsBinaryOperator BIT_XOR = create("^", 7, LEFT_INFIX);

  public static final JsBinaryOperator BIT_OR = create("|", 6, LEFT_INFIX);

  public static final JsBinaryOperator AND = create("&&", 5, LEFT_INFIX);

  public static final JsBinaryOperator OR = create("||", 4, LEFT_INFIX);

  // Precendence 3 is for the condition operator.

  // These assignment operators are right-associatve.
  public static final JsBinaryOperator ASG = create("=", 2, INFIX);
  public static final JsBinaryOperator ASG_ADD = create("+=", 2, INFIX);
  public static final JsBinaryOperator ASG_SUB = create("-=", 2, INFIX);
  public static final JsBinaryOperator ASG_MUL = create("*=", 2, INFIX);
  public static final JsBinaryOperator ASG_DIV = create("/=", 2, INFIX);
  public static final JsBinaryOperator ASG_MOD = create("%=", 2, INFIX);
  public static final JsBinaryOperator ASG_SHL = create("<<=", 2, INFIX);
  public static final JsBinaryOperator ASG_SHR = create(">>=", 2, INFIX);
  public static final JsBinaryOperator ASG_SHRU = create(">>>=", 2, INFIX);
  public static final JsBinaryOperator ASG_BIT_AND = create("&=", 2, INFIX);
  public static final JsBinaryOperator ASG_BIT_OR = create("|=", 2, INFIX);
  public static final JsBinaryOperator ASG_BIT_XOR = create("^=", 2, INFIX);

  public static final JsBinaryOperator COMMA = create(",", 1, LEFT_INFIX);
  
  private static JsBinaryOperator create(String symbol, int precedence, int mask) {
    JsBinaryOperator op = new JsBinaryOperator(symbol, precedence, mask);
    return op;
  }
  private JsBinaryOperator(String symbol, int precedence, int mask) {
    super(symbol, precedence, mask);
  }
}
