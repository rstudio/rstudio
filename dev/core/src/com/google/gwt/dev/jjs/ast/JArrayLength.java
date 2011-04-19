/*
 * Copyright 2010 Google Inc.
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
 * Java array length expression.
 */
public class JArrayLength extends JExpression {

  private JExpression instance;

  public JArrayLength(SourceInfo info, JExpression instance) {
    super(info);
    assert (instance != null);
    this.instance = instance;
  }

  public JExpression getInstance() {
    return instance;
  }

  public JType getType() {
    return JPrimitiveType.INT;
  }

  @Override
  public boolean hasSideEffects() {
    return instance.hasSideEffects();
  }

  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
      instance = visitor.accept(instance);
    }
    visitor.endVisit(this, ctx);
  }
}
