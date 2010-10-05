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

package com.google.gwt.user.client.rpc.core.java.util.logging;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

import java.util.logging.Level;

/**
 * Custom serializer for Level class.
 */
public class Level_CustomFieldSerializer {
  @SuppressWarnings("unused")
  public static void deserialize(SerializationStreamReader reader,
      Level instance) throws SerializationException {
    // Nothing needed
  }

  public static Level instantiate(SerializationStreamReader reader)
  throws SerializationException {
    String name = reader.readString();
    return Level.parse(name);
  }

  public static void serialize(SerializationStreamWriter writer,
      Level level) throws SerializationException {
    writer.writeString(level.getName());
  }
}
