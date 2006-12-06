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
 * A <code>for</code> statement. If specified at all, the initializer part is
 * either a declaration of one or more variables, in which case
 * {@link #getInitVars()} is used, or an expression, in which case
 * {@link #getInitExpr()} is used. In the latter case, the comma operator is
 * often used to create a compound expression.
 * 
 * <p>
 * Note that any of the parts of the <code>for</code> loop header can be
 * <code>null</code>, although the body will never be null.
 */
public class JsFor extends JsStatement {

  public JsFor() {
  }

  public JsStatement getBody() {
    return body;
  }

  public JsExpression getCondition() {
    return condition;
  }

  public JsExpression getIncrExpr() {
    return incrExpr;
  }

  public JsExpression getInitExpr() {
    return initExpr;
  }

  public void setBody(JsStatement body) {
    this.body = body;
  }

  public void setCondition(JsExpression condition) {
    this.condition = condition;
  }

  public void setIncrExpr(JsExpression incrExpr) {
    this.incrExpr = incrExpr;
  }

  public void setInitExpr(JsExpression initExpr) {
    this.initExpr = initExpr;
  }

  public void setInitVars(JsVars initVars) {
    this.initVars = initVars;
  }

  public JsVars getInitVars() {
    return initVars;
  }

  public void traverse(JsVisitor v) {
    if (v.visit(this)) {
      assert (!(initExpr != null && initVars != null));

      if (initExpr != null) {
        initExpr.traverse(v);
      } else if (initVars != null) {
        initVars.traverse(v);
      }

      if (condition != null) {
        condition.traverse(v);
      }

      if (incrExpr != null) {
        incrExpr.traverse(v);
      }
      body.traverse(v);
    }
    v.endVisit(this);
  }

  private JsStatement body;
  private JsExpression condition;
  private JsExpression incrExpr;
  private JsExpression initExpr;
  private JsVars initVars;
}
