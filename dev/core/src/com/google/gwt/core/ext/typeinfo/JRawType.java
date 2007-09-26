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

import java.util.Map;

/**
 * Represents a raw type; that is a generic type with no type arguments.
 */
public class JRawType extends JDelegatingClassType {

  private final Members members = new Members(this);

  public JRawType(JGenericType genericType) {
    super.setBaseType(genericType);
    // TODO: type substitutions setting up fields/methods!
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

  public JGenericType getBaseType() {
    return (JGenericType) baseType;
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
  public String getQualifiedSourceName() {
    return baseType.getQualifiedSourceName();
  }

  @Override
  public String getSimpleSourceName() {
    return baseType.getSimpleSourceName();
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
  public boolean isDefaultInstantiable() {
    return getBaseType().isDefaultInstantiableIfParameterized();
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
  protected JClassType findNestedTypeImpl(String[] typeName, int index) {
    return members.findNestedTypeImpl(typeName, index);
  }

  protected void getOverridableMethodsOnSuperclassesAndThisClass(
      Map<String, JMethod> methodsBySignature) {
    members.getOverridableMethodsOnSuperclassesAndThisClass(methodsBySignature);
  }

  /**
   * Gets the methods declared in interfaces that this type extends. If this
   * type is a class, its own methods are not added. If this type is an
   * interface, its own methods are added. Used internally by
   * {@link #getOverridableMethods()}.
   * 
   * @param methodsBySignature
   */
  protected void getOverridableMethodsOnSuperinterfacesAndMaybeThisInterface(
      Map<String, JMethod> methodsBySignature) {
    members.getOverridableMethodsOnSuperinterfacesAndMaybeThisInterface(methodsBySignature);
  }
}
