/*
 * Copyright 2008 Google Inc.
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
import com.google.gwt.user.server.rpc.SerializationPolicy;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Standard implementation of a {@link SerializationPolicy}.
 */
public class StandardSerializationPolicy extends SerializationPolicy implements
    TypeNameObfuscator {
  /**
   * Field serializable types are primitives and types on the specified
   * whitelist.
   */
  private static boolean isFieldSerializable(Class<?> clazz,
      Map<Class<?>, Boolean> whitelist) {
    if (clazz.isPrimitive()) {
      return true;
    }
    return whitelist.containsKey(clazz);
  }

  /**
   * Instantiable types are primitives and types on the specified whitelist
   * which can be instantiated.
   */
  private static boolean isInstantiable(Class<?> clazz,
      Map<Class<?>, Boolean> whitelist) {
    if (clazz.isPrimitive()) {
      return true;
    }
    Boolean instantiable = whitelist.get(clazz);
    return (instantiable != null && instantiable);
  }

  private final Map<Class<?>, Set<String>> clientFields;
  private final Map<Class<?>, Boolean> deserializationWhitelist;
  private final Map<Class<?>, Boolean> serializationWhitelist;
  private final Map<Class<?>, String> typeIds;
  private final Map<String, Class<?>> typeIdsToClasses = new HashMap<String, Class<?>>();

  /**
   * Constructs a {@link SerializationPolicy} from several {@link Map}s.
   */
  public StandardSerializationPolicy(
      Map<Class<?>, Boolean> serializationWhitelist,
      Map<Class<?>, Boolean> deserializationWhitelist,
      Map<Class<?>, String> obfuscatedTypeIds) {
    this(serializationWhitelist, deserializationWhitelist, obfuscatedTypeIds,
        null);
  }

  /**
   * Constructs a {@link SerializationPolicy} from several {@link Map}s.
   */
  public StandardSerializationPolicy(
      Map<Class<?>, Boolean> serializationWhitelist,
      Map<Class<?>, Boolean> deserializationWhitelist,
      Map<Class<?>, String> obfuscatedTypeIds,
      Map<Class<?>, Set<String>> clientFields) {
    if (serializationWhitelist == null || deserializationWhitelist == null) {
      throw new NullPointerException("whitelist");
    }

    this.serializationWhitelist = serializationWhitelist;
    this.deserializationWhitelist = deserializationWhitelist;
    this.typeIds = obfuscatedTypeIds;
    this.clientFields = clientFields;

    for (Map.Entry<Class<?>, String> entry : obfuscatedTypeIds.entrySet()) {
      assert entry.getKey() != null : "null key";
      assert entry.getValue() != null : "null value for "
          + entry.getKey().getName();
      assert !typeIdsToClasses.containsKey(entry.getValue()) : "Duplicate type id "
          + entry.getValue();
      typeIdsToClasses.put(entry.getValue(), entry.getKey());
    }
  }

  public final String getClassNameForTypeId(String id)
      throws SerializationException {
    Class<?> clazz = typeIdsToClasses.get(id);
    if (clazz == null) {
      return null;
    }

    return clazz.getName();
  }

  @Override
  public Set<String> getClientFieldNamesForEnhancedClass(Class<?> clazz) {
    if (clientFields == null) {
      return null;
    }
    Set<String> fieldNames = clientFields.get(clazz);
    return fieldNames == null ? null : Collections.unmodifiableSet(fieldNames);
  }

  public final String getTypeIdForClass(Class<?> clazz)
      throws SerializationException {
    return typeIds.get(clazz);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.google.gwt.user.server.rpc.SerializationPolicy#shouldDerializeFields
   * (java.lang.String)
   */
  @Override
  public boolean shouldDeserializeFields(Class<?> clazz) {
    return isFieldSerializable(clazz, deserializationWhitelist);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.google.gwt.user.server.rpc.SerializationPolicy#shouldSerializeFields
   * (java.lang.String)
   */
  @Override
  public boolean shouldSerializeFields(Class<?> clazz) {
    return isFieldSerializable(clazz, serializationWhitelist);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.google.gwt.user.server.rpc.SerializationPolicy#validateDeserialize(
   * java.lang.String)
   */
  @Override
  public void validateDeserialize(Class<?> clazz) throws SerializationException {
    if (!isInstantiable(clazz, deserializationWhitelist)) {
      throw new SerializationException(
          "Type '"
              + clazz.getName()
              + "' was not included in the set of types which can be deserialized by this SerializationPolicy or its Class object could not be loaded. For security purposes, this type will not be deserialized.");
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.google.gwt.user.server.rpc.SerializationPolicy#validateSerialize(java
   * .lang.String)
   */
  @Override
  public void validateSerialize(Class<?> clazz) throws SerializationException {
    if (!isInstantiable(clazz, serializationWhitelist)) {
      throw new SerializationException(
          "Type '"
              + clazz.getName()
              + "' was not included in the set of types which can be serialized by this SerializationPolicy or its Class object could not be loaded. For security purposes, this type will not be serialized.");
    }
  }
}
