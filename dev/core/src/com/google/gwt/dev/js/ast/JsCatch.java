/*
 * Copyright 2008 Google Inc.
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

import com.google.gwt.dev.jjs.SourceInfo;

/**
 * Represents a JavaScript catch clause.
 */
public class JsCatch extends JsNode<JsCatch> implements HasCondition {

  protected final JsCatchScope scope;

  private JsBlock body;

  private JsExpression condition;

  private final JsParameter param;

  public JsCatch(SourceInfo sourceInfo, JsScope parent, String ident) {
    super(sourceInfo);
    assert (parent != null);
    scope = new JsCatchScope(parent, ident);
    param = new JsParameter(sourceInfo, scope.findExistingName(ident));
  }

  public JsBlock getBody() {
    return body;
  }

  public JsExpression getCondition() {
    return condition;
  }

  public JsParameter getParameter() {
    return param;
  }

  public JsScope getScope() {
    return scope;
  }

  public void setBody(JsBlock body) {
    this.body = body;
  }

  public void setCondition(JsExpression condition) {
    this.condition = condition;
  }

  public void traverse(JsVisitor v, JsContext<JsCatch> ctx) {
    if (v.visit(this, ctx)) {
      if (condition != null) {
        condition = v.accept(condition);
      }
      body = v.accept(body);
    }
    v.endVisit(this, ctx);
  }
}
