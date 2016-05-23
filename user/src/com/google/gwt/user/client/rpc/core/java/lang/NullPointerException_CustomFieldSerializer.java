/*
 * Copyright 2016 Google Inc.
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
package com.google.gwt.user.client.rpc.core.java.lang;

import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

/**
 * Custom field serializer for {@link java.lang.NullPointerException}.
 * This is necessary since NullPointerException emul extends JsException which is inconsistent
 * with server-side.
 */
public final class NullPointerException_CustomFieldSerializer
    extends CustomFieldSerializer<NullPointerException> {

  public static NullPointerException instantiate(SerializationStreamReader streamReader)
      throws SerializationException {
    return new NullPointerException(streamReader.readString());
  }

  public static void deserialize(
      SerializationStreamReader streamReader, NullPointerException instance)
      throws SerializationException {
    instance.initCause((Throwable) streamReader.readObject());
  }

  public static void serialize(
      SerializationStreamWriter streamWriter, NullPointerException instance)
      throws SerializationException {
    streamWriter.writeString(instance.getMessage());
    streamWriter.writeObject(instance.getCause());
  }

  @Override
  public boolean hasCustomInstantiateInstance() {
    return true;
  }

  @Override
  public NullPointerException instantiateInstance(SerializationStreamReader streamReader)
      throws SerializationException {
    return instantiate(streamReader);
  }

  @Override
  public void deserializeInstance(
      SerializationStreamReader streamReader, NullPointerException instance)
      throws SerializationException {
    deserialize(streamReader, instance);
  }

  @Override
  public void serializeInstance(
      SerializationStreamWriter streamWriter, NullPointerException instance)
      throws SerializationException {
    serialize(streamWriter, instance);
  }
}
