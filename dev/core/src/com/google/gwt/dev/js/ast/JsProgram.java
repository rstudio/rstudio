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

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * A JavaScript program.
 */
public final class JsProgram extends JsNode implements HasScope {

  /**
   * Constructs a JavaScript program object.
   */
  public JsProgram() {
    rootScope = new JsRootScope(this);
    rootScope.setDescription("Global");
    globalBlock = new JsGlobalBlock();
    JsName debugger = rootScope.createSpecialUnobfuscatableName("debugger");
    debuggerStmt = new JsExprStmt(debugger.makeRef());
  }

  /**
   * Gets the {@link JsStatement} to use whenever parsed source include a
   * <code>debugger</code> statement.
   * 
   * @see #setDebuggerStmt(JsStatement)
   */
  public JsStatement getDebuggerStmt() {
    return debuggerStmt;
  }

  /**
   * Specifies a {@link JsStatement} to use whenever parsed source includes a
   * <code>debugger</code> statement.
   * 
   * @see #getDebuggerStmt()
   */
  public void setDebuggerStmt(JsStatement debuggerStmt) {
    this.debuggerStmt = debuggerStmt;
  }

  public JsDecimalLiteral getDecimalLiteral(String value) {
    JsDecimalLiteral lit = (JsDecimalLiteral) decimalLiteralMap.get(value);
    if (lit == null) {
      lit = new JsDecimalLiteral(value);
      decimalLiteralMap.put(value, lit);
    }
    return lit;
  }

  public JsEmpty getEmptyStmt() {
    return emptyStmt;
  }

  public JsBooleanLiteral getFalseLiteral() {
    return falseLiteral;
  }

  /**
   * Gets the one and only global block.
   */
  public JsBlock getGlobalBlock() {
    return globalBlock;
  }

  public JsIntegralLiteral getIntegralLiteral(BigInteger value) {
    JsIntegralLiteral lit = (JsIntegralLiteral) integralLiteralMap.get(value);
    if (lit == null) {
      lit = new JsIntegralLiteral(value);
      integralLiteralMap.put(value, lit);
    }
    return lit;
  }

  public JsNullLiteral getNullLiteral() {
    return nullLiteral;
  }

  /**
   * Gets the one and only root scope.
   */
  public JsScope getScope() {
    return rootScope;
  }

  public JsStringLiteral getStringLiteral(String value) {
    JsStringLiteral lit = (JsStringLiteral) stringLiteralMap.get(value);
    if (lit == null) {
      lit = new JsStringLiteral(value);
      stringLiteralMap.put(value, lit);
    }
    return lit;
  }

  public JsBooleanLiteral getTrueLiteral() {
    return trueLiteral;
  }

  public void traverse(JsVisitor v) {
    if (v.visit(this)) {
      globalBlock.traverse(v);
    }
    v.endVisit(this);
  }

  private final Map decimalLiteralMap = new HashMap();
  private final JsEmpty emptyStmt = new JsEmpty();
  private final JsBooleanLiteral falseLiteral = new JsBooleanLiteral(false);
  private final JsGlobalBlock globalBlock;
  private final Map integralLiteralMap = new HashMap();
  private final JsNullLiteral nullLiteral = new JsNullLiteral();
  private final JsRootScope rootScope;
  private final Map stringLiteralMap = new HashMap();
  private final JsBooleanLiteral trueLiteral = new JsBooleanLiteral(true);
  private JsStatement debuggerStmt;
}
