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

import java.util.ArrayList;
import java.util.List;

/**
 * A JavaScript <code>try</code> statement.
 */
public class JsTry extends JsStatement {

  private final List<JsCatch> catches = new ArrayList<JsCatch>();

  private JsBlock finallyBlock;

  private JsBlock tryBlock;

  public JsTry(SourceInfo sourceInfo) {
    super(sourceInfo);
  }

  public List<JsCatch> getCatches() {
    return catches;
  }

  public JsBlock getFinallyBlock() {
    return finallyBlock;
  }

  @Override
  public NodeKind getKind() {
    return NodeKind.TRY;
  }

  public JsBlock getTryBlock() {
    return tryBlock;
  }

  public void setFinallyBlock(JsBlock block) {
    this.finallyBlock = block;
  }

  public void setTryBlock(JsBlock block) {
    tryBlock = block;
  }

  @Override
  public void traverse(JsVisitor v, JsContext ctx) {
    if (v.visit(this, ctx)) {
      tryBlock = v.accept(tryBlock);
      v.acceptWithInsertRemove(catches);
      if (finallyBlock != null) {
        finallyBlock = v.accept(finallyBlock);
      }
    }
    v.endVisit(this, ctx);
  }
}
