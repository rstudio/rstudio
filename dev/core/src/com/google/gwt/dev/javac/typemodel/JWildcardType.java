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
package com.google.gwt.dev.javac.typemodel;

import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;

/**
 * Represents a wildcard type argument to a parameterized type.
 */
public class JWildcardType extends JDelegatingClassType implements
    com.google.gwt.core.ext.typeinfo.JWildcardType {

  private final BoundType boundType;
  private JClassType[] lazyLowerBounds;
  private JClassType[] lazyUpperBounds;

  JWildcardType(BoundType boundType, JClassType typeBound) {
    this.boundType = boundType;
    super.setBaseType(typeBound);
  }

  @Override
  public JField findField(String name) {
    return getBaseType().findField(name);
  }

  @Override
  public JMethod findMethod(String name, JType[] paramTypes) {
    return getBaseType().findMethod(name, paramTypes);
  }

  @Override
  public BoundType getBoundType() {
    return boundType;
  }

  @Override
  public JClassType getErasedType() {
    return getUpperBound().getErasedType();
  }

  @Override
  public JField getField(String name) {
    return getBaseType().getField(name);
  }

  @Override
  public JField[] getFields() {
    return getBaseType().getFields();
  }

  @Override
  public JClassType getFirstBound() {
    return getBaseType();
  }

  /**
   * Returns the lower bounds of this wildcard type. If no lower bounds were
   * declared, an empty array is returned.
   *
   * @return the lower bounds of this wildcard type
   */
  @Override
  public JClassType[] getLowerBounds() {
    if (lazyLowerBounds == null) {
      if (isUpperBound()) {
        lazyLowerBounds = TypeOracle.NO_JCLASSES;
      } else {
        lazyLowerBounds = new JClassType[]{getFirstBound()};
      }
    }
    return lazyLowerBounds;
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
  public String getQualifiedBinaryName() {
    // TODO(jat): !! does a binary name have meaning for a wildcard?
    return toString(true);
  }

  @Override
  public String getQualifiedSourceName() {
    return toString(false);
  }

  @Override
  public String getSimpleSourceName() {
    return toString(true);
  }

  @Override
  public JClassType[] getSubtypes() {
    if (isUpperBound()) {
      return getFirstBound().getSubtypes();
    }

    // We are not sure what the correct behavior should be for lower bound
    // wildcards. ? super Number contains ? super T for all T extends Number,
    // but it also includes T for Number extends T. For example, Object is a
    // subtype.
    return TypeOracle.NO_JCLASSES;
  }

  @Override
  public JClassType getSuperclass() {
    if (isUpperBound()) {
      // The superclass of an upper bound is the upper bound.
      return getFirstBound();
    }

    // The only safe superclass for a ? super T is Object.
    return getOracle().getJavaLangObject();
  }

  @Override
  public JClassType getUpperBound() {
    if (isUpperBound()) {
      return getFirstBound();
    }

    return getOracle().getJavaLangObject();
  }

  /**
   * Returns the upper bounds of this wildcard type. If no upper bounds were
   * declared, an array containing {@link Object} is returned.
   *
   * @return the upper bounds of this wildcard type
   */
  @Override
  public JClassType[] getUpperBounds() {
    if (lazyUpperBounds == null) {
      if (isUpperBound()) {
        lazyUpperBounds = new JClassType[]{getFirstBound()};
      } else {
        // Object is the default upper bound.
        lazyUpperBounds = new JClassType[]{getOracle().getJavaLangObject()};
      }
    }

    return lazyUpperBounds;
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

  /**
   * Returns <code>true</code> if this instance has the same bounds that are
   * requested.
   *
   * @param otherWildcard
   * @return <code>true</code> if this instance has the same bounds that are
   *         requested
   */
  boolean boundsMatch(JWildcardType otherWildcard) {
    return isUpperBound() == otherWildcard.isUpperBound()
        && getFirstBound() == otherWildcard.getFirstBound();
  }

  @Override
  JClassType getSubstitutedType(JParameterizedType parameterizedType) {
    return getOracle().getWildcardType(boundType,
        getFirstBound().getSubstitutedType(parameterizedType));
  }

  private boolean isUnbound() {
    return boundType == BoundType.UNBOUND;
  }

  private boolean isUpperBound() {
    return boundType != BoundType.SUPER;
  }

  private String toString(boolean simpleName) {
    String str = "?";
    if (isUnbound()) {
      return str;
    } else {
      str += (isUpperBound() ? " extends " : " super ");
      if (simpleName) {
        return str + getFirstBound().getSimpleSourceName();
      } else {
        return str + getFirstBound().getParameterizedQualifiedSourceName();
      }
    }
  }
}
