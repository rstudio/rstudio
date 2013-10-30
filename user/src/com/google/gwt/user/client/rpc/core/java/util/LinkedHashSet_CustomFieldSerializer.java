/*
 * Copyright 2014 Google Inc.
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

import java.util.LinkedHashSet;

/**
 * Custom field serializer for {@link java.util.LinkedHashSet}.
 */
@SuppressWarnings("rawtypes")
public final class LinkedHashSet_CustomFieldSerializer extends CustomFieldSerializer<LinkedHashSet> {

  public static void deserialize(SerializationStreamReader streamReader, LinkedHashSet instance)
      throws SerializationException {
    Collection_CustomFieldSerializerBase.deserialize(streamReader, instance);
  }

  public static void serialize(SerializationStreamWriter streamWriter, LinkedHashSet instance)
      throws SerializationException {
    Collection_CustomFieldSerializerBase.serialize(streamWriter, instance);
  }

  @Override
  public void deserializeInstance(SerializationStreamReader streamReader, LinkedHashSet instance)
      throws SerializationException {
    deserialize(streamReader, instance);
  }

  @Override
  public void serializeInstance(SerializationStreamWriter streamWriter, LinkedHashSet instance)
      throws SerializationException {
    serialize(streamWriter, instance);
  }

}
