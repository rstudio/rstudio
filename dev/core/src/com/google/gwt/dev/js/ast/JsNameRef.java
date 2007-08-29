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
package com.google.gwt.dev.js.ast;

/**
 * Represents a JavaScript expression that references a name.
 */
public final class JsNameRef extends JsExpression /*implements HasName*/ {

  private String ident;
  private JsName name;
  private JsExpression qualifier;

  public JsNameRef(JsName name) {
    this.name = name;
  }

  public JsNameRef(String ident) {
    this.ident = ident;
  }

  public String getIdent() {
    return (name == null) ? ident : name.getIdent();
  }

  public JsExpression getQualifier() {
    return qualifier;
  }

  public String getShortIdent() {
    return (name == null) ? ident : name.getShortIdent();
  }

  @Override
  public boolean isLeaf() {
    if (qualifier == null) {
      return true;
    } else {
      return false;
    }
  }

  public boolean isResolved() {
    return name != null;
  }

  public void resolve(JsName name) {
    this.name = name;
    this.ident = null;
  }

  public void setQualifier(JsExpression qualifier) {
    this.qualifier = qualifier;
  }

  public void traverse(JsVisitor v, JsContext<JsExpression> ctx) {
    if (v.visit(this, ctx)) {
      if (qualifier != null) {
        qualifier = v.accept(qualifier);
      }
    }
    v.endVisit(this, ctx);
  }
}
