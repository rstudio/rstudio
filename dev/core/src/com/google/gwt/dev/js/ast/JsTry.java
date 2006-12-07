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
 * A JavaScript <code>try</code> statement.
 */
public class JsTry extends JsStatement {

  private final JsCatches catches = new JsCatches();

  private JsBlock finallyBlock;

  private JsBlock tryBlock;

  public JsTry() {
  }

  public JsCatches getCatches() {
    return catches;
  }

  public JsBlock getFinallyBlock() {
    return finallyBlock;
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

  public void traverse(JsVisitor v) {
    if (v.visit(this)) {
      tryBlock.traverse(v);
      catches.traverse(v);
      if (finallyBlock != null) {
        finallyBlock.traverse(v);
      }
    }
    v.endVisit(this);
  }
}
