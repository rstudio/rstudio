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
package com.google.gwt.user.server.rpc.impl;

import com.google.gwt.user.client.rpc.SerializationException;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.TreeMap;

/**
 * An interface for serializing and deserializing portions of Object data that
 * are present in the server implementation but are not present in client code.
 * For example, some persistence frameworks make use of server-side bytecode
 * enhancement; the fields added by such enhancement are unknown to the client,
 * and therefore are not handled by normal GWT RPC mechanisms.
 * 
 * <p>
 * This portion of the interface is called from the
 * {@link ServerSerializationStreamReader} and {@link ServerSerializationStreamWriter} classes
 * as part of the server-side marshalling of data for RPC calls.
 * 
 * @see com.google.gwt.user.rebind.rpc.ClientDataSerializer
 */
public abstract class ServerDataSerializer implements
    Comparable<ServerDataSerializer> {

  /**
   * A mapping from ServerDataSerializer names to instances, sorted by name.
   */
  private static TreeMap<String, ServerDataSerializer> serializers =
    new TreeMap<String, ServerDataSerializer>();

  /**
   * All active ServerDataSerializers must be initialized here and placed into
   * the serializers map.
   * 
   * <p>
   * The map must be kept in sync with the one in
   * {@link com.google.gwt.user.rebind.rpc.ClientDataSerializer}.
   */
  static {
    // Load and register a JdoDetachedStateSerializer
    ServerDataSerializer serializer = JdoDetachedStateServerDataSerializer.getInstance();
    serializers.put(serializer.getName(), serializer);
  }

  /**
   * Returns a Collection of all ServerDataSerializer instances, ordered by name.
   * The returned collection is unmodifiable.
   */
  public static Collection<ServerDataSerializer> getSerializers() {
    return Collections.unmodifiableCollection(serializers.values());
  }

  /**
   * Allow ServerDataSerialzer instances to be sorted by class name.
   */
  public int compareTo(ServerDataSerializer other) {
    return getName().compareTo(other.getName());
  }

  /**
   * Custom deserialize server-only data.
   * 
   * @param serializedData the serialized data, as an array of bytes, possible
   *          null.
   * @param instance the Object instance to be modified.
   * @throws SerializationException if the field contents cannot be
   *           reconstructed.
   */
  public abstract void deserializeServerData(byte[] serializedData,
      Object instance) throws SerializationException;

  /**
   * Returns the name of this {@link ServerDataSerializer} instance, used to
   * determine the sorting order when multiple serializers apply to a given
   * class type.
   * 
   * <p>
   * The name must be identical to that of the corresponding
   * {@link ClientDataSerializer}.
   */
  public abstract String getName();

  /**
   * Custom serialize the contents of a server-only field.
   * 
   * @param instance an Object containing server-only data.
   * @return a byte array containing a representation of the field.
   * @throws SerializationException if the instance cannot be serialized by this serializer.
   */
  public abstract byte[] serializeServerData(Object instance)
      throws SerializationException;

  /**
   * Returns true if the instanceClass should be processed by a ServerDataSerializer.
   * 
   * @param instanceClass the class to be queried.
   */
  public abstract boolean shouldSerialize(Class<?> instanceClass);
  
  /**
   * Returns true if the given field should be skipped by the normal RPC mechanism.
   * 
   * @param field the field to be queried.
   */
  public abstract boolean shouldSkipField(Field field);
}
