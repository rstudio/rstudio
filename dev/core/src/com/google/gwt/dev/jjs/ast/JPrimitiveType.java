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
import com.google.gwt.thirdparty.guava.common.collect.ImmutableCollection;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableList;
import com.google.gwt.thirdparty.guava.common.collect.Maps;

import java.util.Map;

/**
 * Base class for all Java primitive types.
 */
public class JPrimitiveType extends JType {
  private static final Map<String, JPrimitiveType> primitiveTypeByName = Maps.newHashMap();

  /*
   * Primitive types are static singletons. Serialization via readResolve().
   */
  public static final JPrimitiveType BOOLEAN = new JPrimitiveType(
      "boolean", "Z", "java.lang.Boolean", JBooleanLiteral.FALSE, Coercion.TO_BOOLEAN);
  public static final JPrimitiveType BYTE =
      new JPrimitiveType("byte", "B", "java.lang.Byte", JIntLiteral.ZERO, Coercion.TO_BYTE);
  public static final JPrimitiveType CHAR =
      new JPrimitiveType("char", "C", "java.lang.Character", JCharLiteral.NULL, Coercion.TO_CHAR);
  public static final JPrimitiveType DOUBLE = new JPrimitiveType(
      "double", "D", "java.lang.Double", JDoubleLiteral.ZERO, Coercion.TO_DOUBLE);
  public static final JPrimitiveType FLOAT =
      new JPrimitiveType("float", "F", "java.lang.Float", JFloatLiteral.ZERO, Coercion.TO_FLOAT);
  public static final JPrimitiveType INT =
      new JPrimitiveType("int", "I", "java.lang.Integer", JIntLiteral.ZERO, Coercion.TO_INT);
  public static final JPrimitiveType LONG =
      new JPrimitiveType("long", "J", "java.lang.Long", JLongLiteral.ZERO, Coercion.TO_LONG);
  public static final JPrimitiveType SHORT =
      new JPrimitiveType("short", "S", "java.lang.Short", JIntLiteral.ZERO, Coercion.TO_SHORT);
  public static final JPrimitiveType VOID =
      new JPrimitiveType("void", "V", "java.lang.VOID", null, Coercion.TO_VOID);

  public static final ImmutableCollection<JPrimitiveType> types = ImmutableList.of(BOOLEAN, BYTE,
      CHAR, DOUBLE, FLOAT, INT, LONG, SHORT, VOID);

  private final transient JValueLiteral defaultValue;
  private final transient String signatureName;
  private final transient String wrapperTypeName;
  private final transient Coercion coercion;

  private JPrimitiveType(String name, String signatureName, String wrapperTypeName,
      JValueLiteral defaultValue, Coercion coercion) {
    super(SourceOrigin.UNKNOWN, name);
    this.defaultValue = defaultValue;
    this.signatureName = StringInterner.get().intern(signatureName);
    this.wrapperTypeName = StringInterner.get().intern(wrapperTypeName);
    this.coercion = coercion;
    primitiveTypeByName.put(this.name, this);
  }

  @Override
  public boolean canBeNull() {
    return false;
  }

  @Override
  public boolean isArrayType() {
    return false;
  }

  @Override
  public boolean isJsType() {
    return false;
  }

  @Override
  public boolean isJsFunction() {
    return false;
  }

  @Override
  public boolean isJsNative() {
    return false;
  }

  @Override
  public boolean canBeImplementedExternally() {
    return false;
  }

  @Override
  public boolean canBeSubclass() {
    return false;
  }

  public JValueLiteral coerce(JValueLiteral literal) {
    return this.coercion.coerce(literal);
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

  @Override
  public JEnumType isEnumOrSubclass() {
    return null;
  }

  @Override
  public JPrimitiveType strengthenToNonNull() {
    return this;
  }

  public String getWrapperTypeName() {
    return wrapperTypeName;
  }

  @Override
  public boolean isFinal() {
    return true;
  }

  @Override
  public boolean isJsoType() {
    return false;
  }

  @Override
  public boolean canBeReferencedExternally() {
    return this != JPrimitiveType.LONG;
  }

  @Override
  public boolean isJavaLangObject() {
    return false;
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
    return primitiveTypeByName.get(typeName);
  }

  /**
   * Canonicalize to singleton; uses {@link JType#name}.
   */
  private Object readResolve() {
    return primitiveTypeByName.get(name);
  }

  private enum Coercion {
    TO_CHAR() {
      @Override
      JValueLiteral coerce(JValueLiteral literal) {
        if (literal instanceof JCharLiteral) {
          return literal;
        }

        Object valueObject = literal.getValueObj();
        if (valueObject instanceof Number) {
          return new JCharLiteral(
              literal.getSourceInfo(), (char) ((Number) valueObject).intValue());
        }
        return null;
      }
    },
    TO_INT() {
      @Override
      JValueLiteral coerce(JValueLiteral literal) {
        Object valueObject = literal.getValueObj();
        if (!(valueObject instanceof Number) && !(valueObject instanceof Character)) {
          return null;
        }
        int value = 0;
        if (valueObject instanceof Number) {
          value = ((Number) valueObject).intValue();
        } else if (valueObject instanceof Character) {
          value = ((Character) valueObject).charValue();
        }
        return new JIntLiteral(literal.getSourceInfo(), value);
      }
    },
    TO_BYTE() {
      @Override
      JValueLiteral coerce(JValueLiteral literal) {
        Object valueObject = literal.getValueObj();
        if (!(valueObject instanceof Number) && !(valueObject instanceof Character)) {
          return null;
        }
        byte value = 0;
        if (valueObject instanceof Number) {
          value = ((Number) valueObject).byteValue();
        } else if (valueObject instanceof Character) {
          value = (byte) ((Character) valueObject).charValue();
        }
        return new JIntLiteral(literal.getSourceInfo(), value);
      }
    },
    TO_SHORT() {
      @Override
      JValueLiteral coerce(JValueLiteral literal) {
        Object valueObject = literal.getValueObj();
        if (!(valueObject instanceof Number) && !(valueObject instanceof Character)) {
          return null;
        }
        short value = 0;
        if (valueObject instanceof Number) {
          value = ((Number) valueObject).shortValue();
        } else if (valueObject instanceof Character) {
          value = (short) ((Character) valueObject).charValue();
        }
        return new JIntLiteral(literal.getSourceInfo(), value);
      }
    },
    TO_LONG() {
      @Override
      JValueLiteral coerce(JValueLiteral literal) {
        Object valueObject = literal.getValueObj();
        if (!(valueObject instanceof Number) && !(valueObject instanceof Character)) {
          return null;
        }
        long value = 0;
        if (valueObject instanceof Number) {
          value = ((Number) valueObject).longValue();
        } else if (valueObject instanceof Character) {
          value = (long) ((Character) valueObject).charValue();
        }
        return new JLongLiteral(literal.getSourceInfo(), value);
      }
    },
    TO_FLOAT() {
      @Override
      JValueLiteral coerce(JValueLiteral literal) {
        Object valueObject = literal.getValueObj();
        if (!(valueObject instanceof Number) && !(valueObject instanceof Character)) {
          return null;
        }
        float value = 0;
        if (valueObject instanceof Number) {
          value = ((Number) valueObject).floatValue();
        } else if (valueObject instanceof Character) {
          value = (float) ((Character) valueObject).charValue();
        }
        return new JFloatLiteral(literal.getSourceInfo(), value);
      }
    },
    TO_DOUBLE() {
      @Override
      JValueLiteral coerce(JValueLiteral literal) {
        Object valueObject = literal.getValueObj();
        if (!(valueObject instanceof Number) && !(valueObject instanceof Character)) {
          return null;
        }
        double value = 0;
        if (valueObject instanceof Number) {
          value = ((Number) valueObject).doubleValue();
        } else if (valueObject instanceof Character) {
          value = (double) ((Character) valueObject).charValue();
        }
        return new JDoubleLiteral(literal.getSourceInfo(), value);
      }
    },
    TO_BOOLEAN() {
      @Override
      public JValueLiteral coerce(JValueLiteral literal) {
        if (literal instanceof JBooleanLiteral) {
          return literal;
        }
        return null;
      }
    },
    TO_VOID() {
      @Override
      public JValueLiteral coerce(JValueLiteral literal) {
        return null;
      }
    };

    /**
     * Coerces a literal into a literal of (possibly) a different type; returns {@code null} if
     * coercion is not valid.
     */
    abstract JValueLiteral coerce(JValueLiteral literal);
  }
}
