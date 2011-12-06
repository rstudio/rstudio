/*
 * Copyright 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.js.ast;

import com.google.gwt.dev.jjs.SourceInfo;

/**
 * Represents a JavaScript do..while statement.
 */
public class JsDoWhile extends JsStatement {

  private JsStatement body;

  private JsExpression condition;

  public JsDoWhile(SourceInfo sourceInfo) {
    super(sourceInfo);
  }

  public JsDoWhile(SourceInfo sourceInfo, JsExpression condition, JsStatement body) {
    super(sourceInfo);
    this.condition = condition;
    this.body = body;
  }

  public JsStatement getBody() {
    return body;
  }

  public JsExpression getCondition() {
    return condition;
  }

  @Override
  public NodeKind getKind() {
    return NodeKind.DO;
  }

  public void setBody(JsStatement body) {
    this.body = body;
  }

  public void setCondition(JsExpression condition) {
    this.condition = condition;
  }

  @Override
  public void traverse(JsVisitor v, JsContext ctx) {
    if (v.visit(this, ctx)) {
      body = v.accept(body);
      condition = v.accept(condition);
    }
    v.endVisit(this, ctx);
  }
}
