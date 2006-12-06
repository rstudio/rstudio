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
 * Custom Serializer for arrays of char.
 */
public class char_Array_CustomFieldSerializer {

  public static void deserialize(SerializationStreamReader streamReader,
      char[] instance) throws SerializationException {
    
    for (int i = 0; i < instance.length; ++i) {
      instance[i] = streamReader.readChar();
    }
  }

  public static void serialize(SerializationStreamWriter streamWriter,
      char[] instance) throws SerializationException {
    
    streamWriter.writeInt(instance.length);
    
    for (int i = 0; i < instance.length; ++i) {
      streamWriter.writeChar(instance[i]);
    }
  }

}
