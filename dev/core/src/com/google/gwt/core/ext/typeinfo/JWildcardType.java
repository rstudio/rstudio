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
    super.setBaseType(bounds.getFirstBound());
    this.bounds = bounds;
  }

  @Override
  public JField findField(String name) {
    return getBaseType().findField(name);
  }

  @Override
  public JMethod findMethod(String name, JType[] paramTypes) {
    return getBaseType().findMethod(name, paramTypes);
  }

  public JBound getBounds() {
    return bounds;
  }

  @Override
  public JField getField(String name) {
    return getBaseType().getField(name);
  }

  @Override
  public JField[] getFields() {
    return getBaseType().getFields();
  }

  public JClassType getFirstBound() {
    return getBounds().getFirstBound();
  }

  @Override
  public JMethod getMethod(String name, JType[] paramTypes)
      throws NotFoundException {
    return getBaseType().getMethod(name, paramTypes);
  }

  @Override
  public JMethod[] getMethods() {
    return getBaseType().getMethods();
  }

  @Override
  public String getQualifiedSourceName() {
    return "?" + bounds.getQualifiedSourceName();
  }

  @Override
  public String getSimpleSourceName() {
    return "?" + bounds.getSimpleSourceName();
  }

  @Override
  public JClassType[] getSubtypes() {
    return bounds.getSubtypes();
  }

  @Override
  public boolean isAssignableFrom(JClassType otherType) {
    if (otherType == this) {
      return true;
    }
    
    return getBounds().isAssignableFrom(otherType);
  }

  @Override
  public boolean isAssignableTo(JClassType otherType) {
    if (otherType == this) {
      return true;
    }
    
    return getBounds().isAssignableTo(otherType);
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

  @Override
  public JWildcardType isWildcard() {
    return this;
  }

  @Override
  JClassType getSubstitutedType(JParameterizedType parameterizedType) {
    JClassType[] currentBounds = bounds.getBounds();
    JClassType[] newBounds = new JClassType[currentBounds.length];
    for (int i = 0; i < currentBounds.length; ++i) {
      newBounds[i] = currentBounds[i].getSubstitutedType(parameterizedType);
    }

    JBound newBound = bounds.isLowerBound() != null
        ? new JLowerBound(newBounds) : new JUpperBound(newBounds);
    return getOracle().getWildcardType(newBound);
  }

  /**
   * Returns <code>true</code> if this instance has the same bounds that are
   * requested.
   * 
   * @param otherBounds
   * @return <code>true</code> if this instance has the same bounds that are
   *         requested
   */
  boolean hasBounds(JBound otherBounds) {
    if ((bounds.isUpperBound() != null && otherBounds.isLowerBound() != null)
        || (bounds.isLowerBound() != null && otherBounds.isUpperBound() != null)) {
      return false;
    }

    JClassType[] boundTypes = bounds.getBounds();
    JClassType[] otherBoundTypes = otherBounds.getBounds();

    if (boundTypes.length != otherBoundTypes.length) {
      return false;
    }

    for (int i = 0; i < boundTypes.length; ++i) {
      if (boundTypes[i] != otherBoundTypes[i]) {
        return false;
      }
    }
    return true;
  }
}
