/*
 * Copyright 2006 Google Inc.
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

public class JPrimitiveType extends JType {

  public static final JPrimitiveType BOOLEAN = create("boolean", "Z");
  public static final JPrimitiveType BYTE = create("byte", "B");
  public static final JPrimitiveType CHAR = create("char", "C");
  public static final JPrimitiveType DOUBLE = create("double", "D");
  public static final JPrimitiveType FLOAT = create("float", "F");
  public static final JPrimitiveType INT = create("int", "I");
  public static final JPrimitiveType LONG = create("long", "J");
  public static final JPrimitiveType SHORT = create("short", "S");
  public static final JPrimitiveType VOID = create("void", "V");

  private static Map map;

  public static JPrimitiveType valueOf(String typeName) {
    return (JPrimitiveType) getMap().get(typeName);
  }

  private static JPrimitiveType create(String name, String jni) {
    JPrimitiveType type = new JPrimitiveType(name, jni);
    Object existing = getMap().put(name, type);
    assert (existing == null);
    return type;
  }

  private static Map getMap() {
    if (map == null) {
      map = new HashMap();
    }
    return map;
  }

  private final String jni;

  private final String name;

  private JPrimitiveType(String name, String jni) {
    this.name = name;
    this.jni = jni;
  }

  public String getJNISignature() {
    return jni;
  }

  public String getQualifiedSourceName() {
    return name;
  }

  public String getSimpleSourceName() {
    return name;
  }

  public JArrayType isArray() {
    // intentional null
    return null;
  }

  public JClassType isClass() {
    // intentional null
    return null;
  }

  public JClassType isInterface() {
    // intentional null
    return null;
  }

  public JParameterizedType isParameterized() {
    // intentional null
    return null;
  }

  public JPrimitiveType isPrimitive() {
    return this;
  }
}
