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
package com.google.gwt.dev.jjs.ast;

/**
 * Java if statement.
 */
public class JIfStatement extends JStatement {

  private JStatement elseStmt;
  private JExpression ifExpr;
  private JStatement thenStmt;

  public JIfStatement(JProgram program, JSourceInfo info,
      JExpression ifExpr, JStatement thenStmt, JStatement elseStmt) {
    super(program, info);
    this.ifExpr = ifExpr;
    this.thenStmt = thenStmt;
    this.elseStmt = elseStmt;
  }

  public JStatement getElseStmt() {
    return elseStmt;
  }

  public JExpression getIfExpr() {
    return ifExpr;
  }

  public JStatement getThenStmt() {
    return thenStmt;
  }

  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
      ifExpr = visitor.accept(ifExpr);
      if (thenStmt != null) {
        thenStmt = visitor.accept(thenStmt);
      }
      if (elseStmt != null) {
        elseStmt = visitor.accept(elseStmt);
      }
    }
    visitor.endVisit(this, ctx);
  }

}
