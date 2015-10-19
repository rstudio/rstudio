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
import com.google.gwt.user.server.rpc.impl.ServerSerializationStreamReader;

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

    public static List<?> instantiate(ServerSerializationStreamReader streamReader,
        Type[] expectedParameterTypes, DequeMap<TypeVariable<?>, Type> resolvedTypes)
        throws SerializationException {
      return com.google.gwt.user.client.rpc.core.java.util.Arrays.ArrayList_CustomFieldSerializer
            .instantiate(streamReader);
    }

    @Override
    public void deserializeInstance(SerializationStreamReader streamReader, List instance)
        throws SerializationException {
      // Handled in instantiateInstance.
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

