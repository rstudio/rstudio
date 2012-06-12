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
package com.google.gwt.user.server.rpc.core.java.util;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.google.gwt.user.client.rpc.core.java.util.EnumMap_CustomFieldSerializer;
import com.google.gwt.user.client.rpc.core.java.util.Map_CustomFieldSerializerBase;
import com.google.gwt.user.server.rpc.ServerCustomFieldSerializer;
import com.google.gwt.user.server.rpc.impl.DequeMap;
import com.google.gwt.user.server.rpc.impl.ServerSerializationStreamReader;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.EnumMap;

/**
 * Custom field serializer for {@link java.util.EnumMap} for the server.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public final class EnumMap_ServerCustomFieldSerializer extends ServerCustomFieldSerializer<EnumMap>
{

    public static void deserialize(ServerSerializationStreamReader streamReader, EnumMap instance,
        Type[] expectedParameterTypes, DequeMap<TypeVariable< ? >, Type> resolvedTypes)
        throws SerializationException {
        Map_ServerCustomFieldSerializerBase.deserialize(streamReader, instance,
          expectedParameterTypes, resolvedTypes);
    }

    @Override
    public void deserializeInstance(SerializationStreamReader streamReader, EnumMap instance)
        throws SerializationException {
        EnumMap_CustomFieldSerializer.deserialize(streamReader, instance);
    }

    @Override
    public void deserializeInstance(ServerSerializationStreamReader streamReader, EnumMap instance,
        Type[] expectedParameterTypes, DequeMap<TypeVariable< ? >, Type> resolvedTypes)
        throws SerializationException {
        deserialize(streamReader, instance, expectedParameterTypes, resolvedTypes);
    }

    @Override
    public boolean hasCustomInstantiateInstance() {
        return true;
    }

    @Override
    public EnumMap instantiateInstance(SerializationStreamReader streamReader)
        throws SerializationException {
        return EnumMap_CustomFieldSerializer.instantiate(streamReader);
    }

    @Override
    public EnumMap instantiateInstance(ServerSerializationStreamReader streamReader,
        Type[] expectedParameterTypes, DequeMap<TypeVariable< ? >, Type> resolvedTypes)
        throws SerializationException {
      return EnumMap_CustomFieldSerializer.instantiate(streamReader);
    }

    @Override
    public void serializeInstance(SerializationStreamWriter streamWriter, EnumMap instance)
        throws SerializationException {
       Class c = instance.getClass();
       Field keyUniverseField;
       Object keyUniverse = null;
       try {
         keyUniverseField = c.getDeclaredField("keyUniverse");
         keyUniverseField.setAccessible(true);
         keyUniverse = keyUniverseField.get(instance);
       } catch (Exception e) {
         throw new SerializationException(e);
       }
       Object exemplar = Array.get(keyUniverse, 0);
       streamWriter.writeObject(exemplar);
       Map_CustomFieldSerializerBase.serialize(streamWriter, instance);
    }
}
