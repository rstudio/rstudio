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
package com.google.gwt.user.server.rpc.impl;

import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.impl.AbstractSerializationStreamReader;
import com.google.gwt.user.server.Base64Utils;
import com.google.gwt.user.server.rpc.RPC;
import com.google.gwt.user.server.rpc.SerializationPolicy;
import com.google.gwt.user.server.rpc.SerializationPolicyProvider;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/**
 * For internal use only. Used for server call serialization. This class is
 * carefully matched with the client-side version.
 */
public final class ServerSerializationStreamReader extends
    AbstractSerializationStreamReader {

  /**
   * Used to accumulate elements while deserializing array types. The generic
   * type of the BoundedList will vary from the component type of the array it
   * is intended to create when the array is of a primitive type.
   * 
   * @param <T> The type of object used to hold the data in the buffer
   */
  private static class BoundedList<T> extends LinkedList<T> {
    private final Class<?> componentType;
    private final int expectedSize;

    public BoundedList(Class<?> componentType, int expectedSize) {
      this.componentType = componentType;
      this.expectedSize = expectedSize;
    }

    @Override
    public boolean add(T o) {
      assert size() < getExpectedSize();
      return super.add(o);
    }

    public Class<?> getComponentType() {
      return componentType;
    }

    public int getExpectedSize() {
      return expectedSize;
    }
  }

  /**
   * Enumeration used to provided typed instance readers.
   */
  private enum ValueReader {
    BOOLEAN {
      @Override
      Object readValue(ServerSerializationStreamReader stream)
          throws SerializationException {
        return stream.readBoolean();
      }
    },
    BYTE {
      @Override
      Object readValue(ServerSerializationStreamReader stream)
          throws SerializationException {
        return stream.readByte();
      }
    },
    CHAR {
      @Override
      Object readValue(ServerSerializationStreamReader stream)
          throws SerializationException {
        return stream.readChar();
      }
    },
    DOUBLE {
      @Override
      Object readValue(ServerSerializationStreamReader stream)
          throws SerializationException {
        return stream.readDouble();
      }
    },
    FLOAT {
      @Override
      Object readValue(ServerSerializationStreamReader stream)
          throws SerializationException {
        return stream.readFloat();
      }
    },
    INT {
      @Override
      Object readValue(ServerSerializationStreamReader stream)
          throws SerializationException {
        return stream.readInt();
      }
    },
    LONG {
      @Override
      Object readValue(ServerSerializationStreamReader stream)
          throws SerializationException {
        return stream.readLong();
      }
    },
    OBJECT {
      @Override
      Object readValue(ServerSerializationStreamReader stream)
          throws SerializationException {
        return stream.readObject();
      }
    },
    SHORT {
      @Override
      Object readValue(ServerSerializationStreamReader stream)
          throws SerializationException {
        return stream.readShort();
      }
    },
    STRING {
      @Override
      Object readValue(ServerSerializationStreamReader stream)
          throws SerializationException {
        return stream.readString();
      }
    };

    abstract Object readValue(ServerSerializationStreamReader stream)
        throws SerializationException;
  }

  /**
   * Enumeration used to provided typed instance readers for vectors.
   */
  private enum VectorReader {
    BOOLEAN_VECTOR {
      @Override
      protected Object readSingleValue(ServerSerializationStreamReader stream)
          throws SerializationException {
        return stream.readBoolean();
      }

      @Override
      protected void setSingleValue(Object array, int index, Object value) {
        Array.setBoolean(array, index, (Boolean) value);
      }
    },
    BYTE_VECTOR {
      @Override
      protected Object readSingleValue(ServerSerializationStreamReader stream)
          throws SerializationException {
        return stream.readByte();
      }

      @Override
      protected void setSingleValue(Object array, int index, Object value) {
        Array.setByte(array, index, (Byte) value);
      }
    },
    CHAR_VECTOR {
      @Override
      protected Object readSingleValue(ServerSerializationStreamReader stream)
          throws SerializationException {
        return stream.readChar();
      }

      @Override
      protected void setSingleValue(Object array, int index, Object value) {
        Array.setChar(array, index, (Character) value);
      }
    },
    DOUBLE_VECTOR {
      @Override
      protected Object readSingleValue(ServerSerializationStreamReader stream)
          throws SerializationException {
        return stream.readDouble();
      }

      @Override
      protected void setSingleValue(Object array, int index, Object value) {
        Array.setDouble(array, index, (Double) value);
      }
    },
    FLOAT_VECTOR {
      @Override
      protected Object readSingleValue(ServerSerializationStreamReader stream)
          throws SerializationException {
        return stream.readFloat();
      }

      @Override
      protected void setSingleValue(Object array, int index, Object value) {
        Array.setFloat(array, index, (Float) value);
      }
    },
    INT_VECTOR {
      @Override
      protected Object readSingleValue(ServerSerializationStreamReader stream)
          throws SerializationException {
        return stream.readInt();
      }

      @Override
      protected void setSingleValue(Object array, int index, Object value) {
        Array.setInt(array, index, (Integer) value);
      }
    },
    LONG_VECTOR {
      @Override
      protected Object readSingleValue(ServerSerializationStreamReader stream)
          throws SerializationException {
        return stream.readLong();
      }

      @Override
      protected void setSingleValue(Object array, int index, Object value) {
        Array.setLong(array, index, (Long) value);
      }
    },
    OBJECT_VECTOR {
      @Override
      protected Object readSingleValue(ServerSerializationStreamReader stream)
          throws SerializationException {
        return stream.readObject();
      }

      @Override
      protected void setSingleValue(Object array, int index, Object value) {
        Array.set(array, index, value);
      }
    },
    SHORT_VECTOR {
      @Override
      protected Object readSingleValue(ServerSerializationStreamReader stream)
          throws SerializationException {
        return stream.readShort();
      }

      @Override
      protected void setSingleValue(Object array, int index, Object value) {
        Array.setShort(array, index, (Short) value);
      }
    },
    STRING_VECTOR {
      @Override
      protected Object readSingleValue(ServerSerializationStreamReader stream)
          throws SerializationException {
        return stream.readString();
      }

      @Override
      protected void setSingleValue(Object array, int index, Object value) {
        Array.set(array, index, value);
      }
    };

    protected abstract Object readSingleValue(
        ServerSerializationStreamReader stream) throws SerializationException;

    protected abstract void setSingleValue(Object array, int index, Object value);

    /**
     * Convert a BoundedList to an array of the correct type. This
     * implementation consumes the BoundedList.
     */
    protected Object toArray(Class<?> componentType, BoundedList<Object> buffer)
        throws SerializationException {
      if (buffer.getExpectedSize() != buffer.size()) {
        throw new SerializationException(
            "Inconsistent number of elements received. Received "
                + buffer.size() + " but expecting " + buffer.getExpectedSize());
      }

      Object arr = Array.newInstance(componentType, buffer.size());

      for (int i = 0, n = buffer.size(); i < n; i++) {
        setSingleValue(arr, i, buffer.removeFirst());
      }

      return arr;
    }

    Object read(ServerSerializationStreamReader stream,
        BoundedList<Object> instance) throws SerializationException {
      for (int i = 0, n = instance.getExpectedSize(); i < n; ++i) {
        instance.add(readSingleValue(stream));
      }

      return toArray(instance.getComponentType(), instance);
    }
  }

  /**
   * Map of {@link Class} objects to {@link ValueReader}s.
   */
  private static final Map<Class<?>, ValueReader> CLASS_TO_VALUE_READER = new IdentityHashMap<Class<?>, ValueReader>();

  /**
   * Map of {@link Class} objects to {@link VectorReader}s.
   */
  private static final Map<Class<?>, VectorReader> CLASS_TO_VECTOR_READER = new IdentityHashMap<Class<?>, VectorReader>();

  private final ClassLoader classLoader;

  private SerializationPolicy serializationPolicy = RPC.getDefaultSerializationPolicy();

  private final SerializationPolicyProvider serializationPolicyProvider;

  /**
   * Used to look up setter methods of the form 'void Class.setXXX(T value)' given a
   * Class type and a field name XXX corresponding to a field of type T.
   */
  private final Map<Class<?>, Map<String, Method>> settersByClass = new HashMap<Class<?>, Map<String, Method>>();

  private String[] stringTable;

  private final ArrayList<String> tokenList = new ArrayList<String>();

  private int tokenListIndex;

  {
    CLASS_TO_VECTOR_READER.put(boolean[].class, VectorReader.BOOLEAN_VECTOR);
    CLASS_TO_VECTOR_READER.put(byte[].class, VectorReader.BYTE_VECTOR);
    CLASS_TO_VECTOR_READER.put(char[].class, VectorReader.CHAR_VECTOR);
    CLASS_TO_VECTOR_READER.put(double[].class, VectorReader.DOUBLE_VECTOR);
    CLASS_TO_VECTOR_READER.put(float[].class, VectorReader.FLOAT_VECTOR);
    CLASS_TO_VECTOR_READER.put(int[].class, VectorReader.INT_VECTOR);
    CLASS_TO_VECTOR_READER.put(long[].class, VectorReader.LONG_VECTOR);
    CLASS_TO_VECTOR_READER.put(Object[].class, VectorReader.OBJECT_VECTOR);
    CLASS_TO_VECTOR_READER.put(short[].class, VectorReader.SHORT_VECTOR);
    CLASS_TO_VECTOR_READER.put(String[].class, VectorReader.STRING_VECTOR);

    CLASS_TO_VALUE_READER.put(boolean.class, ValueReader.BOOLEAN);
    CLASS_TO_VALUE_READER.put(byte.class, ValueReader.BYTE);
    CLASS_TO_VALUE_READER.put(char.class, ValueReader.CHAR);
    CLASS_TO_VALUE_READER.put(double.class, ValueReader.DOUBLE);
    CLASS_TO_VALUE_READER.put(float.class, ValueReader.FLOAT);
    CLASS_TO_VALUE_READER.put(int.class, ValueReader.INT);
    CLASS_TO_VALUE_READER.put(long.class, ValueReader.LONG);
    CLASS_TO_VALUE_READER.put(Object.class, ValueReader.OBJECT);
    CLASS_TO_VALUE_READER.put(short.class, ValueReader.SHORT);
    CLASS_TO_VALUE_READER.put(String.class, ValueReader.STRING);
  }

  public ServerSerializationStreamReader(ClassLoader classLoader,
      SerializationPolicyProvider serializationPolicyProvider) {
    this.classLoader = classLoader;
    this.serializationPolicyProvider = serializationPolicyProvider;
  }

  public Object deserializeValue(Class<?> type) throws SerializationException {
    ValueReader valueReader = CLASS_TO_VALUE_READER.get(type);
    if (valueReader != null) {
      return valueReader.readValue(this);
    } else {
      // Arrays of primitive or reference types need to go through readObject.
      return ValueReader.OBJECT.readValue(this);
    }
  }

  public int getNumberOfTokens() {
    return tokenList.size();
  }

  public SerializationPolicy getSerializationPolicy() {
    return serializationPolicy;
  }

  @Override
  public void prepareToRead(String encodedTokens) throws SerializationException {
    tokenList.clear();
    tokenListIndex = 0;
    stringTable = null;

    int idx = 0, nextIdx;
    while (-1 != (nextIdx = encodedTokens.indexOf(RPC_SEPARATOR_CHAR, idx))) {
      String current = encodedTokens.substring(idx, nextIdx);
      tokenList.add(current);
      idx = nextIdx + 1;
    }
    if (idx == 0) {
      // Didn't find any separator, assume an older version with different
      // separators and get the version as the sequence of digits at the
      // beginning of the encoded string.
      while (idx < encodedTokens.length()
          && Character.isDigit(encodedTokens.charAt(idx))) {
        ++idx;
      }
      if (idx == 0) {
        throw new IncompatibleRemoteServiceException(
            "Malformed or old RPC message received - expecting version "
                + SERIALIZATION_STREAM_VERSION);
      } else {
        int version = Integer.valueOf(encodedTokens.substring(0, idx));
        throw new IncompatibleRemoteServiceException("Expecting version "
            + SERIALIZATION_STREAM_VERSION + " from client, got " + version
            + ".");
      }
    }

    super.prepareToRead(encodedTokens);

    // Check the RPC version number sent by the client
    if (getVersion() != SERIALIZATION_STREAM_VERSION) {
      throw new IncompatibleRemoteServiceException("Expecting version "
          + SERIALIZATION_STREAM_VERSION + " from client, got " + getVersion()
          + ".");
    }

    // Read the type name table
    //
    deserializeStringTable();

    // Write the serialization policy info
    String moduleBaseURL = readString();
    String strongName = readString();
    if (serializationPolicyProvider != null) {
      serializationPolicy = serializationPolicyProvider.getSerializationPolicy(
          moduleBaseURL, strongName);

      if (serializationPolicy == null) {
        throw new NullPointerException(
            "serializationPolicyProvider.getSerializationPolicy()");
      }
    }
  }

  public boolean readBoolean() throws SerializationException {
    return !extract().equals("0");
  }

  public byte readByte() throws SerializationException {
    String value = extract();
    try {
      return Byte.parseByte(value);
    } catch (NumberFormatException e) {
      throw getNumberFormatException(value, "byte",
          Byte.MIN_VALUE, Byte.MAX_VALUE);
    }
  }

  public char readChar() throws SerializationException {
    // just use an int, it's more foolproof
    return (char) readInt();
  }

  public double readDouble() throws SerializationException {
    return Double.parseDouble(extract());
  }

  public float readFloat() throws SerializationException {
    return (float) Double.parseDouble(extract());
  }

  public int readInt() throws SerializationException {
    String value = extract();
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      throw getNumberFormatException(value, "int",
          Integer.MIN_VALUE, Integer.MAX_VALUE);
    }
  }

  public long readLong() throws SerializationException {
    // Keep synchronized with LongLib. The wire format are the two component
    // parts of the double in the client code.
    return (long) readDouble() + (long) readDouble();
  }

  public short readShort() throws SerializationException {
    String value = extract();
    try {
      return Short.parseShort(value);
    } catch (NumberFormatException e) {
      throw getNumberFormatException(value, "short",
          Short.MIN_VALUE, Short.MAX_VALUE);
    }
  }

  public String readString() throws SerializationException {
    return getString(readInt());
  }

  @Override
  protected Object deserialize(String typeSignature)
      throws SerializationException {
    Object instance = null;
    try {
      Class<?> instanceClass;
      if (hasFlags(FLAG_ELIDE_TYPE_NAMES)) {
        if (getSerializationPolicy() instanceof TypeNameObfuscator) {
          TypeNameObfuscator obfuscator = (TypeNameObfuscator) getSerializationPolicy();
          String instanceClassName = obfuscator.getClassNameForTypeId(typeSignature);
          instanceClass = Class.forName(instanceClassName, false, classLoader);
        } else {
          throw new SerializationException(
              "The GWT module was compiled with RPC type name elision enabled, but "
                  + getSerializationPolicy().getClass().getName()
                  + " does not implement " + TypeNameObfuscator.class.getName());
        }
      } else {
        SerializedInstanceReference serializedInstRef = SerializabilityUtil.decodeSerializedInstanceReference(typeSignature);
        instanceClass = Class.forName(serializedInstRef.getName(), false,
            classLoader);
        validateTypeVersions(instanceClass, serializedInstRef);
      }

      assert (serializationPolicy != null);

      serializationPolicy.validateDeserialize(instanceClass);

      Class<?> customSerializer = SerializabilityUtil.hasCustomFieldSerializer(instanceClass);

      int index = reserveDecodedObjectIndex();

      instance = instantiate(customSerializer, instanceClass);

      rememberDecodedObject(index, instance);

      Object replacement = deserializeImpl(customSerializer, instanceClass,
          instance);

      // It's possible that deserializing an object requires the original proxy
      // object to be replaced.
      if (instance != replacement) {
        rememberDecodedObject(index, replacement);
        instance = replacement;
      }

      return instance;

    } catch (ClassNotFoundException e) {
      throw new SerializationException(e);
    } catch (InstantiationException e) {
      throw new SerializationException(e);
    } catch (IllegalAccessException e) {
      throw new SerializationException(e);
    } catch (IllegalArgumentException e) {
      throw new SerializationException(e);
    } catch (InvocationTargetException e) {
      throw new SerializationException(e.getTargetException());
    } catch (NoSuchMethodException e) {
      throw new SerializationException(e);
    }
  }

  @Override
  protected String getString(int index) {
    if (index == 0) {
      return null;
    }
    // index is 1-based
    assert (index > 0);
    assert (index <= stringTable.length);
    return stringTable[index - 1];
  }

  /**
   * Deserialize an instance that is an array. Will default to deserializing as
   * an Object vector if the instance is not a primitive vector.
   * 
   * @param instanceClass
   * @param instance
   * @throws SerializationException
   */
  @SuppressWarnings("unchecked")
  private Object deserializeArray(Class<?> instanceClass, Object instance)
      throws SerializationException {
    assert (instanceClass.isArray());

    BoundedList<Object> buffer = (BoundedList<Object>) instance;
    VectorReader instanceReader = CLASS_TO_VECTOR_READER.get(instanceClass);
    if (instanceReader != null) {
      return instanceReader.read(this, buffer);
    } else {
      return VectorReader.OBJECT_VECTOR.read(this, buffer);
    }
  }

  private void deserializeClass(Class<?> instanceClass, Object instance)
      throws SerializationException, IllegalAccessException,
      NoSuchMethodException, InvocationTargetException, ClassNotFoundException {
    /**
     * A map from field names to corresponding setter methods. The reference
     * will be null for classes that do not require special handling for
     * server-only fields.
     */
    Map<String, Method> setters = null;

    /**
     * A list of fields of this class known to the client. If null, assume the class is not
     * enhanced and don't attempt to deal with server-only fields.
     */
    Set<String> clientFieldNames = serializationPolicy.getClientFieldNamesForEnhancedClass(instanceClass);
    if (clientFieldNames != null) {
      // Read and set server-only instance fields encoded in the RPC data
      try {
        String encodedData = readString();
        if (encodedData != null) {
          byte[] serializedData = Base64Utils.fromBase64(encodedData);
          ByteArrayInputStream baos = new ByteArrayInputStream(serializedData);
          ObjectInputStream ois = new ObjectInputStream(baos);

          int count = ois.readInt();
          for (int i = 0; i < count; i++) {
            String fieldName = (String) ois.readObject();
            Object fieldValue = ois.readObject();
            Field field = instanceClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(instance, fieldValue);
          }
        }
      } catch (IOException e) {
        throw new SerializationException(e);
      } catch (NoSuchFieldException e) {
        throw new SerializationException(e);
      }

      setters = getSetters(instanceClass);
    }

    Field[] serializableFields = SerializabilityUtil.applyFieldSerializationPolicy(instanceClass);
    for (Field declField : serializableFields) {
      assert (declField != null);
      if ((clientFieldNames != null)
          && !clientFieldNames.contains(declField.getName())) {
        continue;
      }

      Object value = deserializeValue(declField.getType());

      String fieldName = declField.getName();
      Method setter;
      /*
       * If setters is non-null and there is a setter method for the given
       * field, call the setter. Otherwise, set the field value directly. For
       * persistence APIs such as JDO, the setter methods have been enhanced to
       * manipulate additional object state, causing direct field writes to fail
       * to update the object state properly.
       */
      if ((setters != null) && ((setter = setters.get(fieldName)) != null)) {
        setter.invoke(instance, value);
      } else {
        boolean isAccessible = declField.isAccessible();
        boolean needsAccessOverride = !isAccessible
            && !Modifier.isPublic(declField.getModifiers());
        if (needsAccessOverride) {
          // Override access restrictions
          declField.setAccessible(true);
        }

        declField.set(instance, value);
      }
    }

    Class<?> superClass = instanceClass.getSuperclass();
    if (serializationPolicy.shouldDeserializeFields(superClass)) {
      deserializeImpl(SerializabilityUtil.hasCustomFieldSerializer(superClass),
          superClass, instance);
    }
  }

  private Object deserializeImpl(Class<?> customSerializer,
      Class<?> instanceClass, Object instance) throws NoSuchMethodException,
      IllegalArgumentException, IllegalAccessException,
      InvocationTargetException, SerializationException, ClassNotFoundException {

    if (customSerializer != null) {
      deserializeWithCustomFieldDeserializer(customSerializer, instanceClass,
          instance);
    } else if (instanceClass.isArray()) {
      instance = deserializeArray(instanceClass, instance);
    } else if (instanceClass.isEnum()) {
      // Enums are deserialized when they are instantiated
    } else {
      deserializeClass(instanceClass, instance);
    }

    return instance;
  }

  private void deserializeStringTable() throws SerializationException {
    int typeNameCount = readInt();
    BoundedList<String> buffer = new BoundedList<String>(String.class,
        typeNameCount);
    for (int typeNameIndex = 0; typeNameIndex < typeNameCount; ++typeNameIndex) {
      String str = extract();
      // Change quoted characters back.
      int idx = str.indexOf('\\');
      if (idx >= 0) {
        StringBuilder buf = new StringBuilder();
        int pos = 0;
        while (idx >= 0) {
          buf.append(str.substring(pos, idx));
          if (++idx == str.length()) {
            throw new SerializationException("Unmatched backslash: \"" + str
                + "\"");
          }
          char ch = str.charAt(idx);
          pos = idx + 1;
          switch (ch) {
            case '0':
              buf.append('\u0000');
              break;
            case '!':
              buf.append(RPC_SEPARATOR_CHAR);
              break;
            case '\\':
              buf.append(ch);
              break;
            case 'u':
              try {
                ch = (char) Integer.parseInt(str.substring(idx + 1, idx + 5),
                    16);
              } catch (NumberFormatException e) {
                throw new SerializationException(
                    "Invalid Unicode escape sequence in \"" + str + "\"");
              }
              buf.append(ch);
              pos += 4;
              break;
            default:
              throw new SerializationException("Unexpected escape character "
                  + ch + " after backslash: \"" + str + "\"");
          }
          idx = str.indexOf('\\', pos);
        }
        buf.append(str.substring(pos));
        str = buf.toString();
      }
      buffer.add(str);
    }

    if (buffer.size() != buffer.getExpectedSize()) {
      throw new SerializationException("Expected " + buffer.getExpectedSize()
          + " string table elements; received " + buffer.size());
    }

    stringTable = buffer.toArray(new String[buffer.getExpectedSize()]);
  }

  private void deserializeWithCustomFieldDeserializer(
      Class<?> customSerializer, Class<?> instanceClass, Object instance)
      throws NoSuchMethodException, IllegalAccessException,
      InvocationTargetException {
    assert (!instanceClass.isArray());

    for (Method method : customSerializer.getMethods()) {
      if ("deserialize".equals(method.getName())) {
        method.invoke(null, this, instance);
        return;
      }
    }
    throw new NoSuchMethodException("deserialize");
  }

  private String extract() throws SerializationException {
    try {
      return tokenList.get(tokenListIndex++);
    } catch (IndexOutOfBoundsException e) {
      throw new SerializationException("Too few tokens in RPC request", e);
    }
  }
  
  /**
   * Returns a suitable NumberFormatException with an explanatory message
   * when a numerical value cannot be parsed according to its expected
   * type.
   *  
   * @param value the value as read from the RPC stream
   * @param type the name of the expected type
   * @param minValue the smallest valid value for the expected type
   * @param maxValue the largest valid value for the expected type
   * @return a NumberFormatException with an explanatory message
   */
  private NumberFormatException getNumberFormatException(String value,
      String type, double minValue, double maxValue) {
    String message = "a non-numerical value";
    try {
      // Check the field contents in order to produce a more comprehensible
      // error message
      double d = Double.parseDouble(value);
      if (d < minValue || d > maxValue) {
        message = "an out-of-range value";
      } else if (d != Math.floor(d)) {
        message = "a fractional value";
      }
    } catch (NumberFormatException e2) {
    }

    return new NumberFormatException("Expected type '" + type + "' but received " +
        message + ": " + value);
  }

  /**
   * Returns a Map from a field name to the setter method for that field, for a
   * given class. The results are computed once for each class and cached.
   * 
   * @param instanceClass the class to query
   * @return a Map from Strings to Methods such that the name <code>XXX</code>
   *         (corresponding to the field <code>T XXX</code>) maps to the method
   *         <code>void setXXX(T value)</code>, or null if no such method exists.
   */
  private Map<String, Method> getSetters(Class<?> instanceClass) {
    synchronized (settersByClass) {
      Map<String, Method> setters = settersByClass.get(instanceClass);
      if (setters == null) {
        setters = new HashMap<String, Method>();

        // Iterate over each field and locate a suitable setter method
        Field[] fields = instanceClass.getDeclaredFields();
        for (Field field : fields) {
          // Consider non-final, non-static, non-transient (or @GwtTransient) fields only
          if (SerializabilityUtil.isNotStaticTransientOrFinal(field)) {
            String fieldName = field.getName();
            String setterName = "set"
              + Character.toUpperCase(fieldName.charAt(0))
              + fieldName.substring(1);
            try {
              Method setter = instanceClass.getMethod(setterName, field.getType());
              setters.put(fieldName, setter);
            } catch (NoSuchMethodException e) {
              // Just leave this field out of the map
            }
          }
        }

        settersByClass.put(instanceClass, setters);
      }

      return setters;
    }
  }

  private Object instantiate(Class<?> customSerializer, Class<?> instanceClass)
      throws InstantiationException, IllegalAccessException,
      IllegalArgumentException, InvocationTargetException,
      NoSuchMethodException, SerializationException {
    if (customSerializer != null) {
      for (Method method : customSerializer.getMethods()) {
        if ("instantiate".equals(method.getName())) {
          return method.invoke(null, this);
        }
      }
      // Ok to not have one.
    }

    if (instanceClass.isArray()) {
      int length = readInt();
      // We don't pre-allocate the array; this prevents an allocation attack
      return new BoundedList<Object>(instanceClass.getComponentType(), length);
    } else if (instanceClass.isEnum()) {
      Enum<?>[] enumConstants = (Enum[]) instanceClass.getEnumConstants();
      int ordinal = readInt();
      assert (ordinal >= 0 && ordinal < enumConstants.length);
      return enumConstants[ordinal];
    } else {
      Constructor<?> constructor = instanceClass.getDeclaredConstructor();
      constructor.setAccessible(true);
      return constructor.newInstance();
    }
  }

  private void validateTypeVersions(Class<?> instanceClass,
      SerializedInstanceReference serializedInstRef)
      throws SerializationException {
    String clientTypeSignature = serializedInstRef.getSignature();
    if (clientTypeSignature.length() == 0) {
      throw new SerializationException("Missing type signature for "
          + instanceClass.getName());
    }

    String serverTypeSignature = SerializabilityUtil.getSerializationSignature(
        instanceClass, serializationPolicy);

    if (!clientTypeSignature.equals(serverTypeSignature)) {
      throw new SerializationException("Invalid type signature for "
          + instanceClass.getName());
    }
  }
}
