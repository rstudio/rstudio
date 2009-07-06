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
package com.google.gwt.user.server.rpc.impl;

import com.google.gwt.user.client.rpc.SerializationException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.BitSet;

/**
 * An implementation of ServerFieldSerializer that handles the jdoDetachedState
 * field in the JDO API, version 2.2.
 */
final class JdoDetachedStateServerDataSerializer extends
    ServerDataSerializer {

  /**
   * A Class object for the javax.jdo.spi.Detachable interface, or null if it is
   * not present in the runtime environment.
   */
  private static Class<?> JAVAX_JDO_SPI_DETACHABLE_CLASS;

  /**
   * A constant indicating an Externalizable entry in the jdoDetachedState
   * Object array.
   */
  private static final int JDO_DETACHED_STATE_ENTRY_EXTERNALIZABLE = 0;

  /**
   * A constant indicating a null entry in the jdoDetachedState Object array.
   */
  private static final int JDO_DETACHED_STATE_ENTRY_NULL = 1;

  /**
   * A constant indicating a Serializable entry in the jdoDetachedState Object
   * array.
   */
  private static final int JDO_DETACHED_STATE_ENTRY_SERIALIZABLE = 2;

  /**
   * A constant indicating the name of the jdoDetachedState field.
   */
  private static final String JDO_DETACHED_STATE_FIELD_NAME = "jdoDetachedState";

  /**
   * A version number for the serialized form of the jdoDetachedState field.
   * Version 1 corresponds to JDO API version 2.2.
   */
  private static final int JDO_DETACHED_STATE_SERIALIZATION_VERSION = 1;

  /**
   * A constant indicating the name of the jdoFlags field.
   */
  private static final String JDO_FLAGS_FIELD_NAME = "jdoFlags";

  /**
   * A constant indicating the "LOAD_REQUIRED" value for the jdoFlags field.
   */
  private static final int JDO_FLAGS_LOAD_REQUIRED = 1;
  
  /**
   * The singleton instance.
   */
  private static final JdoDetachedStateServerDataSerializer theInstance =
    new JdoDetachedStateServerDataSerializer();

  static {
    try {
      JAVAX_JDO_SPI_DETACHABLE_CLASS = Class.forName("javax.jdo.spi.Detachable");
    } catch (ClassNotFoundException e) {
      // Ignore, if JDO is not present in our enviroment the variable will be
      // initialized to null.
    }
  }

  /**
   * Return the unique instance of this class.
   */
  public static JdoDetachedStateServerDataSerializer getInstance() {
    return theInstance;
  }
  
  /**
   * Ensure this class has a singleton instance only.
   */
  private JdoDetachedStateServerDataSerializer() {
  }

  /**
   * Custom deserialize the contents of the jdoDetachedState field.
   * 
   * @param serializedData the serialized data, as an array of bytes, possibly
   *          null.
   * @param instance the Object instance to be modified.
   * @throws SerializationException if the field contents cannot be
   *           reconstructed.
   */
  @Override
  public void deserializeServerData(byte[] serializedData, Object instance)
      throws SerializationException {
    try {
      Class<?> instanceClass = instance.getClass();
      Field jdoDetachedStateField = instanceClass.getDeclaredField(JdoDetachedStateServerDataSerializer.JDO_DETACHED_STATE_FIELD_NAME);
      jdoDetachedStateField.setAccessible(true);

      if (serializedData == null) {
        throw new SerializationException("JDO persistent object serialized data is null");
      }

      ByteArrayInputStream bais = new ByteArrayInputStream(serializedData);
      ObjectInputStream in = new ObjectInputStream(bais);

      // We only understand version 1 (JDO version 2.2) at this time.
      int version = in.readInt();
      if (version != JdoDetachedStateServerDataSerializer.JDO_DETACHED_STATE_SERIALIZATION_VERSION) {
        throw new SerializationException(
            "Got JDO detached state serialization version "
                + version
                + ", expected version "
                + JdoDetachedStateServerDataSerializer.JDO_DETACHED_STATE_SERIALIZATION_VERSION
                + ".");
      }

      Object[] jdoDetachedState = new Object[4];
      for (int i = 0; i < 3; i++) {
        byte type = in.readByte();
        switch (type) {
          case JdoDetachedStateServerDataSerializer.JDO_DETACHED_STATE_ENTRY_NULL:
            jdoDetachedState[i] = null;
            break;

          case JdoDetachedStateServerDataSerializer.JDO_DETACHED_STATE_ENTRY_EXTERNALIZABLE:
            try {
              String className = (String) in.readObject();
              Class<? extends Externalizable> c = Class.forName(className).asSubclass(
                  java.io.Externalizable.class);
              Externalizable e = c.newInstance();
              e.readExternal(in);
              jdoDetachedState[i] = e;
            } catch (ClassCastException e) {
              throw new SerializationException(e);
            } catch (ClassNotFoundException e) {
              throw new SerializationException(e);
            } catch (IllegalAccessException e) {
              throw new SerializationException(e);
            } catch (InstantiationException e) {
              throw new SerializationException(e);
            }
            break;

          case JdoDetachedStateServerDataSerializer.JDO_DETACHED_STATE_ENTRY_SERIALIZABLE:
            try {
              jdoDetachedState[i] = in.readObject();
            } catch (ClassNotFoundException e) {
              throw new SerializationException(e);
            }
            break;
        }
      }

      // Mark all loaded fields as modified
      jdoDetachedState[3] = new BitSet();
      ((BitSet) jdoDetachedState[3]).or((BitSet) jdoDetachedState[2]);

      // Set the field
      jdoDetachedStateField.set(instance, jdoDetachedState);
    } catch (IllegalAccessException e) {
      throw new SerializationException(e);
    } catch (IOException e) {
      throw new SerializationException(
          "An unexpected IOException occured while deserializing jdoDetachedState",
          e);
    } catch (NoSuchFieldException e) {
      throw new SerializationException(e);
    }
  }

  @Override
  public String getName() {
    return "gwt-jdo-jdoDetachedState";
  }

  /**
   * Custom serialize the contents of the jdoDetachedState field. If the field
   * is null, a null array is returned. Otherwise, a byte array is returned with
   * the server-only contents of the instance in a custom serialized form. The
   * current implementation of this method assumes JDO API version 2.2.
   * 
   * @param instance an Object containing server-only data.
   * @return a byte array containing a representation of the field.
   * @throws SerializationException if the instance cannot be serialized by this
   *           serializer.
   */
  @Override
  public byte[] serializeServerData(Object instance)
      throws SerializationException {
    try {
      Class<?> instanceClass = instance.getClass();

      // Ensure the jdoFlags field is not set to LOAD_REQUIRED
      Field jdoFlagsField = instanceClass.getDeclaredField(JDO_FLAGS_FIELD_NAME);
      jdoFlagsField.setAccessible(true);
      byte jdoFlags = ((Byte) jdoFlagsField.get(instance)).byteValue();
      if (jdoFlags == JDO_FLAGS_LOAD_REQUIRED) {
        throw new SerializationException("JDO persistent object data not loaded");
      }

      // Retrieve the jdoDetachedStateField and ensure it is non-null
      Field jdoDetachedStateField = instanceClass.getDeclaredField(JDO_DETACHED_STATE_FIELD_NAME);
      jdoDetachedStateField.setAccessible(true);
      Object[] jdoDetachedState = (Object[]) jdoDetachedStateField.get(instance);
      if (jdoDetachedState == null) {
        throw new SerializationException("JDO persistent object has null jdoDetachedState");
      }

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream out = new ObjectOutputStream(baos);
      
      // Version 1 == JDO API version 2.2
      out.writeInt(JDO_DETACHED_STATE_SERIALIZATION_VERSION);

      // Write only the first 3 fields since the last field will be clobbered
      // on return to the
      // server.
      for (int i = 0; i < 3; i++) {
        Object entry = jdoDetachedState[i];
        if (entry == null) {
          // Null value
          out.writeByte(JDO_DETACHED_STATE_ENTRY_NULL);
        } else if (entry instanceof Externalizable) {
          // Externalizable value
          out.writeByte(JDO_DETACHED_STATE_ENTRY_EXTERNALIZABLE);
          out.writeObject(entry.getClass().getCanonicalName());
          ((Externalizable) entry).writeExternal(out);
        } else if (entry instanceof Serializable) {
          // Serializable value
          out.writeByte(JDO_DETACHED_STATE_ENTRY_SERIALIZABLE);
          out.writeObject(entry);
        } else {
          throw new SerializationException(
              "Entry "
              + i
              + " of jdoDetachedState is neither null, Externalizable nor serializable");
        }
      }

      out.close();
      return baos.toByteArray();
    } catch (IllegalAccessException e) {
      throw new SerializationException(e);
    } catch (IOException e) {
      throw new SerializationException(
          "An unexpected IOException occured while serializing jdoDetachedState",
          e);
    } catch (NoSuchFieldException e) {
      throw new SerializationException(e);
    }
  }

  /**
   * Returns true if the instanceClass implements the javax.jdo.spi.Detachable
   * interface.
   * 
   * @param instanceClass the class to be queried.
   */
  @Override
  public boolean shouldSerialize(Class<?> instanceClass) {
    return JAVAX_JDO_SPI_DETACHABLE_CLASS != null
        && JAVAX_JDO_SPI_DETACHABLE_CLASS.isAssignableFrom(instanceClass);
  }

  @Override
  public boolean shouldSkipField(Field field) {
    return ("jdoDetachedState".equals(field.getName()))
        && (JAVAX_JDO_SPI_DETACHABLE_CLASS != null)
        && (JAVAX_JDO_SPI_DETACHABLE_CLASS.isAssignableFrom(field.getDeclaringClass()));
  }
}
