/*
 * Copyright 2010 Google Inc.
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
package org.hibernate.validator.engine;

import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

/**
 * Custom Serializer for {@link PathImpl}.
 */
public class PathImpl_CustomFieldSerializer extends
    CustomFieldSerializer<PathImpl> {

  @SuppressWarnings("unused")
  public static void deserialize(SerializationStreamReader streamReader,
      PathImpl instance) throws SerializationException {
    // no fields
  }

  public static PathImpl instantiate(SerializationStreamReader streamReader)
      throws SerializationException {
    String propertyPath = streamReader.readString();

    return PathImpl.createPathFromString(propertyPath);
  }

  public static void serialize(SerializationStreamWriter streamWriter,
      PathImpl instance) throws SerializationException {
    streamWriter.writeString(instance.toString());
  }

  @Override
  public void deserializeInstance(SerializationStreamReader streamReader,
      PathImpl instance) throws SerializationException {
    deserialize(streamReader, instance);
  }

  @Override
  public boolean hasCustomInstantiateInstance() {
    return true;
  }

  @Override
  public PathImpl instantiateInstance(SerializationStreamReader streamReader)
      throws SerializationException {
    return instantiate(streamReader);
  }

  @Override
  public void serializeInstance(SerializationStreamWriter streamWriter,
      PathImpl instance) throws SerializationException {
    serialize(streamWriter, instance);
  }
}
