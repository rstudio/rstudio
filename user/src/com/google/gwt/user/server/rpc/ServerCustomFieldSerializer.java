/*
 * Copyright 2011 Google Inc.
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

import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.server.rpc.impl.DequeMap;
import com.google.gwt.user.server.rpc.impl.ServerSerializationStreamReader;

import java.lang.reflect.Type;

/**
 * An interface that may be implemented by server-side class-based custom field
 * serializers.
 * 
 * Usage of this class will reduce the amount of server-side reflection during
 * serialization and provide type safety.
 * 
 * @param <T> the type of the object being serialized
 */
public abstract class ServerCustomFieldSerializer<T> extends CustomFieldSerializer<T> {
  /**
   * Deserializes the content of the object from the
   * {@link ServerSerializationStreamReader}, with type checking.
   * 
   * @param streamReader the {@link ServerSerializationStreamReader} to read the
   *          object's content from
   * @param instance the object instance to deserialize
   * @param instanceClass the class of the instance for type checking purposes
   * @param resolvedTypes map from generic types to actual types
   * 
   * @throws SerializationException if the deserialization operation is not
   *           successful
   */
  public abstract void deserializeInstance(ServerSerializationStreamReader streamReader,
      T instance, Class<?> instanceClass, DequeMap<Type, Type> resolvedTypes)
      throws SerializationException;

  /**
   * Instantiates an object from the {@link ServerSerializationStreamReader},
   * without type checking.
   * 
   * @param streamReader the {@link ServerSerializationStreamReader} to read the
   *          object's content from
   * @return an object that has been loaded from the
   *         {@link ServerSerializationStreamReader}
   * 
   * @throws SerializationException if the instantiation operation is not
   *           successful
   */
  public T instantiateInstance(ServerSerializationStreamReader streamReader)
      throws SerializationException {
    return super.instantiateInstance(streamReader);
  }

  /**
   * Instantiates an object from the {@link ServerSerializationStreamReader},
   * with type checking.
   * <p>
   * Most of the time, this can be left unimplemented and the framework will
   * instantiate the instance itself. This is typically used when the object
   * being deserialized is immutable, hence it has to be created with its state
   * already set.
   * <p>
   * If this is overridden, the
   * {@link CustomFieldSerializer#hasCustomInstantiateInstance()} method must
   * return <code>true</code> in order for the framework to know to call it.
   * 
   * @param streamReader the {@link ServerSerializationStreamReader} to read the
   *          object's content from
   * @param instanceClass the class of the instance for type checking purposes
   * @param resolvedTypes map from generic types to actual types
   * 
   * @return an object that has been loaded from the
   *         {@link ServerSerializationStreamReader}
   * 
   * @throws SerializationException if the instantiation operation is not
   *           successful
   */
  @SuppressWarnings("unused")
  public T instantiateInstance(ServerSerializationStreamReader streamReader,
      Class<?> instanceClass, DequeMap<Type, Type> resolvedTypes)
      throws SerializationException {
    return super.instantiateInstance(streamReader);
  }

}
