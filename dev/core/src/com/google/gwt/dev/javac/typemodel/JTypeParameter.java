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
import com.google.gwt.dev.util.StringInterner;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents one of the type parameters in a generic type.
 */
public class JTypeParameter extends JDelegatingClassType implements
    com.google.gwt.core.ext.typeinfo.JTypeParameter {
  private JClassType[] bounds;
  private JGenericType declaringClass;
  private final int ordinal;
  private final String typeName;

  public JTypeParameter(String typeName, int ordinal) {
    this.typeName = StringInterner.get().intern(typeName);
    this.ordinal = ordinal;
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
  public JClassType[] getBounds() {
    return bounds;
  }

  @Override
  public JGenericType getDeclaringClass() {
    return declaringClass;
  }

  @Override
  public JClassType getEnclosingType() {
    // Type parameters do not have an enclosing type.
    return null;
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
  public String getName() {
    return typeName;
  }

  @Override
  public int getOrdinal() {
    return ordinal;
  }

  @Override
  public String getParameterizedQualifiedSourceName() {
    return typeName;
  }

  @Override
  public String getQualifiedBinaryName() {
    // TODO(jat): !! does a binary name have meaning for a type parameter?
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
    JClassType[] subtypes = super.getSubtypes();
    List<JClassType> intersectionTypes = new ArrayList<JClassType>();

    if (getFirstBound().isInterface() == null
        && isAssignableFrom(getFirstBound())) {
      // Include the first bound as a subtype if it is not an interface and it
      // is assignable to all of our bounds.
      intersectionTypes.add(getFirstBound());
    }

    for (JClassType subtype : subtypes) {
      if (isAssignableFrom(subtype)) {
        intersectionTypes.add(subtype);
      }
    }

    // Only types that intersect with all our bounds make it here.
    return intersectionTypes.toArray(TypeOracle.NO_JCLASSES);
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
  public JTypeParameter isTypeParameter() {
    return this;
  }

  @Override
  public JWildcardType isWildcard() {
    return null;
  }

  public void setBounds(JClassType[] bounds) {
    this.bounds = bounds;
    super.setBaseType(bounds[0]);
  }

  @Override
  public String toString() {
    if (getBaseType().isInterface() != null) {
      return "interface " + getQualifiedSourceName();
    } else {
      return "class " + getQualifiedSourceName();
    }
  }

  @Override
  JClassType getSubstitutedType(JParameterizedType parameterizedType) {
    return parameterizedType.getTypeParameterSubstitution(this);
  }

  void setDeclaringClass(JGenericType declaringClass) {
    this.declaringClass = declaringClass;
  }

  private String toString(boolean simpleName) {
    StringBuffer sb = new StringBuffer();
    sb.append(typeName);
    sb.append(" extends ");
    for (int i = 0; i < bounds.length; ++i) {
      if (i != 0) {
        sb.append(" & ");
      }

      String boundName;
      if (simpleName) {
        boundName = bounds[i].getSimpleSourceName();
      } else {
        boundName = bounds[i].getParameterizedQualifiedSourceName();
      }
      sb.append(boundName);
    }

    return sb.toString();
  }
}
