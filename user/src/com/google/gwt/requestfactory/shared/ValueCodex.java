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
package com.google.gwt.requestfactory.shared;

import com.google.gwt.core.client.JsonUtils;

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
    },
    BIG_INTEGER(BigInteger.class) {
      @Override
      public BigInteger decode(Class<?> clazz, String value) {
        return new BigInteger(value);
      }
    },
    BOOLEAN(Boolean.class, boolean.class) {
      @Override
      public Boolean decode(Class<?> clazz, String value) {
        return Boolean.valueOf(value);
      }
    },
    BYTE(Byte.class, byte.class) {
      @Override
      public Byte decode(Class<?> clazz, String value) {
        return Byte.valueOf(value);
      }
    },
    CHARACTER(Character.class, char.class) {
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
      public String encode(Object value) {
        return String.valueOf(((Date) value).getTime());
      }
    },
    DOUBLE(Double.class, double.class) {
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
      public String encode(Object value) {
        return String.valueOf(((Enum<?>) value).ordinal());
      }
    },
    FLOAT(Float.class, float.class) {
      @Override
      public Float decode(Class<?> clazz, String value) {
        return Float.valueOf(value);
      }
    },
    INTEGER(Integer.class, int.class) {
      @Override
      public Integer decode(Class<?> clazz, String value) {
        return Integer.valueOf(value);
      }
    },
    LONG(Long.class, long.class) {
      @Override
      public Long decode(Class<?> clazz, String value) {
        return Long.valueOf(value);
      }
    },
    SHORT(Short.class, short.class) {
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
      public String encode(Object value) {
        return JsonUtils.escapeValue(String.valueOf(value));
      }
    },
    VOID(Void.class, void.class) {
      @Override
      public Void decode(Class<?> clazz, String value) {
        return null;
      }
    };
    private final Class<?> type;
    private final Class<?> primitiveType;

    Type(Class<?> objectType) {
      this(objectType, null);
    }

    Type(Class<?> objectType, Class<?> primitiveType) {
      this.type = objectType;
      this.primitiveType = primitiveType;
    }

    public abstract Object decode(Class<?> clazz, String value);

    public String encode(Object value) {
      return String.valueOf(value);
    }

    public Class<?> getPrimitiveType() {
      return primitiveType;
    }

    public Class<?> getType() {
      return type;
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
   */
  public static boolean canDecode(Class<?> clazz) {
    return typesByClass.containsKey(clazz);
  }

  /**
   * Convert an encoded representation of a value into a value.
   * 
   * @param <T> the type of value desired
   * @param clazz the type of value desired
   * @param encoded the encoded representation of the value
   * @return the value
   * @throws IllegalArgumentException if <code>clazz</code> is not a supported
   *           value type
   */
  @SuppressWarnings("unchecked")
  public static <T> T convertFromString(Class<T> clazz, String encoded) {
    if (encoded == null) {
      return null;
    }
    return (T) getType(clazz).decode(clazz, encoded);
  }

  /**
   * Convert a value into an encoded string representation.
   * 
   * @param obj the value to encode
   * @return an encoded representation of the object
   * @throws IllegalArgumentException if <code>obj</code> is not of a supported
   *           type
   */
  public static String encodeForJsonPayload(Object obj) {
    return getType(obj.getClass()).encode(obj);
  }

  private static <T> Type getType(Class<T> clazz) {
    Type type = typesByClass.get(clazz);
    if (type == null) {
      // Necessary due to lack of Class.isAssignable() in client-side
      if (clazz.getEnumConstants() != null) {
        return Type.ENUM;
      }
      throw new IllegalArgumentException(clazz.getName());
    }
    return type;
  }
}
