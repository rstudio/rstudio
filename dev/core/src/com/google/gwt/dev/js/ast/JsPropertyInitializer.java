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
 * Used in object literals to specify property values by name.
 */
public class JsPropertyInitializer extends JsNode {

  private JsExpression labelExpr;

  private JsExpression valueExpr;

  public JsPropertyInitializer(SourceInfo sourceInfo) {
    super(sourceInfo);
  }

  public JsPropertyInitializer(SourceInfo sourceInfo, JsExpression labelExpr, JsExpression valueExpr) {
    super(sourceInfo);
    this.labelExpr = labelExpr;
    this.valueExpr = valueExpr;
  }

  @Override
  public NodeKind getKind() {
    return NodeKind.PROPERTY_INIT;
  }

  public JsExpression getLabelExpr() {
    return labelExpr;
  }

  public JsExpression getValueExpr() {
    return valueExpr;
  }

  public boolean hasSideEffects() {
    return labelExpr.hasSideEffects() || valueExpr.hasSideEffects();
  }

  public void setLabelExpr(JsExpression labelExpr) {
    this.labelExpr = labelExpr;
  }

  public void setValueExpr(JsExpression valueExpr) {
    this.valueExpr = valueExpr;
  }

  @Override
  public void traverse(JsVisitor v, JsContext ctx) {
    if (v.visit(this, ctx)) {
      labelExpr = v.accept(labelExpr);
      valueExpr = v.accept(valueExpr);
    }
    v.endVisit(this, ctx);
  }
}
