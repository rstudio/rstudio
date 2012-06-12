/*
 * Copyright 2012 Google Inc.
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
package com.google.gwt.user.client.rpc.core.java.util;

import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.EnumMap;

/**
 * The super source version is used in web mode. This is only called in hosted mode.
 * Custom field serializer for {@link java.util.EnumMap}.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public final class EnumMap_CustomFieldSerializer extends CustomFieldSerializer<EnumMap> {

  public static void deserialize(SerializationStreamReader streamReader, EnumMap instance)
      throws SerializationException {
    Map_CustomFieldSerializerBase.deserialize(streamReader, instance);
  }

  /**
   * EnumMap has no empty constructor; you must provide the class literal to the constructor.
   * However GWT doesn't emulate java.lang.Class.forName() nor is java.util.Class GWT-RPC
   * serializable. In order to get the type across the wire, one enum of the appropriate class is
   * serialized at the beginning of the stream. Upon deserialization this enum's type is
   * introspected and used in the constructor for the new EnumMap.
   */
  public static EnumMap instantiate(SerializationStreamReader streamReader)
      throws SerializationException {
    Object exemplar = streamReader.readObject();
    Class clazz = exemplar.getClass();
    return new EnumMap(clazz);
  }
  
  /**
  * Since this code is only run in hosted mode, reflection can be used.
   */
  public static void serialize(SerializationStreamWriter streamWriter, EnumMap instance)
      throws SerializationException {
    Class c = instance.getClass();
    Field keyUniverseField;
    Object keyUniverse = null;

    try {
      keyUniverseField = c.getDeclaredField("keyUniverse");
      keyUniverseField.setAccessible(true);
      keyUniverse = keyUniverseField.get(instance);
    } catch (IllegalArgumentException e) {
      throw new SerializationException(e);
    } catch (IllegalAccessException e) {
      throw new SerializationException(e);
    } catch (SecurityException e) {
      throw new SerializationException(e);
    } catch (NoSuchFieldException e) {
      throw new SerializationException(e);
    }
    Object exemplar = Array.get(keyUniverse, 0);
    streamWriter.writeObject(exemplar);
    Map_CustomFieldSerializerBase.serialize(streamWriter, instance);
  }

  @Override
  public void deserializeInstance(SerializationStreamReader streamReader, EnumMap instance)
      throws SerializationException {
    deserialize(streamReader, instance);
  }

  @Override
  public boolean hasCustomInstantiateInstance() {
    return true;
  }

  @Override
  public EnumMap instantiateInstance(SerializationStreamReader streamReader)
      throws SerializationException {
    return instantiate(streamReader);
  }

  @Override
  public void serializeInstance(SerializationStreamWriter streamWriter, EnumMap instance)
      throws SerializationException {
    serialize(streamWriter, instance);
  }
}
