/*
 * Copyright 2009 Google Inc.
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
/*
 * author Richard Zschech
 */
package com.google.gwt.user.client.rpc.core.java.math;

import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Custom field serializer for MathContext.
 */
public class MathContext_CustomFieldSerializer extends
    CustomFieldSerializer<MathContext> {

  /**
   * @param streamReader a SerializationStreamReader instance
   * @param instance the instance to be deserialized
   */
  public static void deserialize(SerializationStreamReader streamReader,
      MathContext instance) {
  }

  public static MathContext instantiate(SerializationStreamReader streamReader)
      throws SerializationException {
    return new MathContext(streamReader.readInt(),
        RoundingMode.values()[streamReader.readInt()]);
  }

  public static void serialize(SerializationStreamWriter streamWriter,
      MathContext instance) throws SerializationException {
    streamWriter.writeInt(instance.getPrecision());
    streamWriter.writeInt(instance.getRoundingMode().ordinal());
  }

  @Override
  public void deserializeInstance(SerializationStreamReader streamReader,
      MathContext instance) throws SerializationException {
    deserialize(streamReader, instance);
  }

  @Override
  public boolean hasCustomInstantiateInstance() {
    return true;
  }

  @Override
  public MathContext instantiateInstance(SerializationStreamReader streamReader)
      throws SerializationException {
    return instantiate(streamReader);
  }

  @Override
  public void serializeInstance(SerializationStreamWriter streamWriter,
      MathContext instance) throws SerializationException {
    serialize(streamWriter, instance);
  }
}
