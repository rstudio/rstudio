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
  /**
   * Interesting facts about a method call's polymorphism.
   */
  private static enum Polymorphism {
    /**
     * Optimizers have determined that call target is not overridden.
     */
    CANNOT_BE_POLYMORPHIC,
    /**
     * Normal polymorphic dispatch.
     */
    NORMAL,
    /**
     * An instance call that <i>must</i> be dispatch statically, e.g.
     * super.method() invocations, and super() and this() constructor calls.
     */
    STATIC_DISPATCH_ONLY,
    /**
     * So-named because it's like a 'volatile' field, simply means "do not
     * attempt to optimize this call".
     */
    VOLATILE;

    public boolean canBePolymorphic() {
      return (this != CANNOT_BE_POLYMORPHIC) && (this != STATIC_DISPATCH_ONLY);
    }

    public boolean isStaticDispatchOnly() {
      return this == STATIC_DISPATCH_ONLY;
    }

    public boolean isVolatile() {
      return this == VOLATILE;
    }
  }

  private List<JExpression> args = Collections.emptyList();
  private JExpression instance;
  private JMethod method;
  private JType overriddenReturnType;
  private Polymorphism polymorphism = Polymorphism.NORMAL;
  private boolean markedAsSideAffectFree;

  /**
   * Initialize a new method call equivalent to another one. A new instance must
   * be specified, and the new object has no arguments on initialization. This
   * forces the caller to potentially deal with cloning objects if needed.
   */
  public JMethodCall(JMethodCall other, JExpression instance) {
    super(other.getSourceInfo());
    this.instance = instance;
    this.method = other.method;
    this.overriddenReturnType = other.overriddenReturnType;
    this.polymorphism = other.polymorphism;
    this.markedAsSideAffectFree = other.markedAsSideAffectFree;
  }

  /**
   * Create a method call whose type is overridden to the specified type,
   * ignoring the return type of the target method. This constructor is used
   * during normalizing transformations to preserve type semantics when calling
   * externally-defined compiler implementation methods.
   *
   * For example, Cast.dynamicCast() returns Object but that method is used to
   * implement the cast operation. Using a stronger type on the call expression
   * allows us to preserve type information during the latter phases of
   * compilation.
   */
  public JMethodCall(SourceInfo info, JExpression instance, JMethod method, JExpression... args) {
    super(info);
    assert (method != null);
    assert (instance != null || method.isStatic() || this instanceof JNewInstance);
    this.instance = instance;
    this.method = method;
    this.overriddenReturnType = null;
    addArgs(args);
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

  /**
   * Returns <code>true</code> if the call can dispatch to more than possible
   * target method.
   */
  public boolean canBePolymorphic() {
    return polymorphism.canBePolymorphic() && !method.isFinal() && method.canBePolymorphic();
  }

  /**
   * Creates a new method call to the same method using the same instance but without parameters.
   */
  public JMethodCall cloneWithoutParameters() {
    return new JMethodCall(this, instance);
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

  @Override
  public JType getType() {
    return overriddenReturnType != null ? overriddenReturnType : method.getType();
  }

  public void markSideEffectFree() {
    markedAsSideAffectFree = true;
  }

  @Override
  public boolean hasSideEffects() {
    if (markedAsSideAffectFree) {
      return false;
    }
    // TODO(later): optimize? Be sure to check for clinit when we do.
    return isStaticDispatchOnly() || method.isStatic() ? method.hasSideEffects() : true;
  }

  /**
   * Returns <code>true</code> for calls that <i>must</i> be called statically,
   * e.g. super.method() invocations, and super() and this() constructor calls.
   */
  public boolean isStaticDispatchOnly() {
    return polymorphism.isStaticDispatchOnly();
  }

  /**
   * Returns <code>true</code> for calls that should not be optimized.
   */
  public boolean isVolatile() {
    return polymorphism.isVolatile();
  }

  /**
   * Override the return type.
   */
  public void overrideReturnType(JType overridenReturnType) {
    assert this.overriddenReturnType == null;
    this.overriddenReturnType = overridenReturnType;
  }
  /**
   * Resolve an external reference during AST stitching.
   */
  public void resolve(JMethod newMethod) {
    assert newMethod.replaces(method);
    method = newMethod;
  }

  /**
   * Sets the argument at the specified index.
   */
  public void setArg(int index, JExpression arg) {
    args = Lists.set(args, index, arg);
  }

  /**
   * See {@link #canBePolymorphic()}.
   */
  public void setCannotBePolymorphic() {
    assert polymorphism == Polymorphism.NORMAL;
    assert !method.isAbstract() : "Cannot set static dispacth to an abstract method: " + method;
    polymorphism = Polymorphism.CANNOT_BE_POLYMORPHIC;
  }

  /**
   * See {@link #isStaticDispatchOnly()}.
   */
  public void setStaticDispatchOnly() {
    assert polymorphism == Polymorphism.NORMAL;
    polymorphism = Polymorphism.STATIC_DISPATCH_ONLY;
  }

  /**
   * See {@link #isVolatile()}.
   */
  public void setVolatile() {
    assert polymorphism == Polymorphism.NORMAL;
    polymorphism = Polymorphism.VOLATILE;
  }

  @Override
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
