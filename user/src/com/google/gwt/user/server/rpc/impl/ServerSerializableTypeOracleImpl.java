/*
 * Copyright 2006 Google Inc.
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
 * This class defines an implementation of a serializable type oracle that can
 * be used by the rpc servlet.
 */
public class ServerSerializableTypeOracleImpl implements
    ServerSerializableTypeOracle {

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

  private static boolean containsCachedSerializerForClass(Class instanceType) {
    synchronized (classCustomSerializerCache) {
      return classCustomSerializerCache.containsKey(instanceType);
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

  private String[] packagePaths;

  public ServerSerializableTypeOracleImpl(String[] packagePaths) {
    this.packagePaths = packagePaths;
  }

  public Field[] applyFieldSerializationPolicy(Field[] fields) {
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

  public SerializedInstanceReference decodeSerializedInstanceReference(
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

  public String encodeSerializedInstanceReference(Class instanceType) {
    return instanceType.getName()
        + SerializedInstanceReference.SERIALIZED_REFERENCE_SEPARATOR
        + getSerializationSignature(instanceType);
  }

  public String getSerializationSignature(Class instanceType) {
    String result = getCachedCRCForClass(instanceType);
    if (result == null) {
      CRC32 crc = new CRC32();
      generateSerializationSignature(instanceType, crc);
      result = Long.toString(crc.getValue());
      putCachedCRCForClass(instanceType, result);
    }
    return result;
  }

  public String getSerializedTypeName(Class instanceType) {
    if (instanceType.isPrimitive()) {
      return (String) SERIALIZED_PRIMITIVE_TYPE_NAMES.get(instanceType.getName());
    }

    return instanceType.getName();
  }

  /**
   * This method treats arrays in a special way.
   */
  public Class hasCustomFieldSerializer(Class instanceType) {
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

  public boolean isSerializable(Class instanceType) {
    if (instanceType.isArray()) {
      return isSerializable(instanceType.getComponentType());
    }
    if (instanceType.isPrimitive()) {
      return true;
    }
    if (IsSerializable.class.isAssignableFrom(instanceType)) {
      return true;
    }
    return hasCustomFieldSerializer(instanceType) != null;
  }

  /**
   * This method treats arrays in a special way.
   */
  private Class computeHasCustomFieldSerializer(Class instanceType) {
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

    Class customSerializer = getCustomFieldSerializer(qualifiedTypeName
        + "_CustomFieldSerializer");
    if (customSerializer != null) {
      return customSerializer;
    }

    // Try with the regular name
    String simpleSerializerName = qualifiedTypeName + "_CustomFieldSerializer";
    for (int i = 0; i < packagePaths.length; ++i) {
      Class customSerializerClass = getCustomFieldSerializer(packagePaths[i]
          + "." + simpleSerializerName);
      if (customSerializerClass != null) {
        return customSerializerClass;
      }
    }

    return null;
  }

  private boolean excludeImplementationFromSerializationSignature(
      Class instanceType) {
    if (TYPES_WHOSE_IMPLEMENTATION_IS_EXCLUDED_FROM_SIGNATURES.contains(instanceType)) {
      return true;
    }
    return false;
  }

  private void generateSerializationSignature(Class instanceType, CRC32 crc) {
    crc.update(getSerializedTypeName(instanceType).getBytes());

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

        crc.update(field.getName().getBytes());
        crc.update(getSerializedTypeName(field.getType()).getBytes());
      }

      Class superClass = instanceType.getSuperclass();
      if (superClass != null) {
        generateSerializationSignature(superClass, crc);
      }
    }
  }

  private Class getCustomFieldSerializer(String qualifiedSerialzierName) {
    Class customSerializerClass;
    try {
      customSerializerClass = Class.forName(qualifiedSerialzierName, false,
          this.getClass().getClassLoader());
      return customSerializerClass;
    } catch (ClassNotFoundException e) {
      return null;
    }
  }

  private String[] getPackagePaths() {
    return packagePaths;
  }
}
