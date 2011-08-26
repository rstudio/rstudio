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
import com.google.gwt.user.server.rpc.ServerCustomFieldSerializer;
import com.google.gwt.user.server.rpc.impl.DequeMap;
import com.google.gwt.user.server.rpc.impl.SerializabilityUtil;
import com.google.gwt.user.server.rpc.impl.ServerSerializationStreamReader;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.List;

/**
 * Dummy class for nesting the server-side custom serializer.
 */
public final class Arrays {

  /**
   * Server-side Custom field serializer for {@link java.util.Arrays.ArrayList}.
   */
  @SuppressWarnings("rawtypes")
  public static final class ArrayList_ServerCustomFieldSerializer extends
      ServerCustomFieldSerializer<List> {

    public static String concreteType() {
      return java.util.Arrays.asList().getClass().getName();
    }

    /*
     * Note: the reason this implementation differs from that of a standard List
     * (which serializes a number and then each element) is the requirement that
     * the underlying array retain its correct type across the wire. This gives
     * toArray() results the correct type, and can generate internal
     * ArrayStoreExceptions.
     * 
     * The type checking is messy because we need some way of converting the
     * List<X> or related type that we are expecting into the array type that we
     * are about to try to read. You can't create objects of class Type
     * directly, so we need to create a dummy array and then use it's class as a
     * type.
     */
    public static List<?> instantiate(ServerSerializationStreamReader streamReader,
        Type[] expectedParameterTypes, DequeMap<TypeVariable<?>, Type> resolvedTypes)
        throws SerializationException {
      Class<?> componentClass = SerializabilityUtil.getClassFromType(expectedParameterTypes[0],
          resolvedTypes);
      if (componentClass == null) {
        return com.google.gwt.user.client.rpc.core.java.util.Arrays.ArrayList_CustomFieldSerializer
            .instantiate(streamReader);
      }
      
      Object expectedArray = Array.newInstance(componentClass, 0);
      Object[] array = (Object[]) streamReader.readObject(expectedArray.getClass(), resolvedTypes);
      return java.util.Arrays.asList(array);
    }

    @Override
    public void deserializeInstance(SerializationStreamReader streamReader, List instance)
        throws SerializationException {
      com.google.gwt.user.client.rpc.core.java.util.Arrays.ArrayList_CustomFieldSerializer
          .deserialize(streamReader, instance);
    }

    @SuppressWarnings("unused")
    @Override
    public void deserializeInstance(ServerSerializationStreamReader streamReader, List instance,
        Type[] expectedParameterTypes, DequeMap<TypeVariable<?>, Type> actualParameterTypes)
        throws SerializationException {
      // Handled in instantiateInstance.
    }

    @Override
    public boolean hasCustomInstantiateInstance() {
      return true;
    }

    @Override
    public List instantiateInstance(SerializationStreamReader streamReader)
        throws SerializationException {
      return com.google.gwt.user.client.rpc.core.java.util.Arrays.ArrayList_CustomFieldSerializer
          .instantiate(streamReader);
    }

    @Override
    public List instantiateInstance(ServerSerializationStreamReader streamReader,
        Type[] expectedParameterTypes, DequeMap<TypeVariable<?>, Type> resolvedTypes) throws
        SerializationException {
      return instantiate(streamReader, expectedParameterTypes, resolvedTypes);
    }

    @Override
    public void serializeInstance(SerializationStreamWriter streamWriter, List instance)
        throws SerializationException {
      com.google.gwt.user.client.rpc.core.java.util.Arrays.ArrayList_CustomFieldSerializer
          .serialize(streamWriter, instance);
    }
  }
}
