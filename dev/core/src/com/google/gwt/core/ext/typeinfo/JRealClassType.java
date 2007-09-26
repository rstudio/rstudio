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
import com.google.gwt.dev.util.Util;

import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Type representing a Java class or interface type.
 */
public class JRealClassType extends JClassType {

  private final Set<JClassType> allSubtypes = new HashSet<JClassType>();

  private final Annotations annotations = new Annotations();

  private final int bodyEnd;

  private final int bodyStart;

  private final CompilationUnitProvider cup;

  private final JPackage declaringPackage;

  private final int declEnd;

  private final int declStart;

  private final JClassType enclosingType;

  private final List<JClassType> interfaces = new ArrayList<JClassType>();

  private final boolean isInterface;

  private final boolean isLocalType;

  private String lazyHash;

  private String lazyQualifiedName;

  private final Members members = new Members(this);

  private final HasMetaData metaData = new MetaData();

  private int modifierBits;

  private final String name;

  private final String nestedName;

  private final TypeOracle oracle;

  private JClassType superclass;

  public JRealClassType(TypeOracle oracle, CompilationUnitProvider cup,
      JPackage declaringPackage, JClassType enclosingType, boolean isLocalType,
      String name, int declStart, int declEnd, int bodyStart, int bodyEnd,
      boolean isInterface) {
    oracle.recordTypeInCompilationUnit(cup, this);
    this.oracle = oracle;
    this.cup = cup;
    this.declaringPackage = declaringPackage;
    this.enclosingType = enclosingType;
    this.isLocalType = isLocalType;
    this.name = name;
    this.declStart = declStart;
    this.declEnd = declEnd;
    this.bodyStart = bodyStart;
    this.bodyEnd = bodyEnd;
    this.isInterface = isInterface;
    if (enclosingType == null) {
      // Add myself to my package.
      //
      declaringPackage.addType(this);
      // The nested name of a top-level class is its simple name.
      //
      nestedName = name;
    } else {
      // Add myself to my enclosing type.
      //
      enclosingType.addNestedType(this);
      // Compute my "nested name".
      //
      JClassType enclosing = enclosingType;
      String nn = name;
      do {
        nn = enclosing.getSimpleSourceName() + "." + nn;
        enclosing = enclosing.getEnclosingType();
      } while (enclosing != null);
      nestedName = nn;
    }
  }

  public void addAnnotations(
      Map<Class<? extends Annotation>, Annotation> declaredAnnotations) {
    annotations.addAnnotations(declaredAnnotations);
  }

  public void addImplementedInterface(JClassType intf) {
    assert (intf != null);
    interfaces.add(intf);
  }

  public void addMetaData(String tagName, String[] values) {
    metaData.addMetaData(tagName, values);
  }

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

  public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
    return annotations.getAnnotation(annotationClass);
  }

  public Annotation[] getAnnotations() {
    return annotations.getAnnotations();
  }

  public int getBodyEnd() {
    return bodyEnd;
  }

  public int getBodyStart() {
    return bodyStart;
  }

  public CompilationUnitProvider getCompilationUnit() {
    return cup;
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

  public Annotation[] getDeclaredAnnotations() {
    return annotations.getDeclaredAnnotations();
  }

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

  public JClassType[] getImplementedInterfaces() {
    return interfaces.toArray(TypeOracle.NO_JCLASSES);
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

  public String[][] getMetaData(String tagName) {
    return metaData.getMetaData(tagName);
  }

  public String[] getMetaDataTags() {
    return metaData.getMetaDataTags();
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

  public JPackage getPackage() {
    return declaringPackage;
  }

  @Override
  public String getQualifiedSourceName() {
    if (lazyQualifiedName == null) {
      JPackage pkg = getPackage();
      if (!pkg.isDefault()) {
        lazyQualifiedName = pkg.getName() + "." + makeCompoundName(this);
      } else {
        lazyQualifiedName = makeCompoundName(this);
      }
    }
    return lazyQualifiedName;
  }

  @Override
  public String getSimpleSourceName() {
    return name;
  }

  public JClassType[] getSubtypes() {
    return allSubtypes.toArray(TypeOracle.NO_JCLASSES);
  }

  public JClassType getSuperclass() {
    return superclass;
  }

  public String getTypeHash() throws UnableToCompleteException {
    if (lazyHash == null) {
      char[] source = cup.getSource();
      int length = declEnd - declStart + 1;
      String s = new String(source, declStart, length);
      try {
        lazyHash = Util.computeStrongName(s.getBytes(Util.DEFAULT_ENCODING));
      } catch (UnsupportedEncodingException e) {
        // Details, details...
        throw new UnableToCompleteException();
      }
    }
    return lazyHash;
  }

  public boolean isAbstract() {
    return 0 != (modifierBits & TypeOracle.MOD_ABSTRACT);
  }

  public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
    return annotations.isAnnotationPresent(annotationClass);
  }

  @Override
  public JArrayType isArray() {
    // intentional null
    return null;
  }

  public boolean isAssignableFrom(JClassType possibleSubtype) {
    if (possibleSubtype == this) {
      return true;
    }
    if (allSubtypes.contains(possibleSubtype)) {
      return true;
    } else if (this == getOracle().getJavaLangObject()) {
      // This case handles the odd "every interface is an Object"
      // but doesn't actually have Object as a superclass.
      //
      return true;
    } else {
      return false;
    }
  }

  public boolean isAssignableTo(JClassType possibleSupertype) {
    return possibleSupertype.isAssignableFrom(this);
  }

  @Override
  public JClassType isClass() {
    return isInterface ? null : this;
  }

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
  public boolean isDefaultInstantiable() {
    if (isInterface() != null) {
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
  public JGenericType isGenericType() {
    return null;
  }

  @Override
  public JClassType isInterface() {
    return isInterface ? this : null;
  }

  /**
   * Tests if this type is a local type (within a method).
   * 
   * @return true if this type is a local type, whether it is named or
   *         anonymous.
   */
  public boolean isLocalType() {
    return isLocalType;
  }

  /**
   * Tests if this type is contained within another type.
   * 
   * @return true if this type has an enclosing type, false if this type is a
   *         top-level type
   */
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

  public boolean isPrivate() {
    return 0 != (modifierBits & TypeOracle.MOD_PRIVATE);
  }

  public boolean isProtected() {
    return 0 != (modifierBits & TypeOracle.MOD_PROTECTED);
  }

  public boolean isPublic() {
    return 0 != (modifierBits & TypeOracle.MOD_PUBLIC);
  }

  @Override
  public JRawType isRawType() {
    // TODO Override in JGenericType?
    return null;
  }

  public boolean isStatic() {
    return 0 != (modifierBits & TypeOracle.MOD_STATIC);
  }

  public void setSuperclass(JClassType type) {
    assert (type != null);
    assert (isInterface() == null);
    this.superclass = type;
    JRealClassType realSuperType;
    if (type.isParameterized() != null) {
      realSuperType = type.isParameterized().getBaseType();
    } else if (type.isRawType() != null) {
      realSuperType = type.isRawType().getGenericType();
    } else {
      realSuperType = (JRealClassType) type;
    }
    annotations.setParent(realSuperType.annotations);
  }

  @Override
  public String toString() {
    if (isInterface) {
      return "interface " + getQualifiedSourceName();
    } else {
      return "class " + getQualifiedSourceName();
    }
  }

  protected void acceptSubtype(JClassType me) {
    // TODO(scottb): revisit
    allSubtypes.add(me);
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

  protected int getModifierBits() {
    return modifierBits;
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

  /**
   * Tells this type's superclasses and superinterfaces about it.
   */
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

  protected void removeSubtype(JClassType me) {
    // TODO(scottb): revisit
    allSubtypes.remove(me);

    if (superclass != null) {
      superclass.removeSubtype(me);
    }

    for (int i = 0, n = interfaces.size(); i < n; ++i) {
      JClassType intf = interfaces.get(i);

      intf.removeSubtype(me);
    }
  }

  void notifySuperTypes() {
    notifySuperTypesOf(this);
  }

  /**
   * Removes references to this instance from all of its super types.
   */
  void removeFromSupertypes() {
    removeSubtype(this);
  }

}
