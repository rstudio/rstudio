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
import java.util.Arrays;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a parameterized type in a declaration.
 */
public class JParameterizedType extends JDelegatingClassType {
  /**
   * Create a parameterized type along with any necessary enclosing
   * parameterized types. Enclosing parameterized types are necessary when the
   * base type is a non-static member and the enclosing type is also generic.
   */
  private static JParameterizedType createParameterizedTypeRecursive(
      JGenericType baseType, Map<JClassType, JClassType> substitutionMap) {
    JClassType enclosingType = null;
    if (baseType.isMemberType() && !baseType.isStatic()) {
      JGenericType isGenericEnclosingType = baseType.getEnclosingType().isGenericType();
      if (isGenericEnclosingType != null) {
        enclosingType = createParameterizedTypeRecursive(
            isGenericEnclosingType, substitutionMap);
      }
    }

    JTypeParameter[] typeParameters = baseType.getTypeParameters();
    JClassType[] newTypeArgs = new JClassType[typeParameters.length];
    TypeOracle oracle = baseType.getOracle();
    for (int i = 0; i < newTypeArgs.length; ++i) {
      JClassType newTypeArg = substitutionMap.get(typeParameters[i]);
      if (newTypeArg == null) {
        JBound typeParamBounds = typeParameters[i].getBounds();
        JUpperBound newTypeArgBounds = new JUpperBound(
            typeParamBounds.getFirstBound());
        newTypeArg = oracle.getWildcardType(newTypeArgBounds);
      }

      newTypeArgs[i] = newTypeArg;
    }

    // TODO: this is wrong if the generic type is a non-static inner class.
    JParameterizedType parameterizedType = oracle.getParameterizedType(
        baseType, enclosingType, newTypeArgs);
    return parameterizedType;
  }

  private final JClassType enclosingType;

  private List<JClassType> interfaces;

  private JClassType lazySuperclass;

  private final AbstractMembers members;

  private final List<JClassType> typeArgs = new ArrayList<JClassType>();

  /**
   * This map records the JClassType that should be used in place of a given
   * {@link JTypeParameter}.
   */
  private final Map<JTypeParameter, JClassType> substitutionMap = new IdentityHashMap<JTypeParameter, JClassType>();

  public JParameterizedType(JGenericType baseType, JClassType enclosingType,
      JClassType[] typeArgs) {
    super.setBaseType(baseType);

    this.enclosingType = enclosingType;

    // NOTE: this instance is not considered a nested type of the enclosing type

    final JParameterizedType parameterizedType = this;
    members = new DelegateMembers(this, baseType, new Substitution() {
      public JType getSubstitution(JType type) {
        return type.getSubstitutedType(parameterizedType);
      }
    });

    List<JClassType> typeArgsList = Arrays.asList(typeArgs);
    this.typeArgs.addAll(typeArgsList);
    assert (typeArgsList.indexOf(null) == -1);

    initializeTypeParameterSubstitutionMap();

    // NOTE: Can't perform substitutions until we are done building
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
  public JGenericType getBaseType() {
    return (JGenericType) super.getBaseType();
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
  public JClassType getEnclosingType() {
    return enclosingType;
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
    if (interfaces == null) {
      interfaces = new ArrayList<JClassType>();
      JClassType[] intfs = getBaseType().getImplementedInterfaces();
      for (JClassType intf : intfs) {
        JClassType newIntf = intf.getSubstitutedType(this);
        interfaces.add(newIntf);
      }
    }
    return interfaces.toArray(TypeOracle.NO_JCLASSES);
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

    if (getEnclosingType() != null) {
      sb.append(getEnclosingType().getParameterizedQualifiedSourceName());
      sb.append(".");
      sb.append(getSimpleSourceName());
    } else {
      sb.append(getQualifiedSourceName());
    }

    if (typeArgs.size() > 0) {
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
    } else {
      /*
       * Non-static, inner classes of generic types are modeled as generic, even
       * if they do not declare type parameters or reference the type parameters
       * of their enclosing generic type.
       */
    }

    return sb.toString();
  }

  /**
   * Everything is fully qualified and includes the &lt; and &gt; in the
   * signature.
   */
  @Override
  public String getQualifiedSourceName() {
    return getBaseType().getQualifiedSourceName();
  }

  public JClassType getRawType() {
    return getBaseType().getRawType();
  }

  /**
   * In this case, the raw type name.
   */
  @Override
  public String getSimpleSourceName() {
    return getBaseType().getSimpleSourceName();
  }

  /*
   * Goal: Return a list of possible subtypes of this parameterized type. In the
   * event that we have generic subtypes and we cannot resolve the all of the
   * type arguments, we need to wildcard types in place of the arguments that we
   * cannot resolve.
   * 
   * Algorithm: - Ask generic type for its subtypes - Filter subtypes of the
   * generic which cannot be our subtype.
   */
  @Override
  public JClassType[] getSubtypes() {
    List<JClassType> subtypeList = new ArrayList<JClassType>();

    // Parameterized types are not tracked in the subtype hierarchy; ask base
    // type
    JClassType[] genericSubtypes = getBaseType().getSubtypes();
    for (JClassType subtype : genericSubtypes) {
      Set<JClassType> typeHierarchy = getFlattenedTypeHierarchy(subtype);

      // Could be a subtype depending on how it is substituted
      Map<JClassType, JClassType> substitutions = new IdentityHashMap<JClassType, JClassType>();
      if (isSubtype(subtype, typeHierarchy, substitutions, true)) {
        JGenericType genericType = subtype.isGenericType();
        if (genericType != null) {
          subtype = createParameterizedTypeRecursive(genericType, substitutions);
        }

        subtypeList.add(subtype);
      }
    }

    return subtypeList.toArray(TypeOracle.NO_JCLASSES);
  }

  @Override
  public JClassType getSuperclass() {
    if (isInterface() != null) {
      return null;
    }

    if (lazySuperclass == null) {
      JGenericType baseType = getBaseType();
      JClassType superclass = baseType.getSuperclass();
      assert (superclass != null);

      lazySuperclass = superclass.getSubstitutedType(this);
    }

    return lazySuperclass;
  }

  public JClassType[] getTypeArgs() {
    return typeArgs.toArray(TypeOracle.NO_JCLASSES);
  }

  @Override
  public boolean isAssignableFrom(JClassType possibleSubtype) {
    if (possibleSubtype == this) {
      return true;
    }

    JRawType possibleRawSubtype = possibleSubtype.isRawType();
    if (possibleRawSubtype != null) {
      return getBaseType().isAssignableFrom(possibleRawSubtype.getBaseType());
    }

    Set<JClassType> typeHierarchy = getFlattenedTypeHierarchy(possibleSubtype);
    return isSubtype(possibleSubtype, typeHierarchy,
        new IdentityHashMap<JClassType, JClassType>(), false);
  }

  @Override
  public boolean isAssignableTo(JClassType possibleSupertype) {
    return possibleSupertype.isAssignableFrom(this);
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
  public JWildcardType isWildcard() {
    return null;
  }

  /**
   */
  public void setTypeArguments(JClassType[] typeArgs) {
    this.typeArgs.addAll(Arrays.asList(typeArgs));
  }

  @Override
  public String toString() {
    if (isInterface() != null) {
      return "interface " + getParameterizedQualifiedSourceName();
    }

    return "class " + getParameterizedQualifiedSourceName();
  }

  @Override
  protected JClassType findNestedTypeImpl(String[] typeName, int index) {
    return members.findNestedTypeImpl(typeName, index);
  }

  @Override
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
  @Override
  protected void getOverridableMethodsOnSuperinterfacesAndMaybeThisInterface(
      Map<String, JMethod> methodsBySignature) {
    members.getOverridableMethodsOnSuperinterfacesAndMaybeThisInterface(methodsBySignature);
  }

  @Override
  JClassType getSubstitutedType(JParameterizedType parameterizedType) {
    if (this == parameterizedType) {
      return this;
    }

    JClassType[] newTypeArgs = new JClassType[typeArgs.size()];
    for (int i = 0; i < newTypeArgs.length; ++i) {
      newTypeArgs[i] = typeArgs.get(i).getSubstitutedType(parameterizedType);
    }

    return getOracle().getParameterizedType(getBaseType(), getEnclosingType(),
        newTypeArgs);
  }

  /**
   * Returns the {@link JClassType} that is a substitute for the given
   * {@link JTypeParameter}. If there is no substitution, the original
   * {@link JTypeParameter} is returned.
   */
  JClassType getTypeParameterSubstitution(JTypeParameter typeParameter) {
    JClassType substitute = substitutionMap.get(typeParameter);
    if (substitute != null) {
      return substitute;
    }

    return typeParameter;
  }

  boolean hasTypeArgs(JClassType[] otherArgTypes) {
    if (otherArgTypes.length != typeArgs.size()) {
      return false;
    }

    for (int i = 0; i < otherArgTypes.length; ++i) {
      // Identity tests are ok since identity is durable within an oracle.
      //
      if (otherArgTypes[i] != typeArgs.get(i)) {
        return false;
      }
    }

    return true;
  }

  /**
   * Initialize a map of substitutions for {@link JTypeParameter}s to
   * corresponding {@link JClassType}s.
   */
  void initializeTypeParameterSubstitutionMap() {
    JParameterizedType currentParameterizedType = this;

    while (currentParameterizedType != null) {
      JGenericType genericType = currentParameterizedType.getBaseType();
      JTypeParameter[] typeParameters = genericType.getTypeParameters();
      JClassType[] typeArguments = currentParameterizedType.getTypeArgs();

      for (JTypeParameter typeParameter : typeParameters) {
        substitutionMap.put(typeParameter,
            typeArguments[typeParameter.getOrdinal()]);
      }

      if (currentParameterizedType.isStatic()) {
        break;
      }

      JClassType maybeParameterizedType = currentParameterizedType.getEnclosingType();
      if (maybeParameterizedType == null
          || maybeParameterizedType.isParameterized() == null) {
        break;
      }
      currentParameterizedType = maybeParameterizedType.isParameterized();
    }
  }

  /**
   * Returns the flattened view of the type hierarchy.
   */
  private Set<JClassType> getFlattenedTypeHierarchy(JClassType type) {
    Set<JClassType> typesSeen = new HashSet<JClassType>();
    getFlattenedTypeHierarchyRecursive(type, typesSeen);
    return typesSeen;
  }

  private void getFlattenedTypeHierarchyRecursive(JClassType type,
      Set<JClassType> typesSeen) {
    if (typesSeen.contains(type)) {
      return;
    }
    typesSeen.add(type);

    // Superclass
    JClassType superclass = type.getSuperclass();
    if (superclass != null) {
      getFlattenedTypeHierarchyRecursive(superclass, typesSeen);
    }

    // Check the interfaces
    JClassType[] intfs = type.getImplementedInterfaces();
    for (JClassType intf : intfs) {
      getFlattenedTypeHierarchyRecursive(intf, typesSeen);
    }
  }

  /**
   * Look at the type hierarchy and see if we can find a parameterized type that
   * has the same base type as this instance. If we find one then we check to
   * see if the type arguments are compatible. If they are, then we record what
   * the typeArgument needs to be replaced with in order to make it a proper
   * subtype of this parameterized type.
   */
  private boolean isSubtype(JClassType subtype, Set<JClassType> typeHierarchy,
      Map<JClassType, JClassType> substitutions, boolean lookForSubstitutions) {
    if (typeHierarchy.contains(this)) {
      return true;
    }

    for (JClassType type : typeHierarchy) {
      JParameterizedType parameterizedType = type.isParameterized();
      if (parameterizedType == null) {
        continue;
      }

      if (parameterizedType.getBaseType() != getBaseType()) {
        continue;
      }

      // Check the type arguments to see if they are compatible.
      JClassType[] otherTypeArgs = parameterizedType.getTypeArgs();
      JClassType[] myTypeArgs = getTypeArgs();
      boolean validSubstitution = true;
      for (int i = 0; i < myTypeArgs.length; ++i) {
        JClassType otherTypeArg = otherTypeArgs[i];
        JClassType myTypeArg = myTypeArgs[i];

        validSubstitution = myTypeArg == otherTypeArg;
        if (!validSubstitution) {
          if (lookForSubstitutions) {
            // Make sure that the other type argument is assignable from mine
            validSubstitution = otherTypeArg.isAssignableFrom(myTypeArg);
          } else {
            // Looking for strict subtypes; only wildcards allow a non-exact
            // match
            JWildcardType isWildcard = myTypeArg.isWildcard();
            if (isWildcard != null) {
              validSubstitution = myTypeArg.isAssignableFrom(otherTypeArg);
            }
          }
        }

        if (!validSubstitution) {
          break;
        }

        substitutions.put(otherTypeArg, myTypeArg);
      }

      if (validSubstitution) {
        /*
         * At this point we know that the type can be a subtype and we know the
         * substitution to apply.
         */
        return true;
      }
    }

    // Can't be a subtype regardless of substitution.
    return false;
  }
}