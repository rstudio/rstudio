// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

/**
 * Java method call expression.
 */
public class JMethodCall extends JExpression {

  private final JMethod method;
  private final JType overrideReturnType;
  private boolean canBePolymorphic;
  public final Holder instance = new Holder();
  public HolderList args = new HolderList();

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

  public boolean canBePolymorphic() {
    return canBePolymorphic && !method.isFinal() && !method.isStatic();
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
