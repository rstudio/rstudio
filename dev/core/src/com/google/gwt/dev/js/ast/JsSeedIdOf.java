/*
 * Copyright 2011 Google Inc.
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
 * An AST node whose evaluation results in the numeric seed id of its type.
 */
public class JsSeedIdOf extends JsNameOf {

  private final int seedId;

  public JsSeedIdOf(SourceInfo info, JsName name, int seedId) {
    super(info, name);
    this.seedId = seedId;
  }

  @Override
  public NodeKind getKind() {
    return NodeKind.SEED_ID_OF;
  }

  public int getSeedId() {
    return seedId;
  }

  @Override
  public void traverse(JsVisitor visitor, JsContext ctx) {
    if (visitor.visit(this, ctx)) {
    }
    visitor.endVisit(this, ctx);
  }
}
