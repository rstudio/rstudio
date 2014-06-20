/*
 * Copyright 2013 Google Inc.
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
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JType;

/**
 * TypeCategory classifies Java types into different categories. <p>
 *
 * These are used in Cast checking and array implementation.
 */
public enum TypeCategory {
  /* Make sure this list is kept in sync with the one in Array.java */

  TYPE_JAVA_OBJECT,
  TYPE_JAVA_OBJECT_OR_JSO,
  TYPE_JSO,
  TYPE_JAVA_LANG_OBJECT,
  // If the next three are renamed, please update JProgram.DispatchType's constructor
  TYPE_JAVA_LANG_STRING,
  TYPE_JAVA_LANG_DOUBLE,
  TYPE_JAVA_LANG_BOOLEAN,
  TYPE_JS_PROTOTYPE,
  TYPE_JS_FUNCTION,
  TYPE_PRIMITIVE_LONG,
  TYPE_PRIMITIVE_NUMBER,
  TYPE_PRIMITIVE_BOOLEAN;

  /**
   * Determines the type category for a specific type.
   */
  public static TypeCategory typeCategoryForType(JType type, JProgram program) {
    if (type instanceof JPrimitiveType) {
      if (type == JPrimitiveType.BOOLEAN) {
        return TypeCategory.TYPE_PRIMITIVE_BOOLEAN;
      } else if (type == JPrimitiveType.LONG) {
        return TypeCategory.TYPE_PRIMITIVE_LONG;
      } else {
        return TypeCategory.TYPE_PRIMITIVE_NUMBER;
      }
    }

    assert type instanceof JReferenceType;
    type = type.getUnderlyingType();
    if (type == program.getTypeJavaLangObject()) {
      return TypeCategory.TYPE_JAVA_LANG_OBJECT;
    } else if (program.getRepresentedAsNativeTypesDispatchMap().containsKey(type)) {
      return program.getRepresentedAsNativeTypesDispatchMap().get(type).getTypeCategory();
    } else if (program.typeOracle.isEffectivelyJavaScriptObject(type)) {
      return TypeCategory.TYPE_JSO;
    } else if (program.typeOracle.isDualJsoInterface(type)
        || program.typeOracle.isJsTypeInterfaceWithoutPrototype(type)) {
      return TypeCategory.TYPE_JAVA_OBJECT_OR_JSO;
    } else if (program.typeOracle.isJsTypeInterfaceWithPrototype(type)) {
      return TypeCategory.TYPE_JS_PROTOTYPE;
    } else if (type.isJsFunction()) {
      return TypeCategory.TYPE_JS_FUNCTION;
    }
    return TypeCategory.TYPE_JAVA_OBJECT;
  }
}
