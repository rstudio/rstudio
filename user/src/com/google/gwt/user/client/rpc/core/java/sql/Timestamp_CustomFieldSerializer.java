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
package com.google.gwt.user.client.rpc.core.java.sql;

import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

import java.sql.Timestamp;

/**
 * Custom field serializer for {@link java.sql.Timestamp}.
 */
public final class Timestamp_CustomFieldSerializer extends
    CustomFieldSerializer<Timestamp> {

  public static void deserialize(SerializationStreamReader streamReader,
      Timestamp instance) throws SerializationException {
    instance.setNanos(streamReader.readInt());
  }

  public static Timestamp instantiate(SerializationStreamReader streamReader)
      throws SerializationException {
    return new Timestamp(streamReader.readLong());
  }

  public static void serialize(SerializationStreamWriter streamWriter,
      Timestamp instance) throws SerializationException {
    streamWriter.writeLong(instance.getTime());
    streamWriter.writeInt(instance.getNanos());
  }

  @Override
  public void deserializeInstance(SerializationStreamReader streamReader,
      Timestamp instance) throws SerializationException {
    deserialize(streamReader, instance);
  }

  @Override
  public boolean hasCustomInstantiateInstance() {
    return true;
  }

  @Override
  public Timestamp instantiateInstance(SerializationStreamReader streamReader)
      throws SerializationException {
    return instantiate(streamReader);
  }

  @Override
  public void serializeInstance(SerializationStreamWriter streamWriter,
      Timestamp instance) throws SerializationException {
    serialize(streamWriter, instance);
  }
}
