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
 * Conditional expression.
 */
public class JConditional extends JExpression {

  public final Holder thenExpr = new Holder();
  public final Holder elseExpr = new Holder();
  private final JType type;
  private final Holder ifTest = new Holder();

  public JConditional(JProgram program, JType type, JExpression ifTest,
      JExpression thenExpr, JExpression elseExpr) {
    super(program);
    this.type = type;
    this.ifTest.set(ifTest);
    this.thenExpr.set(thenExpr);
    this.elseExpr.set(elseExpr);
  }

  public JExpression getElseExpr() {
    return elseExpr.get();
  }

  public JExpression getIfTest() {
    return ifTest.get();
  }

  public JExpression getThenExpr() {
    return thenExpr.get();
  }

  public JType getType() {
    // TODO(later): allow multiple types for Type Flow?
    if (type instanceof JReferenceType) {
      return program.generalizeTypes(
          (JReferenceType) thenExpr.get().getType(),
          (JReferenceType) elseExpr.get().getType());
    } else {
      return type;
    }
  }

  public boolean hasSideEffects() {
    return ifTest.get().hasSideEffects() || thenExpr.get().hasSideEffects()
        || elseExpr.get().hasSideEffects();
  }

  public void traverse(JVisitor visitor) {
    traverse(visitor, null);
  }

  public void traverse(JVisitor visitor, Mutator mutator) {
    if (visitor.visit(this, mutator)) {
      ifTest.traverse(visitor);
      thenExpr.traverse(visitor);
      elseExpr.traverse(visitor);
    }
    visitor.endVisit(this, mutator);
  }

}
