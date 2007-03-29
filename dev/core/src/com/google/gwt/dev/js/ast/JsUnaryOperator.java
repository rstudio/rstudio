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
package com.google.gwt.dev.js.ast;

/**
 * A JavaScript unary operator.
 */
public final class JsUnaryOperator extends JsOperator {
  // Precedence indices from "JavaScript - The Definitive Guide" 4th Edition
  // (page 57)
  //

  public static final JsUnaryOperator BIT_NOT = create("~", 14, PREFIX);
  public static final JsUnaryOperator NEG = create("-", 14, PREFIX);
  public static final JsUnaryOperator NOT = create("!", 14, PREFIX);
  public static final JsUnaryOperator DEC = create("--", 14, POSTFIX | PREFIX);
  public static final JsUnaryOperator INC = create("++", 14, POSTFIX | PREFIX);
  public static final JsUnaryOperator DELETE = create("delete", 14, PREFIX);
  public static final JsUnaryOperator TYPEOF = create("typeof", 14, PREFIX);
  public static final JsUnaryOperator VOID = create("void", 14, PREFIX);

  private static JsUnaryOperator create(String symbol, int precedence, int mask) {
    JsUnaryOperator op = new JsUnaryOperator(symbol, precedence, mask);
    return op;
  }

  private JsUnaryOperator(String symbol, int precedence, int mask) {
    super(symbol, precedence, mask);
  }

  public boolean isKeyword() {
    return this == DELETE || this == TYPEOF || this == VOID;
  }

  public boolean isModifying() {
    return this == DEC || this == INC || this == DELETE;
  }
}
