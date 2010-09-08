/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.rpc.server;

import com.google.gwt.rpc.client.ast.BooleanValueCommand;
import com.google.gwt.rpc.client.ast.ByteValueCommand;
import com.google.gwt.rpc.client.ast.CharValueCommand;
import com.google.gwt.rpc.client.ast.DoubleValueCommand;
import com.google.gwt.rpc.client.ast.FloatValueCommand;
import com.google.gwt.rpc.client.ast.IntValueCommand;
import com.google.gwt.rpc.client.ast.LongValueCommand;
import com.google.gwt.rpc.client.ast.ShortValueCommand;
import com.google.gwt.rpc.client.ast.StringValueCommand;
import com.google.gwt.rpc.client.ast.ValueCommand;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.util.Map;

import sun.misc.Unsafe;

/**
 * Contains common utility code.
 */
public class CommandSerializationUtil {

  /**
   * Defines methods for getting and setting fields.
   */
  public interface Accessor {
    boolean canMakeValueCommand();

    /**
     * Indicates if set can be called with a value of the given type.
     */
    boolean canSet(Class<?> clazz);

    Object get(Object instance, Field f);

    Class<?> getTargetType();

    ValueCommand makeValueCommand(Object value);

    Object readNext(SerializationStreamReader reader)
        throws SerializationException;

    void set(Object instance, Field f, Object value);

    void set(Object array, int index, Object value);
  }

  /**
   * Defines type-specific methods of getting and setting fields.
   */
  private static enum TypeAccessor implements Accessor {
    BOOL {
      @Override
      public boolean canSet(Class<?> clazz) {
        return Boolean.class.isAssignableFrom(clazz)
            || Number.class.isAssignableFrom(clazz)
            || String.class.isAssignableFrom(clazz);
      }

      @Override
      public Object get(Object instance, long offset) {
        return theUnsafe.getBoolean(instance, offset);
      }

      @Override
      public Object getDefaultValue() {
        return false;
      }

      @Override
      public Class<?> getTargetType() {
        return boolean.class;
      }

      @Override
      public ValueCommand makeValueCommand(Object value) {
        return new BooleanValueCommand((Boolean) value);
      }

      @Override
      public Object readNext(SerializationStreamReader reader)
          throws SerializationException {
        return reader.readBoolean();
      }

      @Override
      public void set(Object instance, long offset, Object value) {
        theUnsafe.putBoolean(instance, offset, toBoolean(value));
      }

      private boolean toBoolean(Object value) {
        if (value instanceof Number) {
          return ((Number) value).intValue() != 0;
        } else if (value instanceof String) {
          return Boolean.valueOf((String) value);
        } else {
          // returns false if the value is null.
          return Boolean.TRUE.equals(value);
        }
      }

    },
    BYTE {
      @Override
      public Object get(Object instance, long offset) {
        return theUnsafe.getByte(instance, offset);
      }

      @Override
      public Class<?> getTargetType() {
        return byte.class;
      }

      @Override
      public ValueCommand makeValueCommand(Object value) {
        return new ByteValueCommand((Byte) value);
      }

      @Override
      public Object readNext(SerializationStreamReader reader)
          throws SerializationException {
        return reader.readByte();
      }

      @Override
      public void set(Object instance, long offset, Object value) {
        theUnsafe.putByte(instance, offset, ((Number) value).byteValue());
      }
    },
    CHAR {
      @Override
      public Object get(Object instance, long offset) {
        return theUnsafe.getChar(instance, offset);
      }

      @Override
      public Class<?> getTargetType() {
        return char.class;
      }

      @Override
      public ValueCommand makeValueCommand(Object value) {
        return new CharValueCommand((Character) value);
      }

      @Override
      public Object readNext(SerializationStreamReader reader)
          throws SerializationException {
        return reader.readChar();
      }

      @Override
      public void set(Object instance, long offset, Object value) {
        char c = (value instanceof Number) ? (char) ((Number) value).intValue()
            : (Character) value;
        theUnsafe.putChar(instance, offset, c);
      }
    },
    DOUBLE {
      @Override
      public Object get(Object instance, long offset) {
        return theUnsafe.getDouble(instance, offset);
      }

      @Override
      public Class<?> getTargetType() {
        return double.class;
      }

      @Override
      public ValueCommand makeValueCommand(Object value) {
        return new DoubleValueCommand((Double) value);
      }

      @Override
      public Object readNext(SerializationStreamReader reader)
          throws SerializationException {
        return reader.readDouble();
      }

      @Override
      public void set(Object instance, long offset, Object value) {
        theUnsafe.putDouble(instance, offset, ((Number) value).doubleValue());
      }
    },
    FLOAT {
      @Override
      public Object get(Object instance, long offset) {
        return theUnsafe.getFloat(instance, offset);
      }

      @Override
      public Class<?> getTargetType() {
        return float.class;
      }

      @Override
      public ValueCommand makeValueCommand(Object value) {
        return new FloatValueCommand((Float) value);
      }

      @Override
      public Object readNext(SerializationStreamReader reader)
          throws SerializationException {
        return reader.readFloat();
      }

      @Override
      public void set(Object instance, long offset, Object value) {
        theUnsafe.putFloat(instance, offset, ((Number) value).floatValue());
      }
    },
    INT {
      @Override
      public Object get(Object instance, long offset) {
        return theUnsafe.getInt(instance, offset);
      }

      @Override
      public Class<?> getTargetType() {
        return int.class;
      }

      @Override
      public ValueCommand makeValueCommand(Object value) {
        return new IntValueCommand(((Number) value).intValue());
      }

      @Override
      public Object readNext(SerializationStreamReader reader)
          throws SerializationException {
        return reader.readInt();
      }

      @Override
      public void set(Object instance, long offset, Object value) {
        theUnsafe.putInt(instance, offset, ((Number) value).intValue());
      }
    },
    LONG {
      @Override
      public Object get(Object instance, long offset) {
        return theUnsafe.getLong(instance, offset);
      }

      @Override
      public Class<?> getTargetType() {
        return long.class;
      }

      @Override
      public ValueCommand makeValueCommand(Object value) {
        return new LongValueCommand((Long) value);
      }

      @Override
      public Object readNext(SerializationStreamReader reader)
          throws SerializationException {
        return reader.readLong();
      }

      @Override
      public void set(Object instance, long offset, Object value) {
        theUnsafe.putLong(instance, offset, ((Number) value).longValue());
      }
    },
    OBJECT {
      @Override
      public boolean canMakeValueCommand() {
        return false;
      }

      @Override
      public boolean canSet(Class<?> clazz) {
        return Object.class.isAssignableFrom(clazz);
      }

      @Override
      public Object get(Object instance, long offset) {
        return theUnsafe.getObject(instance, offset);
      }

      @Override
      public Object getDefaultValue() {
        return null;
      }

      @Override
      public Class<?> getTargetType() {
        return Object.class;
      }

      @Override
      public ValueCommand makeValueCommand(Object value) {
        throw new RuntimeException("Cannot call makeValueCommand for Objects");
      }

      @Override
      public Object readNext(SerializationStreamReader reader)
          throws SerializationException {
        return reader.readObject();
      }

      @Override
      public void set(Object instance, long offset, Object value) {
        theUnsafe.putObject(instance, offset, value);
      }
    },
    SHORT {
      @Override
      public Object get(Object instance, long offset) {
        return theUnsafe.getShort(instance, offset);
      }

      @Override
      public Class<?> getTargetType() {
        return short.class;
      }

      @Override
      public ValueCommand makeValueCommand(Object value) {
        return new ShortValueCommand((Short) value);
      }

      @Override
      public Object readNext(SerializationStreamReader reader)
          throws SerializationException {
        return reader.readShort();
      }

      @Override
      public void set(Object instance, long offset, Object value) {
        theUnsafe.putShort(instance, offset, ((Number) value).shortValue());
      }
    },
    STRING {
      @Override
      public boolean canSet(Class<?> clazz) {
        return true;
      }

      @Override
      public Object get(Object instance, long offset) {
        return theUnsafe.getObject(instance, offset);
      }

      @Override
      public Object getDefaultValue() {
        return null;
      }

      @Override
      public Class<?> getTargetType() {
        return String.class;
      }

      @Override
      public ValueCommand makeValueCommand(Object value) {
        return new StringValueCommand((String) value);
      }

      @Override
      public Object readNext(SerializationStreamReader reader)
          throws SerializationException {
        return reader.readObject();
      }

      @Override
      public void set(Object instance, long offset, Object value) {
        theUnsafe.putObject(instance, offset, value == null ? null
            : value.toString());
      }
    };

    private final Class<?> arrayType = Array.newInstance(getTargetType(), 0).getClass();
    private final long arrayBase = theUnsafe.arrayBaseOffset(arrayType);
    private final long arrayIndexScale = theUnsafe.arrayIndexScale(arrayType);

    public boolean canMakeValueCommand() {
      return true;
    }

    public boolean canSet(Class<?> clazz) {
      return Number.class.isAssignableFrom(clazz);
    }

    public Object get(Object instance, Field f) {
      long offset = objectFieldOffset(f);
      return get(instance, offset);
    }

    public abstract Object get(Object instance, long offset);

    public Object getDefaultValue() {
      return 0;
    }

    public abstract Class<?> getTargetType();

    public abstract ValueCommand makeValueCommand(Object value);

    public abstract Object readNext(SerializationStreamReader reader)
        throws SerializationException;

    public void set(Object instance, Field f, Object value) {
      long offset = objectFieldOffset(f);
      set(instance, offset, value == null ? getDefaultValue() : value);
    }

    public void set(Object array, int index, Object value) {
      set(array, arrayBase + arrayIndexScale * index, value);
    }

    public abstract void set(Object instance, long offset, Object value);
  }

  private static final Map<Class<?>, Accessor> ACCESSORS = new IdentityHashMap<Class<?>, Accessor>();
  private static final Unsafe theUnsafe;

  static {
    Exception ex = null;
    Unsafe localUnsafe = null;
    try {
      Field f = Unsafe.class.getDeclaredField("theUnsafe");
      f.setAccessible(true);
      localUnsafe = (Unsafe) f.get(null);
    } catch (SecurityException e) {
      ex = e;
    } catch (NoSuchFieldException e) {
      ex = e;
    } catch (IllegalArgumentException e) {
      ex = e;
    } catch (IllegalAccessException e) {
      ex = e;
    }
    if (ex != null) {
      throw new RuntimeException("Unable to get Unsafe instance", ex);
    } else {
      theUnsafe = localUnsafe;
    }

    for (TypeAccessor setter : TypeAccessor.values()) {
      ACCESSORS.put(setter.getTargetType(), setter);
    }
  }

  public static Accessor getAccessor(Class<?> clazz) {
    Accessor toReturn = ACCESSORS.get(clazz);
    if (toReturn == null) {
      toReturn = TypeAccessor.OBJECT;
    }
    return toReturn;
  }

  /**
   * TODO: In the future it may be preferable to use a custom ClassLoader to
   * inject a constructor that will initialize all of the final fields that we
   * care about.
   */
  static <T> T allocateInstance(Class<T> clazz) throws InstantiationException {
    Object obj = theUnsafe.allocateInstance(clazz);
    return clazz.cast(obj);
  }

  private static long objectFieldOffset(Field f) {
    return theUnsafe.objectFieldOffset(f);
  }

  private CommandSerializationUtil() {
  }
}
