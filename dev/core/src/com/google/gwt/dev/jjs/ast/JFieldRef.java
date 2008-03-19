/*
 * Copyright 2008 Google Inc.
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

/**
 * Java field reference expression.
 */
public class JFieldRef extends JVariableRef implements HasEnclosingType {

  /**
   * The enclosing type of this reference.
   */
  private final JReferenceType enclosingType;

  /**
   * The referenced field.
   */
  private JField field;

  /**
   * This can only be null if the referenced field is static.
   */
  private JExpression instance;

  public JFieldRef(JProgram program, SourceInfo info, JExpression instance,
      JField field, JReferenceType enclosingType) {
    super(program, info, field);
    assert (instance != null || field.isStatic());
    this.instance = instance;
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
    return instance;
  }

  public boolean hasSideEffects() {
    // A cross-class reference to a static, non constant field forces clinit
    if (field.isStatic()
        && (!field.isFinal() || !field.isCompileTimeConstant())) {
      if (program.typeOracle.checkClinit(enclosingType,
          field.getEnclosingType())) {
        // Therefore, we have side effects
        return true;
      }
    }

    JExpression expr = instance;
    if (expr == null) {
      return false;
    }
    return expr.hasSideEffects();
  }

  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
      if (instance != null) {
        instance = visitor.accept(instance);
      }
    }
    visitor.endVisit(this, ctx);
  }

}
