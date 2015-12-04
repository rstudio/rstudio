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
package com.google.gwt.dev.jjs.ast;

import com.google.gwt.dev.jjs.SourceInfo;

/**
 * Java local variable definition.
 */
public class JLocal extends JVariable {

  JLocal(SourceInfo info, String name, JType type, boolean isFinal) {
    super(info, name, type, isFinal);
  }

  @Override
  public JLocalRef makeRef(SourceInfo info) {
    return new JLocalRef(info, this);
  }

  @Override
  public void setInitializer(JDeclarationStatement declStmt) {
    this.declStmt = declStmt;
  }

  @Override
  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
      // Do not visit declStmt, it gets visited within its own code block.
    }
    visitor.endVisit(this, ctx);
  }
}
