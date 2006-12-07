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
 * Java do statement.
 */
public class JDoStatement extends JStatement {

  private final Holder testExpr = new Holder();
  public JStatement body;

  public JDoStatement(JProgram program, JExpression testExpr, JStatement body) {
    super(program);
    this.testExpr.set(testExpr);
    this.body = body;
  }

  public JExpression getTestExpr() {
    return testExpr.get();
  }

  public void traverse(JVisitor visitor) {
    if (visitor.visit(this)) {
      testExpr.traverse(visitor);
      if (body != null) {
        body.traverse(visitor);
      }
    }
    visitor.endVisit(this);
  }

}
