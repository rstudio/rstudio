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

import com.google.gwt.core.ext.UnableToCompleteException;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * Type representing a Java array.
 */
public class JArrayType extends JClassType {

  private JType componentType;

  private String lazyQualifiedName;

  private String lazySimpleName;

  private final TypeOracle oracle;

  JArrayType(JType componentType, TypeOracle oracle) {
    this.componentType = componentType;
    this.oracle = oracle;
  }

  @Override
  public void addImplementedInterface(JClassType intf) {
    throw new UnsupportedOperationException("modifying a "
        + getClass().getSimpleName());
  }

  @Override
  public void addMetaData(String tagName, String[] values) {
    throw new UnsupportedOperationException("modifying a "
        + getClass().getSimpleName());
  }

  @Override
  public void addModifierBits(int bits) {
    throw new UnsupportedOperationException("modifying a "
        + getClass().getSimpleName());
  }

  @Override
  public JConstructor findConstructor(JType[] paramTypes) {
    return null;
  }

  @Override
  public JField findField(String name) {
    // TODO length
    return null;
  }

  @Override
  public JMethod findMethod(String name, JType[] paramTypes) {
    // TODO Object
    return null;
  }

  @Override
  public JClassType findNestedType(String typeName) {
    return null;
  }

  @Override
  public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
    return null;
  }

  @Override
  public Annotation[] getAnnotations() {
    return TypeOracle.NO_ANNOTATIONS;
  }

  @Override
  public int getBodyEnd() {
    return 0;
  }

  @Override
  public int getBodyStart() {
    return 0;
  }

  @Override
  public CompilationUnitProvider getCompilationUnit() {
    return null;
  }

  public JType getComponentType() {
    return componentType;
  }

  @Override
  public JConstructor getConstructor(JType[] paramTypes)
      throws NotFoundException {
    return null;
  }

  @Override
  public JConstructor[] getConstructors() {
    return null;
  }

  @Override
  public Annotation[] getDeclaredAnnotations() {
    return TypeOracle.NO_ANNOTATIONS;
  }

  @Override
  public JClassType getEnclosingType() {
    return null;
  }

  @Override
  public JClassType getErasedType() {
    // TODO array of component type
    return this;
  }

  @Override
  public JField getField(String name) {
    // TODO length
    return null;
  }

  @Override
  public JField[] getFields() {
    // TODO length
    return TypeOracle.NO_JFIELDS;
  }

  @Override
  public JClassType[] getImplementedInterfaces() {
    return TypeOracle.NO_JCLASSES;
  }

  public String getJNISignature() {
    return "[" + componentType.getJNISignature();
  }

  public JType getLeafType() {
    return componentType.getLeafType();
  }

  @Override
  public String[][] getMetaData(String tagName) {
    return TypeOracle.NO_STRING_ARR_ARR;
  }

  @Override
  public String[] getMetaDataTags() {
    return TypeOracle.NO_STRINGS;
  }

  @Override
  public JMethod getMethod(String name, JType[] paramTypes)
      throws NotFoundException {
    // TODO Object
    return null;
  }

  @Override
  public JMethod[] getMethods() {
    // TODO Object
    return null;
  }

  @Override
  public String getName() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public JClassType getNestedType(String typeName) throws NotFoundException {
    throw new NotFoundException();
  }

  @Override
  public JClassType[] getNestedTypes() {
    return TypeOracle.NO_JCLASSES;
  }

  @Override
  public TypeOracle getOracle() {
    return oracle;
  }

  @Override
  public JMethod[] getOverloads(String name) {
    // TODO Object
    return null;
  }

  @Override
  public JMethod[] getOverridableMethods() {
    // TODO Object
    return null;
  }

  @Override
  public JPackage getPackage() {
    // TODO
    return null;
  }

  public String getParameterizedQualifiedSourceName() {
    return getComponentType().getParameterizedQualifiedSourceName() + "[]";
  }

  public String getQualifiedSourceName() {
    if (lazyQualifiedName == null) {
      lazyQualifiedName = getComponentType().getQualifiedSourceName() + "[]";
    }
    return lazyQualifiedName;
  }

  public int getRank() {
    JArrayType componentArrayType = componentType.isArray();
    if (componentArrayType != null) {
      return 1 + componentArrayType.getRank();
    }

    return 1;
  }

  public String getSimpleSourceName() {
    if (lazySimpleName == null) {
      lazySimpleName = getComponentType().getSimpleSourceName() + "[]";
    }
    return lazySimpleName;
  }

  @Override
  public JClassType[] getSubtypes() {
    // TODO
    return TypeOracle.NO_JCLASSES;
  }

  @Override
  public JClassType getSuperclass() {
    // TODO Object?
    return null;
  }

  @Override
  public String getTypeHash() throws UnableToCompleteException {
    // TODO
    return null;
  }

  @Override
  public boolean isAbstract() {
    return false;
  }

  @Override
  public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
    return false;
  }

  public JArrayType isArray() {
    return this;
  }

  @Override
  public boolean isAssignableFrom(JClassType possibleSubtype) {
    // TODO
    return false;
  }

  @Override
  public boolean isAssignableTo(JClassType possibleSupertype) {
    // TODO
    return false;
  }

  public JClassType isClass() {
    // intentional null
    return null;
  }

  @Override
  public boolean isDefaultInstantiable() {
    return true;
  }

  @Override
  public JGenericType isGenericType() {
    return null;
  }

  public JClassType isInterface() {
    // intentional null
    return null;
  }

  @Override
  public boolean isLocalType() {
    return false;
  }

  @Override
  public boolean isMemberType() {
    return false;
  }

  public JParameterizedType isParameterized() {
    // intentional null
    return null;
  }

  public JPrimitiveType isPrimitive() {
    // intentional null
    return null;
  }

  @Override
  public boolean isPrivate() {
    return false;
  }

  @Override
  public boolean isProtected() {
    return false;
  }

  @Override
  public boolean isPublic() {
    return true;
  }

  @Override
  public JRawType isRawType() {
    return null;
  }

  @Override
  public boolean isStatic() {
    return true;
  }

  public void setLeafType(JType type) {
    JArrayType componentTypeIsArray = componentType.isArray();
    if (componentTypeIsArray != null) {
      componentTypeIsArray.setLeafType(type);
    } else {
      componentType = type;
    }
  }

  @Override
  public void setSuperclass(JClassType type) {
  }

  public String toString() {
    return getQualifiedSourceName();
  }

  @Override
  protected void acceptSubtype(JClassType me) {
    throw new UnsupportedOperationException("modifying a "
        + getClass().getSimpleName());
  }

  @Override
  protected int getModifierBits() {
    return 0;
  }

  @Override
  protected void getOverridableMethodsOnSuperclassesAndThisClass(
      Map<String, JMethod> methodsBySignature) {
    // TODO Object
  }

  @Override
  protected void getOverridableMethodsOnSuperinterfacesAndMaybeThisInterface(
      Map<String, JMethod> methodsBySignature) {
    // TODO Object
  }

  @Override
  protected void notifySuperTypesOf(JClassType me) {
  }

  @Override
  protected void removeSubtype(JClassType me) {
  }

  @Override
  void addConstructor(JConstructor ctor) {
    throw new UnsupportedOperationException("modifying a "
        + getClass().getSimpleName());
  }

  @Override
  void addField(JField field) {
    throw new UnsupportedOperationException("modifying a "
        + getClass().getSimpleName());
  }

  @Override
  void addMethod(JMethod method) {
    throw new UnsupportedOperationException("modifying a "
        + getClass().getSimpleName());
  }

  @Override
  void addNestedType(JClassType type) {
    throw new UnsupportedOperationException("modifying a "
        + getClass().getSimpleName());
  }

  @Override
  JClassType findNestedTypeImpl(String[] typeName, int index) {
    return null;
  }

  @Override
  void notifySuperTypes() {
  }

  @Override
  void removeFromSupertypes() {
  }
}
