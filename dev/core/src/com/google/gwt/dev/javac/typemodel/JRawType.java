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

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a raw type; that is a generic type with no type arguments.
 */
public class JRawType extends JMaybeParameterizedType implements
    com.google.gwt.core.ext.typeinfo.JRawType {
  private static final Substitution ERASURE_SUBSTITUTION = new Substitution() {
    public JClassType getSubstitution(JClassType type) {
      return type.getErasedType();
    }
  };

  private List<JClassType> interfaces;

  private final AbstractMembers members;

  JRawType(JGenericType genericType) {
    super.setBaseType(genericType);
    members = new DelegateMembers(this, getBaseType(), ERASURE_SUBSTITUTION);
  }

  public JParameterizedType asParameterizedByWildcards() {
    return getBaseType().asParameterizedByWildcards();
  }

  @Override
  public JConstructor findConstructor(JType[] paramTypes) {
    return members.findConstructor(paramTypes);
  }

  @Override
  public JField findField(String name) {
    return members.findField(name);
  }

  @Override
  public JMethod findMethod(String name, JType[] paramTypes) {
    return members.findMethod(name, paramTypes);
  }

  @Override
  public JClassType findNestedType(String typeName) {
    return members.findNestedType(typeName);
  }

  @Override
  public JConstructor getConstructor(JType[] paramTypes)
      throws NotFoundException {
    return members.getConstructor(paramTypes);
  }

  @Override
  public JConstructor[] getConstructors() {
    return members.getConstructors();
  }

  @Override
  public JField getField(String name) {
    return members.getField(name);
  }

  @Override
  public JField[] getFields() {
    return members.getFields();
  }

  public JGenericType getGenericType() {
    return getBaseType();
  }

  @Override
  public JClassType[] getImplementedInterfaces() {
    if (interfaces == null) {
      interfaces = new ArrayList<JClassType>();
      JClassType[] intfs = getBaseType().getImplementedInterfaces();
      for (JClassType intf : intfs) {
        JClassType newIntf = intf.getErasedType();
        interfaces.add(newIntf);
      }
    }
    return interfaces.toArray(TypeOracle.NO_JCLASSES);
  }

  @Override
  public JMethod[] getInheritableMethods() {
    return members.getInheritableMethods();
  }

  @Override
  public JMethod getMethod(String name, JType[] paramTypes)
      throws NotFoundException {
    return members.getMethod(name, paramTypes);
  }

  @Override
  public JMethod[] getMethods() {
    return members.getMethods();
  }

  @Override
  public JClassType getNestedType(String typeName) throws NotFoundException {
    return members.getNestedType(typeName);
  }

  @Override
  public JClassType[] getNestedTypes() {
    return members.getNestedTypes();
  }

  @Override
  public JMethod[] getOverloads(String name) {
    return members.getOverloads(name);
  }

  @Override
  public JMethod[] getOverridableMethods() {
    return members.getOverridableMethods();
  }

  @Override
  public String getParameterizedQualifiedSourceName() {
    return getQualifiedSourceName();
  }

  @Override
  public String getQualifiedBinaryName() {
    return getBaseType().getQualifiedBinaryName();
  }

  @Override
  public String getQualifiedSourceName() {
    return getBaseType().getQualifiedSourceName();
  }

  @Override
  public String getSimpleSourceName() {
    return getBaseType().getSimpleSourceName();
  }

  @Override
  public JClassType[] getSubtypes() {
    JClassType[] baseSubTypes = super.getSubtypes();
    JClassType[] rawSubTypes = new JClassType[baseSubTypes.length];
    for (int i = 0; i < baseSubTypes.length; ++i) {
      JClassType subType = baseSubTypes[i];
      JGenericType isGenericType = subType.isGenericType();
      if (isGenericType != null) {
        rawSubTypes[i] = isGenericType.getRawType();
      } else {
        rawSubTypes[i] = subType;
      }
    }
    return rawSubTypes;
  }

  @Override
  public JClassType getSuperclass() {
    JClassType baseSuper = getBaseType().getSuperclass();
    if (baseSuper == null) {
      return null;
    }

    return baseSuper.getErasedType();
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
    return this;
  }

  @Override
  public JWildcardType isWildcard() {
    return null;
  }

  @Override
  JRawType getSubstitutedType(JParameterizedType parameterizedType) {
    /*
     * Raw types do not participate in substitution.
     */
    return this;
  }
}
