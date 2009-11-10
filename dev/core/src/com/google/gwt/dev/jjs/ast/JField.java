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
public class JField extends JVariable implements CanBeStatic, HasEnclosingType {

  /**
   * Determines whether the variable is final, volatile, or neither.
   */
  public static enum Disposition {
    COMPILE_TIME_CONSTANT, FINAL, NONE, VOLATILE;

    public boolean isFinal() {
      return this == COMPILE_TIME_CONSTANT || this == FINAL;
    }

    private boolean isCompileTimeConstant() {
      return this == COMPILE_TIME_CONSTANT;
    }

    private boolean isVolatile() {
      return this == VOLATILE;
    }
  }

  private final JDeclaredType enclosingType;
  private final boolean isCompileTimeConstant;
  private final boolean isStatic;
  private boolean isVolatile;

  JField(SourceInfo info, String name, JDeclaredType enclosingType,
      JType type, boolean isStatic, Disposition disposition) {
    super(info, name, type, disposition.isFinal());
    this.enclosingType = enclosingType;
    this.isStatic = isStatic;
    this.isCompileTimeConstant = disposition.isCompileTimeConstant();
    this.isVolatile = disposition.isVolatile();
    // Disposition is not cached because we can be set final later.
  }

  public JDeclaredType getEnclosingType() {
    return enclosingType;
  }

  public JValueLiteral getLiteralInitializer() {
    JExpression initializer = getInitializer();
    if (initializer instanceof JValueLiteral) {
      return (JValueLiteral) initializer;
    }
    return null;
  }

  public boolean isCompileTimeConstant() {
    return isCompileTimeConstant;
  }

  public boolean isStatic() {
    return isStatic;
  }

  public boolean isVolatile() {
    return isVolatile;
  }

  @Override
  public void setFinal() {
    if (isVolatile()) {
      throw new IllegalStateException("Volatile fields cannot be set final");
    }
    super.setFinal();
  }

  public void setInitializer(JDeclarationStatement declStmt) {
    this.declStmt = declStmt;
  }

  public void setVolatile() {
    if (isFinal()) {
      throw new IllegalStateException("Final fields cannot be set volatile");
    }
    isVolatile = true;
  }

  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
      // Do not visit declStmt, it gets visited within its own code block.
    }
    visitor.endVisit(this, ctx);
  }

}
