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
package com.google.gwt.dev.jjs.ast.js;

import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JVisitor;

import java.util.ArrayList;

/**
 * Represents multiple ordered expressions as a single compound expression.
 */
public class JMultiExpression extends JExpression {

  public ArrayList<JExpression> exprs = new ArrayList<JExpression>();

  public JMultiExpression(SourceInfo info) {
    super(info);
  }

  public JType getType() {
    int c = exprs.size();
    if (c == 0) {
      return JPrimitiveType.VOID;
    } else {
      return exprs.get(c - 1).getType();
    }
  }

  @Override
  public boolean hasSideEffects() {
    for (int i = 0; i < exprs.size(); ++i) {
      JExpression expr = exprs.get(i);
      if (expr.hasSideEffects()) {
        return true;
      }
    }
    return false;
  }

  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
      visitor.acceptWithInsertRemove(exprs);
    }
    visitor.endVisit(this, ctx);
  }

}
