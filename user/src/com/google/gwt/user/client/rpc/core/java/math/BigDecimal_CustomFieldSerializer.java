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

import java.math.BigDecimal;

/**
 * Custom field serializer for BigDecimal.
 */
public class BigDecimal_CustomFieldSerializer extends
    CustomFieldSerializer<BigDecimal> {

  /**
   * @param streamReader a SerializationStreamReader instance
   * @param instance the instance to be deserialized
   */
  public static void deserialize(SerializationStreamReader streamReader,
      BigDecimal instance) {
  }

  public static BigDecimal instantiate(SerializationStreamReader streamReader)
      throws SerializationException {
    return new BigDecimal(streamReader.readString());
  }

  public static void serialize(SerializationStreamWriter streamWriter,
      BigDecimal instance) throws SerializationException {
    streamWriter.writeString(instance.toString());
  }

  @Override
  public void deserializeInstance(SerializationStreamReader streamReader,
      BigDecimal instance) throws SerializationException {
    deserialize(streamReader, instance);
  }

  @Override
  public boolean hasCustomInstantiateInstance() {
    return true;
  }

  @Override
  public BigDecimal instantiateInstance(SerializationStreamReader streamReader)
      throws SerializationException {
    return instantiate(streamReader);
  }

  @Override
  public void serializeInstance(SerializationStreamWriter streamWriter,
      BigDecimal instance) throws SerializationException {
    serialize(streamWriter, instance);
  }
}
