/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.autobean.shared;

import com.google.gwt.autobean.shared.impl.LazySplittable;
import com.google.gwt.autobean.shared.impl.StringQuoter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Provides unified encoding and decoding of value objects.
 */
public class ValueCodex {
  enum Type {
    BIG_DECIMAL(BigDecimal.class) {
      @Override
      public boolean canUpcast(Object value) {
        return value instanceof BigDecimal;
      }

      @Override
      public BigDecimal decode(Class<?> clazz, String value) {
        return new BigDecimal(value);
      }

      @Override
      public String toJsonExpression(Object value) {
        return StringQuoter.quote(((BigDecimal) value).toString());
      }
    },
    BIG_INTEGER(BigInteger.class) {
      @Override
      public boolean canUpcast(Object value) {
        return value instanceof BigInteger;
      }

      @Override
      public BigInteger decode(Class<?> clazz, String value) {
        return new BigInteger(value);
      }

      @Override
      public String toJsonExpression(Object value) {
        return StringQuoter.quote(((BigInteger) value).toString());
      }
    },
    BOOLEAN(Boolean.class, boolean.class, false) {
      @Override
      public Boolean decode(Class<?> clazz, String value) {
        return Boolean.valueOf(value);
      }
    },
    BYTE(Byte.class, byte.class, (byte) 0) {
      @Override
      public Byte decode(Class<?> clazz, String value) {
        return Byte.valueOf(value);
      }
    },
    CHARACTER(Character.class, char.class, (char) 0) {
      @Override
      public Character decode(Class<?> clazz, String value) {
        return value.charAt(0);
      }
    },
    DATE(Date.class) {
      @Override
      public boolean canUpcast(Object value) {
        return value instanceof Date;
      }

      @Override
      public Date decode(Class<?> clazz, String value) {
        return StringQuoter.tryParseDate(value);
      }

      @Override
      public String toJsonExpression(Object value) {
        return String.valueOf(((Date) value).getTime());
      }
    },
    DOUBLE(Double.class, double.class, 0d) {
      @Override
      public Double decode(Class<?> clazz, String value) {
        return Double.valueOf(value);
      }
    },
    ENUM(Enum.class) {
      @Override
      public Enum<?> decode(Class<?> clazz, String value) {
        return (Enum<?>) clazz.getEnumConstants()[Integer.valueOf(value)];
      }

      @Override
      public String toJsonExpression(Object value) {
        return String.valueOf(((Enum<?>) value).ordinal());
      }
    },
    FLOAT(Float.class, float.class, 0f) {
      @Override
      public Float decode(Class<?> clazz, String value) {
        return Float.valueOf(value);
      }
    },
    INTEGER(Integer.class, int.class, 0) {
      @Override
      public Integer decode(Class<?> clazz, String value) {
        return Integer.valueOf(value);
      }
    },
    LONG(Long.class, long.class, 0L) {
      @Override
      public Long decode(Class<?> clazz, String value) {
        return Long.valueOf(value);
      }

      @Override
      public String toJsonExpression(Object value) {
        // Longs cannot be expressed as a JS double
        return StringQuoter.quote(String.valueOf((Long) value));
      }
    },
    SHORT(Short.class, short.class, (short) 0) {
      @Override
      public Short decode(Class<?> clazz, String value) {
        return Short.valueOf(value);
      }
    },
    STRING(String.class) {
      @Override
      public String decode(Class<?> clazz, String value) {
        return value;
      }

      @Override
      public String toJsonExpression(Object value) {
        return StringQuoter.quote((String) value);
      }
    },
    VOID(Void.class, void.class, null) {
      @Override
      public Void decode(Class<?> clazz, String value) {
        return null;
      }
    };
    private final Object defaultValue;
    private final Class<?> type;
    private final Class<?> primitiveType;

    Type(Class<?> objectType) {
      this(objectType, null, null);
    }

    Type(Class<?> objectType, Class<?> primitiveType, Object defaultValue) {
      this.type = objectType;
      this.primitiveType = primitiveType;
      this.defaultValue = defaultValue;
    }

    /**
     * Determines whether or not the Type can handle the given value via
     * upcasting semantics.
     */
    public boolean canUpcast(Object value) {
      // Most value types are final, so this method is meaningless
      return false;
    }

    public abstract Object decode(Class<?> clazz, String value);

    public Object getDefaultValue() {
      return defaultValue;
    }

    public Class<?> getPrimitiveType() {
      return primitiveType;
    }

    public Class<?> getType() {
      return type;
    }

    public String toJsonExpression(Object value) {
      return String.valueOf(value);
    }
  }

  private static final Set<Class<?>> ALL_VALUE_TYPES;
  private static final Map<Class<?>, Type> TYPES_BY_CLASS;
  static {
    Map<Class<?>, Type> temp = new HashMap<Class<?>, Type>();
    for (Type t : Type.values()) {
      temp.put(t.getType(), t);
      if (t.getPrimitiveType() != null) {
        temp.put(t.getPrimitiveType(), t);
      }
    }
    ALL_VALUE_TYPES = Collections.unmodifiableSet(temp.keySet());
    TYPES_BY_CLASS = Collections.unmodifiableMap(temp);
  }

  /**
   * Returns true if ValueCodex can operate on values of the given type.
   * 
   * @param clazz a Class object
   * @return {@code true} if the given object type can be decoded
   */
  public static boolean canDecode(Class<?> clazz) {
    if (findType(clazz) != null) {
      return true;
    }
    // Use other platform-specific tests
    return ValueCodexHelper.canDecode(clazz);
  }

  public static <T> T decode(Class<T> clazz, Splittable split) {
    if (split == null || split == LazySplittable.NULL) {
      return null;
    }
    return decode(clazz, split.asString());
  }

  @SuppressWarnings("unchecked")
  public static <T> T decode(Class<T> clazz, String string) {
    if (string == null) {
      return null;
    }
    return (T) getTypeOrDie(clazz).decode(clazz, string);
  }

  /**
   * Encode a value object when the wire format type is known. This method
   * should be preferred over {@link #encode(Object)} when possible.
   */
  public static Splittable encode(Class<?> clazz, Object obj) {
    if (obj == null) {
      return LazySplittable.NULL;
    }
    return new LazySplittable(getTypeOrDie(clazz).toJsonExpression(obj));
  }

  public static Splittable encode(Object obj) {
    if (obj == null) {
      return LazySplittable.NULL;
    }
    Type t = findType(obj.getClass());
    // Try upcasting
    if (t == null) {
      for (Type maybe : Type.values()) {
        if (maybe.canUpcast(obj)) {
          t = maybe;
          break;
        }
      }
    }
    if (t == null) {
      throw new UnsupportedOperationException(obj.getClass().getName());
    }
    return new LazySplittable(t.toJsonExpression(obj));
  }

  /**
   * Return all Value types that can be processed by the ValueCodex.
   */
  public static Set<Class<?>> getAllValueTypes() {
    return ALL_VALUE_TYPES;
  }

  /**
   * Returns the uninitialized field value for the given primitive type.
   */
  public static Object getUninitializedFieldValue(Class<?> clazz) {
    Type type = getTypeOrDie(clazz);
    if (clazz.equals(type.getPrimitiveType())) {
      return type.getDefaultValue();
    }
    return null;
  }

  /**
   * May return <code>null</code>.
   */
  private static <T> Type findType(Class<T> clazz) {
    if (clazz.isEnum()) {
      return Type.ENUM;
    }
    return TYPES_BY_CLASS.get(clazz);
  }

  private static <T> Type getTypeOrDie(Class<T> clazz) {
    Type toReturn = findType(clazz);
    if (toReturn == null) {
      throw new UnsupportedOperationException(clazz.getName());
    }
    return toReturn;
  }
}
