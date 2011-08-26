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
import java.lang.reflect.TypeVariable;

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
   * The calling code has verified that the instance this method is
   * deserializing is of the correct type for the RPC call. However, is has not
   * verified the objects that this deserializer will read. It is this method's
   * responsibility to verify the types of objects that it reads. Failure to
   * do so leaves the server vulnerable to an attacker who replaces
   * deserialized data in the RPC message with data that takes an exponential
   * time to deserialize or otherwise causes problems.
   * 
   * In practice, any call to ServerSerilizationStreamReader.readObject() should
   * use the type checking version, passing in the expected type of the object
   * to be read. For classes that deserialize objects of generic types, the
   * expectedParameterTypes array provides the type bound to each type
   * generic parameter defined by the instance. See the built-in GWT
   * server custom field serializers for examples.
   * 
   * @param streamReader the {@link ServerSerializationStreamReader} to read the
   *          object's content from
   * @param instance the object instance to deserialize
   * @param expectedParameterTypes the types we expect for any generic
   *          parameters used by this class, in the order in which they
   *          appear in the instance.getTypeParameters()
   * @param resolvedTypes map from generic types to actual types
   * 
   * @throws SerializationException if the deserialization operation is not
   *           successful
   */
  public abstract void deserializeInstance(ServerSerializationStreamReader streamReader,
      T instance, Type[] expectedParameterTypes,
      DequeMap<TypeVariable<?>, Type> resolvedTypes) throws SerializationException;

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
   * The calling code has verified that the instance this method is
   * instantiating is of the correct type for the RPC call. However, is has not
   * verified the objects that this instantiator will read. It is this method's
   * responsibility to verify the types of objects that it reads. Failure to
   * do so leaves the server vulnerable to an attacker who replaces
   * deserialized data in the RPC message with data that takes an exponential
   * time to instantiate or otherwise causes problems.
   * 
   * In practice, any call to ServerSerilizationStreamReader.readObject() should
   * use the type checking version, passing in the expected type of the object
   * to be read. For classes that instantiate objects of generic types, the
   * expectedParameterTypes array provides the type bound to each type
   * generic parameter defined by the instance. See the built-in GWT
   * server custom field serializers for examples.
   *
   * @param streamReader the {@link ServerSerializationStreamReader} to read the
   *          object's content from
   * @param expectedParameterTypes the types we expect for any generic
   *          parameters used by this class, in the order returned by
   *          instance.getTypeParameters()
   * @param resolvedTypes map from generic types to actual types
   * 
   * @return an object that has been loaded from the
   *         {@link ServerSerializationStreamReader}
   * 
   * @throws SerializationException if the instantiation operation is not
   *           successful
   */
  public T instantiateInstance(ServerSerializationStreamReader streamReader,
      Type[] expectedParameterTypes,
      DequeMap<TypeVariable<?>, Type> resolvedTypes) throws SerializationException {
    return super.instantiateInstance(streamReader);
  }
}
