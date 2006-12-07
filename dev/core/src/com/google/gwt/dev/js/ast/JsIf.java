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
 * Reprents a JavaScript if statement.
 */
public final class JsIf extends JsStatement {

  private JsExpression ifExpr;

  private JsStatement thenStmt;

  private JsStatement elseStmt;

  public JsIf() {
  }

  public JsStatement getElseStmt() {
    return elseStmt;
  }

  public JsExpression getIfExpr() {
    return ifExpr;
  }

  public JsStatement getThenStmt() {
    return thenStmt;
  }

  public void setElseStmt(JsStatement elseStmt) {
    this.elseStmt = elseStmt;
  }

  public void setIfExpr(JsExpression ifExpr) {
    this.ifExpr = ifExpr;
  }

  public void setThenStmt(JsStatement thenStmt) {
    this.thenStmt = thenStmt;
  }

  public void traverse(JsVisitor v) {
    if (v.visit(this)) {
      ifExpr.traverse(v);
      thenStmt.traverse(v);
      if (elseStmt != null) {
        elseStmt.traverse(v);
      }
    }
    v.endVisit(this);
  }
}
