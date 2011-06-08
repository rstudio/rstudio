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
package com.google.gwt.user.rebind.rpc;

import com.google.gwt.core.ext.typeinfo.JArrayType;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JGenericType;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JRawType;
import com.google.gwt.core.ext.typeinfo.JRealClassType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.JTypeParameter;
import com.google.gwt.core.ext.typeinfo.JWildcardType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * This class defines the method
 * {@link #constrainTypeBy(JClassType, JClassType)}.
 */
public class TypeConstrainer {
  /**
   * Check whether two base types have any subclasses in common. Note: this
   * could surely be implemented much more efficiently.
   */
  private static boolean baseTypesOverlap(JClassType type1, JClassType type2) {
    assert (type1 == getBaseType(type1));
    assert (type2 == getBaseType(type2));

    if (type1 == type2) {
      return true;
    }

    HashSet<JClassType> subtypes1 = new HashSet<JClassType>();
    subtypes1.add(type1);
    for (JClassType sub1 : type1.getSubtypes()) {
      subtypes1.add(getBaseType(sub1));
    }

    List<JClassType> subtypes2 = new ArrayList<JClassType>();
    subtypes2.add(type2);
    for (JClassType sub2 : type2.getSubtypes()) {
      subtypes2.add(getBaseType(sub2));
    }

    for (JClassType sub2 : subtypes2) {
      if (subtypes1.contains(sub2)) {
        return true;
      }
    }

    return false;
  }

  private static JClassType getBaseType(JClassType type) {
    return SerializableTypeOracleBuilder.getBaseType(type);
  }

  private static boolean isRealOrParameterized(JClassType type) {
    if (type.isParameterized() != null) {
      return true;
    }
    if (type instanceof JRealClassType) {
      return true;
    }
    return false;
  }

  /**
   * Check whether <code>param</code> occurs anywhere within <code>type</code>.
   */
  private static boolean occurs(final JTypeParameter param, JClassType type) {
    class OccursVisitor extends JTypeVisitor {
      boolean foundIt = false;

      @Override
      public void endVisit(JTypeParameter seenParam) {
        if (seenParam == param) {
          foundIt = true;
        }
      }
    }

    OccursVisitor visitor = new OccursVisitor();
    visitor.accept(type);
    return visitor.foundIt;
  }

  private int freshTypeVariableCounter;

  private final TypeOracle typeOracle;

  public TypeConstrainer(TypeOracle typeOracle) {
    this.typeOracle = typeOracle;
  }

  /**
   * Return a subtype of <code>subType</code> that includes all values in both
   * <code>subType</code> and <code>superType</code>. The returned type must
   * have the same base type as <code>subType</code>. If there are definitely no
   * such values, return <code>null</code>.
   */
  public JClassType constrainTypeBy(JClassType subType, JClassType superType) {
    JParameterizedType superAsParameterized = superType.isParameterized();
    if (superAsParameterized == null) {
      // If the supertype is not parameterized, it will not be possible to
      // constrain
      // the subtype further.
      return subType;
    }

    // Replace each wildcard in the subType with a fresh type variable.
    // These type variables will be the ones that are constrained.
    Map<JTypeParameter, JClassType> constraints = new HashMap<JTypeParameter, JClassType>();
    JClassType subWithWildcardsReplaced =
        replaceWildcardsWithFreshTypeVariables(subType, constraints);

    // Rewrite subType so that it has the same base type as superType.
    JParameterizedType subAsParameterized =
        subWithWildcardsReplaced.asParameterizationOf(superAsParameterized.getBaseType());
    if (subAsParameterized == null) {
      // The subtype's base does not inherit from the supertype's base,
      // so again no constraint will be possible.
      return subType;
    }

    // Check the rewritten type against superType
    if (!typesMatch(subAsParameterized, superAsParameterized, constraints)) {
      // The types are completely incompatible
      return null;
    }

    // Apply the revised constraints to the original type
    return substitute(subWithWildcardsReplaced, constraints);
  }

  /**
   * Check whether two types can have any values in common. The
   * <code>constraints</code> field holds known constraints for type parameters
   * that appear in <code>type1</code>; this method may take advantage of those
   * constraints in its decision, and it may tighten them so long as the
   * tightening does not reject any values from the overlap of the two types.
   * 
   * As an invariant, no key in <code>constraints</code> may occur inside any
   * value in <code>constraints</code>.
   * 
   * Note that this algorithm looks for overlap matches in the arguments of
   * parameterized types rather than looking for exact matches. Looking for
   * overlaps simplifies the algorithm but returns true more often than it has
   * to.
   */
  boolean typesMatch(JClassType type1, JClassType type2, Map<JTypeParameter, JClassType> constraints) {
    JGenericType type1Generic = type1.isGenericType();
    if (type1Generic != null) {
      return typesMatch(type1Generic.asParameterizedByWildcards(), type2, constraints);
    }

    JGenericType type2Generic = type2.isGenericType();
    if (type2Generic != null) {
      return typesMatch(type1, type2Generic.asParameterizedByWildcards(), constraints);
    }

    JWildcardType type1Wild = type1.isWildcard();
    if (type1Wild != null) {
      return typesMatch(type1Wild.getUpperBound(), type2, constraints);
    }

    JWildcardType type2Wild = type2.isWildcard();
    if (type2Wild != null) {
      return typesMatch(type1, type2Wild.getUpperBound(), constraints);
    }

    JRawType type1Raw = type1.isRawType();
    if (type1Raw != null) {
      return typesMatch(type1Raw.asParameterizedByWildcards(), type2, constraints);
    }

    JRawType type2Raw = type2.isRawType();
    if (type2Raw != null) {
      return typesMatch(type1, type2Raw.asParameterizedByWildcards(), constraints);
    }

    // The following assertions are known to be true, given the tests above.
    // assert (type1Generic == null);
    // assert (type2Generic == null);
    // assert (type1Wild == null);
    // assert (type2Wild == null);
    // assert (type1Raw == null);
    // assert (type2Raw == null);

    if (type1 == type2) {
      return true;
    }

    if (constraints.containsKey(type1)) {
      JTypeParameter type1Parameter = (JTypeParameter) type1;
      JClassType type2Class = type2;
      JClassType type1Bound = constraints.get(type1Parameter);
      assert (!occurs(type1Parameter, type1Bound));
      if (!typesMatch(type1Bound, type2, constraints)) {
        return false;
      }

      if (type1Bound.isAssignableFrom(type2Class)) {
        constraints.put(type1Parameter, type2Class);
      }
    }

    if (type1 == typeOracle.getJavaLangObject()) {
      return true;
    }

    if (type2 == typeOracle.getJavaLangObject()) {
      return true;
    }

    JTypeParameter type1Param = type1.isTypeParameter();
    if (type1Param != null) {
      // It would be nice to check that type1Param's bound is a match
      // for type2, but that can introduce infinite recursions.
      return true;
    }

    JTypeParameter type2Param = type2.isTypeParameter();
    if (type2Param != null) {
      // It would be nice to check that type1Param's bound is a match
      // for type2, but that can introduce infinite recursions.
      return true;
    }

    JArrayType type1Array = type1.isArray();
    JArrayType type2Array = type2.isArray();
    if (type1Array != null && type2Array != null) {
      if (typesMatch(type1Array.getComponentType(), type2Array.getComponentType(), constraints)) {
        return true;
      }
    }

    if (isRealOrParameterized(type1) && isRealOrParameterized(type2)) {
      JClassType baseType1 = getBaseType(type1);
      JClassType baseType2 = getBaseType(type2);
      JParameterizedType type1Parameterized = type1.isParameterized();
      JParameterizedType type2Parameterized = type2.isParameterized();

      if (baseType1 == baseType2 && type1Parameterized != null && type2Parameterized != null) {
        // type1 and type2 are parameterized types with the same base type;
        // compare their arguments
        JClassType[] args1 = type1Parameterized.getTypeArgs();
        JClassType[] args2 = type2Parameterized.getTypeArgs();
        boolean allMatch = true;
        for (int i = 0; i < args1.length; i++) {
          if (!typesMatch(args1[i], args2[i], constraints)) {
            allMatch = false;
          }
        }

        if (allMatch) {
          return true;
        }
      } else {
        // The types have different base types, so just compare the base types
        // for overlap.
        if (baseTypesOverlap(baseType1, baseType2)) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * The same as {@link #typesMatch(JClassType, JClassType, Map)}, but
   * additionally support primitive types as well as class types.
   */
  boolean typesMatch(JType type1, JType type2, Map<JTypeParameter, JClassType> constraints) {
    if (type1 == type2) {
      // This covers the case where both are primitives
      return true;
    }

    JClassType type1Class = type1.isClassOrInterface();
    JClassType type2Class = type2.isClassOrInterface();
    if (type1Class != null && type2Class != null && typesMatch(type1Class, type2Class, constraints)) {
      return true;
    }

    return false;
  }

  /**
   * Replace all wildcards in <code>type</code> with a fresh type variable. For
   * each type variable created, add an entry in <code>constraints</code>
   * mapping the type variable to its upper bound.
   */
  private JClassType replaceWildcardsWithFreshTypeVariables(JClassType type,
      final Map<JTypeParameter, JClassType> constraints) {

    JModTypeVisitor replacer = new JModTypeVisitor() {
      @Override
      public void endVisit(JWildcardType wildcardType) {
        // TODO: fix this to not assume the typemodel types.
        com.google.gwt.dev.javac.typemodel.JTypeParameter newParam =
            new com.google.gwt.dev.javac.typemodel.JTypeParameter("TP$"
                + freshTypeVariableCounter++, -1);
        newParam
            .setBounds(new com.google.gwt.dev.javac.typemodel.JClassType[] {(com.google.gwt.dev.javac.typemodel.JClassType) typeOracle
                .getJavaLangObject()});
        constraints.put(newParam, wildcardType.getUpperBound());
        replacement = newParam;
      }
    };

    return replacer.transform(type);
  }

  /**
   * Substitute all occurrences in <code>type</code> of type parameters in
   * <code>constraints</code> for a wildcard bounded by the parameter's entry in
   * <code>constraints</code>. If the argument is <code>null</code>, return
   * <code>null</code>.
   */
  private JClassType substitute(JClassType type, final Map<JTypeParameter, JClassType> constraints) {
    JModTypeVisitor substituter = new JModTypeVisitor() {
      @Override
      public void endVisit(JTypeParameter param) {
        JClassType constr = constraints.get(param);
        if (constr != null) {
          // further transform the substituted type recursively
          replacement = transform(constr);
        }
      }
    };
    return substituter.transform(type);
  }
}
