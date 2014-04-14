/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.dev.jjs.ast;

/**
 * For precedence indices, see the Java Programming Language, 4th Edition, p.
 * 750, Table 2. I just numbered the table top to bottom as 0 through 14. Lower
 * number means higher precedence.
 */
public enum JBinaryOperator {

  // Don't renumber precs without checking implementation of isShiftOperator()

  MUL("*", 3), DIV("/", 3), MOD("%", 3), ADD("+", 4), CONCAT("+", 4), SUB("-", 4), SHL("<<", 5), SHR(
      ">>", 5), SHRU(">>>", 5), LT("<", 6), LTE("<=", 6), GT(">", 6), GTE(">=",
      6), EQ("==", 7), NEQ("!=", 7), BIT_AND("&", 8), BIT_XOR("^", 9), BIT_OR(
      "|", 10), AND("&&", 11), OR("||", 12), ASG("=", 14), ASG_ADD("+=", 14, ADD), ASG_CONCAT("+=", 14, CONCAT), ASG_SUB("-=", 14, SUB), ASG_MUL("*=", 14, MUL), ASG_DIV("/=", 14,
      DIV), ASG_MOD("%=", 14, MOD), ASG_SHL("<<=", 14, SHL), ASG_SHR(">>=", 14,
      SHR), ASG_SHRU(">>>=", 14, SHRU), ASG_BIT_AND("&=", 14, BIT_AND), ASG_BIT_OR(
      "|=", 14, BIT_OR), ASG_BIT_XOR("^=", 14, BIT_XOR);

  private final JBinaryOperator nonAsg;
  private final int precedence;
  private final char[] symbol;

  private JBinaryOperator(String symbol, int precedence) {
    this(symbol, precedence, null);
  }

  private JBinaryOperator(String symbol, int precedence, JBinaryOperator nonAsg) {
    this.symbol = symbol.toCharArray();
    this.precedence = precedence;
    this.nonAsg = nonAsg;
  }

  public JBinaryOperator getNonAssignmentOf() {
    return nonAsg;
  }

  public int getPrecedence() {
    return precedence;
  }

  public char[] getSymbol() {
    return symbol;
  }

  public boolean isAssignment() {
    return (this == ASG) || (getNonAssignmentOf() != null);
  }

  public boolean isShiftOperator() {
    // Fragile implementation.
    return precedence == 5 || (nonAsg != null && nonAsg.precedence == 5);
  }

  @Override
  public String toString() {
    return new String(getSymbol());
  }

}
