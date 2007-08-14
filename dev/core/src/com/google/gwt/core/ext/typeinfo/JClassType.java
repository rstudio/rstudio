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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Type representing a Java class or interface type.
 */
public class JClassType extends JType implements HasMetaData {
  private final Set allSubtypes = new HashSet();

  private final int bodyEnd;

  private final int bodyStart;

  private JMethod[] cachedOverridableMethods;

  private final List constructors = new ArrayList();

  private final CompilationUnitProvider cup;

  private final JPackage declaringPackage;

  private final int declEnd;

  private final int declStart;

  private final JClassType enclosingType;

  private final Map fields = new HashMap();

  private final List interfaces = new ArrayList();

  private final boolean isInterface;

  private final boolean isLocalType;

  private String lazyHash;

  private String lazyQualifiedName;

  private final HasMetaData metaData = new MetaData();

  private final Map methods = new HashMap();

  private int modifierBits;

  private final String name;

  private final String nestedName;

  private final Map nestedTypes = new HashMap();

  private final TypeOracle oracle;

  private JClassType superclass;

  public JClassType(TypeOracle oracle, CompilationUnitProvider cup,
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

  public JConstructor findConstructor(JType[] paramTypes) {
    JConstructor[] ctors = getConstructors();
    for (int i = 0; i < ctors.length; i++) {
      JConstructor candidate = ctors[i];
      if (candidate.hasParamTypes(paramTypes)) {
        return candidate;
      }
    }
    return null;
  }

  public JField findField(String name) {
    return (JField) fields.get(name);
  }

  public JMethod findMethod(String name, JType[] paramTypes) {
    JMethod[] overloads = getOverloads(name);
    for (int i = 0; i < overloads.length; i++) {
      JMethod candidate = overloads[i];
      if (candidate.hasParamTypes(paramTypes)) {
        return candidate;
      }
    }
    return null;
  }

  public JClassType findNestedType(String typeName) {
    String[] parts = typeName.split("\\.");
    return findNestedTypeImpl(parts, 0);
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

  public JConstructor getConstructor(JType[] paramTypes)
      throws NotFoundException {
    JConstructor result = findConstructor(paramTypes);
    if (result == null) {
      throw new NotFoundException();
    }
    return result;
  }

  public JConstructor[] getConstructors() {
    return (JConstructor[]) constructors.toArray(TypeOracle.NO_JCTORS);
  }

  public JClassType getEnclosingType() {
    return enclosingType;
  }

  public JField getField(String name) {
    JField field = findField(name);
    assert (field != null);
    return field;
  }

  public JField[] getFields() {
    return (JField[]) fields.values().toArray(TypeOracle.NO_JFIELDS);
  }

  public JClassType[] getImplementedInterfaces() {
    return (JClassType[]) interfaces.toArray(TypeOracle.NO_JCLASSES);
  }

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

  public JMethod getMethod(String name, JType[] paramTypes)
      throws NotFoundException {
    JMethod result = findMethod(name, paramTypes);
    if (result == null) {
      throw new NotFoundException();
    }
    return result;
  }

  /*
   * Returns the declared methods of this class (not any superclasses or
   * superinterfaces).
   */
  public JMethod[] getMethods() {
    List resultMethods = new ArrayList();
    for (Iterator iter = methods.values().iterator(); iter.hasNext();) {
      List overloads = (List) iter.next();
      resultMethods.addAll(overloads);
    }
    return (JMethod[]) resultMethods.toArray(TypeOracle.NO_JMETHODS);
  }

  public String getName() {
    return nestedName;
  }

  public JClassType getNestedType(String typeName) throws NotFoundException {
    JClassType result = findNestedType(typeName);
    if (result == null) {
      throw new NotFoundException();
    }
    return result;
  }

  public JClassType[] getNestedTypes() {
    return (JClassType[]) nestedTypes.values().toArray(TypeOracle.NO_JCLASSES);
  }

  public TypeOracle getOracle() {
    return oracle;
  }

  public JMethod[] getOverloads(String name) {
    List resultMethods = (List) methods.get(name);
    if (resultMethods != null) {
      return (JMethod[]) resultMethods.toArray(TypeOracle.NO_JMETHODS);
    } else {
      return TypeOracle.NO_JMETHODS;
    }
  }

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
  public JMethod[] getOverridableMethods() {
    if (cachedOverridableMethods == null) {
      Map methodsBySignature = new TreeMap();
      getOverridableMethodsOnSuperinterfacesAndMaybeThisInterface(methodsBySignature);
      if (isClass() != null) {
        getOverridableMethodsOnSuperclassesAndThisClass(methodsBySignature);
      }
      int size = methodsBySignature.size();
      Collection leafMethods = methodsBySignature.values();
      cachedOverridableMethods = (JMethod[]) leafMethods.toArray(new JMethod[size]);
    }
    return cachedOverridableMethods;
  }

  public JPackage getPackage() {
    return declaringPackage;
  }

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

  public String getSimpleSourceName() {
    return name;
  }

  public JClassType[] getSubtypes() {
    return (JClassType[]) allSubtypes.toArray(TypeOracle.NO_JCLASSES);
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
    if (constructors.isEmpty()) {
      return true;
    }
    JConstructor ctor = findConstructor(TypeOracle.NO_JTYPES);
    if (ctor != null) {
      return true;
    }
    return false;
  }

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

  public JParameterizedType isParameterized() {
    // intentional null
    return null;
  }

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

  public boolean isStatic() {
    return 0 != (modifierBits & TypeOracle.MOD_STATIC);
  }

  public void setSuperclass(JClassType type) {
    assert (type != null);
    assert (isInterface() == null);
    this.superclass = type;
  }

  public String toString() {
    if (isInterface) {
      return "interface " + getQualifiedSourceName();
    } else {
      return "class " + getQualifiedSourceName();
    }
  }

  protected int getModifierBits() {
    return modifierBits;
  }

  void addConstructor(JConstructor ctor) {
    assert (!constructors.contains(ctor));
    constructors.add(ctor);
  }

  void addField(JField field) {
    Object existing = fields.put(field.getName(), field);
    assert (existing == null);
  }

  void addMethod(JMethod method) {
    String methodName = method.getName();
    List overloads = (List) methods.get(methodName);
    if (overloads == null) {
      overloads = new ArrayList();
      methods.put(methodName, overloads);
    }
    overloads.add(method);
  }

  void addNestedType(JClassType type) {
    Object existing = nestedTypes.put(type.getSimpleSourceName(), type);
  }

  JClassType findNestedTypeImpl(String[] typeName, int index) {
    JClassType found = (JClassType) nestedTypes.get(typeName[index]);
    if (found == null) {
      return null;
    } else if (index < typeName.length - 1) {
      return found.findNestedTypeImpl(typeName, index + 1);
    } else {
      return found;
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

  private void acceptSubtype(JClassType me) {
    allSubtypes.add(me);
    notifySuperTypesOf(me);
  }

  private String computeInternalSignature(JMethod method) {
    StringBuffer sb = new StringBuffer();
    sb.setLength(0);
    sb.append(method.getName());
    JParameter[] params = method.getParameters();
    for (int j = 0; j < params.length; j++) {
      JParameter param = params[j];
      sb.append("/");
      sb.append(param.getType().getQualifiedSourceName());
    }
    return sb.toString();
  }

  private void getOverridableMethodsOnSuperclassesAndThisClass(
      Map methodsBySignature) {
    assert (isClass() != null);

    // Recurse first so that more derived methods will clobber less derived
    // methods.
    JClassType superClass = getSuperclass();
    if (superClass != null) {
      superClass.getOverridableMethodsOnSuperclassesAndThisClass(methodsBySignature);
    }

    JMethod[] declaredMethods = getMethods();
    for (int i = 0; i < declaredMethods.length; i++) {
      JMethod method = declaredMethods[i];

      // Ensure that this method is overridable.
      if (method.isFinal() || method.isPrivate()) {
        // We cannot override this method, so skip it.
        continue;
      }

      // We can override this method, so record it.
      String sig = computeInternalSignature(method);
      methodsBySignature.put(sig, method);
    }
  }

  /**
   * Gets the methods declared in interfaces that this type extends. If this
   * type is a class, its own methods are not added. If this type is an
   * interface, its own methods are added. Used internally by
   * {@link #getOverridableMethods()}.
   * 
   * @param methodsBySignature
   */
  private void getOverridableMethodsOnSuperinterfacesAndMaybeThisInterface(
      Map methodsBySignature) {
    // Recurse first so that more derived methods will clobber less derived
    // methods.
    JClassType[] superIntfs = getImplementedInterfaces();
    for (int i = 0; i < superIntfs.length; i++) {
      JClassType superIntf = superIntfs[i];
      superIntf.getOverridableMethodsOnSuperinterfacesAndMaybeThisInterface(methodsBySignature);
    }

    if (isInterface() == null) {
      // This is not an interface, so we're done after having visited its
      // implemented interfaces.
      return;
    }

    JMethod[] declaredMethods = getMethods();
    for (int i = 0; i < declaredMethods.length; i++) {
      JMethod method = declaredMethods[i];

      String sig = computeInternalSignature(method);
      JMethod existing = (JMethod) methodsBySignature.get(sig);
      if (existing != null) {
        JClassType existingType = existing.getEnclosingType();
        JClassType thisType = method.getEnclosingType();
        if (thisType.isAssignableFrom(existingType)) {
          // The existing method is in a more-derived type, so don't replace it.
          continue;
        }
      }
      methodsBySignature.put(sig, method);
    }
  }

  private String makeCompoundName(JClassType type) {
    if (type.enclosingType == null) {
      return type.name;
    } else {
      return makeCompoundName(type.enclosingType) + "." + type.name;
    }
  }

  /**
   * Tells this type's superclasses and superinterfaces about it.
   */
  private void notifySuperTypesOf(JClassType me) {
    if (superclass != null) {
      superclass.acceptSubtype(me);
    }
    for (int i = 0, n = interfaces.size(); i < n; ++i) {
      JClassType intf = (JClassType) interfaces.get(i);
      intf.acceptSubtype(me);
    }
  }

  private void removeSubtype(JClassType me) {
    allSubtypes.remove(me);

    if (superclass != null) {
      superclass.removeSubtype(me);
    }

    for (int i = 0, n = interfaces.size(); i < n; ++i) {
      JClassType intf = (JClassType) interfaces.get(i);

      intf.removeSubtype(me);
    }
  }
}
