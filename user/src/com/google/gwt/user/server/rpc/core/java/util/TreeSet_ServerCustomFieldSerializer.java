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
package com.google.gwt.user.server.rpc.core.java.util;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.google.gwt.user.client.rpc.core.java.util.Collection_CustomFieldSerializerBase;
import com.google.gwt.user.client.rpc.core.java.util.TreeSet_CustomFieldSerializer;
import com.google.gwt.user.server.rpc.ServerCustomFieldSerializer;
import com.google.gwt.user.server.rpc.impl.DequeMap;
import com.google.gwt.user.server.rpc.impl.ServerSerializationStreamReader;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Comparator;
import java.util.TreeSet;

/**
 * Custom field serializer for {@link java.util.TreeMap}.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class TreeSet_ServerCustomFieldSerializer extends ServerCustomFieldSerializer<TreeSet> {

  public static void deserialize(ServerSerializationStreamReader streamReader, TreeSet instance,
      Type[] expectedParameterTypes, DequeMap<TypeVariable<?>, Type> resolvedTypes) throws
      SerializationException {
    Collection_ServerCustomFieldSerializerBase.deserialize(streamReader, instance,
        expectedParameterTypes, resolvedTypes);
  }

  @SuppressWarnings({"cast", "unused"})
  public static TreeSet instantiate(ServerSerializationStreamReader streamReader,
      Type[] expectedParameterTypes, DequeMap<TypeVariable<?>, Type> resolvedTypes) throws
      SerializationException {
    return new TreeSet((Comparator) streamReader.readObject((Type) Comparator.class,
        resolvedTypes));
  }

  @Override
  public void deserializeInstance(SerializationStreamReader streamReader, TreeSet instance)
      throws SerializationException {
    Collection_CustomFieldSerializerBase.deserialize(streamReader, instance);
  }

  @Override
  public void deserializeInstance(ServerSerializationStreamReader streamReader, TreeSet instance,
      Type[] expectedParameterTypes, DequeMap<TypeVariable<?>, Type> resolvedTypes) throws
      SerializationException {
    deserialize(streamReader, instance, expectedParameterTypes, resolvedTypes);
  }

  @Override
  public boolean hasCustomInstantiateInstance() {
    return true;
  }

  @Override
  public TreeSet instantiateInstance(SerializationStreamReader streamReader)
      throws SerializationException {
    return TreeSet_CustomFieldSerializer.instantiate(streamReader);
  }

  @Override
  public TreeSet instantiateInstance(ServerSerializationStreamReader streamReader,
      Type[] expectedParameterTypes, DequeMap<TypeVariable<?>, Type> resolvedTypes) throws
      SerializationException {
    return instantiate(streamReader, expectedParameterTypes, resolvedTypes);
  }

  @Override
  public void serializeInstance(SerializationStreamWriter streamWriter, TreeSet instance)
      throws SerializationException {
    TreeSet_CustomFieldSerializer.serialize(streamWriter, instance);
  }
}
