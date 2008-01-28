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
package com.google.gwt.core.ext.typeinfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents one of the type parameters in a generic type.
 */
public class JTypeParameter extends JDelegatingClassType implements HasBounds {
  private JBound bounds;
  private final int ordinal;
  private final String typeName;

  public JTypeParameter(String typeName, int ordinal) {
    this.typeName = typeName;
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

  public JBound getBounds() {
    return bounds;
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
  public String getParameterizedQualifiedSourceName() {
    return typeName;
  }

  @Override
  public String getQualifiedSourceName() {
    return typeName + bounds.getQualifiedSourceName();
  }

  @Override
  public String getSimpleSourceName() {
    return typeName + bounds.getSimpleSourceName();
  }
  
  @Override 
  public JClassType[] getSubtypes() {
    JClassType[] subtypes = super.getSubtypes();
    List<JClassType> intersectionTypes = new ArrayList<JClassType>();
    
    if (getFirstBound().isInterface() == null && isAssignableFrom(getFirstBound())) {
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
  public JTypeParameter isTypeParameter() {
    return this;
  }

  @Override
  public JWildcardType isWildcard() {
    return null;
  }

  public void setBounds(JBound bounds) {
    this.bounds = bounds;
    super.setBaseType(bounds.getFirstBound());
  }

  @Override
  public String toString() {
    if (getBaseType().isInterface() != null) {
      return "interface " + getQualifiedSourceName();
    } else {
      return "class " + getQualifiedSourceName();
    }
  }

  int getOrdinal() {
    return ordinal;
  }

  @Override
  JClassType getSubstitutedType(JParameterizedType parameterizedType) {
    return parameterizedType.getTypeParameterSubstitution(this);
  }
}
