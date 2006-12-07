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
 * New array experssion.
 */
public class JNewArray extends JExpression implements HasSettableType {

  public HolderList dims = null;
  public HolderList initializers = null;
  private JArrayType arrayType;

  public JNewArray(JProgram program, JArrayType arrayType) {
    super(program);
    this.arrayType = arrayType;
  }

  public JArrayType getArrayType() {
    return arrayType;
  }

  public JType getType() {
    return arrayType;
  }

  public boolean hasSideEffects() {
    return true;
  }

  public void setType(JType arrayType) {
    this.arrayType = (JArrayType) arrayType;
  }

  public void traverse(JVisitor visitor) {
    traverse(visitor, null);
  }

  public void traverse(JVisitor visitor, Mutator mutator) {
    if (visitor.visit(this, mutator)) {
      assert ((dims != null) ^ (initializers != null));

      if (dims != null) {
        dims.traverse(visitor);
      }

      if (initializers != null) {
        initializers.traverse(visitor);
      }
    }
    visitor.endVisit(this, mutator);
  }
}
