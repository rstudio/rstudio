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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Type representing a Java class or interface type.
 */
public abstract class JClassType extends JType implements HasAnnotations,
    HasMetaData {

  /**
   * Returns all of the superclasses and superinterfaces for a given type
   * including the type itself.
   */
  protected static Set<JClassType> getFlattenedSuperTypeHierarchy(
      JClassType type) {
    Set<JClassType> typesSeen = new HashSet<JClassType>();
    getFlattenedSuperTypeHierarchyRecursive(type, typesSeen);
    return typesSeen;
  }

  /**
   * Returns the {@link JGenericType} base type if the otherType is raw or
   * parameterized type.
   */
  protected static JClassType maybeGetGenericBaseType(JClassType otherType) {
    if (otherType.isParameterized() != null) {
      return otherType.isParameterized().getBaseType();
    } else if (otherType.isRawType() != null) {
      return otherType.isRawType().getGenericType();
    }
     
    return otherType;
  }

  private static void getFlattenedSuperTypeHierarchyRecursive(JClassType type,
      Set<JClassType> typesSeen) {
    if (typesSeen.contains(type)) {
      return;
    }
    typesSeen.add(type);

    // Superclass
    JClassType superclass = type.getSuperclass();
    if (superclass != null) {
      getFlattenedSuperTypeHierarchyRecursive(superclass, typesSeen);
    }

    // Check the interfaces
    JClassType[] intfs = type.getImplementedInterfaces();
    for (JClassType intf : intfs) {
      getFlattenedSuperTypeHierarchyRecursive(intf, typesSeen);
    }
  }
  
  public abstract void addImplementedInterface(JClassType intf);

  public abstract void addMetaData(String tagName, String[] values);

  public abstract void addModifierBits(int bits);

  public abstract JConstructor findConstructor(JType[] paramTypes);

  public abstract JField findField(String name);

  public abstract JMethod findMethod(String name, JType[] paramTypes);

  public abstract JClassType findNestedType(String typeName);

  public abstract <T extends Annotation> T getAnnotation(
      Class<T> annotationClass);

  public abstract Annotation[] getAnnotations();

  public abstract int getBodyEnd();

  public abstract int getBodyStart();

  public abstract CompilationUnitProvider getCompilationUnit();

  public abstract JConstructor getConstructor(JType[] paramTypes)
      throws NotFoundException;

  public abstract JConstructor[] getConstructors();

  public abstract Annotation[] getDeclaredAnnotations();

  public abstract JClassType getEnclosingType();

  public abstract JClassType getErasedType();

  public abstract JField getField(String name);

  public abstract JField[] getFields();

  public abstract JClassType[] getImplementedInterfaces();

  public abstract String[][] getMetaData(String tagName);

  public abstract String[] getMetaDataTags();

  public abstract JMethod getMethod(String name, JType[] paramTypes)
      throws NotFoundException;

  /*
   * Returns the declared methods of this class (not any superclasses or
   * superinterfaces).
   */
  public abstract JMethod[] getMethods();

  public abstract String getName();

  public abstract JClassType getNestedType(String typeName)
      throws NotFoundException;

  public abstract JClassType[] getNestedTypes();

  public abstract TypeOracle getOracle();

  public abstract JMethod[] getOverloads(String name);

  /**
   * Iterates over the most-derived declaration of each unique overridable
   * method available in the type hierarchy of the specified type, including
   * those found in superclasses and superinterfaces. A method is overridable if
   * it is not <code>final</code> and its accessibility is <code>public</code>,
   * <code>protected</code>, or package protected.
   * 
   * Deferred binding generators often need to generate method implementations;
   * this method offers a convenient way to find candidate methods to implement.
   * 
   * Note that the behavior does not match
   * {@link Class#getMethod(String, Class[])}, which does not return the most
   * derived method in some cases.
   * 
   * @return an array of {@link JMethod} objects representing overridable
   *         methods
   */
  public abstract JMethod[] getOverridableMethods();

  public abstract JPackage getPackage();

  public abstract JClassType[] getSubtypes();

  public abstract JClassType getSuperclass();

  public abstract String getTypeHash() throws UnableToCompleteException;

  public abstract boolean isAbstract();

  public abstract boolean isAnnotationPresent(
      Class<? extends Annotation> annotationClass);

  public abstract boolean isAssignableFrom(JClassType possibleSubtype);

  public abstract boolean isAssignableTo(JClassType possibleSupertype);

  /**
   * Determines if the class can be constructed using a simple <code>new</code>
   * operation. Specifically, the class must
   * <ul>
   * <li>be a class rather than an interface, </li>
   * <li>have either no constructors or a parameterless constructor, and</li>
   * <li>be a top-level class or a static nested class.</li>
   * </ul>
   * 
   * @return <code>true</code> if the type is default instantiable, or
   *         <code>false</code> otherwise
   */
  public abstract boolean isDefaultInstantiable();

  public abstract JGenericType isGenericType();

  @Override
  public abstract JClassType isInterface();

  /**
   * Tests if this type is a local type (within a method).
   * 
   * @return true if this type is a local type, whether it is named or
   *         anonymous.
   */
  public abstract boolean isLocalType();

  /**
   * Tests if this type is contained within another type.
   * 
   * @return true if this type has an enclosing type, false if this type is a
   *         top-level type
   */
  public abstract boolean isMemberType();

  public abstract boolean isPrivate();

  public abstract boolean isProtected();

  public abstract boolean isPublic();

  public abstract boolean isStatic();

  public abstract void setSuperclass(JClassType type);

  @Override
  public String toString() {
    return this.getQualifiedSourceName();
  }

  protected abstract void acceptSubtype(JClassType me);

  protected abstract int getModifierBits();

  protected abstract void getOverridableMethodsOnSuperclassesAndThisClass(
      Map<String, JMethod> methodsBySignature);

  /**
   * Gets the methods declared in interfaces that this type extends. If this
   * type is a class, its own methods are not added. If this type is an
   * interface, its own methods are added. Used internally by
   * {@link #getOverridableMethods()}.
   * 
   * @param methodsBySignature
   */
  protected abstract void getOverridableMethodsOnSuperinterfacesAndMaybeThisInterface(
      Map<String, JMethod> methodsBySignature);

  protected final String makeCompoundName(JClassType type) {
    if (type.getEnclosingType() == null) {
      return type.getSimpleSourceName();
    } else {
      return makeCompoundName(type.getEnclosingType()) + "."
          + type.getSimpleSourceName();
    }
  }

  /**
   * Tells this type's superclasses and superinterfaces about it.
   */
  protected abstract void notifySuperTypesOf(JClassType me);

  protected abstract void removeSubtype(JClassType me);

  abstract void addConstructor(JConstructor ctor);

  abstract void addField(JField field);

  abstract void addMethod(JMethod method);

  abstract void addNestedType(JClassType type);

  abstract JClassType findNestedTypeImpl(String[] typeName, int index);

  @Override
  abstract JClassType getSubstitutedType(JParameterizedType parameterizedType);

  abstract void notifySuperTypes();

  /**
   * Removes references to this instance from all of its super types.
   */
  abstract void removeFromSupertypes();
}
