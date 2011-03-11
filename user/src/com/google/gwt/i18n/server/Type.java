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
package com.google.gwt.i18n.server;

import com.google.gwt.safehtml.shared.SafeHtml;

import java.sql.Date;
import java.util.Map;

/**
 * A return type or parameter type in a Messages/Constants method.
 */
public class Type {

  /**
   * An array type.
   */
  public static class ArrayType extends Type {

    private final Type componentType;

    public ArrayType(String sourceName, Type componentType) {
      super(sourceName);
      this.componentType = componentType;
    }

    @Override
    public Type getComponentType() {
      return componentType;
    }
  }

  /**
   * An enum type.
   */
  public static class EnumType extends Type {

    private final String[] values;

    public EnumType(String sourceName, String[] values) {
      super(sourceName);
      this.values = values;
    }

    @Override
    public String[] getEnumValues() {
      return values;
    }

    public int getOrdinal(String name) {
      for (int i = 0; i < values.length; ++i) {
        if (values[i].equals(name)) {
          return i;
        }
      }
      return -1;
    }
  }

  /**
   * A list type.
   */
  public static class ListType extends Type {

    private final Type componentType;

    public ListType(String sourceName, Type componentType) {
      super(sourceName);
      this.componentType = componentType;
    }

    @Override
    public Type getComponentType() {
      return componentType;
    }
  }

  // Singletons for most types, only array/list and generic user objects aren't
  public static final Type BOOLEAN = new Type("boolean");

  public static final Type BYTE = new Type("byte");

  public static final Type CHAR = new Type("char");

  public static final Type DATE = new Type(Date.class.getCanonicalName());

  public static final Type DOUBLE = new Type("double");

  public static final Type FLOAT = new Type("float");

  public static final Type INT = new Type("int");

  public static final Type LONG = new Type("long");

  public static final Type NUMBER = new Type("Number");

  public static final Type OBJECT = new Type("Object");

  public static final Type SHORT = new Type("short");

  public static final Type SAFEHTML = new Type(SafeHtml.class.getCanonicalName());

  public static final Type STRING = new Type("String");

  public static final Type STRING_MAP = new Type(Map.class.getCanonicalName()
      + "<String, String>");

  private final String sourceName;

  public Type(String sourceName) {
    this.sourceName = sourceName;
  }

  /**
   * Get the component type, if this type is an array or list.
   * 
   * @return component type, or null if not an array or list
   */
  public Type getComponentType() {
    return null;
  }

  /**
   * Get the list of value names, if this type is an enum.
   * 
   * @return values, or null if not an enum
   */
  public String[] getEnumValues() {
    return null;
  }

  /**
   * Get the name of this type as it would appear in source code.
   * 
   * @return source name
   */
  public String getSourceName() {
    return sourceName;
  }

  @Override
  public String toString() {
    return sourceName;
  }
}