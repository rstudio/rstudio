/*
 * Copyright 2010 Google Inc.
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

import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Dummy class for nesting the custom serializer.
 */
public final class Collections {

  /**
   * Custom field serializer for {@link java.util.Collections.EmptyList}.
   */
  @SuppressWarnings("rawtypes")
  public static final class EmptyList_CustomFieldSerializer extends
      CustomFieldSerializer<List> {

    public static String concreteType() {
      return java.util.Collections.emptyList().getClass().getName();
    }

    @SuppressWarnings("unused")
    public static void deserialize(SerializationStreamReader streamReader,
        List instance) throws SerializationException {
      // Handled in instantiate.
    }

    @SuppressWarnings("unused")
    public static List instantiate(SerializationStreamReader streamReader)
        throws SerializationException {
      return java.util.Collections.emptyList();
    }

    @SuppressWarnings("unused")
    public static void serialize(SerializationStreamWriter streamWriter,
        List instance) throws SerializationException {
      // Nothing to serialize -- instantiate always returns the same thing
    }

    @Override
    public void deserializeInstance(SerializationStreamReader streamReader,
        List instance) throws SerializationException {
      deserialize(streamReader, instance);
    }

    @Override
    public boolean hasCustomInstantiateInstance() {
      return true;
    }

    @Override
    public List instantiateInstance(SerializationStreamReader streamReader)
        throws SerializationException {
      return instantiate(streamReader);
    }

    @Override
    public void serializeInstance(SerializationStreamWriter streamWriter,
        List instance) throws SerializationException {
      serialize(streamWriter, instance);
    }
  }

  /**
   * Custom field serializer for {@link java.util.Collections.EmptyMap}.
   */
  @SuppressWarnings("rawtypes")
  public static final class EmptyMap_CustomFieldSerializer extends
      CustomFieldSerializer<Map> {

    public static String concreteType() {
      return java.util.Collections.emptyMap().getClass().getName();
    }

    @SuppressWarnings("unused")
    public static void deserialize(SerializationStreamReader streamReader,
        Map instance) throws SerializationException {
      // Handled in instantiate.
    }

    @SuppressWarnings("unused")
    public static Map instantiate(SerializationStreamReader streamReader)
        throws SerializationException {
      return java.util.Collections.emptyMap();
    }

    @SuppressWarnings("unused")
    public static void serialize(SerializationStreamWriter streamWriter,
        Map instance) throws SerializationException {
      // Nothing to serialize -- instantiate always returns the same thing
    }

    @Override
    public void deserializeInstance(SerializationStreamReader streamReader,
        Map instance) throws SerializationException {
      deserialize(streamReader, instance);
    }

    @Override
    public boolean hasCustomInstantiateInstance() {
      return true;
    }

    @Override
    public Map instantiateInstance(SerializationStreamReader streamReader)
        throws SerializationException {
      return instantiate(streamReader);
    }

    @Override
    public void serializeInstance(SerializationStreamWriter streamWriter,
        Map instance) throws SerializationException {
      serialize(streamWriter, instance);
    }
  }

  /**
   * Custom field serializer for {@link java.util.Collections.EmptySet}.
   */
  @SuppressWarnings("rawtypes")
  public static final class EmptySet_CustomFieldSerializer extends
      CustomFieldSerializer<Set> {

    public static String concreteType() {
      return java.util.Collections.emptySet().getClass().getName();
    }

    @SuppressWarnings("unused")
    public static void deserialize(SerializationStreamReader streamReader,
        Set instance) throws SerializationException {
      // Handled in instantiate.
    }

    @SuppressWarnings("unused")
    public static Set instantiate(SerializationStreamReader streamReader)
        throws SerializationException {
      return java.util.Collections.emptySet();
    }

    @SuppressWarnings("unused")
    public static void serialize(SerializationStreamWriter streamWriter,
        Set instance) throws SerializationException {
      // Nothing to serialize -- instantiate always returns the same thing
    }

    @Override
    public void deserializeInstance(SerializationStreamReader streamReader,
        Set instance) throws SerializationException {
      deserialize(streamReader, instance);
    }

    @Override
    public boolean hasCustomInstantiateInstance() {
      return true;
    }

    @Override
    public Set instantiateInstance(SerializationStreamReader streamReader)
        throws SerializationException {
      return instantiate(streamReader);
    }

    @Override
    public void serializeInstance(SerializationStreamWriter streamWriter,
        Set instance) throws SerializationException {
      serialize(streamWriter, instance);
    }
  }

  /**
   * Custom field serializer for {@link java.util.Collections.SingletonList}.
   */
  @SuppressWarnings("rawtypes")
  public static final class SingletonList_CustomFieldSerializer extends
      CustomFieldSerializer<List> {

    public static String concreteType() {
      return java.util.Collections.singletonList(null).getClass().getName();
    }

    @SuppressWarnings("unused")
    public static void deserialize(SerializationStreamReader streamReader,
        List instance) throws SerializationException {
    }

    public static List instantiate(SerializationStreamReader streamReader)
        throws SerializationException {
      return java.util.Collections.singletonList(streamReader.readObject());
    }

    public static void serialize(SerializationStreamWriter streamWriter,
        List instance) throws SerializationException {
      streamWriter.writeObject(instance.get(0));
    }

    @Override
    public void deserializeInstance(SerializationStreamReader streamReader,
        List instance) throws SerializationException {
      deserialize(streamReader, instance);
    }

    @Override
    public boolean hasCustomInstantiateInstance() {
      return true;
    }

    @Override
    public List instantiateInstance(SerializationStreamReader streamReader)
        throws SerializationException {
      return instantiate(streamReader);
    }

    @Override
    public void serializeInstance(SerializationStreamWriter streamWriter,
        List instance) throws SerializationException {
      serialize(streamWriter, instance);
    }
  }
}
