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

import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Custom field serializer for {@link java.util.LinkedHashMap} for the server
 * (uses reflection).
 */
@SuppressWarnings("rawtypes")
public final class LinkedHashMap_CustomFieldSerializer extends
    CustomFieldSerializer<LinkedHashMap> {

  /**
   * We use an atomic reference to avoid having to synchronize. This is safe
   * because it's only used as a cache; it's okay to read a stale value.
   */
  private static AtomicReference<Field> accessOrderField = new AtomicReference<Field>(
      null);

  private static Object KEY1 = new Object();
  private static Object KEY2 = new Object();

  /**
   * We use an atomic reference to avoid having to synchronize. This is safe
   * because it's only used as a cache; it's okay to read a stale value.
   */
  private static AtomicBoolean reflectionHasFailed = new AtomicBoolean(false);

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
  @SuppressWarnings("unchecked")
  public static boolean getAccessOrderNoReflection(LinkedHashMap instance) {
    /*
     * Clone the instance so our modifications won't affect the original. In
     * particular, if the original overrides removeEldestEntry, adding elements
     * to the map could cause existing elements to be removed.
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
    instance.put(KEY1, KEY1); // INSERT key1
    instance.put(KEY2, KEY2); // INSERT key2
    instance.get(KEY1); // ACCESS key1
    return instance.keySet().iterator().next() == KEY2;
  }

  public static LinkedHashMap instantiate(SerializationStreamReader streamReader)
      throws SerializationException {
    boolean accessOrder = streamReader.readBoolean();
    return new LinkedHashMap(16, .75f, accessOrder);
  }

  public static void serialize(SerializationStreamWriter streamWriter,
      LinkedHashMap instance) throws SerializationException {
    streamWriter.writeBoolean(getAccessOrder(instance));
    Map_CustomFieldSerializerBase.serialize(streamWriter, instance);
  }

  private static boolean getAccessOrder(LinkedHashMap instance) {
    if (!reflectionHasFailed.get()) {
      try {
        Field f = accessOrderField.get();
        if (f == null || !f.isAccessible()) {
          f = LinkedHashMap.class.getDeclaredField("accessOrder");
          synchronized (f) {
            // Ensure all threads can see the accessibility.
            f.setAccessible(true);
          }
          accessOrderField.set(f);
        }
        return ((Boolean) f.get(instance)).booleanValue();
      } catch (SecurityException e) {
        // fall through
      } catch (NoSuchFieldException e) {
        // fall through
      } catch (IllegalArgumentException e) {
        // fall through
      } catch (IllegalAccessException e) {
        // fall through
      }
      reflectionHasFailed.set(true);
    }

    // Use a (possibly slower) technique that does not require reflection.
    return getAccessOrderNoReflection(instance);
  }

  @Override
  public void deserializeInstance(SerializationStreamReader streamReader,
      LinkedHashMap instance) throws SerializationException {
    deserialize(streamReader, instance);
  }

  @Override
  public boolean hasCustomInstantiateInstance() {
    return true;
  }

  @Override
  public LinkedHashMap instantiateInstance(SerializationStreamReader streamReader)
      throws SerializationException {
    return instantiate(streamReader);
  }

  @Override
  public void serializeInstance(SerializationStreamWriter streamWriter,
      LinkedHashMap instance) throws SerializationException {
    serialize(streamWriter, instance);
  }
}
