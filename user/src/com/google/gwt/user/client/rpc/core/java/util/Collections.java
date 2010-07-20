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

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Dummy class for nesting the custom serializer.
 */
public final class Collections {

  /**
   * Custom field serializer for {@link java.util.Collections$EmptyList}.
   */
  public static final class EmptyList_CustomFieldSerializer {

    @SuppressWarnings({"unused", "unchecked"})
    public static void deserialize(SerializationStreamReader streamReader,
        List instance) throws SerializationException {
      // Handled in instantiate.
    }

    @SuppressWarnings({"unused", "unchecked"})
    public static List instantiate(SerializationStreamReader streamReader)
        throws SerializationException {
      return java.util.Collections.emptyList();
    }

    @SuppressWarnings({"unused", "unchecked"})
    public static void serialize(SerializationStreamWriter streamWriter,
        List instance) throws SerializationException {
      // Nothing to serialize -- instantiate always returns the same thing
    }
  }

  /**
   * Custom field serializer for {@link java.util.Collections$EmptyMap}.
   */
  public static final class EmptyMap_CustomFieldSerializer {

    @SuppressWarnings({"unused", "unchecked"})
    public static void deserialize(SerializationStreamReader streamReader,
        Map instance) throws SerializationException {
      // Handled in instantiate.
    }

    @SuppressWarnings({"unused", "unchecked"})
    public static Map instantiate(SerializationStreamReader streamReader)
        throws SerializationException {
      return java.util.Collections.emptyMap();
    }

    @SuppressWarnings({"unused", "unchecked"})
    public static void serialize(SerializationStreamWriter streamWriter,
        Map instance) throws SerializationException {
      // Nothing to serialize -- instantiate always returns the same thing
    }
  }

  /**
   * Custom field serializer for {@link java.util.Collections$EmptySet}.
   */
  public static final class EmptySet_CustomFieldSerializer {

    @SuppressWarnings({"unused", "unchecked"})
    public static void deserialize(SerializationStreamReader streamReader,
        Set instance) throws SerializationException {
      // Handled in instantiate.
    }

    @SuppressWarnings({"unused", "unchecked"})
    public static Set instantiate(SerializationStreamReader streamReader)
        throws SerializationException {
      return java.util.Collections.emptySet();
    }

    @SuppressWarnings({"unused", "unchecked"})
    public static void serialize(SerializationStreamWriter streamWriter,
        Set instance) throws SerializationException {
      // Nothing to serialize -- instantiate always returns the same thing
    }
  }

  /**
   * Custom field serializer for {@link java.util.Collections$SingletonList}.
   */
  public static final class SingletonList_CustomFieldSerializer {

    @SuppressWarnings({"unused", "unchecked"})
    public static void deserialize(SerializationStreamReader streamReader,
        List instance) throws SerializationException {
    }

    @SuppressWarnings("unchecked")
    public static List instantiate(SerializationStreamReader streamReader)
        throws SerializationException {
      return java.util.Collections.singletonList(streamReader.readObject());
    }

    @SuppressWarnings("unchecked")
    public static void serialize(SerializationStreamWriter streamWriter,
        List instance) throws SerializationException {
      streamWriter.writeObject(instance.get(0));
    }
  }

  /**
   * Custom field serializer for {@link java.util.Collections$SingletonMap}.
   */
  public static final class SingletonMap_CustomFieldSerializer {

    @SuppressWarnings({"unused", "unchecked"})
    public static void deserialize(SerializationStreamReader streamReader,
        Map instance) throws SerializationException {
    }

    @SuppressWarnings("unchecked")
    public static Map instantiate(SerializationStreamReader streamReader)
        throws SerializationException {
      Object key = streamReader.readObject();
      Object value = streamReader.readObject();
      return java.util.Collections.singletonMap(key, value);
    }

    @SuppressWarnings("unchecked")
    public static void serialize(SerializationStreamWriter streamWriter,
        Map instance) throws SerializationException {
      Map.Entry entry = (Map.Entry) instance.entrySet().iterator().next();
      streamWriter.writeObject(entry.getKey());
      streamWriter.writeObject(entry.getValue());
    }
  }

  /**
   * Custom field serializer for {@link java.util.Collections$UnmodifiableCollection}.
   */
  public static final class UnmodifiableCollection_CustomFieldSerializer {

    @SuppressWarnings({"unused", "unchecked"})
    public static void deserialize(SerializationStreamReader streamReader,
        Collection instance) throws SerializationException {
    }

    @SuppressWarnings("unchecked")
    public static Collection instantiate(SerializationStreamReader streamReader)
        throws SerializationException {
      Collection collection = new ArrayList();
      Collection_CustomFieldSerializerBase.deserialize(streamReader, collection);
      return java.util.Collections.unmodifiableCollection(collection);
    }

    @SuppressWarnings("unchecked")
    public static void serialize(SerializationStreamWriter streamWriter,
        Collection instance) throws SerializationException {
      Collection_CustomFieldSerializerBase.serialize(streamWriter, instance);
    }
  }

  /**
   * Custom field serializer for {@link java.util.Collections$UnmodifiableList}.
   */
  public static final class UnmodifiableList_CustomFieldSerializer {

    @SuppressWarnings({"unused", "unchecked"})
    public static void deserialize(SerializationStreamReader streamReader,
        List instance) throws SerializationException {
    }

    @SuppressWarnings("unchecked")
    public static List instantiate(SerializationStreamReader streamReader)
        throws SerializationException {
      ArrayList list = new ArrayList();
      Collection_CustomFieldSerializerBase.deserialize(streamReader, list);
      return java.util.Collections.unmodifiableList(list);
    }

    @SuppressWarnings("unchecked")
    public static void serialize(SerializationStreamWriter streamWriter,
        List instance) throws SerializationException {
      Collection_CustomFieldSerializerBase.serialize(streamWriter, instance);
    }
  }

  /**
   * Custom field serializer for {@link java.util.Collections$UnmodifiableMap}.
   */
  public static final class UnmodifiableMap_CustomFieldSerializer {

    @SuppressWarnings({"unused", "unchecked"})
    public static void deserialize(SerializationStreamReader streamReader,
        Map instance) throws SerializationException {
    }

    @SuppressWarnings("unchecked")
    public static Map instantiate(SerializationStreamReader streamReader)
        throws SerializationException {
      HashMap map = new HashMap();
      Map_CustomFieldSerializerBase.deserialize(streamReader, map);
      return java.util.Collections.unmodifiableMap(map);
    }

    @SuppressWarnings("unchecked")
    public static void serialize(SerializationStreamWriter streamWriter,
        Map instance) throws SerializationException {
      Map_CustomFieldSerializerBase.serialize(streamWriter, instance);
    }
  }

  /**
   * Custom field serializer for {@link java.util.Collections$UnmodifiableRandomAccessList}.
   */
  public static final class UnmodifiableRandomAccessList_CustomFieldSerializer {

    @SuppressWarnings({"unused", "unchecked"})
    public static void deserialize(SerializationStreamReader streamReader,
        List instance) throws SerializationException {
    }

    @SuppressWarnings("unchecked")
    public static List instantiate(SerializationStreamReader streamReader)
        throws SerializationException {
      ArrayList list = new ArrayList();
      Collection_CustomFieldSerializerBase.deserialize(streamReader, list);
      return java.util.Collections.unmodifiableList(list);
    }

    @SuppressWarnings("unchecked")
    public static void serialize(SerializationStreamWriter streamWriter,
        List instance) throws SerializationException {
      Collection_CustomFieldSerializerBase.serialize(streamWriter, instance);
    }
  }

  /**
   * Custom field serializer for {@link java.util.Collections$UnmodifiableSet}.
   */
  public static final class UnmodifiableSet_CustomFieldSerializer {

    @SuppressWarnings({"unused", "unchecked"})
    public static void deserialize(SerializationStreamReader streamReader,
        Set instance) throws SerializationException {
    }

    @SuppressWarnings("unchecked")
    public static Set instantiate(SerializationStreamReader streamReader)
        throws SerializationException {
      HashSet set = new HashSet();
      Collection_CustomFieldSerializerBase.deserialize(streamReader, set);
      return java.util.Collections.unmodifiableSet(set);
    }

    @SuppressWarnings("unchecked")
    public static void serialize(SerializationStreamWriter streamWriter,
        Set instance) throws SerializationException {
      Collection_CustomFieldSerializerBase.serialize(streamWriter, instance);
    }
  }

  /**
   * Custom field serializer for {@link java.util.Collections$UnmodifiableSortedMap}.
   */
  public static final class UnmodifiableSortedMap_CustomFieldSerializer {

    @SuppressWarnings({"unused", "unchecked"})
    public static void deserialize(SerializationStreamReader streamReader,
        SortedMap instance) throws SerializationException {
    }

    @SuppressWarnings("unchecked")
    public static SortedMap instantiate(SerializationStreamReader streamReader)
        throws SerializationException {
      Comparator comparator = (Comparator) streamReader.readObject();
      TreeMap map = new TreeMap(comparator);
      Map_CustomFieldSerializerBase.deserialize(streamReader, map);
      return java.util.Collections.unmodifiableSortedMap(map);
    }

    @SuppressWarnings("unchecked")
    public static void serialize(SerializationStreamWriter streamWriter,
        SortedMap instance) throws SerializationException {
      streamWriter.writeObject(instance.comparator());
      Map_CustomFieldSerializerBase.serialize(streamWriter, instance);
    }
  }

  /**
   * Custom field serializer for {@link java.util.Collections$UnmodifiableSortedSet}.
   */
  public static final class UnmodifiableSortedSet_CustomFieldSerializer {

    @SuppressWarnings({"unused", "unchecked"})
    public static void deserialize(SerializationStreamReader streamReader,
        SortedSet instance) throws SerializationException {
    }

    @SuppressWarnings("unchecked")
    public static SortedSet instantiate(SerializationStreamReader streamReader)
        throws SerializationException {
      Comparator comparator = (Comparator) streamReader.readObject();
      TreeSet set = new TreeSet(comparator);
      Collection_CustomFieldSerializerBase.deserialize(streamReader, set);
      return java.util.Collections.unmodifiableSortedSet(set);
    }

    @SuppressWarnings("unchecked")
    public static void serialize(SerializationStreamWriter streamWriter,
        SortedSet instance) throws SerializationException {
      streamWriter.writeObject(instance.comparator());
      Collection_CustomFieldSerializerBase.serialize(streamWriter, instance);
    }
  }
}
