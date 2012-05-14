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
package com.google.web.bindery.autobean.shared;

import com.google.web.bindery.autobean.shared.impl.StringQuoter;

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
      public BigDecimal decode(Class<?> clazz, Splittable value) {
        return new BigDecimal(value.asString());
      }

      @Override
      public Splittable encode(Object value) {
        return StringQuoter.create(((BigDecimal) value).toString());
      }
    },
    BIG_INTEGER(BigInteger.class) {
      @Override
      public boolean canUpcast(Object value) {
        return value instanceof BigInteger;
      }

      @Override
      public BigInteger decode(Class<?> clazz, Splittable value) {
        return new BigInteger(value.asString());
      }

      @Override
      public Splittable encode(Object value) {
        return StringQuoter.create(((BigInteger) value).toString());
      }
    },
    BOOLEAN(Boolean.class, boolean.class, false) {
      @Override
      public Boolean decode(Class<?> clazz, Splittable value) {
        return value.asBoolean();
      }

      @Override
      public Splittable encode(Object value) {
        return StringQuoter.create((Boolean) value);
      }
    },
    BYTE(Byte.class, byte.class, (byte) 0) {
      @Override
      public Byte decode(Class<?> clazz, Splittable value) {
        return (byte) value.asNumber();
      }

      @Override
      public Splittable encode(Object value) {
        return StringQuoter.create((Byte) value);
      }
    },
    CHARACTER(Character.class, char.class, (char) 0) {
      @Override
      public Character decode(Class<?> clazz, Splittable value) {
        return value.asString().charAt(0);
      }

      @Override
      public Splittable encode(Object value) {
        return StringQuoter.create(String.valueOf((Character) value));
      }
    },
    DATE(Date.class) {
      @Override
      public boolean canUpcast(Object value) {
        return value instanceof Date;
      }

      @Override
      public Date decode(Class<?> clazz, Splittable value) {
        if (value.isNumber()) {
          return new Date((long) value.asNumber());
        }
        return StringQuoter.tryParseDate(value.asString());
      }

      @Override
      public Splittable encode(Object value) {
        return StringQuoter.create(String.valueOf(((Date) value).getTime()));
      }
    },
    DOUBLE(Double.class, double.class, 0d) {
      @Override
      public Double decode(Class<?> clazz, Splittable value) {
        return value.asNumber();
      }

      @Override
      public Splittable encode(Object value) {
        return StringQuoter.create((Double) value);
      }
    },
    ENUM(Enum.class) {
      @Override
      public boolean canUpcast(Object value) {
        return value instanceof Enum;
      }

      @Override
      public Enum<?> decode(Class<?> clazz, Splittable value) {
        return (Enum<?>) clazz.getEnumConstants()[(int) value.asNumber()];
      }

      @Override
      public Splittable encode(Object value) {
        return StringQuoter.create(((Enum<?>) value).ordinal());
      }
    },
    FLOAT(Float.class, float.class, 0f) {
      @Override
      public Float decode(Class<?> clazz, Splittable value) {
        return (float) value.asNumber();
      }

      @Override
      public Splittable encode(Object value) {
        return StringQuoter.create((Float) value);
      }
    },
    INTEGER(Integer.class, int.class, 0) {
      @Override
      public Integer decode(Class<?> clazz, Splittable value) {
        return Integer.valueOf((int) value.asNumber());
      }

      @Override
      public Splittable encode(Object value) {
        return StringQuoter.create((Integer) value);
      }
    },
    LONG(Long.class, long.class, 0L) {
      @Override
      public Long decode(Class<?> clazz, Splittable value) {
        if (value.isNumber()) {
          return Long.valueOf((long) value.asNumber());
        }
        return Long.parseLong(value.asString());
      }

      @Override
      public Splittable encode(Object value) {
        return StringQuoter.create(String.valueOf((Long) value));
      }
    },
    SHORT(Short.class, short.class, (short) 0) {
      @Override
      public Short decode(Class<?> clazz, Splittable value) {
        return (short) value.asNumber();
      }

      @Override
      public Splittable encode(Object value) {
        return StringQuoter.create((Short) value);
      }
    },
    STRING(String.class) {
      @Override
      public String decode(Class<?> clazz, Splittable value) {
        return value.asString();
      }

      @Override
      public Splittable encode(Object value) {
        return StringQuoter.create((String) value);
      }
    },
    SPLITTABLE(Splittable.class) {
      @Override
      public Splittable decode(Class<?> clazz, Splittable value) {
        return value;
      }

      @Override
      public Splittable encode(Object value) {
        return (Splittable) value;
      }
    },
    VOID(Void.class, void.class, null) {
      @Override
      public Void decode(Class<?> clazz, Splittable value) {
        return null;
      }

      @Override
      public Splittable encode(Object value) {
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
     * 
     * @param value a value Object
     */
    public boolean canUpcast(Object value) {
      // Most value types are final, so this method is meaningless
      return false;
    }

    public abstract Object decode(Class<?> clazz, Splittable value);

    public abstract Splittable encode(Object value);

    public Object getDefaultValue() {
      return defaultValue;
    }

    public Class<?> getPrimitiveType() {
      return primitiveType;
    }

    public Class<?> getType() {
      return type;
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

  @SuppressWarnings("unchecked")
  public static <T> T decode(Class<T> clazz, Splittable split) {
    if (split == null || split == Splittable.NULL) {
      return null;
    }
    return (T) getTypeOrDie(clazz).decode(clazz, split);
  }

  /**
   * No callers in GWT codebase.
   * 
   * @deprecated use {@link #decode(Class, Splittable)} instead.
   * @throws UnsupportedOperationException
   */
  @Deprecated
  public static <T> T decode(Class<T> clazz, String string) {
    throw new UnsupportedOperationException();
  }

  /**
   * Encode a value object when the wire format type is known. This method
   * should be preferred over {@link #encode(Object)} when possible.
   */
  public static Splittable encode(Class<?> clazz, Object obj) {
    if (obj == null) {
      return Splittable.NULL;
    }
    return getTypeOrDie(clazz).encode(obj);
  }

  public static Splittable encode(Object obj) {
    if (obj == null) {
      return Splittable.NULL;
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
    return t.encode(obj);
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
