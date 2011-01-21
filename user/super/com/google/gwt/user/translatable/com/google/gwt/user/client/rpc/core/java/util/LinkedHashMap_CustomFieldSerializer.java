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

import java.util.LinkedHashMap;

/**
 * Custom field serializer for {@link java.util.LinkedHashMap}, which uses
 * JSNI.
 */
public final class LinkedHashMap_CustomFieldSerializer extends
    CustomFieldSerializer<LinkedHashMap> {

  public static void deserialize(SerializationStreamReader streamReader,
      LinkedHashMap instance) throws SerializationException {
    Map_CustomFieldSerializerBase.deserialize(streamReader, instance);
  }
  
  @SuppressWarnings("unchecked") // raw LinkedHashMap
  // Included for testability
  public static boolean getAccessOrderNoReflection(LinkedHashMap instance) {  
    return getAccessOrder(instance);
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

  @SuppressWarnings("unchecked") // raw LinkedHashMap
  private static native boolean getAccessOrder(LinkedHashMap instance) /*-{
    return instance.@java.util.LinkedHashMap::accessOrder;
  }-*/;

  public void deserializeInstance(SerializationStreamReader streamReader,
      LinkedHashMap instance) throws SerializationException {
    deserialize(streamReader, instance);
  }

  @Override
  public boolean hasCustomInstantiateInstance() {
    return true;
  }

  @Override
  public LinkedHashMap instantiateInstance(
      SerializationStreamReader streamReader) throws SerializationException {
    return instantiate(streamReader);
  }

  public void serializeInstance(SerializationStreamWriter streamWriter,
      LinkedHashMap instance) throws SerializationException {
    serialize(streamWriter, instance);
  }
}
