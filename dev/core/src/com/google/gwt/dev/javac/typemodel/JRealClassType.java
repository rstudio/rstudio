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
import com.google.gwt.dev.util.StringInterner;
import com.google.gwt.dev.util.collect.IdentitySets;
import com.google.gwt.dev.util.collect.Lists;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Type representing a Java class or interface type that a user would declare.
 */
public class JRealClassType extends JClassType implements
    com.google.gwt.core.ext.typeinfo.JRealClassType {

  private Set<JClassType> allSubtypes = IdentitySets.create();

  private final Annotations annotations = new Annotations();

  private final JPackage declaringPackage;

  /**
   * Set when this class is resolved, then never modified.
   */
  private JClassType enclosingType;

  private List<JClassType> interfaces = Lists.create();

  private final boolean isInterface;

  private String lazyQualifiedBinaryName;

  private String lazyQualifiedName;

  private final Members members = new Members(this);

  private int modifierBits;

  private final String name;

  private final String nestedName;

  private final TypeOracle oracle;

  private JClassType superclass;

  private long lastModifiedTime;

  /**
   * Create a class type that reflects an actual type.
   * 
   * @param oracle
   * @param declaringPackage
   * @param enclosingTypeName the fully qualified source name of the enclosing
   *          class or null if a top-level class - setEnclosingType must be
   *          called later with the proper enclosing type if this is non-null
   * @param name
   * @param isInterface
   */
  JRealClassType(TypeOracle oracle, JPackage declaringPackage, String enclosingTypeName,
      String name, boolean isInterface) {
    this.oracle = oracle;
    this.declaringPackage = declaringPackage;
    this.name = StringInterner.get().intern(name);
    this.isInterface = isInterface;
    if (enclosingTypeName == null) {
      // Add myself to my package.
      //
      declaringPackage.addType(this);
      // The nested name of a top-level class is its simple name.
      //
      nestedName = name;
    } else {
      // Compute my "nested name".
      //
      nestedName = enclosingTypeName + "." + name;

      // We will add ourselves to the enclosing class when it is set in
      // setEnclosingType().
    }
    oracle.addNewType(this);
  }

  public void addLastModifiedTime(long lastModifiedTime) {
    this.lastModifiedTime = lastModifiedTime;
  }

  @Override
  public void addModifierBits(int bits) {
    modifierBits |= bits;
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
  public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
    return annotations.getAnnotation(annotationClass);
  }

  @Override
  public Annotation[] getAnnotations() {
    return annotations.getAnnotations();
  }

  @Override
  public JConstructor getConstructor(JType[] paramTypes) throws NotFoundException {
    return members.getConstructor(paramTypes);
  }

  @Override
  public JConstructor[] getConstructors() {
    return members.getConstructors();
  }

  @Override
  public Annotation[] getDeclaredAnnotations() {
    return annotations.getDeclaredAnnotations();
  }

  @Override
  public JClassType getEnclosingType() {
    return enclosingType;
  }

  @Override
  public JClassType getErasedType() {
    return this;
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
  public JClassType[] getImplementedInterfaces() {
    return interfaces.toArray(TypeOracle.NO_JCLASSES);
  }

  @Override
  public JMethod[] getInheritableMethods() {
    return members.getInheritableMethods();
  }

  @Override
  public String getJNISignature() {
    String typeName = nestedName.replace('.', '$');
    String packageName = getPackage().getName().replace('.', '/');
    if (packageName.length() > 0) {
      packageName += "/";
    }
    return "L" + packageName + typeName + ";";
  }

  @Override
  public long getLastModifiedTime() {
    return lastModifiedTime;
  }

  @Override
  public JMethod getMethod(String name, JType[] paramTypes) throws NotFoundException {
    return members.getMethod(name, paramTypes);
  }

  @Override
  public JMethod[] getMethods() {
    return members.getMethods();
  }

  @Override
  public String getName() {
    return nestedName;
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
  public TypeOracle getOracle() {
    return oracle;
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
  public JPackage getPackage() {
    return declaringPackage;
  }

  @Override
  public String getQualifiedBinaryName() {
    if (lazyQualifiedBinaryName == null) {
      lazyQualifiedBinaryName = "";
      JPackage pkg = getPackage();
      if (!pkg.isDefault()) {
        lazyQualifiedBinaryName = pkg.getName() + ".";
      }
      lazyQualifiedBinaryName += nestedName.replace('.', '$');
    }
    return lazyQualifiedBinaryName;
  }

  @Override
  public String getQualifiedSourceName() {
    if (lazyQualifiedName == null) {
      JPackage pkg = getPackage();
      if (!pkg.isDefault()) {
        lazyQualifiedName = pkg.getName() + "." + nestedName;
      } else {
        lazyQualifiedName = nestedName;
      }
      lazyQualifiedName = StringInterner.get().intern(lazyQualifiedName);
    }
    return lazyQualifiedName;
  }

  @Override
  public String getSimpleSourceName() {
    return name;
  }

  @Override
  public JClassType[] getSubtypes() {
    return allSubtypes.toArray(TypeOracle.NO_JCLASSES);
  }

  @Override
  public JClassType getSuperclass() {
    return superclass;
  }

  @Override
  public boolean isAbstract() {
    return 0 != (modifierBits & TypeOracle.MOD_ABSTRACT);
  }

  @Override
  public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
    return annotations.isAnnotationPresent(annotationClass);
  }

  @Override
  public JArrayType isArray() {
    // intentional null
    return null;
  }

  @Override
  public JClassType isClass() {
    return isInterface ? null : this;
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
  public boolean isDefaultInstantiable() {
    if (isInterface() != null || isAbstract()) {
      return false;
    }
    if (isMemberType() && !isStatic()) {
      return false;
    }
    if (getConstructors().length == 0) {
      return true;
    }
    JConstructor ctor = findConstructor(TypeOracle.NO_JTYPES);
    if (ctor != null) {
      return true;
    }
    return false;
  }

  @Override
  public JEnumType isEnum() {
    return null;
  }

  @Override
  public boolean isFinal() {
    return 0 != (getModifierBits() & TypeOracle.MOD_FINAL);
  }

  @Override
  public JGenericType isGenericType() {
    return null;
  }

  @Override
  public JClassType isInterface() {
    return isInterface ? this : null;
  }

  /**
   * Tests if this type is contained within another type.
   * 
   * @return true if this type has an enclosing type, false if this type is a
   *         top-level type
   */
  @Override
  public boolean isMemberType() {
    return enclosingType != null;
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
    return 0 != (modifierBits & TypeOracle.MOD_PRIVATE);
  }

  @Override
  public boolean isProtected() {
    return 0 != (modifierBits & TypeOracle.MOD_PROTECTED);
  }

  @Override
  public boolean isPublic() {
    return 0 != (modifierBits & TypeOracle.MOD_PUBLIC);
  }

  @Override
  public JRawType isRawType() {
    // TODO Override in JGenericType?
    return null;
  }

  @Override
  public boolean isStatic() {
    return 0 != (modifierBits & TypeOracle.MOD_STATIC);
  }

  @Override
  public JWildcardType isWildcard() {
    return null;
  }

  @Override
  public String toString() {
    if (isInterface) {
      return "interface " + getQualifiedSourceName();
    } else {
      return "class " + getQualifiedSourceName();
    }
  }

  @Override
  protected void acceptSubtype(JClassType me) {
    allSubtypes = IdentitySets.add(allSubtypes, me);
    notifySuperTypesOf(me);
  }

  @Override
  protected void addConstructor(JConstructor ctor) {
    members.addConstructor(ctor);
  }

  @Override
  protected void addField(JField field) {
    members.addField(field);
  }

  @Override
  protected void addMethod(JMethod method) {
    members.addMethod(method);
  }

  @Override
  protected void addNestedType(JClassType type) {
    members.addNestedType(type);
  }

  @Override
  protected JClassType findNestedTypeImpl(String[] typeName, int index) {
    return members.findNestedTypeImpl(typeName, index);
  }

  @Override
  protected void getInheritableMethodsOnSuperclassesAndThisClass(
      Map<String, JMethod> methodsBySignature) {
    members.getInheritableMethodsOnSuperclassesAndThisClass(methodsBySignature);
  }

  /**
   * Gets the methods declared in interfaces that this type extends. If this
   * type is a class, its own methods are not added. If this type is an
   * interface, its own methods are added. Used internally by
   * {@link #getOverridableMethods()}.
   * 
   * @param methodsBySignature
   */
  @Override
  protected void getInheritableMethodsOnSuperinterfacesAndMaybeThisInterface(
      Map<String, JMethod> methodsBySignature) {
    members.getInheritableMethodsOnSuperinterfacesAndMaybeThisInterface(methodsBySignature);
  }

  @Override
  protected int getModifierBits() {
    return modifierBits;
  }

  /**
   * Tells this type's superclasses and superinterfaces about it.
   */
  @Override
  protected void notifySuperTypesOf(JClassType me) {
    // TODO(scottb): revisit
    if (superclass != null) {
      superclass.acceptSubtype(me);
    }
    for (int i = 0, n = interfaces.size(); i < n; ++i) {
      JClassType intf = interfaces.get(i);
      intf.acceptSubtype(me);
    }
  }

  @Override
  protected void removeSubtype(JClassType me) {
    allSubtypes = IdentitySets.remove(allSubtypes, me);

    if (superclass != null) {
      superclass.removeSubtype(me);
    }

    for (int i = 0, n = interfaces.size(); i < n; ++i) {
      JClassType intf = interfaces.get(i);

      intf.removeSubtype(me);
    }
  }

  void addAnnotations(Map<Class<? extends Annotation>, Annotation> declaredAnnotations) {
    annotations.addAnnotations(declaredAnnotations);
  }

  @Override
  void addImplementedInterface(JClassType intf) {
    assert (intf != null);
    interfaces = Lists.add(interfaces, intf);
  }

  @Override
  JRealClassType getSubstitutedType(JParameterizedType parameterizedType) {
    return this;
  }

  @Override
  void notifySuperTypes() {
    notifySuperTypesOf(this);
  }

  /**
   * Removes references to this instance from all of its super types.
   */
  @Override
  void removeFromSupertypes() {
    removeSubtype(this);
  }

  void setEnclosingType(JClassType enclosingType) {
    assert this.enclosingType == null;
    assert enclosingType != null;

    this.enclosingType = enclosingType;

    // Add myself to my enclosing type.
    JRawType rawType = enclosingType.isRawType();
    if (rawType != null) {
      enclosingType = rawType.getGenericType();
    }
    enclosingType.addNestedType(this);
  }

  @Override
  void setSuperclass(JClassType type) {
    assert (type != null);
    assert (isInterface() == null);
    this.superclass = type;
    JRealClassType realSuperType;
    if (type.isParameterized() != null) {
      realSuperType = type.isParameterized().getBaseType();
    } else if (type.isRawType() != null) {
      realSuperType = type.isRawType().getGenericType();
    } else if (type instanceof JRealClassType) {
      realSuperType = (JRealClassType) type;
    } else {
      throw new IllegalArgumentException("Unknown type for " + type);
    }
    annotations.setParent(realSuperType.annotations);
  }
}
