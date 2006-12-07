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
package com.google.gwt.dev.jjs.ast;

/**
 * Java array reference expression.
 */
public class JArrayRef extends JExpression {

  public final Holder instance = new Holder();
  public final Holder indexExpr = new Holder();

  public JArrayRef(JProgram program, JExpression instance, JExpression indexExpr) {
    super(program);
    this.instance.set(instance);
    this.indexExpr.set(indexExpr);
  }

  public JExpression getIndexExpr() {
    return indexExpr.get();
  }

  public JExpression getInstance() {
    return instance.get();
  }

  public JType getType() {
    JType type = instance.get().getType();
    if (type == program.getTypeNull()) {
      return type;
    }
    JArrayType arrayType = (JArrayType) type;
    return arrayType.getElementType();
  }

  public boolean hasSideEffects() {
    return instance.get().hasSideEffects() || indexExpr.get().hasSideEffects();
  }

  public void traverse(JVisitor visitor) {
    traverse(visitor, null);
  }

  public void traverse(JVisitor visitor, Mutator mutator) {
    if (visitor.visit(this, mutator)) {
      this.instance.traverse(visitor);
      this.indexExpr.traverse(visitor);
    }
    visitor.endVisit(this, mutator);
  }

}
