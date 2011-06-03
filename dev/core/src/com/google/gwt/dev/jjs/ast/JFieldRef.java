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
  private final JDeclaredType enclosingType;

  /**
   * This can only be null if the referenced field is static.
   */
  private JExpression instance;

  /**
   * An overridden type for this reference. Normally the type of a field
   * reference is the same as the type of the field itself. That default can be
   * overridden by setting this field.
   */
  private final JType overriddenType;

  public JFieldRef(SourceInfo info, JExpression instance, JField field, JDeclaredType enclosingType) {
    this(info, instance, field, enclosingType, null);
  }

  public JFieldRef(SourceInfo info, JExpression instance, JField field,
      JDeclaredType enclosingType, JType overriddenType) {
    super(info, field);
    assert (instance != null || field.isStatic());
    assert (enclosingType != null);
    this.instance = instance;
    this.enclosingType = enclosingType;
    this.overriddenType = overriddenType;
  }

  public JDeclaredType getEnclosingType() {
    return enclosingType;
  }

  public JField getField() {
    return (JField) getTarget();
  }

  public JExpression getInstance() {
    return instance;
  }

  @Override
  public JType getType() {
    if (overriddenType != null) {
      return overriddenType;
    }
    return super.getType();
  }

  public boolean hasClinit() {
    JField field = getField();
    // A cross-class reference to a static, non constant field forces clinit
    if (!field.isStatic()) {
      return false;
    }
    if (field.isFinal() && field.isCompileTimeConstant()) {
      return false;
    }
    return getEnclosingType().checkClinitTo(field.getEnclosingType());
  }

  @Override
  public boolean hasSideEffects() {
    if (hasClinit()) {
      return true;
    }
    JExpression expr = instance;
    if (expr == null) {
      return false;
    }
    return expr.hasSideEffects();
  }

  /**
   * Resolve an external reference during AST stitching.
   */
  public void resolve(JField newField) {
    assert newField.replaces(getField());
    target = newField;
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
