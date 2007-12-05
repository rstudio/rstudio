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
  private static final JArrayType[] NO_JARRAYS = new JArrayType[0];

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
    return null;
  }

  @Override
  public JMethod findMethod(String name, JType[] paramTypes) {
    return getOracle().getJavaLangObject().findMethod(name, paramTypes);
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
    throw new NotFoundException();
  }

  @Override
  public JConstructor[] getConstructors() {
    return TypeOracle.NO_JCTORS;
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
    return getOracle().getArrayType(getComponentType().getErasedType());
  }

  @Override
  public JField getField(String name) {
    return null;
  }

  @Override
  public JField[] getFields() {
    return TypeOracle.NO_JFIELDS;
  }

  @Override
  public JClassType[] getImplementedInterfaces() {
    return TypeOracle.NO_JCLASSES;
  }

  @Override
  public String getJNISignature() {
    return "[" + componentType.getJNISignature();
  }

  @Override
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
    return getOracle().getJavaLangObject().getMethod(name, paramTypes);
  }

  @Override
  public JMethod[] getMethods() {
    return getOracle().getJavaLangObject().getMethods();
  }

  @Override
  public String getName() {
    return getSimpleSourceName();
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
    return getOracle().getJavaLangObject().getOverloads(name);
  }

  @Override
  public JMethod[] getOverridableMethods() {
    return getOracle().getJavaLangObject().getOverridableMethods();
  }

  @Override
  public JPackage getPackage() {
    JType leafType = getLeafType();
    if (leafType.isPrimitive() != null) {
      // TODO: is there a default package?
      return null;
    }

    JClassType leafClass = (JClassType) leafType;
    return leafClass.getPackage();
  }

  @Override
  public String getParameterizedQualifiedSourceName() {
    return getComponentType().getParameterizedQualifiedSourceName() + "[]";
  }

  @Override
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

  @Override
  public String getSimpleSourceName() {
    if (lazySimpleName == null) {
      lazySimpleName = getComponentType().getSimpleSourceName() + "[]";
    }
    return lazySimpleName;
  }

  @Override
  public JArrayType[] getSubtypes() {
    if (getComponentType().isPrimitive() != null) {
      return NO_JARRAYS;
    }

    JClassType componentClass = (JClassType) getComponentType();
    JClassType[] componentSubtypes = componentClass.getSubtypes();
    JArrayType[] arraySubtypes = new JArrayType[componentSubtypes.length];
    for (int i = 0; i < componentSubtypes.length; ++i) {
      arraySubtypes[i] = getOracle().getArrayType(componentSubtypes[i]);
    }

    return arraySubtypes;
  }

  @Override
  public JClassType getSuperclass() {
    return getOracle().getJavaLangObject();
  }

  @Override
  public String getTypeHash() throws UnableToCompleteException {
    JType leafType = getLeafType();
    JClassType leafClassType = leafType.isClassOrInterface();
    if (leafClassType != null) {
      return leafClassType.getTypeHash();
    }

    // Arrays of primitive types should have a stable hash
    assert (leafType.isPrimitive() != null);

    return leafType.getQualifiedSourceName();
  }

  @Override
  public boolean isAbstract() {
    return false;
  }

  @Override
  public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
    return false;
  }

  @Override
  public JArrayType isArray() {
    return this;
  }

  @Override
  public boolean isAssignableFrom(JClassType possibleSubtype) {
    if (this == possibleSubtype) {
      // type is assignable to itself
      return true;
    }

    JArrayType possibleSubtypeArray = possibleSubtype.isArray();
    if (possibleSubtypeArray == null) {
      // possible subtype must be an array to be assignable
      return false;
    }

    JType thisComponentType = getComponentType();
    JType otherComponentType = possibleSubtypeArray.getComponentType();

    if (thisComponentType.isPrimitive() != null
        || otherComponentType.isPrimitive() != null) {
      /*
       * Since this was not equal to the possible subtype, we know that either
       * the dimensions are off or the component types are off
       */
      return false;
    }

    assert (thisComponentType instanceof JClassType);
    assert (otherComponentType instanceof JClassType);

    JClassType thisComponentClassType = (JClassType) thisComponentType;
    JClassType otherComponentClassType = (JClassType) otherComponentType;

    return thisComponentClassType.isAssignableFrom(otherComponentClassType);
  }

  @Override
  public boolean isAssignableTo(JClassType possibleSupertype) {
    return possibleSupertype.isAssignableFrom(this);
  }

  @Override
  public JClassType isClass() {
    // intentional null
    return null;
  }

  @Override
  public boolean isDefaultInstantiable() {
    return true;
  }

  @Override
  public JEnumType isEnum() {
    return null;
  }

  @Override
  public JGenericType isGenericType() {
    return null;
  }

  @Override
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

  @Override
  public JParameterizedType isParameterized() {
    // intentional null
    return null;
  }

  @Override
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

  @Override
  public JWildcardType isWildcard() {
    return null;
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

  @Override
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
    getOracle().getJavaLangObject().getOverridableMethodsOnSuperclassesAndThisClass(
        methodsBySignature);
  }

  @Override
  protected void getOverridableMethodsOnSuperinterfacesAndMaybeThisInterface(
      Map<String, JMethod> methodsBySignature) {
    getOracle().getJavaLangObject().getOverridableMethodsOnSuperinterfacesAndMaybeThisInterface(
        methodsBySignature);
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
  JArrayType getSubstitutedType(JParameterizedType parameterizedType) {
    return oracle.getArrayType(getComponentType().getSubstitutedType(
        parameterizedType));
  }

  @Override
  void notifySuperTypes() {
  }

  @Override
  void removeFromSupertypes() {
  }
}
