/*
 * Copyright 2007 Google Inc.
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

/**
 * Represents one of the type parameters in a generic type.
 */
public class JTypeParameter extends JDelegatingClassType implements HasBounds {
  private JBound bounds;
  private final JGenericType declaringClass;
  private final JAbstractMethod declaringMethod;
  private final String typeName;

  public JTypeParameter(String typeName, JAbstractMethod declaringMethod) {
    this.typeName = typeName;
    this.declaringMethod = declaringMethod;
    this.declaringClass = null;
    declaringMethod.addTypeParameter(this);
  }

  public JTypeParameter(String typeName, JGenericType declaringClass) {
    this.typeName = typeName;
    this.declaringClass = declaringClass;
    this.declaringMethod = null;
    declaringClass.addTypeParameter(this);
  }

  @Override
  public JField findField(String name) {
    return baseType.findField(name);
  }

  @Override
  public JMethod findMethod(String name, JType[] paramTypes) {
    return baseType.findMethod(name, paramTypes);
  }

  public JBound getBounds() {
    return bounds;
  }

  public JRealClassType getDeclaringClass() {
    return declaringClass;
  }

  @Override
  public JField getField(String name) {
    return baseType.getField(name);
  }

  @Override
  public JField[] getFields() {
    return baseType.getFields();
  }

  public JClassType getFirstBound() {
    return baseType;
  }

  @Override
  public JMethod getMethod(String name, JType[] paramTypes)
      throws NotFoundException {
    return baseType.getMethod(name, paramTypes);
  }

  @Override
  public JMethod[] getMethods() {
    return baseType.getMethods();
  }

  @Override
  public String getName() {
    return typeName;
  }
  
  @Override
  public String getParameterizedQualifiedSourceName() {
    return typeName;
  }
  
  @Override
  public String getQualifiedSourceName() {
    return typeName;
  }

  @Override
  public String getSimpleSourceName() {
    return typeName;
  }

  @Override
  public JGenericType isGenericType() {
    return null;
  }

  @Override
  public JParameterizedType isParameterized() {
    return null;
  }

  @Override
  public JRawType isRawType() {
    return null;
  }

  public void setBounds(JBound bounds) {
    this.bounds = bounds;
    super.setBaseType(bounds.getFirstBound());
  }

  @Override
  public String toString() {
    if (baseType.isInterface() != null) {
      return "interface " + getQualifiedSourceName();
    } else {
      return "class " + getQualifiedSourceName();
    }
  }
}
