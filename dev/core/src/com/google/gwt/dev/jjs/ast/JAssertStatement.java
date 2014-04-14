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

/**
 * Java assert statement.
 */
public class JAssertStatement extends JStatement {

  private JExpression arg;
  private JExpression testExpr;

  public JAssertStatement(SourceInfo info, JExpression testExpr, JExpression arg) {
    super(info);
    this.testExpr = testExpr;
    this.arg = arg;
  }

  public JExpression getArg() {
    return arg;
  }

  public JExpression getTestExpr() {
    return testExpr;
  }

  @Override
  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
      testExpr = visitor.accept(testExpr);
      if (arg != null) {
        arg = visitor.accept(arg);
      }
    }
    visitor.endVisit(this, ctx);
  }
}
