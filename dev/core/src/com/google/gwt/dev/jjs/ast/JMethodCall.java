/*
 * Copyright 2007 Google Inc.
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

import java.util.ArrayList;

/**
 * Java method call expression.
 */
public class JMethodCall extends JExpression {

  private ArrayList args = new ArrayList();
  private JExpression instance;
  private final JMethod method;
  private final JType overrideReturnType;
  private boolean staticDispatchOnly = false;

  public JMethodCall(JProgram program, SourceInfo info, JExpression instance,
      JMethod method) {
    super(program, info);
    assert (instance != null || method.isStatic());
    this.instance = instance;
    this.method = method;
    this.staticDispatchOnly = false;
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
  public JMethodCall(JProgram program, SourceInfo info, JExpression instance,
      JMethod method, JType overrideReturnType) {
    super(program, info);
    this.instance = instance;
    this.method = method;
    assert (overrideReturnType != null);
    this.overrideReturnType = overrideReturnType;
  }

  public JMethodCall(JProgram program, SourceInfo info, JExpression instance,
      JMethod method, boolean staticDispatchOnly) {
    super(program, info);
    this.instance = instance;
    this.method = method;
    this.staticDispatchOnly = staticDispatchOnly;
    this.overrideReturnType = null;
  }

  public boolean canBePolymorphic() {
    return !staticDispatchOnly && !method.isFinal() && !method.isStatic();
  }

  public ArrayList getArgs() {
    return args;
  }

  public JExpression getInstance() {
    return instance;
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

  public boolean isStaticDispatchOnly() {
    return staticDispatchOnly;
  }

  public void setStaticDispatchOnly() {
    this.staticDispatchOnly = true;
  }

  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
      if (instance != null) {
        instance = visitor.accept(instance);
      }
      visitor.accept(args);
    }
    visitor.endVisit(this, ctx);
  }

}
