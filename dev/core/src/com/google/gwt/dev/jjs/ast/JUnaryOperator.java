// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

public class JUnaryOperator {

  public static final JUnaryOperator INC = new JUnaryOperator("++");
  public static final JUnaryOperator DEC = new JUnaryOperator("--");
  public static final JUnaryOperator NEG = new JUnaryOperator("-");
  public static final JUnaryOperator NOT = new JUnaryOperator("!");
  public static final JUnaryOperator BIT_NOT = new JUnaryOperator("~");

  private final char[] fSymbol;

  private JUnaryOperator(String symbol) {
    fSymbol = symbol.toCharArray();
  }

  public char[] getSymbol() {
    return fSymbol;
  }

  public String toString() {
    return new String(getSymbol());
  }

}
