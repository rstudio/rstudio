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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides unified encoding and decoding of value objects.
 */
public class ValueCodex {
  enum Type {
    BIG_DECIMAL(BigDecimal.class) {
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
      public Date decode(Class<?> clazz, String value) {
        return new Date(Long.valueOf(value));
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

  private static Map<Class<?>, Type> typesByClass = new HashMap<Class<?>, Type>();
  static {
    for (Type t : Type.values()) {
      typesByClass.put(t.getType(), t);
      if (t.getPrimitiveType() != null) {
        typesByClass.put(t.getPrimitiveType(), t);
      }
    }
  }

  /**
   * Returns true if ValueCodex can operate on values of the given type.
   * 
   * @param clazz a Class object
   * @return {@code true} if the given object type can be decoded
   */
  public static boolean canDecode(Class<?> clazz) {
    return findType(clazz) != null;
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

  public static Splittable encode(Object obj) {
    if (obj == null) {
      return LazySplittable.NULL;
    }
    return new LazySplittable(
        getTypeOrDie(obj.getClass()).toJsonExpression(obj));
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
    Type type = typesByClass.get(clazz);
    if (type == null) {
      // Necessary due to lack of Class.isAssignable() in client-side
      if (clazz.getEnumConstants() != null) {
        return Type.ENUM;
      }
    }
    return type;
  }

  private static <T> Type getTypeOrDie(Class<T> clazz) {
    Type toReturn = findType(clazz);
    if (toReturn == null) {
      throw new UnsupportedOperationException(clazz.getName());
    }
    return toReturn;
  }
}
