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
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JRawType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.JWildcardType;

/**
 * <p>
 * This visitor supports transforming interior parts of a type. Subclasses
 * should override the appropriate <code>endVisit</code> methods and, when such
 * a method wants to replace its argument, assign the replacement to the field
 * {@link #replacement}.
 * </p>
 * 
 * <p>
 * Any specified replacements must be sensible no matter what context they might
 * be used in. Specifically:
 * </p>
 * <ul>
 * <li>Primitive types may only be replaced by primitive types.
 * <li>Reference types may only be replaced by reference types.
 * <li>Generic types may only be replaced by generic types, and the replacement
 * should have the same number of type parameters as the original.
 * </ul>
 */
class JModTypeVisitor extends JTypeVisitor {
  /**
   * A subclass's <code>endVisit</code> method can indicate that it wants to
   * replace its argument by assigning the replacement to this field.
   */
  protected JType replacement;

  @Override
  public void accept(JType type) {
    replacement = type;
    acceptChildren(type);
    endVisit(replacement);
  }

  public JClassType transform(JClassType type) {
    // transforming a class type should give a class type back
    return (JClassType) transform((JType) type);
  }

  public JGenericType transform(JGenericType type) {
    // transforming a generic type should give a generic type back
    return (JGenericType) transform((JType) type);
  }

  public JPrimitiveType transform(JPrimitiveType type) {
    // transforming a primitive type should give a primitive type back
    return (JPrimitiveType) transform((JType) type);
  }

  public JType transform(JType type) {
    accept(type);
    return replacement;
  }

  /**
   * Do the same thing as {@link JTypeVisitor#acceptChildren(JType)}, but if any
   * children types are replaced, reconstruct a version of <code>type</code>
   * that has those corresponding replacements made and assign the new type to
   * {@link #replacement}.
   */
  @Override
  protected void acceptChildren(JType type) {
    JArrayType typeArray = type.isArray();
    if (typeArray != null) {
      JType oldComponentType = typeArray.getComponentType();
      JType newComponentType = transform(oldComponentType);

      if (oldComponentType == newComponentType) {
        replacement = type;
      } else {
        replacement = typeArray.getOracle().getArrayType(newComponentType);
      }
    }

    JParameterizedType typeParameterized = type.isParameterized();
    if (typeParameterized != null) {
      JGenericType oldBaseType = typeParameterized.getBaseType();
      JGenericType newBaseType = transform(oldBaseType);

      JClassType oldEnclosingType = typeParameterized.getEnclosingType();
      JClassType newEnclosingType = oldEnclosingType == null ? null : transform(oldEnclosingType);

      JClassType[] oldTypeArgs = typeParameterized.getTypeArgs();
      JClassType[] newTypeArgs = new JClassType[oldTypeArgs.length];
      boolean argsAllSame = true;
      for (int i = 0; i < oldTypeArgs.length; i++) {
        newTypeArgs[i] = transform(oldTypeArgs[i]);
        if (newTypeArgs[i] != oldTypeArgs[i]) {
          argsAllSame = false;
        }
      }

      if (argsAllSame && oldBaseType == newBaseType && oldEnclosingType == newEnclosingType) {
        replacement = type;
      } else {
        replacement =
            typeParameterized.getOracle().getParameterizedType(newBaseType, newEnclosingType,
                newTypeArgs);
      }
    }

    JRawType typeRaw = type.isRawType();
    if (typeRaw != null) {
      JGenericType oldBaseType = typeRaw.getBaseType();
      JGenericType newBaseType = transform(oldBaseType);

      if (oldBaseType == newBaseType) {
        replacement = type;
      } else {
        replacement = newBaseType.getRawType();
      }
    }

    JWildcardType typeWild = type.isWildcard();
    if (typeWild != null) {
      JClassType oldBound = typeWild.getFirstBound();
      JClassType newBound = transform(oldBound);

      if (oldBound == newBound) {
        replacement = type;
      } else {
        replacement = typeWild.getOracle().getWildcardType(typeWild.getBoundType(), newBound);
      }
    }
  }
}
