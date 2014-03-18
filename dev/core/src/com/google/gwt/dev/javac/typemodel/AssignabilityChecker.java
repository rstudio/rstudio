/*
 * Copyright 2014 Google Inc.
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

import java.util.Set;

/**
 * A helper class to check assignability of types.
 */
class AssignabilityChecker {

  public boolean isAssignable(JClassType from, JClassType to) {
    from = convertToRawIfGeneric(from);
    to = convertToRawIfGeneric(to);

    if (to == from) {
      return true;
    }

    if (to.isWildcard() != null) {
      return isAssignableToWildcardType(from, to.isWildcard());
    }

    if (from.isTypeParameter() != null) {
      return isAssignableFromAny(from.isTypeParameter().getBounds(), to);
    }

    if (from.isWildcard() != null) {
      return isAssignableFromAny(from.isWildcard().getUpperBounds(), to);
    }

    if (from.isArray() != null) {
      return isAssignableFromGenericArrayType(from.isArray(), to);
    }

    if (to.isParameterized() != null) {
      return isAssignableToParameterizedType(from, to.isParameterized());
    }

    if (to.isTypeParameter() != null) {
      // type inference is not supported (yet)
      return isAssignableFromAll(from, to.isTypeParameter().getBounds());
    }

    if (to.isArray() != null) {
      return false;
    }

    // Only remaining cases for 'to' are being real-class or raw type
    assert to instanceof JRealClassType || to instanceof JRawType;

    return isAssignableFromRaw(from, to);
  }

  private boolean isAssignableFromAny(JClassType[] fromTypes, JClassType to) {
    for (JClassType from : fromTypes) {
      if (isAssignable(from, to)) {
        return true;
      }
    }
    return false;
  }

  private boolean isAssignableToWildcardType(JClassType from, JWildcardType to) {
    // if "to" is <? extends Foo>, "from" can be:
    // Foo, SubFoo, <? extends Foo>, <? extends SubFoo>, <T extends Foo> or <T extends SubFoo>.
    // if "to" is <? super Foo>, "from" can be:
    // Foo, SuperFoo, <? super Foo> or <? super SuperFoo>.
    return isAssignable(from, supertypeBound(to)) && isAssignableBySubtypeBound(from, to);
  }

  private boolean isAssignableBySubtypeBound(JClassType from, JWildcardType to) {
    JClassType toSubtypeBound = subtypeBound(to);
    if (toSubtypeBound == null) {
      return true;
    }
    JClassType fromSubtypeBound = subtypeBound(from);
    if (fromSubtypeBound == null) {
      return false;
    }
    return isAssignable(toSubtypeBound, fromSubtypeBound);
  }

  private boolean isAssignableToParameterizedType(JClassType from, JParameterizedType to) {
    // If "to" is "List<? extends CharSequence>" and "from" is StringArrayList,
    // First step is to figure out StringArrayList "is-a" List<E> and <E> is String.
    JMaybeParameterizedType parentOfFrom = asParamterizationOf(from, to);
    if (parentOfFrom == null) {
      return false;
    }

    if (parentOfFrom.isRawType() != null) {
      return true;
    }

    // If it is not raw, then it should be parameterized
    JParameterizedType parameterizedParentOfFrom = parentOfFrom.isParameterized();
    assert parameterizedParentOfFrom != null;

    JClassType[] fromTypeArgs = parameterizedParentOfFrom.getTypeArgs();
    JClassType[] toTypeArgs = to.getTypeArgs();
    for (int i = 0; i < fromTypeArgs.length; i++) {
      if (!matchTypeArgument(fromTypeArgs[i], toTypeArgs[i])) {
        return false;
      }
    }
    return true;
  }

  private boolean matchTypeArgument(JClassType from, JClassType to) {
    if (from == to) {
      return true;
    }

    if (to.isWildcard() != null) {
      return isAssignableToWildcardType(from, to.isWildcard());
    }

    return false;
  }

  private boolean isAssignableFromAll(JClassType from, JClassType[] toTypes) {
    for (JClassType to : toTypes) {
      if (!isAssignable(from, to)) {
        return false;
      }
    }
    return true;
  }

  private boolean isAssignableFromGenericArrayType(JArrayType from, JClassType to) {
    if (to.isArray() != null) {

      JType fromComponentType = from.getComponentType();
      JType toComponentType = to.isArray().getComponentType();

      if (toComponentType.isPrimitive() != null || fromComponentType.isPrimitive() != null) {
        // Only scenario for this to be assignable is; this two being equal, but we wouldn't have
        // reached here in that case
        return false;
      }

      return isAssignable((JClassType) fromComponentType, (JClassType) toComponentType);
    }

    return isJavaLangObject(to);
  }

  private boolean isAssignableFromRaw(JClassType from, JClassType to) {
    if (isJavaLangObject(to)) {
      return true;
    }

    Set<JClassType> fromSuperTypeHierarchy = from.getFlattenedSupertypeHierarchy();

    // Shortcut: 'to' is one of the parents.
    if (fromSuperTypeHierarchy.contains(to)) {
      return true;
    }

    // Fallback to checking erased types if it is raw.
    if (to.isRawType() != null) {
      for (JClassType fromSuper : fromSuperTypeHierarchy) {
        if (fromSuper.getErasedType() == to.getErasedType()) {
          return true;
        }
      }
    }

    return false;
  }

  private static boolean isJavaLangObject(JClassType type) {
    return type == type.getOracle().getJavaLangObject();
  }

  private static JClassType convertToRawIfGeneric(JClassType from) {
    return from.isGenericType() != null ? from.isGenericType().getRawType() : from;
  }

  private static JClassType supertypeBound(JWildcardType type) {
    // If type is <? super ? super Foo>, this will return Foo.
    // (Even if you cannot write such code in java, the type can resolve to that)
    JClassType upperBound = type.getUpperBound();
    return upperBound.isWildcard() != null ? supertypeBound(upperBound.isWildcard()) : upperBound;
  }

  private static JClassType subtypeBound(JWildcardType type) {
    // If type is <? extends ? extends Foo>, this will return Foo.
    // (Even if you cannot write such code in java, the type can resolve to that)
    JClassType[] lowerBounds = type.getLowerBounds();
    return lowerBounds.length == 1 ? subtypeBound(lowerBounds[0]) : null;
  }

  private static JClassType subtypeBound(JClassType type) {
    return type.isWildcard() != null ? subtypeBound(type.isWildcard()) : type;
  }

  private static JMaybeParameterizedType asParamterizationOf(JClassType from,
      JParameterizedType to) {
    for (JClassType parent : from.getFlattenedSupertypeHierarchy()) {
      JMaybeParameterizedType maybeParameterized = parent.isMaybeParameterizedType();
      if (maybeParameterized != null && maybeParameterized.getBaseType() == to.getBaseType()) {
        return maybeParameterized;
      }
    }
    return null;
  }
}
