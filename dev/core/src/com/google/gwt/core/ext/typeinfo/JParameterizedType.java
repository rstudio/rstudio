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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents a parameterized type in a declaration.
 */
public class JParameterizedType extends JDelegatingClassType {

  private final Members members = new Members(this);

  private final List<JClassType> typeArgs = new ArrayList<JClassType>();

  JParameterizedType(JGenericType baseType) {
    super.setBaseType(baseType);
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

  /**
   * @deprecated see {@link #getQualifiedSourceName()}
   */
  @Deprecated
  public String getNonParameterizedQualifiedSourceName() {
    return getQualifiedSourceName();
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
    StringBuffer sb = new StringBuffer();
    sb.append(getQualifiedSourceName());

    sb.append('<');
    boolean needComma = false;
    for (JType typeArg : typeArgs) {
      if (needComma) {
        sb.append(", ");
      } else {
        needComma = true;
      }
      sb.append(typeArg.getParameterizedQualifiedSourceName());
    }
    sb.append('>');
    return sb.toString();
  }

  /**
   * Everything is fully qualified and includes the &lt; and &gt; in the
   * signature.
   */
  public String getQualifiedSourceName() {
    return getBaseType().getQualifiedSourceName();
  }

  public JClassType getRawType() {
    return getBaseType().getRawType();
  }

  /**
   * In this case, the raw type name.
   */
  public String getSimpleSourceName() {
    return getRawType().getSimpleSourceName();
  }

  public JClassType[] getTypeArgs() {
    return typeArgs.toArray(TypeOracle.NO_JCLASSES);
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
    return this;
  }

  @Override
  public JRawType isRawType() {
    return null;
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

  void addTypeArg(JClassType type) {
    assert (type.isPrimitive() == null);
    typeArgs.add(type);
  }

}
