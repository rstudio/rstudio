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
package com.google.gwt.core.ext.typeinfo;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * Represents a constructor declaration.
 */
public class JConstructor extends JAbstractMethod {
  private final JClassType enclosingType;

  public JConstructor(JClassType enclosingType, String name, int declStart,
      int declEnd, int bodyStart, int bodyEnd) {
    this(enclosingType, name, declStart, declEnd, bodyStart, bodyEnd, null, null);
  }

  public JConstructor(JClassType enclosingType, String name, int declStart,
      int declEnd, int bodyStart, int bodyEnd,
      Map<Class<? extends Annotation>, Annotation> declaredAnnotations,
      JTypeParameter[] jtypeParameters) {
    super(name, declStart, declEnd, bodyStart, bodyEnd, declaredAnnotations, 
        jtypeParameters);
    
    this.enclosingType = enclosingType;
    enclosingType.addConstructor(this);
  }

  JConstructor(JClassType enclosingType, JConstructor ctor) {
    super(ctor);
    this.enclosingType = enclosingType;
  }

  @Override
  public JClassType getEnclosingType() {
    return enclosingType;
  }

  @Override
  public String getReadableDeclaration() {
    String[] names = TypeOracle.modifierBitsToNames(getModifierBits());
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < names.length; i++) {
      sb.append(names[i]);
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
