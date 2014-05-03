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
package com.google.gwt.dev.jjs.ast;

import com.google.gwt.dev.jjs.SourceOrigin;
import com.google.gwt.dev.util.StringInterner;
import com.google.gwt.dev.util.collect.HashMap;

import java.util.Map;

/**
 * Base class for all Java primitive types.
 */
public class JPrimitiveType extends JType {
  /*
   * Primitive types are static singletons. Serialization via readResolve().
   */

  private static final class Singletons {
    public static final Map<String, JPrimitiveType> map = new HashMap<String, JPrimitiveType>();
  }

  public static final JPrimitiveType BOOLEAN = new JPrimitiveType("boolean", "Z",
      "java.lang.Boolean", JBooleanLiteral.FALSE);

  public static final JPrimitiveType BYTE = new JPrimitiveType("byte", "B", "java.lang.Byte",
      JIntLiteral.ZERO);

  public static final JPrimitiveType CHAR = new JPrimitiveType("char", "C", "java.lang.Character",
      JCharLiteral.NULL);

  public static final JPrimitiveType DOUBLE = new JPrimitiveType("double", "D", "java.lang.Double",
      JDoubleLiteral.ZERO);

  public static final JPrimitiveType FLOAT = new JPrimitiveType("float", "F", "java.lang.Float",
      JFloatLiteral.ZERO);

  public static final JPrimitiveType INT = new JPrimitiveType("int", "I", "java.lang.Integer",
      JIntLiteral.ZERO);

  public static final JPrimitiveType LONG = new JPrimitiveType("long", "J", "java.lang.Long",
      JLongLiteral.ZERO);

  public static final JPrimitiveType SHORT = new JPrimitiveType("short", "S", "java.lang.Short",
      JIntLiteral.ZERO);

  public static final JPrimitiveType VOID = new JPrimitiveType("void", "V", "java.lang.Void", null);

  private final transient JValueLiteral defaultValue;

  private final transient String signatureName;

  private final transient String wrapperTypeName;

  private JPrimitiveType(String name, String signatureName, String wrapperTypeName,
      JValueLiteral defaultValue) {
    super(SourceOrigin.UNKNOWN, name);
    this.defaultValue = defaultValue;
    this.signatureName = StringInterner.get().intern(signatureName);
    this.wrapperTypeName = StringInterner.get().intern(wrapperTypeName);
    Singletons.map.put(this.name, this);
  }

  /**
   * Returns a literal which has been coerced to this type, or <code>null</code>
   * if no such coercion is possible.
   */
  public JValueLiteral coerceLiteral(JValueLiteral value) {
    if (defaultValue != null) {
      return defaultValue.cloneFrom(value);
    }
    return null;
  }

  @Override
  public String getClassLiteralFactoryMethod() {
    return "Class.createForPrimitive";
  }

  @Override
  public final JLiteral getDefaultValue() {
    return defaultValue;
  }

  @Override
  public String getJavahSignatureName() {
    return signatureName;
  }

  @Override
  public String getJsniSignatureName() {
    return signatureName;
  }

  public String getWrapperTypeName() {
    return wrapperTypeName;
  }

  @Override
  public boolean isFinal() {
    return true;
  }

  @Override
  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
    }
    visitor.endVisit(this, ctx);
  }

  /**
   * Returns the JPrimitiveType instance corresponding to {@code typeName} or {@code null} if
   * typeName is not the name of a primitive type.
   */
  public static JPrimitiveType getType(String typeName) {
    return Singletons.map.get(typeName);
  }

  /**
   * Canonicalize to singleton; uses {@link JType#name}.
   */
  private Object readResolve() {
    return Singletons.map.get(name);
  }
}
