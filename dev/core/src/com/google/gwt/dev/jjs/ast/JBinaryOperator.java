// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

/**
 * For precedence indices, see the Java Programming Language, 4th Edition, p.
 * 750, Table 2. I just numbered the table top to bottom as 0 through 14. Lower
 * number means higher precedence.
 */
public class JBinaryOperator {

  public static final JBinaryOperator MUL = new JBinaryOperator("*", 3);
  public static final JBinaryOperator DIV = new JBinaryOperator("/", 3);
  public static final JBinaryOperator MOD = new JBinaryOperator("%", 3);
  public static final JBinaryOperator ADD = new JBinaryOperator("+", 4);
  public static final JBinaryOperator SUB = new JBinaryOperator("-", 4);

  public static final JBinaryOperator SHL = new JBinaryOperator("<<", 5);
  public static final JBinaryOperator SHR = new JBinaryOperator(">>", 5);
  public static final JBinaryOperator SHRU = new JBinaryOperator(">>>", 5);

  public static final JBinaryOperator LT = new JBinaryOperator("<", 6);
  public static final JBinaryOperator LTE = new JBinaryOperator("<=", 6);
  public static final JBinaryOperator GT = new JBinaryOperator(">", 6);
  public static final JBinaryOperator GTE = new JBinaryOperator(">=", 6);

  public static final JBinaryOperator EQ = new JBinaryOperator("==", 7);
  public static final JBinaryOperator NEQ = new JBinaryOperator("!=", 7);

  public static final JBinaryOperator BIT_AND = new JBinaryOperator("&", 8);

  public static final JBinaryOperator BIT_XOR = new JBinaryOperator("^", 9);

  public static final JBinaryOperator BIT_OR = new JBinaryOperator("|", 10);

  public static final JBinaryOperator AND = new JBinaryOperator("&&", 11);

  public static final JBinaryOperator OR = new JBinaryOperator("||", 12);

  // Don't renumber ASG precs without checking implementation of isAssignment()
  public static final JBinaryOperator ASG = new JBinaryOperator("=", 14);
  public static final JBinaryOperator ASG_ADD = new JBinaryOperator("+=", 14);
  public static final JBinaryOperator ASG_SUB = new JBinaryOperator("-=", 14);
  public static final JBinaryOperator ASG_MUL = new JBinaryOperator("*=", 14);
  public static final JBinaryOperator ASG_DIV = new JBinaryOperator("/=", 14);
  public static final JBinaryOperator ASG_MOD = new JBinaryOperator("%=", 14);
  public static final JBinaryOperator ASG_SHL = new JBinaryOperator("<<=", 14);
  public static final JBinaryOperator ASG_SHR = new JBinaryOperator(">>=", 14);
  public static final JBinaryOperator ASG_SHRU = new JBinaryOperator(">>>=", 14);
  public static final JBinaryOperator ASG_BIT_AND = new JBinaryOperator("&=", 14);
  public static final JBinaryOperator ASG_BIT_OR = new JBinaryOperator("|=", 14);
  public static final JBinaryOperator ASG_BIT_XOR = new JBinaryOperator("^=", 14);

  private final char[] fSymbol;
  private final int fPrecedence;

  private JBinaryOperator(String symbol, int precedence) {
    fSymbol = symbol.toCharArray();
    fPrecedence = precedence;
  }

  public char[] getSymbol() {
    return fSymbol;
  }

  public int getPrecedence() {
    return fPrecedence;
  }

  public boolean isAssignment() {
    /*
     * Beware, flaky! Maybe I should have added Yet Another Field to
     * BinaryOperator?
     */
    return (fPrecedence == ASG.getPrecedence());
  }

  public String toString() {
    return new String(getSymbol());
  }

}
