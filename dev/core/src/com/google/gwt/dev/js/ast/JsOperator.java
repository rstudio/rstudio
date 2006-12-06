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
 * A JavaScript operator.
 */
public class JsOperator {

  protected static final int LEFT = 0x01;
  protected static final int INFIX = 0x02;
  protected static final int POSTFIX = 0x04;
  protected static final int PREFIX = 0x08;

  protected JsOperator(String symbol, int precedence, int mask) {
    this.symbol = symbol;
    this.precedence = precedence;
    this.mask = mask;
  }

  public int getPrecedence() {
    return precedence;
  }

  public String getSymbol() {
    return symbol;
  }

  public boolean isPrecedenceLessThan(JsOperator other) {
    return precedence < other.precedence;
  }

  public boolean isLeftAssociative() {
    return (mask & LEFT) != 0;
  }

  public boolean isValidPostfix() {
    return (mask & POSTFIX) != 0;
  }

  public boolean isValidPrefix() {
    return (mask & PREFIX) != 0;
  }

  public boolean isValidInfix() {
    return (mask & INFIX) != 0;
  }

  public String toString() {
    return symbol;
  }
  
  private final int mask;
  private final int precedence;
  private final String symbol;
}
