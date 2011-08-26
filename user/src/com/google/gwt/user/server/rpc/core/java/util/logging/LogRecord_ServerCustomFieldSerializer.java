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

package com.google.gwt.user.server.rpc.core.java.util.logging;

import com.google.gwt.core.client.impl.SerializableThrowable;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.google.gwt.user.client.rpc.core.java.util.logging.LogRecord_CustomFieldSerializer;
import com.google.gwt.user.server.rpc.ServerCustomFieldSerializer;
import com.google.gwt.user.server.rpc.impl.DequeMap;
import com.google.gwt.user.server.rpc.impl.ServerSerializationStreamReader;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.logging.LogRecord;

/**
 * Custom serializer for LogRecord.
 */
public class LogRecord_ServerCustomFieldSerializer extends ServerCustomFieldSerializer<LogRecord> {
  @SuppressWarnings("unused")
  public static void deserialize(ServerSerializationStreamReader streamReader, LogRecord instance,
      Type[] expectedParameterTypes, DequeMap<TypeVariable<?>, Type> resolvedTypes) throws
      SerializationException {
    String loggerName = streamReader.readString();
    Long millis = streamReader.readLong();
    Object throwable = streamReader.readObject(SerializableThrowable.class, resolvedTypes);

    instance.setLoggerName(loggerName);
    instance.setMillis(millis);
    if (throwable != null && throwable instanceof SerializableThrowable) {
      instance.setThrown(((SerializableThrowable) throwable).getThrowable());
    }
  }

  @Override
  public void deserializeInstance(SerializationStreamReader streamReader, LogRecord instance)
      throws SerializationException {
    LogRecord_CustomFieldSerializer.deserialize(streamReader, instance);
  }

  @Override
  public void deserializeInstance(ServerSerializationStreamReader streamReader, LogRecord instance,
      Type[] expectedParameterTypes, DequeMap<TypeVariable<?>, Type> resolvedTypes) throws
      SerializationException {
    deserialize(streamReader, instance, expectedParameterTypes, resolvedTypes);
  }

  @Override
  public boolean hasCustomInstantiateInstance() {
    return true;
  }

  @Override
  public LogRecord instantiateInstance(ServerSerializationStreamReader reader,
      Type[] expectedParameterTypes, DequeMap<TypeVariable<?>, Type> resolvedTypes) throws
      SerializationException {
    return LogRecord_CustomFieldSerializer.instantiate(reader);
  }

  @Override
  public void serializeInstance(SerializationStreamWriter writer, LogRecord lr)
      throws SerializationException {
    LogRecord_CustomFieldSerializer.serialize(writer, lr);
  }
}
