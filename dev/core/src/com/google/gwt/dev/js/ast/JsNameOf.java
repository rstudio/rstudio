/*
 * Copyright 2009 Google Inc.
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
 * An AST node whose evaluation results in the string name of its node.
 */
public class JsNameOf extends JsExpression {
  private final JsName name;

  public JsNameOf(SourceInfo info, HasName node) {
    this(info, node.getName());
  }

  public JsNameOf(SourceInfo info, JsName name) {
    super(info);
    this.name = name;
  }

  @Override
  public NodeKind getKind() {
    return NodeKind.NAME_OF;
  }

  public JsName getName() {
    return name;
  }

  @Override
  public boolean hasSideEffects() {
    return false;
  }

  @Override
  public boolean isDefinitelyNotNull() {
    // GenerateJsAST would have already replaced unnamed references with null
    return true;
  }

  @Override
  public boolean isDefinitelyNull() {
    return false;
  }

  @Override
  public void traverse(JsVisitor visitor, JsContext ctx) {
    if (visitor.visit(this, ctx)) {
    }
    visitor.endVisit(this, ctx);
  }
}
