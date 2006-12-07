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
 * Java instance of expression. 
 */
public class JInstanceOf extends JExpression {

  public final JReferenceType testType;
  public final Holder expr = new Holder();

  public JInstanceOf(JProgram program, JReferenceType testType,
      JExpression expression) {
    super(program);
    this.testType = testType;
    this.expr.set(expression);
  }

  public JExpression getExpression() {
    return expr.get();
  }

  public JType getType() {
    return program.getTypePrimitiveBoolean();
  }

  public boolean hasSideEffects() {
    return false;
  }

  public void traverse(JVisitor visitor) {
    traverse(visitor, null);
  }

  public void traverse(JVisitor visitor, Mutator mutator) {
    if (visitor.visit(this, mutator)) {
      expr.traverse(visitor);
    }
    visitor.endVisit(this, mutator);
  }

}
