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
package com.google.gwt.user.server.rpc.impl;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.impl.AbstractSerializationStreamReader;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

/**
 * For internal use only. Used for server call serialization. This class is
 * carefully matched with the client-side version.
 */
public final class ServerSerializationStreamReader extends
    AbstractSerializationStreamReader {

  private ServerSerializableTypeOracle serializableTypeOracle;

  private String[] stringTable;

  private ArrayList tokenList = new ArrayList();

  private int tokenListIndex;

  public ServerSerializationStreamReader(
      ServerSerializableTypeOracle serializableTypeOracle) {
    this.serializableTypeOracle = serializableTypeOracle;
  }

  public Object deserializeValue(Class type) throws SerializationException {
    if (type == boolean.class) {
      return Boolean.valueOf(readBoolean());
    } else if (type == byte.class) {
      return new Byte(readByte());
    } else if (type == char.class) {
      return new Character(readChar());
    } else if (type == double.class) {
      return new Double(readDouble());
    } else if (type == float.class) {
      return new Float(readFloat());
    } else if (type == int.class) {
      return new Integer(readInt());
    } else if (type == long.class) {
      return new Long(readLong());
    } else if (type == short.class) {
      return new Short(readShort());
    } else if (type == String.class) {
      return readString();
    }

    return readObject();
  }

  public void prepareToRead(String encodedTokens) throws SerializationException {
    tokenList.clear();
    tokenListIndex = 0;
    stringTable = null;

    int idx = 0, nextIdx;
    while (-1 != (nextIdx = encodedTokens.indexOf('\uffff', idx))) {
      String current = encodedTokens.substring(idx, nextIdx);
      tokenList.add(current);
      idx = nextIdx + 1;
    }

    super.prepareToRead(encodedTokens);

    // Read the type name table
    //
    deserializeStringTable();
  }

  public boolean readBoolean() {
    return !extract().equals("0");
  }

  public byte readByte() {
    return Byte.parseByte(extract());
  }

  public char readChar() {
    // just use an int, it's more foolproof
    return (char) Integer.parseInt(extract());
  }

  public double readDouble() {
    return Double.parseDouble(extract());
  }

  public float readFloat() {
    return Float.parseFloat(extract());
  }

  public int readInt() {
    return Integer.parseInt(extract());
  }

  public long readLong() {
    return Long.parseLong(extract());
  }

  public short readShort() {
    return Short.parseShort(extract());
  }

  public String readString() throws SerializationException {
    return getString(readInt());
  }

  protected Object deserialize(String typeSignature)
      throws SerializationException {
    Object instance = null;
    SerializedInstanceReference serializedInstRef = serializableTypeOracle.decodeSerializedInstanceReference(typeSignature);

    try {
      Class instanceClass = Class.forName(serializedInstRef.getName(), false,
          this.getClass().getClassLoader());

      if (!serializableTypeOracle.isSerializable(instanceClass)) {
        throw new SerializationException("Class '" + instanceClass.getName()
            + "' is not serializable");
      }

      validateTypeVersions(instanceClass, serializedInstRef);

      Class customSerializer = serializableTypeOracle.hasCustomFieldSerializer(instanceClass);

      instance = instantiate(customSerializer, instanceClass);

      rememberDecodedObject(instance);

      deserializeImpl(customSerializer, instanceClass, instance);

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
      throw new SerializationException(e);

    } catch (NoSuchMethodException e) {
      throw new SerializationException(e);
    }
  }

  protected String getString(int index) {
    if (index == 0) {
      return null;
    }
    // index is 1-based
    assert (index > 0);
    assert (index <= stringTable.length);
    return stringTable[index - 1];
  }

  private void deserializeImpl(Class customSerializer, Class instanceClass,
      Object instance) throws NoSuchMethodException, IllegalArgumentException,
      IllegalAccessException, InvocationTargetException,
      SerializationException, ClassNotFoundException {
    if (customSerializer != null) {
      deserializeWithCustomFieldDeserializer(customSerializer, instanceClass,
          instance);
    } else {
      deserializeWithDefaultFieldDeserializer(instanceClass, instance);
    }
  }

  private void deserializeStringTable() {
    int typeNameCount = readInt();
    stringTable = new String[typeNameCount];
    for (int typeNameIndex = 0; typeNameIndex < typeNameCount; ++typeNameIndex) {
      stringTable[typeNameIndex] = extract();
    }
  }

  private void deserializeWithCustomFieldDeserializer(Class customSerializer,
      Class instanceClass, Object instance) throws ClassNotFoundException,
      NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    if (instanceClass.isArray()) {
      Class componentType = instanceClass.getComponentType();
      if (!componentType.isPrimitive()) {
        instanceClass = Class.forName("[Ljava.lang.Object;");
      }
    }
    Method deserialize = customSerializer.getMethod("deserialize", new Class[] {
        SerializationStreamReader.class, instanceClass});
    deserialize.invoke(null, new Object[] {this, instance});
  }

  private void deserializeWithDefaultFieldDeserializer(Class instanceClass,
      Object instance) throws SerializationException, IllegalAccessException,
      NoSuchMethodException, InvocationTargetException, ClassNotFoundException {
    Field[] declFields = instanceClass.getDeclaredFields();
    Field[] serializableFields = serializableTypeOracle.applyFieldSerializationPolicy(declFields);

    for (int index = 0; index < serializableFields.length; ++index) {
      Field declField = serializableFields[index];
      assert (declField != null);

      Object value = deserializeValue(declField.getType());

      boolean isAccessible = declField.isAccessible();
      boolean needsAccessOverride = !isAccessible
          && !Modifier.isPublic(declField.getModifiers());
      if (needsAccessOverride) {
        // Override access restrictions
        declField.setAccessible(true);
      }

      declField.set(instance, value);

      if (needsAccessOverride) {
        // Restore access restrictions
        declField.setAccessible(isAccessible);
      }
    }

    Class superClass = instanceClass.getSuperclass();
    if (superClass != null && serializableTypeOracle.isSerializable(superClass)) {
      deserializeImpl(
          serializableTypeOracle.hasCustomFieldSerializer(superClass),
          superClass, instance);
    }
  }

  private String extract() {
    return (String) tokenList.get(tokenListIndex++);
  }

  private Object instantiate(Class customSerializer, Class instanceClass)
      throws InstantiationException, IllegalAccessException,
      IllegalArgumentException, InvocationTargetException {
    if (customSerializer != null) {
      try {
        Method instantiate = customSerializer.getMethod("instantiate",
            new Class[] {SerializationStreamReader.class});
        return instantiate.invoke(null, new Object[] {this});
      } catch (NoSuchMethodException e) {
        // purposely ignored
      }
    }

    if (instanceClass.isArray()) {
      int length = readInt();
      Class componentType = instanceClass.getComponentType();
      return Array.newInstance(componentType, length);
    } else {
      return instanceClass.newInstance();
    }
  }

  private void validateTypeVersions(Class instanceClass,
      SerializedInstanceReference serializedInstRef)
      throws SerializationException {
    String clientTypeSignature = serializedInstRef.getSignature();
    if (clientTypeSignature.length() == 0) {
      if (shouldEnforceTypeVersioning()) {
        // TODO(mmendez): add a more descriptive error message here
        throw new SerializationException();
      }

      return;
    }

    String serverTypeSignature = serializableTypeOracle.getSerializationSignature(instanceClass);

    if (!clientTypeSignature.equals(serverTypeSignature)) {
      throw new SerializationException("Invalid type signature for "
          + instanceClass.getName());
    }
  }
}
