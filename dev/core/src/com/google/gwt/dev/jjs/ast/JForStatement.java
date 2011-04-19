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
package com.google.gwt.dev.jjs.ast;

import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.util.collect.Lists;

import java.util.List;

/**
 * Java for statement.
 */
public class JForStatement extends JStatement {

  private JStatement body;
  private List<JExpressionStatement> increments;
  private List<JStatement> initializers;
  private JExpression testExpr;

  public JForStatement(SourceInfo info, List<JStatement> initializers, JExpression testExpr,
      List<JExpressionStatement> increments, JStatement body) {
    super(info);
    this.initializers = Lists.normalize(initializers);
    this.testExpr = testExpr;
    this.increments = Lists.normalize(increments);
    this.body = body;
  }

  public JStatement getBody() {
    return body;
  }

  public List<JExpressionStatement> getIncrements() {
    return increments;
  }

  public List<JStatement> getInitializers() {
    return initializers;
  }

  public JExpression getTestExpr() {
    return testExpr;
  }

  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
      initializers = visitor.acceptWithInsertRemoveImmutable(initializers);
      if (testExpr != null) {
        testExpr = visitor.accept(testExpr);
      }
      increments = visitor.acceptWithInsertRemoveImmutable(increments);
      if (body != null) {
        body = visitor.accept(body);
      }
    }
    visitor.endVisit(this, ctx);
  }

}
