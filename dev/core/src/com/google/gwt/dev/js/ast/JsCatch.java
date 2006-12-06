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
 * Represents a JavaScript catch clause.
 */
public class JsCatch extends JsNode implements HasCondition, HasName {

  public JsCatch(JsName name) {
    this.name = name;
  }

  public JsBlock getBody() {
    return body;
  }

  public void setBody(JsBlock body) {
    this.body = body;
  }

  public JsExpression getCondition() {
    return condition;
  }

  public JsName getName() {
    return name;
  }

  public void setCondition(JsExpression condition) {
    this.condition = condition;
  }

  public void traverse(JsVisitor v) {
    if (v.visit(this)) {
      if (condition != null) {
        condition.traverse(v);
      }
      body.traverse(v);
    }
    v.endVisit(this);
  }

  private JsBlock body;
  private JsExpression condition;
  private final JsName name;
}
