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
 * Represents a wildcard type argument to a parameterized type.
 */
public class JWildcardType extends JDelegatingClassType implements HasBounds {

  private final JBound bounds;

  public JWildcardType(JBound bounds) {
    this.bounds = bounds;
    super.setBaseType(bounds.getFirstBound());
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
  public String getQualifiedSourceName() {
    return "?";
  }

  @Override
  public String getSimpleSourceName() {
    return "?";
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
}
