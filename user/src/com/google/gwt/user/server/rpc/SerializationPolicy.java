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
package com.google.gwt.user.server.rpc;

import com.google.gwt.user.client.rpc.SerializationException;

import java.util.Set;

/**
 * This is an abstract class for representing the serialization policy for a
 * given module and
 * {@link com.google.gwt.user.client.rpc.RemoteService RemoteService}.
 * The serialize and deserialize queries are from the perspective
 * of the server, not the web browser.
 */
public abstract class SerializationPolicy {

  /**
   * Returns the field names of the given class known to the client for classes
   * that are expected to be enhanced on the server to have additional fields,
   * or null for classes that are not expected to be enhanced.
   * 
   * @param clazz the class to test
   * @return a set containing client field names, or null
   */
  public Set<String> getClientFieldNamesForEnhancedClass(Class<?> clazz) {
    // Ignore the possibility of server-side enhancement for legacy classes.
    return null;
  }
  
  /**
   * Returns <code>true</code> if the class' fields should be deserialized.
   * 
   * @param clazz the class to test
   * @return <code>true</code> if the class' fields should be deserialized
   */
  public abstract boolean shouldDeserializeFields(Class<?> clazz);

  /**
   * Returns <code>true</code> if the class' fields should be serialized.
   * 
   * @param clazz the class to test
   * @return <code>true</code> if the class' fields should be serialized
   */
  public abstract boolean shouldSerializeFields(Class<?> clazz);

  /**
   * Validates that the specified class should be deserialized from a stream.
   * 
   * @param clazz the class to validate
   * @throws SerializationException if the class is not allowed to be
   *           deserialized
   */
  public abstract void validateDeserialize(Class<?> clazz)
      throws SerializationException;

  /**
   * Validates that the specified class should be serialized into a stream.
   * 
   * @param clazz the class to validate
   * @throws SerializationException if the class is not allowed to be serialized
   */
  public abstract void validateSerialize(Class<?> clazz)
      throws SerializationException;
}
