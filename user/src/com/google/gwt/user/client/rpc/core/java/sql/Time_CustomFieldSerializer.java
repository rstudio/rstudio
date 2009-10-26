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

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

import java.sql.Time;

/**
 * Custom field serializer for {@link java.sql.Time}. We use the three-arg
 * constructor due to differences in implementations when using the single-arg
 * constructor (is the day Jan 1 1970, what are the millis).
 */
public final class Time_CustomFieldSerializer {

  @SuppressWarnings("unused")
  public static void deserialize(SerializationStreamReader streamReader,
      Time instance) {
    // No fields
  }

  public static Time instantiate(SerializationStreamReader streamReader)
      throws SerializationException {
    return new Time(streamReader.readLong());
  }

  public static void serialize(SerializationStreamWriter streamWriter,
      Time instance) throws SerializationException {
    streamWriter.writeLong(instance.getTime());
  }
}
