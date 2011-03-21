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

import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * Custom field serializer for {@link java.util.HashMap}.
 */
@SuppressWarnings("rawtypes")
public final class Map_CustomFieldSerializerBase {

  @SuppressWarnings("unchecked")
  public static void deserialize(SerializationStreamReader streamReader,
      Map instance) throws SerializationException {
    int size = streamReader.readInt();

    for (int i = 0; i < size; ++i) {
      Object key = streamReader.readObject();
      Object value = streamReader.readObject();

      instance.put(key, value);
    }
  }

  @SuppressWarnings("unchecked")
  public static void serialize(SerializationStreamWriter streamWriter,
      Map instance) throws SerializationException {
    int size = instance.size();
    streamWriter.writeInt(size);

    for (Entry entry : (Set<Entry>) instance.entrySet()) {
      streamWriter.writeObject(entry.getKey());
      streamWriter.writeObject(entry.getValue());
    }
  }
}