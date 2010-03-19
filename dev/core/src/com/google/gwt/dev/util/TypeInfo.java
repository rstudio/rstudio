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
package com.google.gwt.dev.util;

import java.lang.reflect.Method;

/**
 * Java type helpers used.
 */
public class TypeInfo {

  public static final int NOT_FOUND = 0; // used with all of the enumerated
  public static final int TYPE_ARRAY = 0x200000;
  public static final int TYPE_COLLECTION = 0x180000;
  public static final int TYPE_COLLECTION_LIST = 0x100000;
  public static final int TYPE_COLLECTION_SET = 0x080000;
  public static final int TYPE_PRIM = 0x07fc00;
  public static final int TYPE_PRIM_BOOLEAN = 0x008000;
  public static final int TYPE_PRIM_BYTE = 0x000800;
  public static final int TYPE_PRIM_CHAR = 0x000400;
  public static final int TYPE_PRIM_DOUBLE = 0x020000;
  public static final int TYPE_PRIM_FLOAT = 0x010000;
  public static final int TYPE_PRIM_INT = 0x002000;
  public static final int TYPE_PRIM_LONG = 0x004000;
  public static final int TYPE_PRIM_SHORT = 0x001000;
  public static final int TYPE_PRIM_VOID = 0x040000;
  public static final int TYPE_USER = 0x400000;
  public static final int TYPE_WRAP = 0x0003ff;
  public static final int TYPE_WRAP_BOOLEAN = 0x000040;
  public static final int TYPE_WRAP_BYTE = 0x000004;
  public static final int TYPE_WRAP_CHAR = 0x000002;
  public static final int TYPE_WRAP_DATE = 0x000200;
  public static final int TYPE_WRAP_DOUBLE = 0x000100;
  public static final int TYPE_WRAP_FLOAT = 0x000080;
  public static final int TYPE_WRAP_INT = 0x000010;
  public static final int TYPE_WRAP_LONG = 0x000020;
  public static final int TYPE_WRAP_SHORT = 0x000008;
  // types below
  public static final int TYPE_WRAP_STRING = 0x000001;

  public static int classifyType(Class<?> type) {
    int ret = isPrimitiveType(type);
    if (ret != 0) {
      return ret;
    }

    ret = isPrimitiveWrapperType(type);
    if (ret != 0) {
      return ret;
    }

    if (type.isArray()) {
      return TYPE_ARRAY;
    }

    return TYPE_USER;
  }

  public static Method getInterfaceMethod(Class<?> intf, String methodName,
      Class<?>[] paramTypes, boolean includeInherited) {
    try {
      return intf.getDeclaredMethod(methodName, paramTypes);
    } catch (NoSuchMethodException e) {
      if (includeInherited) {
        Class<?>[] superintfs = intf.getInterfaces();
        for (int i = 0; i < superintfs.length; i++) {
          Method method = getInterfaceMethod(superintfs[i], methodName,
            paramTypes, true);
          if (method != null) {
            return method;
          }
        }
      }

      return null;
    }
  }

  public static String getJavahMangledIdent(String before) {
    StringBuffer after = new StringBuffer();
    char[] chars = before.toCharArray();
    for (int i = 0; i < chars.length; i++) {
      char c = chars[i];
      if (c == '_') {
        after.append("_1");
      } else if (c == ';') {
        after.append("_2");
      } else if (c == '[') {
        after.append("_3");
      } else if (c == '/') {
        after.append('_');
      } else if (Character.isJavaIdentifierPart(c)) {
        after.append(c);
      } else {
        throw new UnsupportedOperationException("Cannot handle non-ascii yet");
      }
    }
    return after.toString();
  }

  public static String getSourceRepresentation(Class<?> type) {
    // Primitives
    //
    if (type.equals(Integer.TYPE)) {
      return "int";
    } else if (type.equals(Long.TYPE)) {
      return "long";
    } else if (type.equals(Short.TYPE)) {
      return "short";
    } else if (type.equals(Byte.TYPE)) {
      return "byte";
    } else if (type.equals(Character.TYPE)) {
      return "char";
    } else if (type.equals(Boolean.TYPE)) {
      return "boolean";
    } else if (type.equals(Float.TYPE)) {
      return "float";
    } else if (type.equals(Double.TYPE)) {
      return "double";
    }

    // Arrays
    //
    if (type.isArray()) {
      Class<?> componentType = type.getComponentType();
      return getSourceRepresentation(componentType) + "[]";
    }

    // Everything else
    //
    return type.getName().replace('$', '.');
  }

  public static int isPrimitiveType(Class<?> type) {
    if (type.equals(Integer.TYPE)) {
      return TYPE_PRIM_INT;
    } else if (type.equals(Long.TYPE)) {
      return TYPE_PRIM_LONG;
    } else if (type.equals(Character.TYPE)) {
      return TYPE_PRIM_CHAR;
    } else if (type.equals(Byte.TYPE)) {
      return TYPE_PRIM_BYTE;
    } else if (type.equals(Short.TYPE)) {
      return TYPE_PRIM_SHORT;
    } else if (type.equals(Boolean.TYPE)) {
      return TYPE_PRIM_BOOLEAN;
    } else if (type.equals(Float.TYPE)) {
      return TYPE_PRIM_FLOAT;
    } else if (type.equals(Double.TYPE)) {
      return TYPE_PRIM_DOUBLE;
    } else if (type.equals(Void.TYPE)) {
      return TYPE_PRIM_VOID;
    } else {
      return NOT_FOUND;
    }
  }

  public static int isPrimitiveWrapperType(Class<?> type) {
    if (type.equals(String.class)) {
      return TYPE_WRAP_STRING;
    } else if (type.equals(Integer.class)) {
      return TYPE_WRAP_INT;
    } else if (type.equals(Long.class)) {
      return TYPE_WRAP_LONG;
    } else if (type.equals(Character.class)) {
      return TYPE_WRAP_CHAR;
    } else if (type.equals(Byte.class)) {
      return TYPE_WRAP_BYTE;
    } else if (type.equals(Short.class)) {
      return TYPE_WRAP_SHORT;
    } else if (type.equals(Boolean.class)) {
      return TYPE_WRAP_BOOLEAN;
    } else if (type.equals(Float.class)) {
      return TYPE_WRAP_FLOAT;
    } else if (type.equals(Double.class)) {
      return TYPE_WRAP_DOUBLE;
    } else {
      return NOT_FOUND;
    }
  }
}