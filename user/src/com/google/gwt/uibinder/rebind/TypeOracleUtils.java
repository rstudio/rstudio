/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.uibinder.rebind;

import com.google.gwt.core.ext.typeinfo.JArrayType;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JConstructor;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;

/**
 * Utilities for functionality that really should be part of TypeOracle.
 */
public class TypeOracleUtils {

  /**
   * Check for a constructor which is compatible with the supplied argument
   * types.
   * 
   * @param type
   * @param argTypes
   * @return true if a constructor compatible with the supplied arguments exists
   */
  public static boolean hasCompatibleConstructor(JClassType type, JType... argTypes) {
    // Note that this does not return the constructor, since that is a more
    // complicated decision about finding the best matching arguments where
    // more than one are compatible.
    for (JConstructor ctor : type.getConstructors()) {
      if (typesAreCompatible(ctor.getParameterTypes(), argTypes, ctor.isVarArgs())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Return true if the supplied argument type is assignment compatible with a
   * declared parameter type.
   * 
   * @param paramType
   * @param argType
   * @return true if the argument type is compatible with the parameter type
   */
  public static boolean typeIsCompatible(JType paramType, JType argType) {
    if (paramType == argType) {
      return true;
    }
    JClassType paramClass = paramType.isClassOrInterface();
    if (paramClass != null) {
      JClassType argClass = argType.isClassOrInterface();
      return argClass != null && paramClass.isAssignableFrom(argClass);
    }
    JArrayType paramArray = paramType.isArray();
    if (paramArray != null) {
      JArrayType argArray = argType.isArray();
      return argArray != null && typeIsCompatible(paramArray.getComponentType(),
          argArray.getComponentType());
    }
    if (paramType instanceof JPrimitiveType && argType instanceof JPrimitiveType) {
      return isWideningPrimitiveConversion((JPrimitiveType) paramType, (JPrimitiveType) argType);
    }
    // TODO: handle autoboxing?
    return false;
  }

  /**
   * Check if the types of supplied arguments are compatible with the parameter
   * types of a method.
   * 
   * @param paramTypes
   * @param argTypes
   * @param varArgs true if the method is a varargs method
   * @return true if all argument types are compatible with the parameter types
   */
  public static boolean typesAreCompatible(JType[] paramTypes, JType[] argTypes, boolean varArgs) {
    int expectedArgs = paramTypes.length;
    int actualArgs = argTypes.length;
    int comparedArgs = expectedArgs;
    if (varArgs) {
      comparedArgs--;
      if (actualArgs != expectedArgs || !typeIsCompatible(paramTypes[comparedArgs], argTypes[comparedArgs])) {
        if (actualArgs < comparedArgs) {
          return false;
        }
        JArrayType varargsArrayType = paramTypes[comparedArgs].isArray();
        assert varargsArrayType != null;
        JType varargsType = varargsArrayType.getComponentType();
        for (int i = comparedArgs; i < actualArgs; ++i) {
          if (!typeIsCompatible(varargsType, argTypes[i])) {
            return false;
          }
        }
      }
    } else if (actualArgs != expectedArgs) {
      return false;
    }
    for (int i = 0; i < comparedArgs; ++i) {
      if (!typeIsCompatible(paramTypes[i], argTypes[i])) {
        return false;
      }
    }
    return true;
  }

  /**
   * Check for a widening primitive conversion.  See
   * <a href="http://java.sun.com/docs/books/jls/second_edition/html/conversions.doc.html#25214">JLS 5.1.2</a>.
   * 
   * @param paramType
   * @param argType
   * @return true if assigning argType to paramType is a widening conversion
   */
  private static boolean isWideningPrimitiveConversion(JPrimitiveType paramType, JPrimitiveType argType) {
    switch (paramType) {
      case DOUBLE:
        return argType != JPrimitiveType.BOOLEAN;
      case FLOAT:
        return argType != JPrimitiveType.BOOLEAN && argType != JPrimitiveType.DOUBLE;
      case LONG:
        return argType != JPrimitiveType.BOOLEAN && argType != JPrimitiveType.DOUBLE
            && argType != JPrimitiveType.FLOAT;
      case INT:
        return argType == JPrimitiveType.BYTE || argType == JPrimitiveType.SHORT
            || argType == JPrimitiveType.CHAR;
      case SHORT:
        return argType == JPrimitiveType.BYTE;
      default:
        return false;
    }
  }

  private TypeOracleUtils() {
  }
}
