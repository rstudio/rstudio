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
package com.google.gwt.dev.jjs.ast.js;

import com.google.gwt.dev.jjs.ast.HolderList;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.Mutator;

/**
 * A JSON-style list of JS expressions.
 */
public class JsonArray extends JExpression {

  public HolderList exprs = new HolderList();

  public JsonArray(JProgram program) {
    super(program);
  }

  public JType getType() {
    return program.getTypeVoid();
  }

  public boolean hasSideEffects() {
    return true;
  }

  public void traverse(JVisitor visitor) {
    traverse(visitor, null);
  }

  public void traverse(JVisitor visitor, Mutator mutator) {
    if (visitor.visit(this, mutator)) {
      exprs.traverse(visitor);
    }
    visitor.endVisit(this, mutator);
  }

}
