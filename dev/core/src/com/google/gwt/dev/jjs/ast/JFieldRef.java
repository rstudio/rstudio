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
        && program.typeOracle.hasClinit(fieldEncloser)) {
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
