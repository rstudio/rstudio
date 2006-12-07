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
 * Java method call expression.
 */
public class JMethodCall extends JExpression {

  public final Holder instance = new Holder();
  public HolderList args = new HolderList();
  private final JMethod method;
  private final JType overrideReturnType;
  private boolean canBePolymorphic;

  public JMethodCall(JProgram program, JExpression instance, JMethod method) {
    super(program);
    this.instance.set(instance);
    this.method = method;
    this.canBePolymorphic = false;
    this.overrideReturnType = null;
  }

  /**
   * Create a method call whose type is overriden to the specified type,
   * ignoring the return type of the target method. This constructor is used
   * during normalizing transformations to preserve type semantics when calling
   * externally-defined compiler implementation methods.
   * 
   * For example, Cast.dynamicCast() returns Object but that method is used to
   * implement the cast operation. Using a stronger type on the call expression
   * allows us to preserve type information during the latter phases of
   * compilation.
   */
  public JMethodCall(JProgram program, JExpression instance, JMethod method,
      JType overrideReturnType) {
    super(program);
    this.instance.set(instance);
    this.method = method;
    this.canBePolymorphic = false;
    assert (overrideReturnType != null);
    this.overrideReturnType = overrideReturnType;
  }

  public boolean canBePolymorphic() {
    return canBePolymorphic && !method.isFinal() && !method.isStatic();
  }

  public JExpression getInstance() {
    return instance.get();
  }

  public JMethod getTarget() {
    return method;
  }

  public JType getType() {
    if (overrideReturnType != null) {
      return overrideReturnType;
    } else {
      return method.getType();
    }
  }

  public boolean hasSideEffects() {
    // TODO(later): optimize? Be sure to check for clinit when we do.
    return true;
  }

  public void setCanBePolymorphic(boolean canBePolymorphic) {
    this.canBePolymorphic = canBePolymorphic;
  }

  public void traverse(JVisitor visitor) {
    traverse(visitor, null);
  }

  public void traverse(JVisitor visitor, Mutator mutator) {
    if (visitor.visit(this, mutator)) {
      instance.traverse(visitor);
      args.traverse(visitor);
    }
    visitor.endVisit(this, mutator);
  }

}
