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
package com.google.gwt.user.client.rpc.impl;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

/**
 * Contract for any class that can serialize and restore class into a
 * serialization stream.
 */
public interface Serializer {

  /**
   * Restore an instantiated object from the serialized stream.
   */
  void deserialize(SerializationStreamReader stream, Object instance,
      String typeSignature) throws SerializationException;

  /**
   * Return the serialization signature for the given type.
   */
  String getSerializationSignature(Class<?> clazz);

  /**
   * Instantiate an object of the given typeName from the serialized stream.
   */
  Object instantiate(SerializationStreamReader stream, String typeSignature)
      throws SerializationException;

  /**
   * Save an instance into the serialization stream.
   */
  void serialize(SerializationStreamWriter stream, Object instance,
      String typeSignature) throws SerializationException;
}
