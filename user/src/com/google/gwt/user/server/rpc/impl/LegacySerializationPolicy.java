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

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.server.rpc.SerializationPolicy;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * A serialization policy compatible with GWT 1.3.3 RPC. This is used when no
 * serialization policy file is present.
 * 
 * <p>
 * The set of allowed types are:
 * </p>
 * <ol>
 * <li>Primitives</li>
 * <li>Types assignable to {@link IsSerializable}</li>
 * <li>Types with custom field serializers</li>
 * <li>Arrays of the above types</li>
 * </ol>
 * <p>
 * Types that derive from {@link Serializable} but do not meet any of the above
 * criteria may not be serialized as leaf types. However, their fields may be
 * serialized as super types of a legal type.
 * </p>
 */
public class LegacySerializationPolicy extends SerializationPolicy implements
    TypeNameObfuscator {

  private static final String ELISION_ERROR = "Type name elision in RPC "
      + "payloads is only supported if the RPC whitelist file is used.";

  /**
   * Many JRE types would appear to be {@link Serializable} on the server.
   * However, clients would not see these types as being {@link Serializable}
   * due to mismatches between the GWT JRE emulation and the real JRE. As a
   * workaround, this blacklist specifies a list of problematic types which
   * should be seen as not implementing {@link Serializable} for the purpose
   * matching the client's expectations. Note that a type on this list may still
   * be serializable via a custom serializer.
   */
  private static final Class<?>[] JRE_BLACKLIST = {
      java.lang.ArrayStoreException.class, java.lang.AssertionError.class,
      java.lang.Boolean.class, java.lang.Byte.class, java.lang.Character.class,
      java.lang.Class.class, java.lang.ClassCastException.class,
      java.lang.Double.class, java.lang.Error.class, java.lang.Float.class,
      java.lang.IllegalArgumentException.class,
      java.lang.IllegalStateException.class,
      java.lang.IndexOutOfBoundsException.class, java.lang.Integer.class,
      java.lang.Long.class, java.lang.NegativeArraySizeException.class,
      java.lang.NullPointerException.class, java.lang.Number.class,
      java.lang.NumberFormatException.class, java.lang.Short.class,
      java.lang.StackTraceElement.class, java.lang.String.class,
      java.lang.StringBuffer.class,
      java.lang.StringIndexOutOfBoundsException.class,
      java.lang.UnsupportedOperationException.class, java.util.ArrayList.class,
      java.util.ConcurrentModificationException.class, java.util.Date.class,
      java.util.EmptyStackException.class, java.util.EventObject.class,
      java.util.HashMap.class, java.util.HashSet.class,
      java.util.MissingResourceException.class,
      java.util.NoSuchElementException.class, java.util.Stack.class,
      java.util.TooManyListenersException.class, java.util.Vector.class};

  private static final Set<Class<?>> JRE_BLACKSET = new HashSet<Class<?>>(
      Arrays.asList(JRE_BLACKLIST));

  private static final LegacySerializationPolicy sInstance = new LegacySerializationPolicy();

  public static LegacySerializationPolicy getInstance() {
    return sInstance;
  }

  /**
   * Singleton.
   */
  private LegacySerializationPolicy() {
  }

  /**
   * Implemented to fail with a useful error message.
   */
  public final String getClassNameForTypeId(String id)
      throws SerializationException {
    throw new SerializationException(ELISION_ERROR);
  }
  
  /**
   * Implemented to fail with a useful error message.
   */
  public final String getTypeIdForClass(Class<?> clazz)
      throws SerializationException {
    throw new SerializationException(ELISION_ERROR);
  }

  @Override
  public boolean shouldDeserializeFields(Class<?> clazz) {
    return isFieldSerializable(clazz);
  }

  @Override
  public boolean shouldSerializeFields(Class<?> clazz) {
    return isFieldSerializable(clazz);
  }

  @Override
  public void validateDeserialize(Class<?> clazz) throws SerializationException {
    if (!isInstantiable(clazz)) {
      throw new SerializationException("Type '" + clazz.getName()
          + "' was not assignable to '" + IsSerializable.class.getName()
          + "' and did not have a custom field serializer. "
          + "For security purposes, this type will not be deserialized.");
    }
  }

  @Override
  public void validateSerialize(Class<?> clazz) throws SerializationException {
    if (!isInstantiable(clazz)) {
      throw new SerializationException("Type '" + clazz.getName()
          + "' was not assignable to '" + IsSerializable.class.getName()
          + "' and did not have a custom field serializer."
          + "For security purposes, this type will not be serialized.");
    }
  }

  /**
   * Field serializable types are primitives, {@line IsSerializable},
   * {@link Serializable}, types with custom serializers, and any arrays of
   * those types.
   */
  private boolean isFieldSerializable(Class<?> clazz) {
    if (isInstantiable(clazz)) {
      return true;
    }
    if (Serializable.class.isAssignableFrom(clazz)) {
      return !JRE_BLACKSET.contains(clazz);
    }
    return false;
  }

  /**
   * Instantiable types are primitives, {@line IsSerializable}, types with
   * custom serializers, and any arrays of those types. Merely
   * {@link Serializable} types cannot be instantiated or serialized directly
   * (only as super types of legacy serializable types).
   */
  private boolean isInstantiable(Class<?> clazz) {
    if (clazz.isPrimitive()) {
      return true;
    }
    if (clazz.isArray()) {
      return isInstantiable(clazz.getComponentType());
    }
    if (IsSerializable.class.isAssignableFrom(clazz)) {
      return true;
    }
    return SerializabilityUtil.hasCustomFieldSerializer(clazz) != null;
  }
}
