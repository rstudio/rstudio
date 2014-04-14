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
package com.google.gwt.dev.jjs.ast.js;

import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * A JSON-style list of JS expressions.
 */
public class JsonArray extends JExpression {

  private final List<JExpression> exprs = new ArrayList<JExpression>();

  private JClassType jsoType;

  public JsonArray(SourceInfo sourceInfo, JClassType jsoType) {
    super(sourceInfo);
    this.jsoType = jsoType;
  }

  public List<JExpression> getExprs() {
    return exprs;
  }

  @Override
  public JClassType getType() {
    return jsoType;
  }

  @Override
  public boolean hasSideEffects() {
    for (int i = 0, c = getExprs().size(); i < c; ++i) {
      if (exprs.get(i).hasSideEffects()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Resolve an external references during AST stitching.
   */
  public void resolve(JClassType jsoType) {
    assert jsoType.replaces(this.jsoType);
    this.jsoType = jsoType;
  }

  @Override
  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
      visitor.accept(exprs);
    }
    visitor.endVisit(this, ctx);
  }

}
