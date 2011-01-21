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
package com.google.gwt.user.client.rpc;

/**
 * An interface that may be implemented by class-based custom field serializers
 * which will reduce the amount of server-side reflection during serialization,
 * hence improving their serialization performance.
 *
 * @param <T> the type of the object being serialized
 */
public abstract class CustomFieldSerializer<T> {

  /**
   * Deserializes the content of the object from the
   * {@link SerializationStreamReader}.
   *
   * @param streamReader the {@link SerializationStreamReader} to read the
   *        object's content from
   * @param instance the object instance to deserialize
   *
   * @throws SerializationException if the deserialization operation is not
   *        successful
   */
  public abstract void deserializeInstance(
      SerializationStreamReader streamReader, T instance)
      throws SerializationException;

  /**
   * @return <code>true</code> if a specialist {@link #instantiateInstance} is
   *         implemented; <code>false</code> otherwise
   */
  public boolean hasCustomInstantiateInstance() {
    return false;
  }

  /**
   * Instantiates an object from the {@link SerializationStreamReader}.
   * <p>
   * Most of the time, this can be left unimplemented and the framework
   * will instantiate the instance itself.  This is typically used when the
   * object being deserialized is immutable, hence it has to be created with
   * its state already set.
   * <p>
   * If this is overridden, the {@link #hasCustomInstantiateInstance} method
   * must return <code>true</code> in order for the framework to know to call
   * it.
   *
   * @param streamReader the {@link SerializationStreamReader} to read the
   *        object's content from
   *
   * @return an object that has been loaded from the
   *         {@link SerializationStreamReader}
   *
   * @throws SerializationException if the instantiation operation is not
   *        successful
   */
  public T instantiateInstance(SerializationStreamReader streamReader)
      throws SerializationException {
    throw new SerializationException(
        "instantiateInstance is not supported by " + getClass().getName());
  }

  /**
   * Serializes the content of the object into the
   * {@link SerializationStreamWriter}.
   *
   * @param streamWriter the {@link SerializationStreamWriter} to write the
   *        object's content to
   * @param instance the object instance to serialize
   *
   * @throws SerializationException if the serialization operation is not
   *        successful
   */
  public abstract void serializeInstance(SerializationStreamWriter streamWriter,
      T instance) throws SerializationException;
}
