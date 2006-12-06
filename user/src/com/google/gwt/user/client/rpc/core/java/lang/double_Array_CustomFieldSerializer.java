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
 * Custom Serializer for arrays of double.
 */
public class double_Array_CustomFieldSerializer {

  public static void deserialize(SerializationStreamReader streamReader,
      double[] instance) throws SerializationException {
    for (int itemIndex = 0; itemIndex < instance.length; ++itemIndex) {
      instance[itemIndex] = streamReader.readDouble();
    }
  }

  public static void serialize(SerializationStreamWriter streamWriter,
      double[] instance) throws SerializationException {
    int itemCount = instance.length;
    streamWriter.writeInt(itemCount);
    for (int itemIndex = 0; itemIndex < itemCount; ++itemIndex) {
      streamWriter.writeDouble(instance[itemIndex]);
    }
  }

}
