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
package com.google.gwt.user.client.rpc.core.java.util;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;

/**
 * Custom field serializer for {@link java.util.LinkedHashMap} for the server
 * (uses reflection).
 */
public final class LinkedHashMap_CustomFieldSerializer {

  @SuppressWarnings("unchecked") // raw LinkedHashMap
  public static void deserialize(SerializationStreamReader streamReader,
      LinkedHashMap instance) throws SerializationException {
    Map_CustomFieldSerializerBase.deserialize(streamReader, instance);
  }

  @SuppressWarnings("unchecked") // raw LinkedHashMap
  public static LinkedHashMap instantiate(SerializationStreamReader streamReader)
      throws SerializationException {
    boolean accessOrder = streamReader.readBoolean();
    return new LinkedHashMap(16, .75f, accessOrder);
  }

  @SuppressWarnings("unchecked") // raw LinkedHashMap
  public static void serialize(SerializationStreamWriter streamWriter,
      LinkedHashMap instance) throws SerializationException {
    streamWriter.writeBoolean(getAccessOrder(instance));
    Map_CustomFieldSerializerBase.serialize(streamWriter, instance);
  }

  @SuppressWarnings("unchecked") // raw LinkedHashMap
  private static boolean getAccessOrder(LinkedHashMap instance)
      throws SerializationException {
    Field accessOrderField;
    try {
      accessOrderField = LinkedHashMap.class.getDeclaredField("accessOrder");
      accessOrderField.setAccessible(true);
      return ((Boolean) accessOrderField.get(instance)).booleanValue();
    } catch (SecurityException e) {
      throw new SerializationException("Can't get accessOrder field", e);
    } catch (NoSuchFieldException e) {
      throw new SerializationException("Can't get accessOrder field", e);
    } catch (IllegalArgumentException e) {
      throw new SerializationException("Can't get accessOrder field", e);
    } catch (IllegalAccessException e) {
      throw new SerializationException("Can't get accessOrder field", e);
    }
  }
}
