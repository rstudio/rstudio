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
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.thirdparty.guava.common.collect.Lists;

import java.util.Arrays;
import java.util.List;

/**
 * A JSON-style list of JS expressions.
 */
public class JsonArray extends JExpression {

  private final List<JExpression> expressions = Lists.newArrayList();

  // JsonArray objects are typed as either JavaScriptObject, Object[], or native JsType[] depending
  // on the use.
  private JType arrayType;

  public JsonArray(SourceInfo sourceInfo, JType arrayType, List<JExpression> expressions) {
    super(sourceInfo);
    this.arrayType = arrayType;
    this.expressions.addAll(expressions);
  }

  public JsonArray(SourceInfo sourceInfo, JType arrayType, JExpression... expressions) {
    this(sourceInfo, arrayType, Arrays.asList(expressions));
  }

  public List<JExpression> getExpressions() {
    return expressions;
  }

  @Override
  public JType getType() {
    return arrayType;
  }

  @Override
  public boolean hasSideEffects() {
    for (JExpression expression : expressions) {
      if (expression.hasSideEffects()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Resolve an external references during AST stitching.
   */
  public void resolve(JType arrayType) {
    assert arrayType.replaces(this.arrayType);
    this.arrayType = arrayType;
  }

  @Override
  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
      visitor.accept(expressions);
    }
    visitor.endVisit(this, ctx);
  }

}
