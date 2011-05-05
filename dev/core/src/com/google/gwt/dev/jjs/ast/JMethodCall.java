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
import com.google.gwt.dev.util.collect.Lists;

import java.util.Collections;
import java.util.List;

/**
 * Java method call expression.
 */
public class JMethodCall extends JExpression {

  private List<JExpression> args = Collections.emptyList();
  private boolean cannotBePolymorphic = false;
  private JExpression instance;
  private final JMethod method;
  private JType type;
  private boolean staticDispatchOnly = false;

  /**
   * Initialize a new method call equivalent to another one. A new instance must
   * be specified, and the new object has no arguments on initialization. This
   * forces the caller to potentially deal with cloning objects if needed.
   */
  public JMethodCall(JMethodCall other, JExpression instance) {
    super(other.getSourceInfo());
    this.instance = instance;
    this.cannotBePolymorphic = other.cannotBePolymorphic;
    this.method = other.method;
    this.type = other.type;
    this.staticDispatchOnly = other.staticDispatchOnly;
  }

  public JMethodCall(SourceInfo info, JExpression instance, JMethod method) {
    this(info, instance, method, method.getType());
  }

  /**
   * Create a method call whose type is set to the specified type, rather than
   * the return type of the target method. This constructor is used during
   * normalizing transformations to preserve type semantics when calling
   * externally-defined compiler implementation methods.
   * 
   * For example, Cast.dynamicCast() returns Object but that method is used to
   * implement the cast operation. Using a stronger type on the call expression
   * allows us to preserve type information during the latter phases of
   * compilation.
   */
  public JMethodCall(SourceInfo info, JExpression instance, JMethod method, JType type) {
    super(info);
    assert (method != null);
    assert (instance != null || !method.needsVtable());
    this.instance = instance;
    this.method = method;
    assert (type != null);
    this.type = type;
  }

  /**
   * Inserts an argument at the specified index.
   */
  public void addArg(int index, JExpression toAdd) {
    args = Lists.add(args, index, toAdd);
  }

  /**
   * Adds an argument to this method.
   */
  public void addArg(JExpression toAdd) {
    args = Lists.add(args, toAdd);
  }

  /**
   * Adds an argument to this method.
   */
  public void addArgs(JExpression... toAdd) {
    args = Lists.addAll(args, toAdd);
  }

  /**
   * Adds arguments to this method.
   */
  public void addArgs(List<JExpression> toAdd) {
    args = Lists.addAll(args, toAdd);
  }

  public boolean canBePolymorphic() {
    return !cannotBePolymorphic && !staticDispatchOnly && !method.isFinal()
        && method.canBePolymorphic();
  }

  /**
   * Returns the call arguments.
   */
  public List<JExpression> getArgs() {
    return args;
  }

  public JExpression getInstance() {
    return instance;
  }

  public JMethod getTarget() {
    return method;
  }

  public JType getType() {
    return type;
  }

  @Override
  public boolean hasSideEffects() {
    // TODO(later): optimize? Be sure to check for clinit when we do.
    return true;
  }

  public boolean isStaticDispatchOnly() {
    return staticDispatchOnly;
  }

  /**
   * Removes the argument at the specified index.
   */
  public void removeArg(int index) {
    args = Lists.remove(args, index);
  }

  /**
   * Sets the argument at the specified index.
   */
  public void setArg(int index, JExpression arg) {
    args = Lists.set(args, index, arg);
  }

  public void setCannotBePolymorphic() {
    this.cannotBePolymorphic = true;
  }

  public void setStaticDispatchOnly() {
    this.staticDispatchOnly = true;
  }

  /**
   * Set an explicit result type different from the natural method target.
   */
  public void setType(JType returnType) {
    this.type = returnType;
  }

  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
      visitChildren(visitor);
    }
    visitor.endVisit(this, ctx);
  }

  protected void visitChildren(JVisitor visitor) {
    if (instance != null) {
      instance = visitor.accept(instance);
    }
    args = visitor.acceptImmutable(args);
  }

}
