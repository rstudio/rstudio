/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.user.server.rpc.core.java.lang;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.google.gwt.user.client.rpc.core.java.lang.Object_Array_CustomFieldSerializer;
import com.google.gwt.user.server.rpc.ServerCustomFieldSerializer;
import com.google.gwt.user.server.rpc.impl.DequeMap;
import com.google.gwt.user.server.rpc.impl.ServerSerializationStreamReader;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

/**
 * Server-side Custom Serializer for arrays of {@link java.lang.Object}.
 */
public class Object_Array_ServerCustomFieldSerializer
    extends ServerCustomFieldSerializer<Object[]> {

  public static void deserialize(ServerSerializationStreamReader streamReader,
      Object[] instance, Type[] expectedParameterTypes,
      DequeMap<TypeVariable<?>, Type> resolvedTypes) throws SerializationException {
    for (int itemIndex = 0; itemIndex < instance.length; ++itemIndex) {
      instance[itemIndex] = streamReader.readObject(expectedParameterTypes[0], resolvedTypes);
    }
  }

  @Override
  public void deserializeInstance(SerializationStreamReader streamReader,
      Object[] instance) throws SerializationException {
    Object_Array_CustomFieldSerializer.deserialize(streamReader, instance);
  }

  @Override
  public void deserializeInstance(ServerSerializationStreamReader streamReader,
      Object[] instance, Type[] expectedParameterTypes,
      DequeMap<TypeVariable<?>, Type> resolvedTypes) throws SerializationException {
    deserialize(streamReader, instance, expectedParameterTypes, resolvedTypes);
  }

  @Override
  public void serializeInstance(SerializationStreamWriter streamWriter,
      Object[] instance) throws SerializationException {
    Object_Array_CustomFieldSerializer.serialize(streamWriter, instance);
  }
}
