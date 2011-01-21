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

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.google.gwt.user.client.rpc.GwtTransient;
import com.google.gwt.user.server.rpc.SerializationPolicy;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.CRC32;

/**
 * Serialization utility class used by the server-side RPC code.
 */
public class SerializabilityUtil {

  public static final String DEFAULT_ENCODING = "UTF-8";

  /**
   * Comparator used to sort fields.
   */
  public static final Comparator<Field> FIELD_COMPARATOR = new Comparator<Field>() {
    public int compare(Field f1, Field f2) {
      return f1.getName().compareTo(f2.getName());
    }
  };

  /**
   * A permanent cache of all computed CRCs on classes. This is safe to do
   * because a Class is guaranteed not to change within the lifetime of a
   * ClassLoader (and thus, this Map). Access must be synchronized.
   * 
   * NOTE: after synchronizing on this field, it's possible to additionally
   * synchronize on {@link #classCustomSerializerCache} or
   * {@link #classSerializableFieldsCache}, so be aware deadlock potential when
   * changing this code.
   */
  private static final Map<Class<?>, String> classCRC32Cache = new IdentityHashMap<Class<?>, String>();

  /**
   * A permanent cache of all serializable fields on classes. This is safe to do
   * because a Class is guaranteed not to change within the lifetime of a
   * ClassLoader (and thus, this Map). Access must be synchronized.
   * 
   * NOTE: to prevent deadlock, you may NOT synchronize {@link #classCRC32Cache}
   * after synchronizing on this field.
   */
  private static final Map<Class<?>, Field[]> classSerializableFieldsCache = new IdentityHashMap<Class<?>, Field[]>();

  /**
   * A permanent cache of all which classes onto custom field serializers. This
   * is safe to do because a Class is guaranteed not to change within the
   * lifetime of a ClassLoader (and thus, this Map). Access must be
   * synchronized.
   * 
   * NOTE: to prevent deadlock, you may NOT synchronize {@link #classCRC32Cache}
   * after synchronizing on this field.
   */
  private static final Map<Class<?>, Class<?>> classCustomSerializerCache = new IdentityHashMap<Class<?>, Class<?>>();

  /**
   * Map of {@link Class} objects to singleton instances of that
   * {@link CustomFieldSerializer}.
   */
  private static final Map<Class<?>, CustomFieldSerializer<?>>
      CLASS_TO_SERIALIZER_INSTANCE =
      new IdentityHashMap<Class<?>, CustomFieldSerializer<?>>();

  private static final String JRE_SERIALIZER_PACKAGE = "com.google.gwt.user.client.rpc.core";

  /**
   * A re-usable, non-functional {@link CustomFieldSerializer} for when the
   * Custom Field Serializer does not implement the
   * {@link CustomFieldSerializer} interface.
   */
  private static final CustomFieldSerializer NO_SUCH_SERIALIZER =
      new CustomFieldSerializer<Object>() {
        @Override
        public void deserializeInstance(SerializationStreamReader
            streamReader, Object instance) {
          throw new AssertionError("This should never be called.");
        }

        @Override
        public void serializeInstance(SerializationStreamWriter streamWriter,
            Object instance) {
          throw new AssertionError("This should never be called.");
        }
      };

  private static final Map<String, String> SERIALIZED_PRIMITIVE_TYPE_NAMES = new HashMap<String, String>();

  private static final Set<Class<?>> TYPES_WHOSE_IMPLEMENTATION_IS_EXCLUDED_FROM_SIGNATURES = new HashSet<Class<?>>();

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

    try {
      /*
       * Work around for incompatible type hierarchy (and therefore signature)
       * between JUnit3 and JUnit4. Do this via reflection so we don't force the
       * server to depend on JUnit.
       */
      Class<?> clazz = Class.forName("junit.framework.AssertionFailedError");
      TYPES_WHOSE_IMPLEMENTATION_IS_EXCLUDED_FROM_SIGNATURES.add(clazz);
    } catch (ClassNotFoundException dontCare) {
    }
  }

  /**
   * Returns the fields of a particular class that can be considered for
   * serialization. The returned list will be sorted into a canonical order to
   * ensure consistent answers.
   * 
   * TODO: this method needs a better name, I think.
   */
  public static Field[] applyFieldSerializationPolicy(Class<?> clazz) {
    Field[] serializableFields;
    synchronized (classSerializableFieldsCache) {
      serializableFields = classSerializableFieldsCache.get(clazz);
      if (serializableFields == null) {
        ArrayList<Field> fieldList = new ArrayList<Field>();
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
          if (fieldQualifiesForSerialization(field)) {
            fieldList.add(field);
          }
        }
        serializableFields = fieldList.toArray(new Field[fieldList.size()]);

        // sort the fields by name
        Arrays.sort(serializableFields, 0, serializableFields.length,
            FIELD_COMPARATOR);

        classSerializableFieldsCache.put(clazz, serializableFields);
      }
    }
    return serializableFields;
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

  public static String encodeSerializedInstanceReference(Class<?> instanceType, SerializationPolicy policy) {
    return instanceType.getName()
        + SerializedInstanceReference.SERIALIZED_REFERENCE_SEPARATOR
        + getSerializationSignature(instanceType, policy);
  }
  
  public static String getSerializationSignature(Class<?> instanceType,
      SerializationPolicy policy) {
    String result;
    synchronized (classCRC32Cache) {
      result = classCRC32Cache.get(instanceType);
      if (result == null) {
        CRC32 crc = new CRC32();
        try {
          generateSerializationSignature(instanceType, crc, policy);
        } catch (UnsupportedEncodingException e) {
          throw new RuntimeException(
              "Could not compute the serialization signature", e);
        }
        result = Long.toString(crc.getValue());
        classCRC32Cache.put(instanceType, result);
      }
    }
    return result;
  }

  public static String getSerializedTypeName(Class<?> instanceType) {
    if (instanceType.isPrimitive()) {
      return SERIALIZED_PRIMITIVE_TYPE_NAMES.get(instanceType.getName());
    }

    return instanceType.getName();
  }

  /**
   * Returns the {@link Class} which can serialize the given instance type, or
   * <code>null</code> if this class has no custom field serializer. Note that
   * arrays never have custom field serializers.
   */
  public static Class<?> hasCustomFieldSerializer(Class<?> instanceType) {
    assert (instanceType != null);
    if (instanceType.isArray()) {
      return null;
    }

    Class<?> result;
    synchronized (classCustomSerializerCache) {
      result = classCustomSerializerCache.get(instanceType);
      if (result == null) {
        result = computeHasCustomFieldSerializer(instanceType);
        if (result == null) {
          /*
           * Use (result == instanceType) as a sentinel value when the class has
           * no custom field serializer. We avoid using null as the sentinel
           * value, because that would necessitate an additional containsKey()
           * check in the most common case, inside the synchronized block.
           */
          result = instanceType;
        }
        classCustomSerializerCache.put(instanceType, result);
      }
    }
    return (result == instanceType) ? null : result;
  }

  static boolean isNotStaticTransientOrFinal(Field field) {
    /*
     * Only serialize fields that are not static, transient (including @GwtTransient), or final.
     */
    int fieldModifiers = field.getModifiers();
    return !Modifier.isStatic(fieldModifiers)
        && !Modifier.isTransient(fieldModifiers)
        && !field.isAnnotationPresent(GwtTransient.class)
        && !Modifier.isFinal(fieldModifiers);
  }

  /**
   * Loads a {@link CustomFieldSerializer} from a class that may implement that
   * interface.
   *
   * @param customSerializerClass the Custom Field Serializer class
   *
   * @return an instance the class provided if it implements
   *         {@link CustomFieldSerializer} or {@code null} if it does not
   *
   * @throws SerializationException if the load process encounters an
   *         unexpected problem
   */
  static CustomFieldSerializer<?> loadCustomFieldSerializer(
      final Class<?> customSerializerClass) throws SerializationException {
    /**
     * Note that neither reading or writing to the CLASS_TO_SERIALIZER_INSTANCE
     * is synchronized for performance reasons.  This could cause get misses,
     * put misses and the same CustomFieldSerializer to be instantiated more
     * than once, but none of these are critical operations as
     * CLASS_TO_SERIALIZER_INSTANCE is only a performance improving cache.
     */
    CustomFieldSerializer<?> customFieldSerializer =
        CLASS_TO_SERIALIZER_INSTANCE.get(customSerializerClass);
    if (customFieldSerializer == null) {
      if (CustomFieldSerializer.class.isAssignableFrom(customSerializerClass)) {
        try {
          customFieldSerializer =
              (CustomFieldSerializer) customSerializerClass.newInstance();
        } catch (InstantiationException e) {
          throw new SerializationException(e);

        } catch (IllegalAccessException e) {
          throw new SerializationException(e);
        }
      } else {
        customFieldSerializer = NO_SUCH_SERIALIZER;
      }
      CLASS_TO_SERIALIZER_INSTANCE.put(customSerializerClass,
          customFieldSerializer);
    }
    if (customFieldSerializer == NO_SUCH_SERIALIZER) {
      return null;
    } else {
      return customFieldSerializer;
    }
  }

  /**
   * This method treats arrays in a special way.
   */
  private static Class<?> computeHasCustomFieldSerializer(Class<?> instanceType) {
    assert (instanceType != null);
    String qualifiedTypeName = instanceType.getName();
    /*
     * This class is called from client code running in Development Mode as well
     * as server code running in the servlet container. In Development Mode, we
     * want to load classes through the
     * CompilingClassLoader$MultiParentClassLoader, not the system classloader.
     */
    ClassLoader classLoader = GWT.isClient()
        ? SerializabilityUtil.class.getClassLoader()
        : Thread.currentThread().getContextClassLoader();
    String simpleSerializerName = qualifiedTypeName + "_CustomFieldSerializer";
    Class<?> customSerializer = getCustomFieldSerializer(classLoader,
        simpleSerializerName);
    if (customSerializer != null) {
      return customSerializer;
    }

    // Try with the regular name
    Class<?> customSerializerClass = getCustomFieldSerializer(classLoader,
        JRE_SERIALIZER_PACKAGE + "." + simpleSerializerName);
    if (customSerializerClass != null) {
      return customSerializerClass;
    }

    return null;
  }

  private static boolean excludeImplementationFromSerializationSignature(
      Class<?> instanceType) {
    if (TYPES_WHOSE_IMPLEMENTATION_IS_EXCLUDED_FROM_SIGNATURES.contains(instanceType)) {
      return true;
    }
    return false;
  }

  private static boolean fieldQualifiesForSerialization(Field field) {
    if (Throwable.class == field.getDeclaringClass()) {
      /**
       * Only serialize Throwable's detailMessage field; all others are ignored.
       * 
       * NOTE: Changing the set of fields that we serialize for Throwable will
       * necessitate a change to our JRE emulation's version of Throwable.
       */
      if ("detailMessage".equals(field.getName())) {
        assert (isNotStaticTransientOrFinal(field));
        return true;
      } else {
        return false;
      }
    } else {
      return isNotStaticTransientOrFinal(field);
    }
  }

  private static void generateSerializationSignature(Class<?> instanceType,
      CRC32 crc, SerializationPolicy policy) throws UnsupportedEncodingException {
    crc.update(getSerializedTypeName(instanceType).getBytes(DEFAULT_ENCODING));

    if (excludeImplementationFromSerializationSignature(instanceType)) {
      return;
    }

    Class<?> customSerializer = hasCustomFieldSerializer(instanceType);
    if (customSerializer != null) {
      generateSerializationSignature(customSerializer, crc, policy);
    } else if (instanceType.isArray()) {
      generateSerializationSignature(instanceType.getComponentType(), crc, policy);
    } else if (!instanceType.isPrimitive()) {
      Field[] fields = applyFieldSerializationPolicy(instanceType);
      Set<String> clientFieldNames = policy.getClientFieldNamesForEnhancedClass(instanceType);
      for (Field field : fields) {
        assert (field != null);
        /**
         * If clientFieldNames is non-null, use only the fields listed there
         * to generate the signature.  Otherwise, use all known fields.
         */
        if ((clientFieldNames == null) || clientFieldNames.contains(field.getName())) {
          crc.update(field.getName().getBytes(DEFAULT_ENCODING));
          crc.update(getSerializedTypeName(field.getType()).getBytes(
              DEFAULT_ENCODING));
        }
      }

      Class<?> superClass = instanceType.getSuperclass();
      if (superClass != null) {
        generateSerializationSignature(superClass, crc, policy);
      }
    }
  }

  private static Class<?> getCustomFieldSerializer(ClassLoader classLoader,
      String qualifiedSerialzierName) {
    try {
      Class<?> customSerializerClass = Class.forName(qualifiedSerialzierName,
          false, classLoader);
      return customSerializerClass;
    } catch (ClassNotFoundException e) {
      return null;
    }
  }
}
