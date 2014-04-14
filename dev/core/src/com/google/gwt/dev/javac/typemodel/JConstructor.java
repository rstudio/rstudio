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
package com.google.gwt.dev.javac.typemodel;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * Represents a constructor declaration.
 */
public class JConstructor extends JAbstractMethod implements
    com.google.gwt.core.ext.typeinfo.JConstructor {
  private final JClassType enclosingType;

  JConstructor(JClassType enclosingType, JConstructor ctor) {
    super(ctor);
    this.enclosingType = enclosingType;
  }

  JConstructor(JClassType enclosingType, String name) {
    this(enclosingType, name, null, null);
  }

  JConstructor(JClassType enclosingType, String name,
      Map<Class<? extends Annotation>, Annotation> declaredAnnotations,
      JTypeParameter[] jtypeParameters) {
    super(name, declaredAnnotations, jtypeParameters);

    this.enclosingType = enclosingType;
    enclosingType.addConstructor(this);
  }

  @Override
  public JClassType getEnclosingType() {
    return enclosingType;
  }

  @Override
  public String getJsniSignature() {
    StringBuilder sb = new StringBuilder("@");
    sb.append(getEnclosingType().getQualifiedSourceName());
    sb.append("::new(");
    for (JParameter param : getParameters()) {
      sb.append(param.getType().getJNISignature());
    }
    sb.append(")");
    return sb.toString();
  }

  @Override
  public String getReadableDeclaration() {
    String[] names = TypeOracle.modifierBitsToNamesForMethod(getModifierBits());
    StringBuilder sb = new StringBuilder();
    for (String name : names) {
      sb.append(name);
      sb.append(" ");
    }
    if (getTypeParameters().length > 0) {
      toStringTypeParams(sb);
      sb.append(" ");
    }
    sb.append(getName());
    toStringParamsAndThrows(sb);
    return sb.toString();
  }

  @Override
  public JConstructor isConstructor() {
    return this;
  }

  @Override
  public JMethod isMethod() {
    return null;
  }

  @Override
  public String toString() {
    return getReadableDeclaration();
  }
}
