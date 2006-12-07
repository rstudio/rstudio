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
 * Represents a JavaScript function expression.
 */
public final class JsFunction extends JsExpression implements HasName, HasScope {

  private JsBlock body;

  private JsName name;

  private final JsParameters params = new JsParameters();

  private final JsScope scope;

  /**
   * Creates an anonymous function.
   */
  public JsFunction(JsScope parent) {
    this(parent, null);
  }

  /**
   * Creates an named function.
   */
  public JsFunction(JsScope parent, JsName name) {
    assert (parent != null);
    this.scope = new JsScope(parent);
    setName(name);
  }

  public void accept(JsVisitor v) {
    v.visit(this);
  }

  public JsBlock getBody() {
    return body;
  }

  public JsName getName() {
    return name;
  }

  public JsParameters getParameters() {
    return params;
  }

  public JsScope getScope() {
    return scope;
  }

  public void setBody(JsBlock body) {
    this.body = body;
  }

  public void setName(JsName name) {
    this.name = name;
    String desc = "function <anonymous>";
    if (name != null) {
      desc = "function " + name.getIdent();
    }
    scope.setDescription(desc);
  }

  public void traverse(JsVisitor v) {
    if (v.visit(this)) {
      params.traverse(v);
      body.traverse(v);
    }
    v.endVisit(this);
  }
}
