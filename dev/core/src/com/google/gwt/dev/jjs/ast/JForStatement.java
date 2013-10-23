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
import com.google.gwt.thirdparty.guava.common.collect.Lists;

import java.util.List;

/**
 * AST node representing a Java {@code for} statement.
 */
public class JForStatement extends JStatement {

  private JStatement body;
  private List<JStatement> initializers;
  private JExpression condition;
  private JExpression increments;

  /**
   * Creates an AST node that represents a Java for statement. {@code condition} and
   * {@code increments} can be null to denote an empty component.
   */
  public JForStatement(SourceInfo info, List<JStatement> initializers, JExpression condition,
      JExpression increments, JStatement body) {
    super(info);
    this.initializers = Lists.newArrayList(initializers);
    this.condition = condition;
    this.increments = increments;
    this.body = body;
  }

  /**
   * Returns the {@code for} statement body.
   */
  public JStatement getBody() {
    return body;
  }

  /**
   * Returns the increments (3rd component) expression.
   */
  public JExpression getIncrements() {
    return increments;
  }

  /**
   * Returns the initializer (1st component) statements.
   */
  public List<JStatement> getInitializers() {
    return initializers;
  }

  /**
   * Returns the condition (2nd component) expression.
   */
  public JExpression getCondition() {
    return condition;
  }

  @Override
  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
      initializers = visitor.acceptWithInsertRemoveImmutable(initializers);
      if (condition != null) {
        condition = visitor.accept(condition);
      }
      if (increments != null) {
        increments = visitor.accept(increments);
      }
      if (body != null) {
        body = visitor.accept(body);
      }
    }
    visitor.endVisit(this, ctx);
  }
}
