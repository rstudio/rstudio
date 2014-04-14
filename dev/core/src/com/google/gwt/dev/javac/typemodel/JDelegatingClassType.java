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

import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * Base class for types that delegate to another type, such as a JTypeParameter
 * or JParameterizedType.
 */
abstract class JDelegatingClassType extends JClassType {

  private JClassType baseType;

  JDelegatingClassType() {
  }

  /**
   * Delegating types generally cannot be constructed.
   */
  @Override
  public JConstructor findConstructor(JType[] paramTypes) {
    return null;
  }

  /**
   * Subclasses will generally need to echo modified fields.
   */
  @Override
  public abstract JField findField(String name);

  /**
   * Subclasses will generally need to echo modified methods.
   */
  @Override
  public abstract JMethod findMethod(String name, JType[] paramTypes);

  @Override
  public JClassType findNestedType(String typeName) {
    return baseType.findNestedType(typeName);
  }

  @Override
  public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
    return baseType.getAnnotation(annotationClass);
  }

  @Override
  public Annotation[] getAnnotations() {
    return baseType.getAnnotations();
  }

  public JClassType getBaseType() {
    return baseType;
  }

  /**
   * Delegating types generally cannot be constructed.
   */
  @Override
  public JConstructor getConstructor(JType[] paramTypes)
      throws NotFoundException {
    throw new NotFoundException();
  }

  /**
   * Delegating types generally cannot be constructed.
   */
  @Override
  public JConstructor[] getConstructors() {
    return TypeOracle.NO_JCTORS;
  }

  @Override
  public Annotation[] getDeclaredAnnotations() {
    return baseType.getDeclaredAnnotations();
  }

  @Override
  public JClassType getEnclosingType() {
    // TODO this can be wrong if the enclosing type is a parameterized type. For
    // example, if a generic class has a non-static generic inner class.
    return baseType.getEnclosingType();
  }

  @Override
  public JClassType getErasedType() {
    return baseType.getErasedType();
  }

  /**
   * Subclasses will generally need to echo modified fields.
   */
  @Override
  public abstract JField getField(String name);

  /**
   * Subclasses will generally need to echo modified fields.
   */
  @Override
  public abstract JField[] getFields();

  @Override
  public JClassType[] getImplementedInterfaces() {
    return baseType.getImplementedInterfaces();
  }

  @Override
  public JMethod[] getInheritableMethods() {
    return baseType.getInheritableMethods();
  }

  @Override
  public String getJNISignature() {
    return baseType.getJNISignature();
  }

  /**
   * Subclasses will generally need to echo modified methods.
   */
  @Override
  public abstract JMethod getMethod(String name, JType[] paramTypes)
      throws NotFoundException;

  /**
   * Subclasses will generally need to echo modified methods.
   */
  @Override
  public abstract JMethod[] getMethods();

  @Override
  public String getName() {
    return baseType.getName();
  }

  @Override
  public JClassType getNestedType(String typeName) throws NotFoundException {
    return baseType.getNestedType(typeName);
  }

  @Override
  public JClassType[] getNestedTypes() {
    return baseType.getNestedTypes();
  }

  @Override
  public TypeOracle getOracle() {
    return baseType.getOracle();
  }

  @Override
  public JMethod[] getOverloads(String name) {
    return baseType.getOverloads(name);
  }

  @Override
  public JMethod[] getOverridableMethods() {
    return baseType.getOverridableMethods();
  }

  @Override
  public JPackage getPackage() {
    return baseType.getPackage();
  }

  @Override
  public JClassType[] getSubtypes() {
    return baseType.getSubtypes();
  }

  @Override
  public JClassType getSuperclass() {
    return baseType.getSuperclass();
  }

  @Override
  public boolean isAbstract() {
    return baseType.isAbstract();
  }

  @Override
  public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
    return baseType.isAnnotationPresent(annotationClass);
  }

  @Override
  public final JArrayType isArray() {
    return null;
  }

  @Override
  public JClassType isClass() {
    if (baseType.isClass() != null) {
      return this;
    } else {
      return null;
    }
  }

  @Override
  public JClassType isClassOrInterface() {
    if (baseType.isClassOrInterface() != null) {
      return this;
    } else {
      return null;
    }
  }

  @Override
  public boolean isDefaultInstantiable() {
    return baseType.isDefaultInstantiable();
  }

  @Override
  public final JEnumType isEnum() {
    return null;
  }

  @Override
  public boolean isFinal() {
    return baseType.isFinal();
  }

  @Override
  public JClassType isInterface() {
    if (baseType.isInterface() != null) {
      return this;
    } else {
      return null;
    }
  }

  @Override
  public boolean isMemberType() {
    return baseType.isMemberType();
  }

  @Override
  public final JPrimitiveType isPrimitive() {
    return null;
  }

  @Override
  public boolean isPrivate() {
    return baseType.isPrivate();
  }

  @Override
  public boolean isProtected() {
    return baseType.isProtected();
  }

  @Override
  public boolean isPublic() {
    return baseType.isPublic();
  }

  @Override
  public boolean isStatic() {
    return baseType.isStatic();
  }

  @Override
  public String toString() {
    if (baseType.isInterface() != null) {
      return "interface " + getQualifiedSourceName();
    } else {
      return "class " + getQualifiedSourceName();
    }
  }

  @Override
  protected void acceptSubtype(JClassType me) {
    baseType.acceptSubtype(me);
  }

  @Override
  protected void getInheritableMethodsOnSuperclassesAndThisClass(
      Map<String, JMethod> methodsBySignature) {
    baseType.getInheritableMethodsOnSuperclassesAndThisClass(methodsBySignature);
  }

  @Override
  protected void getInheritableMethodsOnSuperinterfacesAndMaybeThisInterface(
      Map<String, JMethod> methodsBySignature) {
    baseType.getInheritableMethodsOnSuperinterfacesAndMaybeThisInterface(methodsBySignature);
  }

  @Override
  protected int getModifierBits() {
    return baseType.getModifierBits();
  }

  @Override
  protected void notifySuperTypesOf(JClassType me) {
  }

  @Override
  protected void removeSubtype(JClassType me) {
  }

  @Override
  final void addConstructor(JConstructor ctor) {
    throw new UnsupportedOperationException("modifying a "
        + getClass().getSimpleName());
  }

  @Override
  final void addField(JField field) {
    throw new UnsupportedOperationException("modifying a "
        + getClass().getSimpleName());
  }

  @Override
  final void addImplementedInterface(JClassType intf) {
    throw new UnsupportedOperationException("modifying a "
        + getClass().getSimpleName());
  }

  @Override
  final void addMethod(JMethod method) {
    throw new UnsupportedOperationException("modifying a "
        + getClass().getSimpleName());
  }

  @Override
  final void addModifierBits(int bits) {
    throw new UnsupportedOperationException("modifying a "
        + getClass().getSimpleName());
  }

  @Override
  final void addNestedType(JClassType type) {
    throw new UnsupportedOperationException("modifying a "
        + getClass().getSimpleName());
  }

  @Override
  JClassType findNestedTypeImpl(String[] typeName, int index) {
    return baseType.findNestedTypeImpl(typeName, index);
  }

  @Override
  void notifySuperTypes() {
  }

  @Override
  void removeFromSupertypes() {
  }

  final void setBaseType(JClassType baseType) {
    this.baseType = baseType;
  }

  @Override
  void setSuperclass(JClassType type) {
    throw new UnsupportedOperationException("modifying a "
        + getClass().getSimpleName());
  }
}
