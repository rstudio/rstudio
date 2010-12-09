/*
 * Copyright 2010 Google Inc.
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

import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * Type used to represent any non-primitive type.
 */
@SuppressWarnings("deprecation")
public interface JClassType extends JType, HasAnnotations, HasMetaData {

  JParameterizedType asParameterizationOf(JGenericType type);

  /**
   * Find an annotation on a type or on one of its superclasses or
   * superinterfaces.
   * <p>
   * This provides semantics similar to that of
   * {@link java.lang.annotation.Inherited} except that it checks all types to
   * which this type is assignable. {@code @Inherited} only works on
   * superclasses, not superinterfaces.
   * <p>
   * Annotations present on the superclass chain will be returned preferentially
   * over those found in the superinterface hierarchy. Note that the annotation
   * does not need to be tagged with {@code @Inherited} in order to be returned
   * from the superclass chain.
   * 
   * @param annotationType the type of the annotation to look for
   * @return the desired annotation or <code>null</code> if the annotation is
   *         not present in the type's type hierarchy
   */
  <T extends Annotation> T findAnnotationInTypeHierarchy(Class<T> annotationType);

  JConstructor findConstructor(JType[] paramTypes);

  JField findField(String name);

  JMethod findMethod(String name, JType[] paramTypes);

  JClassType findNestedType(String typeName);

  JConstructor getConstructor(JType[] paramTypes) throws NotFoundException;

  JConstructor[] getConstructors();

  JClassType getEnclosingType();

  JClassType getErasedType();

  JField getField(String name);

  JField[] getFields();

  /**
   * Returns all of the superclasses and superinterfaces for a given type
   * including the type itself. The returned set maintains an internal
   * breadth-first ordering of the type, followed by its interfaces (and their
   * super-interfaces), then the supertype and its interfaces, and so on.
   */
  Set<? extends JClassType> getFlattenedSupertypeHierarchy();

  JClassType[] getImplementedInterfaces();

  /**
   * Iterates over the most-derived declaration of each unique inheritable
   * method available in the type hierarchy of the specified type, including
   * those found in superclasses and superinterfaces. A method is inheritable if
   * its accessibility is <code>public</code>, <code>protected</code>, or
   * package protected.
   * 
   * This method offers a convenient way for Generators to find candidate
   * methods to call from a subclass.
   * 
   * @return an array of {@link JMethod} objects representing inheritable
   *         methods
   */
  JMethod[] getInheritableMethods();

  JMethod getMethod(String name, JType[] paramTypes) throws NotFoundException;

  /*
   * Returns the declared methods of this class (not any superclasses or
   * superinterfaces).
   */
  JMethod[] getMethods();

  String getName();

  JClassType getNestedType(String typeName) throws NotFoundException;

  JClassType[] getNestedTypes();

  TypeOracle getOracle();

  JMethod[] getOverloads(String name);

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
  JMethod[] getOverridableMethods();

  JPackage getPackage();

  JClassType[] getSubtypes();

  JClassType getSuperclass();

  boolean isAbstract();

  /**
   * Returns <code>true</code> if this {@link JClassType} is assignable from the
   * specified {@link JClassType} parameter.
   * 
   * @param possibleSubtype possible subtype of this {@link JClassType}
   * @return <code>true</code> if this {@link JClassType} is assignable from the
   *         specified {@link JClassType} parameter
   * 
   * @throws NullPointerException if <code>possibleSubtype</code> is
   *           <code>null</code>
   */
  boolean isAssignableFrom(JClassType possibleSubtype);

  /**
   * Returns <code>true</code> if this {@link JClassType} is assignable to the
   * specified {@link JClassType} parameter.
   * 
   * @param possibleSupertype possible supertype of this {@link JClassType}
   * @return <code>true</code> if this {@link JClassType} is assignable to the
   *         specified {@link JClassType} parameter
   * 
   * @throws NullPointerException if <code>possibleSupertype</code> is
   *           <code>null</code>
   */
  boolean isAssignableTo(JClassType possibleSupertype);

  /**
   * Determines if the class can be constructed using a simple <code>new</code>
   * operation. Specifically, the class must
   * <ul>
   * <li>be a class rather than an interface,</li>
   * <li>have either no constructors or a parameterless constructor, and</li>
   * <li>be a top-level class or a static nested class.</li>
   * </ul>
   * 
   * @return <code>true</code> if the type is default instantiable, or
   *         <code>false</code> otherwise
   */
  boolean isDefaultInstantiable();

  /**
   * Returns true if the type may be enhanced on the server to contain extra
   * fields that are unknown to client code.
   * 
   * @return <code>true</code> if the type might be enhanced on the server
   */
  boolean isEnhanced();

  boolean isFinal();

  /**
   * @deprecated local types are not modeled
   */
  @Deprecated
  boolean isLocalType();

  /**
   * Tests if this type is contained within another type.
   * 
   * @return true if this type has an enclosing type, false if this type is a
   *         top-level type
   */
  boolean isMemberType();

  boolean isPrivate();

  boolean isProtected();

  boolean isPublic();

  boolean isStatic();

  /**
   * Indicates that the type may be enhanced on the server to contain extra
   * fields that are unknown to client code.
   * 
   * TODO(rice): find a better way to do this.
   */
  void setEnhanced();
}
