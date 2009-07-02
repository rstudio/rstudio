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
package com.google.gwt.user.rebind.rpc;

import com.google.gwt.core.ext.typeinfo.JClassType;

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
 * {@link FieldSerializerCreator} as part of the generation of client-side field
 * serializers.
 * 
 * @see com.google.gwt.user.server.rpc.impl.ServerDataSerializer
 */
public abstract class ClientDataSerializer implements
    Comparable<ClientDataSerializer> {

  /**
   * A mapping from ClientDataSerializer names to instances, sorted by name.
   */
  private static TreeMap<String, ClientDataSerializer> serializers =
    new TreeMap<String, ClientDataSerializer>();

  /**
   * All active ServerDataSerializers must be initialized here and placed into
   * the serializers map.
   * 
   * <p>
   * The map must be kept in sync with the one in
   * {@link com.google.gwt.user.server.rpc.impl.ServerDataSerializer}.
   */
  static {
    // Load and register a JdoDetachedStateSerializer
    ClientDataSerializer serializer = JdoDetachedStateClientDataSerializer.getInstance();
    serializers.put(serializer.getName(), serializer);
  }

  /**
   * Returns a Collection of all ClientDataSerializer instances, ordered by name.
   * The returned collection is unmodifiable.
   */
  public static Collection<ClientDataSerializer> getSerializers() {
    return Collections.unmodifiableCollection(serializers.values());
  }

  /**
   * Allow ServerDataSerialzer instances to be sorted by class name.
   */
  public int compareTo(ClientDataSerializer other) {
    return getName().compareTo(other.getName());
  }

  /**
   * Returns the name of this {@link ServerDataSerializer} instance, used to
   * determine the sorting order when multiple serializers apply to a given
   * class type.  The name will be used as a key to store the serialized data
   * on the client.
   * 
   * <p>
   * The name must be identical to that of the corresponding
   * {@link ServerDataSerializer}.
   */
  public abstract String getName();

  /**
   * Returns true if the given classType should be processed by a
   * ServerClientSerializer.
   * 
   * @param classType the class type to be queried.
   */
  public abstract boolean shouldSerialize(JClassType classType);
}
