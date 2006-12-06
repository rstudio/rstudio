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

public class JMethod extends JAbstractMethod {

  public JMethod(JClassType enclosingType, String name, int declStart,
      int declEnd, int bodyStart, int bodyEnd) {
    super(name, declStart, declEnd, bodyStart, bodyEnd);
    this.enclosingType = enclosingType;
    enclosingType.addMethod(this);
  }

  public JClassType getEnclosingType() {
    return enclosingType;
  }

  public String getReadableDeclaration() {
    return getReadableDeclaration(getModifierBits());
  }

  public String getReadableDeclaration(boolean noAccess, boolean noNative,
      boolean noStatic, boolean noFinal, boolean noAbstract) {
    int bits = getModifierBits();
    if (noAccess) {
      bits &= ~(TypeOracle.MOD_PUBLIC | TypeOracle.MOD_PRIVATE | TypeOracle.MOD_PROTECTED);
    }
    if (noNative) {
      bits &= ~TypeOracle.MOD_NATIVE;
    }
    if (noStatic) {
      bits &= ~TypeOracle.MOD_STATIC;
    }
    if (noFinal) {
      bits &= ~TypeOracle.MOD_FINAL;
    }
    if (noAbstract) {
      bits &= ~TypeOracle.MOD_ABSTRACT;
    }
    return getReadableDeclaration(bits);
  }

  public JType getReturnType() {
    return returnType;
  }

  public boolean isAbstract() {
    return 0 != (getModifierBits() & TypeOracle.MOD_ABSTRACT);
  }

  public JConstructor isConstructor() {
    return null;
  }

  public boolean isFinal() {
    return 0 != (getModifierBits() & TypeOracle.MOD_FINAL);
  }

  public JMethod isMethod() {
    return this;
  }

  public boolean isNative() {
    return 0 != (getModifierBits() & TypeOracle.MOD_NATIVE);
  }

  public boolean isStatic() {
    return 0 != (getModifierBits() & TypeOracle.MOD_STATIC);
  }

  public String toString() {
    return getReadableDeclaration();
  }

  String getReadableDeclaration(int modifierBits) {
    String[] names = TypeOracle.modifierBitsToNames(modifierBits);
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < names.length; i++) {
      sb.append(names[i]);
      sb.append(" ");
    }
    sb.append(returnType.getParameterizedQualifiedSourceName());
    sb.append(" ");
    sb.append(getName());

    toStringParamsAndThrows(sb);

    return sb.toString();
  }

  public void setReturnType(JType type) {
    returnType = type;
  }

  private final JClassType enclosingType;
  private JType returnType;
}
