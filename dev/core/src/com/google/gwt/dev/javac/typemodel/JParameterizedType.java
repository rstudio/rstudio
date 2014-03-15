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

import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.JWildcardType.BoundType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.dev.util.collect.IdentityHashMap;
import com.google.gwt.dev.util.collect.Lists;
import com.google.gwt.dev.util.collect.Maps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a parameterized type in a declaration.
 */
public class JParameterizedType extends JMaybeParameterizedType implements
    com.google.gwt.core.ext.typeinfo.JParameterizedType {
  /**
   * Create a parameterized type along with any necessary enclosing
   * parameterized types. Enclosing parameterized types are necessary when the
   * base type is a non-static member and the enclosing type is also generic.
   */
  private static JParameterizedType createParameterizedTypeRecursive(
      JGenericType baseType, Map<JTypeParameter, JClassType> substitutionMap) {
    JClassType enclosingType = baseType.getEnclosingType();
    if (baseType.isMemberType() && !baseType.isStatic()) {
      // This base type is a non-static generic type so we build the necessary
      // enclosing parameterized type and update the enclosing type to be
      // a parameterized type.
      JGenericType isGenericEnclosingType = enclosingType.isGenericType();
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
        newTypeArg = oracle.getWildcardType(BoundType.EXTENDS,
            typeParameters[i].getFirstBound());
      }

      newTypeArgs[i] = newTypeArg;
    }

    JParameterizedType parameterizedType = oracle.getParameterizedType(
        baseType, enclosingType, newTypeArgs);
    return parameterizedType;
  }

  private final JClassType enclosingType;

  private List<JClassType> interfaces;

  /**
   * This map records the JClassType that should be used in place of a given
   * {@link JTypeParameter}.
   */
  private Map<JTypeParameter, JClassType> lazySubstitutionMap;

  private JClassType lazySuperclass;

  private final AbstractMembers members;

  private final List<JClassType> typeArgs;

  JParameterizedType(JGenericType baseType, JClassType enclosingType,
      JClassType[] typeArgs) {
    super.setBaseType(baseType);

    this.enclosingType = enclosingType;

    // NOTE: this instance is not considered a nested type of the enclosing type

    final JParameterizedType parameterizedType = this;
    members = new DelegateMembers(this, baseType, new Substitution() {
      @Override
      public JClassType getSubstitution(JClassType type) {
        return type.getSubstitutedType(parameterizedType);
      }
    });

    this.typeArgs = Lists.create(typeArgs);
    assert (this.typeArgs.indexOf(null) == -1) : "Unresolved typeArg creating JParameterizedType from "
        + baseType;

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
      interfaces = Lists.normalize(interfaces);
    }
    return interfaces.toArray(TypeOracle.NO_JCLASSES);
  }

  @Override
  public JMethod[] getInheritableMethods() {
    return members.getInheritableMethods();
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
   * @deprecated See {@link #getQualifiedSourceName()}
   */
  @Override
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

  @Override
  public String getQualifiedBinaryName() {
    return getBaseType().getQualifiedBinaryName();
  }

  /**
   * Everything is fully qualified and includes the &lt; and &gt; in the
   * signature.
   */
  @Override
  public String getQualifiedSourceName() {
    return getBaseType().getQualifiedSourceName();
  }

  @Override
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
      // Could be a subtype depending on how it is substituted
      Map<JTypeParameter, JClassType> substitutions = findSubtypeSubstitution(subtype);
      if (substitutions != null) {
        JGenericType genericType = subtype.isGenericType();
        if (genericType != null) {
          subtype = createParameterizedTypeRecursive(genericType, substitutions);
        } else {
          // If this is not a generic type then there should not be any
          // substitution.
          assert (substitutions.isEmpty());
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

  @Override
  public JClassType[] getTypeArgs() {
    return typeArgs.toArray(TypeOracle.NO_JCLASSES);
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
  JClassType getSubstitutedType(JParameterizedType parameterizedType) {
    maybeInitializeTypeParameterSubstitutionMap();

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
    maybeInitializeTypeParameterSubstitutionMap();

    JClassType substitute = lazySubstitutionMap.get(typeParameter);
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
   * corresponding {@link JClassType}s. This can only be initialized after the
   * TypeOracleUpdater has fully resolved all of the {@link JClassType}s.
   */
  void maybeInitializeTypeParameterSubstitutionMap() {
    if (lazySubstitutionMap != null) {
      return;
    }
    lazySubstitutionMap = new IdentityHashMap<JTypeParameter, JClassType>();

    JParameterizedType currentParameterizedType = this;

    while (currentParameterizedType != null) {
      JGenericType genericType = currentParameterizedType.getBaseType();
      JTypeParameter[] typeParameters = genericType.getTypeParameters();
      JClassType[] typeArguments = currentParameterizedType.getTypeArgs();

      for (JTypeParameter typeParameter : typeParameters) {
        lazySubstitutionMap.put(typeParameter,
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
    lazySubstitutionMap = Maps.normalize(lazySubstitutionMap);
  }

  void setTypeArguments(JClassType[] typeArgs) {
    this.typeArgs.addAll(Arrays.asList(typeArgs));
  }

  /**
   * Returns a map of substitutions that will make the subtype a proper subtype
   * of this parameterized type. The map maybe empty in the case that it is
   * already an exact subtype.
   */
  private Map<JTypeParameter, JClassType> findSubtypeSubstitution(
      JClassType subtype) {
    Map<JTypeParameter, JClassType> substitutions = new IdentityHashMap<JTypeParameter, JClassType>();

    // Get the supertype hierarchy. If this JParameterizedType exists
    // exactly in this set we are done.
    Set<JClassType> supertypeHierarchy = getFlattenedSuperTypeHierarchy(subtype);
    if (supertypeHierarchy.contains(this)) {
      return substitutions;
    }

    /*
     * Try to find a parameterized supertype whose base type is the same as our
     * own. Because that parameterized supertype might be made into ourself via
     * substitution.
     */
    for (JClassType candidate : supertypeHierarchy) {
      JParameterizedType parameterizedCandidate = candidate.isParameterized();
      if (parameterizedCandidate == null) {
        // If not parameterized then there is no substitution possible.
        continue;
      }

      if (parameterizedCandidate.getBaseType() != getBaseType()) {
        // This candidate be parameterized to us.
        continue;
      }

      /*
       * We have a parameterization of our base type. Now we need to see if it
       * is possible to parameterize subtype such that candidate becomes
       * equivalent to us.
       */
      JClassType[] candidateTypeArgs = parameterizedCandidate.getTypeArgs();
      JClassType[] myTypeArgs = getTypeArgs();
      for (int i = 0; i < myTypeArgs.length; ++i) {
        JClassType otherTypeArg = candidateTypeArgs[i];
        JClassType myTypeArg = myTypeArgs[i];

        if (myTypeArg == otherTypeArg) {
          // There are identical so there is no substitution that is needed.
          continue;
        }

        JTypeParameter otherTypeParameter = otherTypeArg.isTypeParameter();
        if (otherTypeParameter == null) {
          // Not a type parameter and not equal so no substitution can make it
          // equal.
          return null;
        }

        if (!otherTypeParameter.isAssignableFrom(myTypeArg)) {
          // Make sure that my type argument can be substituted for this type
          // parameter.
          return null;
        }

        substitutions.put(otherTypeParameter, myTypeArg);
      }
    }

    // Legal substitution can be made and is record in substitutions.
    return substitutions;
  }
}
