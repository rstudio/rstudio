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
package com.google.gwt.core.ext.typeinfo;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a primitive type in a declaration.
 */
public class JPrimitiveType extends JType {
  public static final JPrimitiveType BOOLEAN = create("boolean",
      JniConstants.DESC_BOOLEAN);
  public static final JPrimitiveType BYTE = create("byte",
      JniConstants.DESC_BYTE);
  public static final JPrimitiveType CHAR = create("char",
      JniConstants.DESC_CHAR);
  public static final JPrimitiveType DOUBLE = create("double",
      JniConstants.DESC_DOUBLE);
  public static final JPrimitiveType FLOAT = create("float",
      JniConstants.DESC_FLOAT);
  public static final JPrimitiveType INT = create("int", JniConstants.DESC_INT);
  public static final JPrimitiveType LONG = create("long",
      JniConstants.DESC_LONG);
  public static final JPrimitiveType SHORT = create("short",
      JniConstants.DESC_SHORT);
  public static final JPrimitiveType VOID = create("void",
      JniConstants.DESC_VOID);

  private static Map<String, JPrimitiveType> map;

  public static JPrimitiveType valueOf(String typeName) {
    return getMap().get(typeName);
  }

  private static JPrimitiveType create(String name, char jni) {
    JPrimitiveType type = new JPrimitiveType(name, String.valueOf(jni));
    Object existing = getMap().put(name, type);
    assert (existing == null);
    return type;
  }

  private static Map<String, JPrimitiveType> getMap() {
    if (map == null) {
      map = new HashMap<String, JPrimitiveType>();
    }
    return map;
  }

  private final String jni;

  private final String name;

  private JPrimitiveType(String name, String jni) {
    this.name = name;
    this.jni = jni;
  }

  @Override
  public JType getErasedType() {
    return this;
  }

  @Override
  public String getJNISignature() {
    return jni;
  }

  @Override
  public String getQualifiedSourceName() {
    return name;
  }

  @Override
  public String getSimpleSourceName() {
    return name;
  }

  @Override
  public JArrayType isArray() {
    // intentional null
    return null;
  }

  @Override
  public JClassType isClass() {
    // intentional null
    return null;
  }

  @Override
  public JEnumType isEnum() {
    return null;
  }

  @Override
  public JGenericType isGenericType() {
    return null;
  }

  @Override
  public JClassType isInterface() {
    // intentional null
    return null;
  }

  @Override
  public JParameterizedType isParameterized() {
    // intentional null
    return null;
  }

  @Override
  public JPrimitiveType isPrimitive() {
    return this;
  }

  @Override
  public JRawType isRawType() {
    // intentional null
    return null;
  }

  @Override
  public JWildcardType isWildcard() {
    return null;
  }

  @Override
  public String toString() {
    return name;
  }
  
  @Override
  JPrimitiveType getSubstitutedType(JParameterizedType parameterizedType) {
    return this;
  }
}
