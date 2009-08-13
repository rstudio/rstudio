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

  /**
   * Infers the value of the private accessOrder field of instance by examining
   * its behavior on a set of test inputs, without using reflection. Note that
   * this implementation clones the instance, which could be slow.
   * 
   * @param instance the instance to check
   * @return the value of instance.accessOrder
   */
  @SuppressWarnings("unchecked") // raw LinkedHashMap
  public static boolean getAccessOrderNoReflection(LinkedHashMap instance) {    
    /*
     * Clone the instance so our modifications won't affect the original.
     * In particular, if the original overrides removeEldestEntry, adding
     * elements to the map could cause existing elements to be removed.
     */
    instance = (LinkedHashMap) instance.clone();
    instance.clear();

    /*
     * We insert key1, then key2, after which we access key1. We then iterate
     * over the key set and observe the order in which keys are returned. The
     * iterator will return keys in the order of least recent insertion or
     * access, depending on the value of the accessOrder field within the
     * LinkedHashMap instance. If the iterator is ordered by least recent
     * insertion (accessOrder = false), we will encounter key1 first since key2
     * has been inserted more recently. If it is ordered by least recent access
     * (accessOrder = true), we will encounter key2 first, since key1 has been
     * accessed more recently.
     */
    Object key1 = new Object();
    Object key2 = new Object();
    instance.put(key1, key1); // INSERT key1
    instance.put(key2, key2); // INSERT key2
    instance.get(key1);       // ACCESS key1
    boolean accessOrder = false;
    for (Object key : instance.keySet()) {
      if (key == key1) {
        break;
      }
      if (key == key2) {
        accessOrder = true;
        break;
      }
    }

    return accessOrder;
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
  private static boolean getAccessOrder(LinkedHashMap instance) {
    Field accessOrderField;
    try {
      accessOrderField = LinkedHashMap.class.getDeclaredField("accessOrder");
      accessOrderField.setAccessible(true);
      return ((Boolean) accessOrderField.get(instance)).booleanValue();
    } catch (SecurityException e) {
      // fall through
    } catch (NoSuchFieldException e) {
      // fall through
    } catch (IllegalArgumentException e) {
      // fall through
    } catch (IllegalAccessException e) {
      // fall through
    }
    
    // Use a (possibly slower) technique that does not require reflection.
    return getAccessOrderNoReflection(instance);
  }
}
