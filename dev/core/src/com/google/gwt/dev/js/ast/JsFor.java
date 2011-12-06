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
 * A <code>for</code> statement. If specified at all, the initializer part is either a declaration
 * of one or more variables, in which case {@link #getInitVars()} is used, or an expression, in
 * which case {@link #getInitExpr()} is used. In the latter case, the comma operator is often used
 * to create a compound expression.
 * 
 * <p>
 * Note that any of the parts of the <code>for</code> loop header can be <code>null</code>, although
 * the body will never be null.
 */
public class JsFor extends JsStatement {

  private JsStatement body;

  private JsExpression condition;

  private JsExpression incrExpr;

  private JsExpression initExpr;

  private JsVars initVars;

  public JsFor(SourceInfo sourceInfo) {
    super(sourceInfo);
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

  public JsVars getInitVars() {
    return initVars;
  }

  @Override
  public NodeKind getKind() {
    return NodeKind.FOR;
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

  @Override
  public void traverse(JsVisitor v, JsContext ctx) {
    if (v.visit(this, ctx)) {
      assert (!(initExpr != null && initVars != null));

      if (initExpr != null) {
        initExpr = v.accept(initExpr);
      } else if (initVars != null) {
        initVars = v.accept(initVars);
      }

      if (condition != null) {
        condition = v.accept(condition);
      }

      if (incrExpr != null) {
        incrExpr = v.accept(incrExpr);
      }
      body = v.accept(body);
    }
    v.endVisit(this, ctx);
  }
}
