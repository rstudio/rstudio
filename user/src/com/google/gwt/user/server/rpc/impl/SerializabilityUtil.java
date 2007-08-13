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
package com.google.gwt.user.server.rpc.impl;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.CRC32;

/**
 * Serialization utility class used by the server-side RPC code.
 */
public class SerializabilityUtil {

  public static final String DEFAULT_ENCODING = "UTF-8";
  
  /**
   * A permanent cache of all computed CRCs on classes. This is safe to do
   * because a Class is guaranteed not to change within the lifetime of a
   * ClassLoader (and thus, this Map). Access must be synchronized.
   */
  private static final Map classCRC32Cache = new HashMap();

  /**
   * A permanent cache of all which classes onto custom field serializers. This
   * is safe to do because a Class is guaranteed not to change within the
   * lifetime of a ClassLoader (and thus, this Map). Access must be
   * synchronized.
   */
  private static final Map classCustomSerializerCache = new HashMap();

  private static final String JRE_SERIALIZER_PACKAGE = "com.google.gwt.user.client.rpc.core";

  private static final Map SERIALIZED_PRIMITIVE_TYPE_NAMES = new HashMap();

  private static final Set TYPES_WHOSE_IMPLEMENTATION_IS_EXCLUDED_FROM_SIGNATURES = new HashSet();

  static {

    SERIALIZED_PRIMITIVE_TYPE_NAMES.put(boolean.class.getName(), "Z");
    SERIALIZED_PRIMITIVE_TYPE_NAMES.put(byte.class.getName(), "B");
    SERIALIZED_PRIMITIVE_TYPE_NAMES.put(char.class.getName(), "C");
    SERIALIZED_PRIMITIVE_TYPE_NAMES.put(double.class.getName(), "D");
    SERIALIZED_PRIMITIVE_TYPE_NAMES.put(float.class.getName(), "F");
    SERIALIZED_PRIMITIVE_TYPE_NAMES.put(int.class.getName(), "I");
    SERIALIZED_PRIMITIVE_TYPE_NAMES.put(long.class.getName(), "J");
    SERIALIZED_PRIMITIVE_TYPE_NAMES.put(short.class.getName(), "S");

    TYPES_WHOSE_IMPLEMENTATION_IS_EXCLUDED_FROM_SIGNATURES.add(Boolean.class);
    TYPES_WHOSE_IMPLEMENTATION_IS_EXCLUDED_FROM_SIGNATURES.add(Byte.class);
    TYPES_WHOSE_IMPLEMENTATION_IS_EXCLUDED_FROM_SIGNATURES.add(Character.class);
    TYPES_WHOSE_IMPLEMENTATION_IS_EXCLUDED_FROM_SIGNATURES.add(Double.class);
    TYPES_WHOSE_IMPLEMENTATION_IS_EXCLUDED_FROM_SIGNATURES.add(Exception.class);
    TYPES_WHOSE_IMPLEMENTATION_IS_EXCLUDED_FROM_SIGNATURES.add(Float.class);
    TYPES_WHOSE_IMPLEMENTATION_IS_EXCLUDED_FROM_SIGNATURES.add(Integer.class);
    TYPES_WHOSE_IMPLEMENTATION_IS_EXCLUDED_FROM_SIGNATURES.add(Long.class);
    TYPES_WHOSE_IMPLEMENTATION_IS_EXCLUDED_FROM_SIGNATURES.add(Object.class);
    TYPES_WHOSE_IMPLEMENTATION_IS_EXCLUDED_FROM_SIGNATURES.add(Short.class);
    TYPES_WHOSE_IMPLEMENTATION_IS_EXCLUDED_FROM_SIGNATURES.add(String.class);
    TYPES_WHOSE_IMPLEMENTATION_IS_EXCLUDED_FROM_SIGNATURES.add(Throwable.class);
  }

  public static Field[] applyFieldSerializationPolicy(Field[] fields) {
    ArrayList fieldList = new ArrayList();
    for (int index = 0; index < fields.length; ++index) {
      Field field = fields[index];
      assert (field != null);

      int fieldModifiers = field.getModifiers();
      if (Modifier.isStatic(fieldModifiers)
          || Modifier.isTransient(fieldModifiers)
          || Modifier.isFinal(fieldModifiers)) {
        continue;
      }

      fieldList.add(field);
    }

    Field[] fieldSubset = (Field[]) fieldList.toArray(new Field[fieldList.size()]);

    // sort the fields by name
    Comparator comparator = new Comparator() {
      public int compare(Object o1, Object o2) {
        Field f1 = (Field) o1;
        Field f2 = (Field) o2;

        return f1.getName().compareTo(f2.getName());
      }
    };
    Arrays.sort(fieldSubset, 0, fieldSubset.length, comparator);

    return fieldSubset;
  }

  public static SerializedInstanceReference decodeSerializedInstanceReference(
      String encodedSerializedInstanceReference) {
    final String[] components = encodedSerializedInstanceReference.split(SerializedInstanceReference.SERIALIZED_REFERENCE_SEPARATOR);
    return new SerializedInstanceReference() {
      public String getName() {
        return components.length > 0 ? components[0] : "";
      }

      public String getSignature() {
        return components.length > 1 ? components[1] : "";
      }
    };
  }

  public static String encodeSerializedInstanceReference(Class instanceType) {
    return instanceType.getName()
        + SerializedInstanceReference.SERIALIZED_REFERENCE_SEPARATOR
        + getSerializationSignature(instanceType);
  }

  public static String getSerializationSignature(Class instanceType) {
    String result = getCachedCRCForClass(instanceType);
    if (result == null) {
      CRC32 crc = new CRC32();
      try {
        generateSerializationSignature(instanceType, crc);
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(
            "Could not compute the serialization signature", e);
      }
      result = Long.toString(crc.getValue());
      putCachedCRCForClass(instanceType, result);
    }
    return result;
  }

  public static String getSerializedTypeName(Class instanceType) {
    if (instanceType.isPrimitive()) {
      return (String) SERIALIZED_PRIMITIVE_TYPE_NAMES.get(instanceType.getName());
    }

    return instanceType.getName();
  }

  /**
   * This method treats arrays in a special way.
   */
  public static Class hasCustomFieldSerializer(Class instanceType) {
    assert (instanceType != null);
    Class result = getCachedSerializerForClass(instanceType);
    if (result != null) {
      // this class has a custom serializer
      return result;
    }
    if (containsCachedSerializerForClass(instanceType)) {
      // this class definitely has no custom serializer
      return null;
    }
    // compute whether this class has a custom serializer
    result = computeHasCustomFieldSerializer(instanceType);
    putCachedSerializerForClass(instanceType, result);
    return result;
  }

  /**
   * This method treats arrays in a special way.
   */
  private static Class computeHasCustomFieldSerializer(Class instanceType) {
    assert (instanceType != null);

    String qualifiedTypeName;

    if (instanceType.isArray()) {
      Class componentType = instanceType.getComponentType();

      if (componentType.isPrimitive()) {
        qualifiedTypeName = "java.lang." + componentType.getName();
      } else {
        qualifiedTypeName = Object.class.getName();
      }

      qualifiedTypeName += "_Array";

    } else {
      qualifiedTypeName = instanceType.getName();
    }

    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    String simpleSerializerName = qualifiedTypeName + "_CustomFieldSerializer";
    Class customSerializer = getCustomFieldSerializer(classLoader,
        simpleSerializerName);
    if (customSerializer != null) {
      return customSerializer;
    }

    // Try with the regular name
    Class customSerializerClass = getCustomFieldSerializer(classLoader,
        JRE_SERIALIZER_PACKAGE + "." + simpleSerializerName);
    if (customSerializerClass != null) {
      return customSerializerClass;
    }

    return null;
  }

  private static boolean containsCachedSerializerForClass(Class instanceType) {
    synchronized (classCustomSerializerCache) {
      return classCustomSerializerCache.containsKey(instanceType);
    }
  }

  private static boolean excludeImplementationFromSerializationSignature(
      Class instanceType) {
    if (TYPES_WHOSE_IMPLEMENTATION_IS_EXCLUDED_FROM_SIGNATURES.contains(instanceType)) {
      return true;
    }
    return false;
  }

  private static void generateSerializationSignature(Class instanceType,
      CRC32 crc) throws UnsupportedEncodingException {
    crc.update(getSerializedTypeName(instanceType).getBytes(
        DEFAULT_ENCODING));

    if (excludeImplementationFromSerializationSignature(instanceType)) {
      return;
    }

    Class customSerializer = hasCustomFieldSerializer(instanceType);
    if (customSerializer != null) {
      generateSerializationSignature(customSerializer, crc);
    } else if (instanceType.isArray()) {
      generateSerializationSignature(instanceType.getComponentType(), crc);
    } else if (!instanceType.isPrimitive()) {
      Field[] fields = applyFieldSerializationPolicy(instanceType.getDeclaredFields());
      for (int i = 0; i < fields.length; ++i) {
        Field field = fields[i];
        assert (field != null);

        crc.update(field.getName().getBytes(DEFAULT_ENCODING));
        crc.update(getSerializedTypeName(field.getType()).getBytes(
            DEFAULT_ENCODING));
      }

      Class superClass = instanceType.getSuperclass();
      if (superClass != null) {
        generateSerializationSignature(superClass, crc);
      }
    }
  }

  private static String getCachedCRCForClass(Class instanceType) {
    synchronized (classCRC32Cache) {
      return (String) classCRC32Cache.get(instanceType);
    }
  }

  private static Class getCachedSerializerForClass(Class instanceType) {
    synchronized (classCustomSerializerCache) {
      return (Class) classCustomSerializerCache.get(instanceType);
    }
  }

  private static Class getCustomFieldSerializer(ClassLoader classLoader,
      String qualifiedSerialzierName) {
    try {
      Class customSerializerClass = Class.forName(qualifiedSerialzierName,
          false, classLoader);
      return customSerializerClass;
    } catch (ClassNotFoundException e) {
      return null;
    }
  }

  private static void putCachedCRCForClass(Class instanceType, String crc32) {
    synchronized (classCRC32Cache) {
      classCRC32Cache.put(instanceType, crc32);
    }
  }

  private static void putCachedSerializerForClass(Class instanceType,
      Class customFieldSerializer) {
    synchronized (classCustomSerializerCache) {
      classCustomSerializerCache.put(instanceType, customFieldSerializer);
    }
  }
}
