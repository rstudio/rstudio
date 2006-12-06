// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

/**
 * Java field reference expression.
 */
public class JFieldRef extends JVariableRef implements HasEnclosingType {

  /**
   * This can only be null if the referenced field is static.
   */
  public final Holder instance = new Holder();

  /**
   * The referenced field.
   */
  public JField field;

  /**
   * The enclosing type of this reference.
   */
  private final JReferenceType enclosingType;

  public JFieldRef(JProgram program, JExpression instance, JField field,
      JReferenceType enclosingType) {
    super(program, field);
    this.instance.set(instance);
    this.field = field;
    this.enclosingType = enclosingType;
  }

  public JReferenceType getEnclosingType() {
    return enclosingType;
  }

  public JField getField() {
    return field;
  }

  public JExpression getInstance() {
    return instance.get();
  }

  public boolean hasSideEffects() {
    // A cross-class reference to a static, non constant field forces clinit
    if (field.isStatic()
      && (!field.isFinal() || field.constInitializer == null)) {
      JReferenceType fieldEncloser = field.getEnclosingType();
      if (enclosingType != fieldEncloser
        && fProgram.typeOracle.hasClinit(fieldEncloser)) {
        // Therefore, we have side effects
        return true;
      }
    }

    JExpression expr = instance.get();
    if (expr == null) {
      return false;
    }
    return expr.hasSideEffects();
  }

  public void traverse(JVisitor visitor) {
    traverse(visitor, null);
  }

  public void traverse(JVisitor visitor, Mutator mutator) {
    if (visitor.visit(this, mutator)) {
      instance.traverse(visitor);
    }
    visitor.endVisit(this, mutator);
  }

}
