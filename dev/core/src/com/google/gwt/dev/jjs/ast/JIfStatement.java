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

  private final Holder ifExpr = new Holder();
  public JStatement thenStmt;
  public JStatement elseStmt;

  public JIfStatement(JProgram program, JExpression ifExpr,
      JStatement thenStmt, JStatement elseStmt) {
    super(program);
    this.ifExpr.set(ifExpr);
    this.thenStmt = thenStmt;
    this.elseStmt = elseStmt;
  }

  public JExpression getIfExpr() {
    return ifExpr.get();
  }

  public void traverse(JVisitor visitor) {
    if (visitor.visit(this)) {
      ifExpr.traverse(visitor);
      if (thenStmt != null) {
        thenStmt.traverse(visitor);
      }
      
      if (elseStmt != null) {
        elseStmt.traverse(visitor);
      }
    }
    visitor.endVisit(this);
  }

}
