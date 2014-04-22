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
import com.google.gwt.dev.util.collect.HashSet;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Type used to represent any non-primitive type.
 */
public abstract class JClassType implements
    com.google.gwt.core.ext.typeinfo.JClassType {

  /**
   * Returns all of the superclasses and superinterfaces for a given type
   * including the type itself. The returned set maintains an internal
   * breadth-first ordering of the type, followed by its interfaces (and their
   * super-interfaces), then the supertype and its interfaces, and so on.
   */
  protected static Set<JClassType> getFlattenedSuperTypeHierarchy(
      JClassType type) {
    Set<JClassType> flattened = type.flattenedSupertypes;
    if (flattened == null) {
      flattened = new LinkedHashSet<JClassType>();
      getFlattenedSuperTypeHierarchyRecursive(type, flattened);
      // flattened.size() > 1 for all types other than Object
      type.flattenedSupertypes = Collections.unmodifiableSet(flattened);
    }
    return flattened;
  }

  private static void getFlattenedSuperTypeHierarchyRecursive(JClassType type,
      Set<JClassType> typesSeen) {
    if (typesSeen.contains(type)) {
      return;
    }
    typesSeen.add(type);

    // Check the interfaces
    JClassType[] intfs = type.getImplementedInterfaces();
    for (JClassType intf : intfs) {
      typesSeen.addAll(getFlattenedSuperTypeHierarchy(intf));
    }

    // Superclass
    JClassType superclass = type.getSuperclass();
    if (superclass != null) {
      typesSeen.addAll(getFlattenedSuperTypeHierarchy(superclass));
    }
  }

  /**
   * Cached set of supertypes for this type (including itself). If null, the set
   * has not been calculated yet.
   */
  private Set<JClassType> flattenedSupertypes;

  /**
   * True if this type may be enhanced with server-only fields. This property is
   * 'sticky' and may be set but not unset, since we need to generate the
   * relevant RPC code for handling the server fields if there is any chance the
   * class will be enhanced.
   */
  private boolean isEnhanced = false;

  @Override
  public JParameterizedType asParameterizationOf(
      com.google.gwt.core.ext.typeinfo.JGenericType type) {
    Set<JClassType> supertypes = getFlattenedSuperTypeHierarchy(this);
    for (JClassType supertype : supertypes) {
      JParameterizedType isParameterized = supertype.isParameterized();
      if (isParameterized != null && isParameterized.getBaseType() == type) {
        return isParameterized;
      }

      JRawType isRaw = supertype.isRawType();
      if (isRaw != null && isRaw.getBaseType() == type) {
        return isRaw.asParameterizedByWildcards();
      }
    }

    return null;
  }

  /**
   * All types use identity for comparison.
   */
  @Override
  public final boolean equals(Object obj) {
    return super.equals(obj);
  }

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
  @Override
  public <T extends Annotation> T findAnnotationInTypeHierarchy(
      Class<T> annotationType) {

    // Remember what we've seen to avoid loops
    Set<JClassType> seen = new HashSet<JClassType>();

    // Work queue
    List<JClassType> searchTypes = new LinkedList<JClassType>();
    searchTypes.add(this);

    T toReturn = null;

    while (!searchTypes.isEmpty()) {
      JClassType current = searchTypes.remove(0);

      if (!seen.add(current)) {
        continue;
      }

      toReturn = current.getAnnotation(annotationType);
      if (toReturn != null) {
        /*
         * First one wins. It might be desirable at some point to have a
         * variation that can return more than one instance of the annotation if
         * it is present on multiple supertypes.
         */
        break;
      }

      if (current.getSuperclass() != null) {
        // Add the superclass at the front of the list
        searchTypes.add(0, current.getSuperclass());
      }

      // Superinterfaces
      Collections.addAll(searchTypes, current.getImplementedInterfaces());
    }

    return toReturn;
  }

  @Override
  public abstract JConstructor findConstructor(JType[] paramTypes);

  @Override
  public abstract JField findField(String name);

  @Override
  public abstract JMethod findMethod(String name, JType[] paramTypes);

  @Override
  public abstract JClassType findNestedType(String typeName);

  @Override
  public abstract <T extends Annotation> T getAnnotation(
      Class<T> annotationClass);

  @Override
  public abstract Annotation[] getAnnotations();

  @Override
  public abstract JConstructor getConstructor(JType[] paramTypes)
      throws NotFoundException;

  @Override
  public abstract JConstructor[] getConstructors();

  @Override
  public abstract Annotation[] getDeclaredAnnotations();

  @Override
  public abstract JClassType getEnclosingType();

  @Override
  public abstract JClassType getErasedType();

  @Override
  public abstract JField getField(String name);

  @Override
  public abstract JField[] getFields();

  /**
   * Returns all of the superclasses and superinterfaces for a given type
   * including the type itself. The returned set maintains an internal
   * breadth-first ordering of the type, followed by its interfaces (and their
   * super-interfaces), then the supertype and its interfaces, and so on.
   */
  @Override
  public Set<JClassType> getFlattenedSupertypeHierarchy() {
    // Retuns an immutable set
    return getFlattenedSuperTypeHierarchy(this);
  }

  @Override
  public abstract JClassType[] getImplementedInterfaces();

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
  @Override
  public abstract JMethod[] getInheritableMethods();

  @Override
  public abstract String getJNISignature();

  @Override
  public JType getLeafType() {
    return this;
  }

  @Override
  @Deprecated
  public final String[][] getMetaData(String tagName) {
    return TypeOracle.NO_STRING_ARR_ARR;
  }

  @Override
  @Deprecated
  public final String[] getMetaDataTags() {
    return TypeOracle.NO_STRINGS;
  }

  @Override
  public abstract JMethod getMethod(String name, JType[] paramTypes)
      throws NotFoundException;

  /*
   * Returns the declared methods of this class (not any superclasses or
   * superinterfaces).
   */
  @Override
  public abstract JMethod[] getMethods();

  @Override
  public abstract String getName();

  @Override
  public abstract JClassType getNestedType(String typeName)
      throws NotFoundException;

  @Override
  public abstract JClassType[] getNestedTypes();

  @Override
  public abstract TypeOracle getOracle();

  @Override
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
  @Override
  public abstract JMethod[] getOverridableMethods();

  @Override
  public abstract JPackage getPackage();

  @Override
  public String getParameterizedQualifiedSourceName() {
    return getQualifiedSourceName();
  }

  /**
   * TODO(scottb): remove if we can resolve param names differently.
   */
  @Override
  public abstract String getQualifiedBinaryName();

  @Override
  public abstract String getQualifiedSourceName();

  @Override
  public abstract String getSimpleSourceName();

  /**
   * Returns all subtypes for this type.  This means various things:
   * <ol>
   *   <li>Array: subtypes are those array types with the same number of dimensions as this type,
   *   and whose base element type is a subtype of this type's element type.</li>
   *   <li>Wildcards: if ? extends X, subtypes are subtypes of X.  If ? super X: no subtypes.
   *   <li>Named type parameter: subtypes are (1) those subtypes of the "base" type that are
   *   assignable to this, plus (2) the first bound of the parameter, if the bound is both
   *   assignable to this and not an interface.  The "base" is the type itself, except for
   *   delegating types, where the base is the result of <code>getBaseType()</code>.</li>
   *   <li>Real class: subtypes are those real classes that are subtypes of this class in the
   *   class hierarchy.  Interfaces aren't subtypes of {@code Object}.</li>
   *   <li>Parameterized type: subtypes are those subtypes of the generic type of this type which
   *   can be parameterized with the right types so that they have this type as a supertype.
   *   The subtypes are parameterized as described, with minimal parameterization.</li>
   *   <li>Raw type: subtypes are the subtypes of the generic type of this type, as raw types if
   *   they themselves are generic.</li>
   * </ol>
   */
  @Override
  public abstract JClassType[] getSubtypes();

  @Override
  public abstract JClassType getSuperclass();

  /**
   * All types use identity for comparison.
   */
  @Override
  public final int hashCode() {
    return super.hashCode();
  }

  @Override
  public abstract boolean isAbstract();

  /**
   * Returns this instance if it is a annotation or <code>null</code> if it is
   * not.
   *
   * @return this instance if it is a annotation or <code>null</code> if it is
   *         not
   */
  @Override
  public JAnnotationType isAnnotation() {
    return null;
  }

  @Override
  public abstract boolean isAnnotationPresent(
      Class<? extends Annotation> annotationClass);

  @Override
  public abstract JArrayType isArray();

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
  @Override
  public boolean isAssignableFrom(
      com.google.gwt.core.ext.typeinfo.JClassType possibleSubtype) {
    if (possibleSubtype == null) {
      throw new NullPointerException("possibleSubtype");
    }

    return new AssignabilityChecker().isAssignable((JClassType) possibleSubtype, this);
  }

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
  @Override
  public boolean isAssignableTo(
      com.google.gwt.core.ext.typeinfo.JClassType possibleSupertype) {
    if (possibleSupertype == null) {
      throw new NullPointerException("possibleSupertype");
    }

    return new AssignabilityChecker().isAssignable(this, (JClassType) possibleSupertype);
  }

  @Override
  public abstract JClassType isClass();

  @Override
  public JClassType isClassOrInterface() {
    JClassType type = isClass();
    if (type != null) {
      return type;
    }
    return isInterface();
  }

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
  @Override
  public abstract boolean isDefaultInstantiable();

  /**
   * Returns true if the type may be enhanced on the server to contain extra
   * fields that are unknown to client code.
   *
   * @return <code>true</code> if the type might be enhanced on the server
   */
  @Override
  public final boolean isEnhanced() {
    return isEnhanced;
  }

  /**
   * Returns this instance if it is an enumeration or <code>null</code> if it is
   * not.
   *
   * @return this instance if it is an enumeration or <code>null</code> if it is
   *         not
   */
  @Override
  public abstract JEnumType isEnum();

  @Override
  public abstract boolean isFinal();

  @Override
  public abstract JGenericType isGenericType();

  @Override
  public abstract JClassType isInterface();

  /**
   * @deprecated local types are not modeled
   */
  @Override
  @Deprecated
  public final boolean isLocalType() {
    return false;
  }

  /**
   * Tests if this type is contained within another type.
   *
   * @return true if this type has an enclosing type, false if this type is a
   *         top-level type
   */
  @Override
  public abstract boolean isMemberType();

  @Override
  public abstract JParameterizedType isParameterized();

  @Override
  public abstract JPrimitiveType isPrimitive();

  @Override
  public abstract boolean isPrivate();

  @Override
  public abstract boolean isProtected();

  @Override
  public abstract boolean isPublic();

  @Override
  public boolean isPackageProtected() {
    return !isPrivate() && !isPublic() && !isProtected();
  }

  // TODO: Rename this to isRaw
  @Override
  public abstract JRawType isRawType();

  @Override
  public abstract boolean isStatic();

  @Override
  public JTypeParameter isTypeParameter() {
    return null;
  }

  @Override
  public abstract JWildcardType isWildcard();

  /**
   * Indicates that the type may be enhanced on the server to contain extra
   * fields that are unknown to client code.
   *
   * TODO(rice): find a better way to do this.
   */
  @Override
  public void setEnhanced() {
    this.isEnhanced = true;
  }

  @Override
  public String toString() {
    return this.getQualifiedSourceName();
  }

  protected abstract void acceptSubtype(JClassType me);

  protected abstract void getInheritableMethodsOnSuperclassesAndThisClass(
      Map<String, JMethod> methodsBySignature);

  /**
   * Gets the methods declared in interfaces that this type extends. If this
   * type is a class, its own methods are not added. If this type is an
   * interface, its own methods are added. Used internally by
   * {@link #getOverridableMethods()}.
   *
   * @param methodsBySignature
   */
  protected abstract void getInheritableMethodsOnSuperinterfacesAndMaybeThisInterface(
      Map<String, JMethod> methodsBySignature);

  protected abstract int getModifierBits();

  protected JMaybeParameterizedType isMaybeParameterizedType() {
    return null;
  }

  /**
   * Tells this type's superclasses and superinterfaces about it.
   */
  protected abstract void notifySuperTypesOf(JClassType me);

  protected abstract void removeSubtype(JClassType me);

  abstract void addConstructor(JConstructor ctor);

  abstract void addField(JField field);

  abstract void addImplementedInterface(JClassType intf);

  abstract void addMethod(JMethod method);

  abstract void addModifierBits(int bits);

  abstract void addNestedType(JClassType type);

  abstract JClassType findNestedTypeImpl(String[] typeName, int index);

  /**
   * Returns either the substitution of this type based on the parameterized
   * type or this instance.
   *
   * @param parameterizedType
   * @return either the substitution of this type based on the parameterized
   *         type or this instance
   */
  abstract JClassType getSubstitutedType(JParameterizedType parameterizedType);

  abstract void notifySuperTypes();

  /**
   * Removes references to this instance from all of its super types.
   */
  abstract void removeFromSupertypes();

  abstract void setSuperclass(JClassType type);
}
