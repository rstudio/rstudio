/*
 * Copyright 2013 Google Inc.
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
package com.google.gwt.user.client.rpc.core.com.google.gwt.core.shared;

import com.google.gwt.core.shared.SerializableThrowable;
import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

/**
 * Custom field serializer for SerializableThrowable.
 */
public final class SerializableThrowable_CustomFieldSerializer
    extends CustomFieldSerializer<SerializableThrowable> {

  public static SerializableThrowable instantiate(SerializationStreamReader streamReader)
      throws SerializationException {
    return new SerializableThrowable(null, streamReader.readString());
  }

  public static void deserialize(
      SerializationStreamReader streamReader, SerializableThrowable instance)
      throws SerializationException {
    String type = streamReader.readString();
    boolean typeIsExact = streamReader.readBoolean();
    instance.setDesignatedType(type, typeIsExact);
    instance.setStackTrace((StackTraceElement[]) streamReader.readObject());
    instance.initCause((Throwable) streamReader.readObject());
  }

  public static void serialize(
      SerializationStreamWriter streamWriter, SerializableThrowable instance)
      throws SerializationException {
    streamWriter.writeString(instance.getMessage());
    streamWriter.writeString(instance.getDesignatedType());
    streamWriter.writeBoolean(instance.isExactDesignatedTypeKnown());
    streamWriter.writeObject(instance.getStackTrace());
    streamWriter.writeObject(instance.getCause());
  }

  @Override
  public boolean hasCustomInstantiateInstance() {
    return true;
  }

  @Override
  public SerializableThrowable instantiateInstance(SerializationStreamReader streamReader)
      throws SerializationException {
    return instantiate(streamReader);
  }

  @Override
  public void deserializeInstance(
      SerializationStreamReader streamReader, SerializableThrowable instance)
      throws SerializationException {
    deserialize(streamReader, instance);
  }

  @Override
  public void serializeInstance(
      SerializationStreamWriter streamWriter, SerializableThrowable instance)
      throws SerializationException {
    serialize(streamWriter, instance);
  }
}
