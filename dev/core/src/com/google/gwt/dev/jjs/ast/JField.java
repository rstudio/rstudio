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
 * Java field definition.
 */
public class JField extends JVariable implements CanBeStatic, HasEnclosingType,
    CanHaveInitializer {

  private final JReferenceType enclosingType;
  private final boolean isCompileTimeConstant;
  private final boolean isStatic;
  private boolean isVolatile;

  JField(JProgram program, SourceInfo info, String name,
      JReferenceType enclosingType, JType type, boolean isStatic,
      boolean isFinal, boolean isCompileTimeConstant) {
    super(program, info, name, type, isFinal);
    this.enclosingType = enclosingType;
    this.isStatic = isStatic;
    this.isCompileTimeConstant = isCompileTimeConstant;
    assert (isFinal || !isCompileTimeConstant);
  }

  public JReferenceType getEnclosingType() {
    return enclosingType;
  }

  public JValueLiteral getLiteralInitializer() {
    if (initializer instanceof JValueLiteral) {
      return (JValueLiteral) initializer;
    }
    return null;
  }

  public boolean isCompileTimeConstant() {
    return isCompileTimeConstant;
  }

  @Override
  public boolean isFinal() {
    return !isVolatile && super.isFinal();
  }

  public boolean isStatic() {
    return isStatic;
  }

  public void setInitializer(JExpression initializer) {
    this.initializer = initializer;
  }

  public void setVolatile() {
    isVolatile = true;
  }

  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
    }
    visitor.endVisit(this, ctx);
  }

}
