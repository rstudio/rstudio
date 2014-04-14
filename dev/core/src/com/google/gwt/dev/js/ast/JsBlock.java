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
 * Represents a JavaScript block statement.
 */
public class JsBlock extends JsStatement {

  private final List<JsStatement> stmts = new ArrayList<JsStatement>();

  public JsBlock(SourceInfo sourceInfo) {
    super(sourceInfo);
  }

  @Override
  public NodeKind getKind() {
    return NodeKind.BLOCK;
  }

  public List<JsStatement> getStatements() {
    return stmts;
  }

  public boolean isGlobalBlock() {
    return false;
  }

  @Override
  public void traverse(JsVisitor v, JsContext ctx) {
    if (v.visit(this, ctx)) {
      v.acceptWithInsertRemove(stmts);
    }
    v.endVisit(this, ctx);
  }

  @Override
  public boolean unconditionalControlBreak() {
    for (JsStatement stmt : stmts) {
      if (stmt.unconditionalControlBreak()) {
        return true;
      }
    }
    return false;
  }
}
