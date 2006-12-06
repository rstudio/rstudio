/*
 * Copyright 2006 Google Inc.
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

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

/**
 * Custom field serializer for {@link java.lang.Long}.
 */
public final class Long_CustomFieldSerializer {

  public static void deserialize(SerializationStreamReader streamReader,
      Long instance) {
    // No fields
  }

  public static Long instantiate(SerializationStreamReader streamReader)
      throws SerializationException {
    return new Long(streamReader.readLong());
  }

  public static void serialize(SerializationStreamWriter streamWriter,
      Long instance) throws SerializationException {
    streamWriter.writeLong(instance.longValue());
  }

}
