/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.user.client.rpc.core.java.util;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

import java.util.List;

/**
 * Dummy class for nesting the custom serializer.
 */
public final class Arrays {

  /**
   * Custom field serializer for {@link java.util.Arrays$ArrayList}.
   */
  public static final class ArrayList_CustomFieldSerializer {
    /*
     * Note: the reason this implementation differs from that of a standard List
     * (which serializes a number and then each element) is the requirement that
     * the underlying array retain its correct type across the wire. This gives
     * toArray() results the correct type, and can generate internal
     * ArrayStoreExceptions.
     */
    @SuppressWarnings("unused")
    public static void deserialize(SerializationStreamReader streamReader,
        List<?> instance) throws SerializationException {
      // Handled in instantiate.
    }

    public static List<?> instantiate(SerializationStreamReader streamReader)
        throws SerializationException {
      Object[] array = (Object[]) streamReader.readObject();
      return java.util.Arrays.asList(array);
    }

    public static void serialize(SerializationStreamWriter streamWriter,
        List<?> instance) throws SerializationException {
      Object[] array;
      if (GWT.isScript()) {
        // Violator pattern.
        array = getArray0(instance);
      } else {
        // Clone the underlying array.
        array = instance.toArray();
      }
      streamWriter.writeObject(array);
    }

    private static native Object[] getArray0(List<?> instance) /*-{
      return instance.@java.util.Arrays$ArrayList::array;
    }-*/;
  }
}
