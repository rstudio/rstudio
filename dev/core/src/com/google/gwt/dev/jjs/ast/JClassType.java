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
 * Java class type reference expression.
 */
public class JClassType extends JDeclaredType implements CanBeSetFinal {

  private static class ExternalSerializedForm implements Serializable {
    private final String name;

    public ExternalSerializedForm(JClassType classType) {
      name = classType.getName();
    }

    private Object readResolve() {
      return new JClassType(name);
    }
  }

  private final boolean isAbstract;
  private boolean isFinal;
  private JClassType superClass;
  private boolean isJsPrototype = false;

  public JClassType(SourceInfo info, String name, boolean isAbstract, boolean isFinal) {
    super(info, name);
    this.isAbstract = isAbstract;
    this.isFinal = isFinal;
  }

  /**
   * Construct a bare-bones deserialized external class.
   */
  JClassType(String name) {
    super(SourceOrigin.UNKNOWN, name);
    isAbstract = false;
    setExternal(true);
  }

  @Override
  public String getClassLiteralFactoryMethod() {
    return "Class.createForClass";
  }

  @Override
  public final JClassType getSuperClass() {
    return superClass;
  }

  @Override
  public boolean isAbstract() {
    return isAbstract;
  }

  public JEnumType isEnumOrSubclass() {
    if (getSuperClass() != null) {
      return getSuperClass().isEnumOrSubclass();
    }
    return null;
  }

  @Override
  public boolean isFinal() {
    return isFinal;
  }

  @Override
  public void setFinal() {
    isFinal = true;
  }

  /**
   * Sets this type's super class.
   */
  public final void setSuperClass(JClassType superClass) {
    this.superClass = superClass;
  }

  public boolean isJsPrototypeStub() {
    return isJsPrototype;
  }

  public void setJsPrototypeStub(boolean isJsPrototype) {
    this.isJsPrototype = isJsPrototype;
  }

  @Override
  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
      fields = visitor.acceptWithInsertRemoveImmutable(fields);
      methods = visitor.acceptWithInsertRemoveImmutable(methods);
    }
    visitor.endVisit(this, ctx);
  }

  @Override
  protected Object writeReplace() {
    if (isExternal()) {
      return new ExternalSerializedForm(this);
    } else {
      return this;
    }
  }
}
