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

import java.util.List;

/**
 * Java for statement.
 */
public class JForStatement extends JStatement {

  public final JStatement body;
  private final List/* <JStatement> */initializers;
  private final Holder testExpr = new Holder();
  private final List/* <JExpressionStatement> */increments;

  public JForStatement(JProgram program, List/* <JStatement> */initializers,
      JExpression testExpr, List/* <JExpressionStatement> */increments,
      JStatement body) {
    super(program);
    this.initializers = initializers;
    this.testExpr.set(testExpr);
    this.increments = increments;
    this.body = body;
  }

  public List/* <JExpressionStatement> */getIncrements() {
    return increments;
  }

  public List/* <JStatement> */getInitializers() {
    return initializers;
  }

  public JExpression getTestExpr() {
    return testExpr.get();
  }

  public void traverse(JVisitor visitor) {
    if (visitor.visit(this)) {
      for (int i = 0; i < initializers.size(); ++i) {
        JStatement stmt = (JStatement) initializers.get(i);
        stmt.traverse(visitor);
      }
      testExpr.traverse(visitor);
      for (int i = 0; i < increments.size(); ++i) {
        JExpressionStatement stmt = (JExpressionStatement) increments.get(i);
        stmt.traverse(visitor);
      }
      if (body != null) {
        body.traverse(visitor);
      }
    }
    visitor.endVisit(this);
  }

}
