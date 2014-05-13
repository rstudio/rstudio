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
import com.google.gwt.dev.jjs.SourceOrigin;

import java.io.Serializable;

/**
 * Java field definition.
 */
public class JField extends JVariable implements CanBeStatic, HasEnclosingType {

  /**
   * Determines whether the variable is final, volatile, or neither.
   */
  public static enum Disposition {
    COMPILE_TIME_CONSTANT, FINAL, NONE, THIS_REF, VOLATILE;

    public boolean isFinal() {
      return this == COMPILE_TIME_CONSTANT || this == FINAL || this == THIS_REF;
    }

    public boolean isThisRef() {
      return this == THIS_REF;
    }

    private boolean isCompileTimeConstant() {
      return this == COMPILE_TIME_CONSTANT;
    }

    private boolean isVolatile() {
      return this == VOLATILE;
    }
  }

  private String exportName;

  public void setExportName(String exportName) {
    this.exportName = exportName;
  }

  public String getExportName() {
    return exportName;
  }

  private static class ExternalSerializedForm implements Serializable {

    private final JDeclaredType enclosingType;
    private final String signature;

    public ExternalSerializedForm(JField field) {
      enclosingType = field.getEnclosingType();
      signature = field.getSignature();
    }

    private Object readResolve() {
      String name = signature.substring(0, signature.indexOf(':'));
      JField result =
          new JField(SourceOrigin.UNKNOWN, name, enclosingType, JNullType.INSTANCE, false,
              Disposition.NONE);
      result.signature = signature;
      return result;
    }
  }

  private static class ExternalSerializedNullField implements Serializable {
    public static final ExternalSerializedNullField INSTANCE = new ExternalSerializedNullField();

    private Object readResolve() {
      return NULL_FIELD;
    }
  }

  public static final JField NULL_FIELD = new JField(SourceOrigin.UNKNOWN, "nullField", null,
      JNullType.INSTANCE, false, Disposition.FINAL);

  private final JDeclaredType enclosingType;
  private final boolean isCompileTimeConstant;
  private final boolean isStatic;
  private final boolean isThisRef;
  private boolean isVolatile;
  private transient String signature;

  public JField(SourceInfo info, String name, JDeclaredType enclosingType, JType type,
      boolean isStatic, Disposition disposition) {
    super(info, name, type, disposition.isFinal());
    this.enclosingType = enclosingType;
    this.isStatic = isStatic;
    this.isCompileTimeConstant = disposition.isCompileTimeConstant();
    this.isVolatile = disposition.isVolatile();
    this.isThisRef = disposition.isThisRef();
    // Disposition is not cached because we can be set final later.
  }

  public String getFullName() {
    return getEnclosingType().getName() + "." + getName();
  }

  @Override
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

  public String getQualifiedExportName() {
    if ("".equals(exportName)) {
        return getEnclosingType().getQualifiedExportName() + "." + getName();
    } else {
      return exportName;
    }
  }

  public String getSignature() {
    if (signature == null) {
      StringBuilder sb = new StringBuilder();
      sb.append(getName());
      sb.append(':');
      sb.append(getType().getJsniSignatureName());
      signature = sb.toString();
    }
    return signature;
  }

  public boolean isCompileTimeConstant() {
    return isCompileTimeConstant;
  }

  public boolean isExternal() {
    return getEnclosingType() != null && getEnclosingType().isExternal();
  }

  @Override
  public boolean isStatic() {
    return isStatic;
  }

  public boolean isThisRef() {
    return isThisRef;
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

  @Override
  public void setInitializer(JDeclarationStatement declStmt) {
    this.declStmt = declStmt;
  }

  public void setVolatile() {
    if (isFinal()) {
      throw new IllegalStateException("Final fields cannot be set volatile");
    }
    isVolatile = true;
  }

  @Override
  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
      // Do not visit declStmt, it gets visited within its own code block.
    }
    visitor.endVisit(this, ctx);
  }

  protected Object writeReplace() {
    if (isExternal()) {
      return new ExternalSerializedForm(this);
    } else if (this == NULL_FIELD) {
      return ExternalSerializedNullField.INSTANCE;
    } else {
      return this;
    }
  }

  boolean replaces(JField originalField) {
    if (this == originalField) {
      return true;
    }
    return originalField.isExternal() && originalField.getSignature().equals(this.getSignature())
        && this.getEnclosingType().replaces(originalField.getEnclosingType());
  }
}
