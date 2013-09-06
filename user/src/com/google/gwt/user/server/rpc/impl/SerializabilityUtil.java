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

import com.google.gwt.core.shared.GWT;
import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.GwtTransient;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.google.gwt.user.server.rpc.RPCServletUtils;
import com.google.gwt.user.server.rpc.SerializationPolicy;
import com.google.gwt.user.server.rpc.ServerCustomFieldSerializer;

import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.CRC32;

/**
 * Serialization utility class used by the server-side RPC code.
 */
public class SerializabilityUtil {
  /**
   * Comparator used to sort fields.
   */
  public static final Comparator<Field> FIELD_COMPARATOR = new Comparator<Field>() {
    @Override
    public int compare(Field f1, Field f2) {
      return f1.getName().compareTo(f2.getName());
    }
  };

  /**
   * A permanent cache of all computed CRCs on classes. This is safe to do
   * because a Class is guaranteed not to change within the lifetime of a
   * ClassLoader (and thus, this Map).
   */
  private static final Map<Class<?>, String> classCRC32Cache =
      new ConcurrentHashMap<Class<?>, String>();

  /**
   * A permanent cache of all serializable fields on classes. This is safe to do
   * because a Class is guaranteed not to change within the lifetime of a
   * ClassLoader (and thus, this Map).
   */
  private static final Map<Class<?>, Field[]> classSerializableFieldsCache =
      new ConcurrentHashMap<Class<?>, Field[]>();

  /**
   * A permanent cache of all which classes onto custom field serializers. This
   * is safe to do because a Class is guaranteed not to change within the
   * lifetime of a ClassLoader (and thus, this Map).
   */
  private static final Map<Class<?>, Class<?>> classCustomSerializerCache =
      new ConcurrentHashMap<Class<?>, Class<?>>();

  /**
   * A permanent cache of all which classes onto server-side custom field
   * serializers. This is safe to do because a Class is guaranteed not to change
   * within the lifetime of a ClassLoader (and thus, this Map).
   */
  private static final Map<Class<?>, Class<?>> classServerCustomSerializerCache =
      new ConcurrentHashMap<Class<?>, Class<?>>();

  /**
   * Map of {@link Class} objects to singleton instances of that
   * {@link CustomFieldSerializer}.
   */
  private static final Map<Class<?>, CustomFieldSerializer<?>> CLASS_TO_SERIALIZER_INSTANCE =
      new ConcurrentHashMap<Class<?>, CustomFieldSerializer<?>>();

  private static final String JRE_SERVER_SERIALIZER_PACKAGE = "com.google.gwt.user.server.rpc.core";
  private static final String JRE_SERIALIZER_PACKAGE = "com.google.gwt.user.client.rpc.core";

  /**
   * A re-usable, non-functional {@link ServerCustomFieldSerializer} for when
   * the Server Custom Field Serializer does not implement the
   * {@link ServerCustomFieldSerializer} interface.
   */
  private static final ServerCustomFieldSerializer<?> NO_SUCH_SERIALIZER =
      new ServerCustomFieldSerializer<Object>() {
        @Override
        public void deserializeInstance(SerializationStreamReader streamReader, Object instance) {
          throw new AssertionError("This should never be called.");
        }

        @Override
        public void deserializeInstance(ServerSerializationStreamReader streamReader,
            Object instance, Type[] expectedParameterTypes,
            DequeMap<TypeVariable<?>, Type> resolvedTypes) throws SerializationException {
          throw new SerializationException("This should never be called.");
        }

        @Override
        public void serializeInstance(SerializationStreamWriter streamWriter, Object instance) {
          throw new AssertionError("This should never be called.");
        }
      };

  private static final Map<String, String> SERIALIZED_PRIMITIVE_TYPE_NAMES =
      new HashMap<String, String>();

  private static final Set<Class<?>> TYPES_WHOSE_IMPLEMENTATION_IS_EXCLUDED_FROM_SIGNATURES =
      new HashSet<Class<?>>();

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
      // Empty because we don't care
    }
  }

  /**
   * Returns the fields of a particular class that can be considered for
   * serialization. The returned list will be sorted into a canonical order to
   * ensure consistent answers.
   */
  public static Field[] applyFieldSerializationPolicy(Class<?> clazz) {
    Field[] serializableFields;
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
      Arrays.sort(serializableFields, 0, serializableFields.length, FIELD_COMPARATOR);

      classSerializableFieldsCache.put(clazz, serializableFields);
    }
    return serializableFields;
  }

  public static SerializedInstanceReference decodeSerializedInstanceReference(
      String encodedSerializedInstanceReference) {
    final String[] components =
        encodedSerializedInstanceReference
            .split(SerializedInstanceReference.SERIALIZED_REFERENCE_SEPARATOR);
    return new SerializedInstanceReference() {
      @Override
      public String getName() {
        return components.length > 0 ? components[0] : "";
      }

      @Override
      public String getSignature() {
        return components.length > 1 ? components[1] : "";
      }
    };
  }

  public static String encodeSerializedInstanceReference(Class<?> instanceType,
      SerializationPolicy policy) {
    return instanceType.getName() + SerializedInstanceReference.SERIALIZED_REFERENCE_SEPARATOR
        + getSerializationSignature(instanceType, policy);
  }

  /**
   * Resolve type variables to concrete types if possible. Otherwise, just return
   * the type variable.
   *
   * @param unresolved The type to resolve
   * @param resolvedTypes A map of generic types to actual types.
   * @return The actual type, which may be of any subclass of Type.
   */
  public static Type findActualType(Type unresolved,
      DequeMap<TypeVariable<?>, Type> resolvedTypes) {

    // Handle simple cases quickly.
    if (!(unresolved instanceof TypeVariable<?>)) {
      return unresolved;
    }
    TypeVariable<?> var = (TypeVariable<?>) unresolved;
    Type target = resolvedTypes.get(var);
    if (target == null || target == var) {
      return var;
    }
    if (!(target instanceof TypeVariable<?>)) {
      return target;
    }

    // Type variables that point to other type variables might form a cycle, which
    // means they're all equivalent. Keep track of visited type variables to detect this.
    Set<TypeVariable<?>> seen = new HashSet<TypeVariable<?>>();
    seen.add(var);
    var = (TypeVariable<?>) target;
    seen.add(var);

    while (true) {
      target = resolvedTypes.get(var);
      if (target == null || target == var) {
        return var;
      }
      if (!(target instanceof TypeVariable<?>)) {
        return target;
      }
      var = (TypeVariable<?>) target;
      if (!seen.add(var)) {
        // Cycle detected; returning an arbitrary var in the cycle.
        return var;
      }
    }
  }

  /**
   * Determine the expected types for any instance type parameters.
   *
   * This method also determines whether or not the instance can be assigned to
   * the expected type. We combine the tasks because they require traversing the
   * same data structures.
   *
   * @param instanceClass The instance for which we want generic parameter types
   * @param expectedType The type we are expecting this instance to be
   * @param resolvedTypes The types that have been resolved to actual values
   * @return The expected types of the instance class' parameters. If null, the
   *     instance class is not assignable to the expected type.
   */
  public static Type[] findExpectedParameterTypes(Class<?> instanceClass,
      Type expectedType, DequeMap<TypeVariable<?>, Type> resolvedTypes) {
    // Make a copy of the instance parameters from its class
    TypeVariable<?>[] instanceTypes = instanceClass.getTypeParameters();
    Type[] expectedParameterTypes = Arrays.copyOf(instanceTypes, instanceTypes.length,
        Type[].class);

    // Determine the type we are really expecting, to the best of our knowledge
    if (expectedType == null) {
      // Match up parameters assuming that the instance class is the best match
      // for the expected class, which we can only do if we have resolved types.
      if (resolvedTypes != null) {
        findInstanceParameters(instanceClass, resolvedTypes, expectedParameterTypes);
      }

      // With no expected type, the instance is assignable and we fall through
    } else {
      // First determine what type we are really expecting. The type may still
      // be a TypeVariable<?> at this time when we are deserializing class
      // fields or components of another data structure.
      Type actualType = findActualType(expectedType, resolvedTypes);

      // Try to match the instanceClass to the expected type, updating the
      // expectedParameterTypes so that we can track them back to the resolved
      // types once we determine what class to treat the instance as. In
      // this method, only type information from the instance is used to resolve
      // types because the information in the resolved types map is only
      // relevant to the expected type(s). We still pass in the resolvedTypes
      // because we may need to resolve expected types.
      // Note that certain expected types may require the instance to extend
      // or implement multiple classes or interfaces, so we may capture multiple
      // types here.
      Set<Class<?>> expectedInstanceClasses = new HashSet<Class<?>>();
      if (!findExpectedInstanceClass(instanceClass, actualType,
          resolvedTypes, expectedInstanceClasses, expectedParameterTypes)) {
        // If we could not match the instance to the expected, it is not assignable
        // and we are done.
        return null;
      }

      // Now that we know what class the instance should be,
      // get any remaining parameters using resolved types.
      if (resolvedTypes != null) {
        for (Class<?> expectedClass : expectedInstanceClasses) {
          findInstanceParameters(expectedClass, resolvedTypes, expectedParameterTypes);
        }
      }
    }

    return expectedParameterTypes;
  }

  /**
   * Find the Class that a given type refers to.
   *
   * @param type The type of interest
   * @return The Class that type represents
   */
  public static Class<?> getClassFromType(Type type,
      DequeMap<TypeVariable<?>, Type> resolvedTypes) {
    Type actualType = findActualType(type, resolvedTypes);
    if (actualType instanceof Class) {
      return (Class<?>) actualType;
    }
    if (type instanceof ParameterizedType) {
      return getClassFromType(((ParameterizedType) actualType).getRawType(), resolvedTypes);
    }
    return null;
  }

  public static String getSerializationSignature(Class<?> instanceType,
      SerializationPolicy policy) {
    String result;
    result = classCRC32Cache.get(instanceType);
    if (result == null) {
      CRC32 crc = new CRC32();
      try {
        generateSerializationSignature(instanceType, crc, policy);
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException("Could not compute the serialization signature", e);
      }
      result = Long.toString(crc.getValue());
      classCRC32Cache.put(instanceType, result);
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
   * <code>null</code> if this class has no custom field serializer.
   *
   * Note that arrays never have custom field serializers.
   */
  public static Class<?> hasCustomFieldSerializer(Class<?> instanceType) {
    assert (instanceType != null);
    if (instanceType.isArray()) {
      return null;
    }

    Class<?> result;
    result = classCustomSerializerCache.get(instanceType);
    if (result == null) {
      result = computeHasCustomFieldSerializer(instanceType, false);
      if (result == null) {
        /*
         * Use (result == instanceType) as a sentinel value when the class has
         * no custom field serializer. We avoid using null as the sentinel
         * value, because that would necessitate an additional containsKey()
         * check in the most common case.
         */
        result = instanceType;
      }
      classCustomSerializerCache.put(instanceType, result);
    }
    return (result == instanceType) ? null : result;
  }

  /**
   * Returns the server-side {@link Class} which can serialize the given
   * instance type, or <code>null</code> if this class has no type-checking
   * custom field serializer.
   *
   * Note that arrays never have custom field serializers.
   */
  public static Class<?> hasServerCustomFieldSerializer(Class<?> instanceType) {
    assert (instanceType != null);
    if (instanceType.isArray()) {
      return null;
    }

    Class<?> result;
    result = classServerCustomSerializerCache.get(instanceType);
    if (result == null) {
      result = computeHasCustomFieldSerializer(instanceType, true);
      if (result == null) {
        /*
         * Use (result == instanceType) as a sentinel value when the class has
         * no custom field serializer. We avoid using null as the sentinel
         * value, because that would necessitate an additional containsKey()
         * check in the most common case.
         */
        result = instanceType;
      }
      classServerCustomSerializerCache.put(instanceType, result);
    }
    return (result == instanceType) ? null : result;
  }

  /**
   * Remove all of the actual types that arose from the given type.
   *
   * This method should always be called after a corresponding call to
   * resolveTypes.
   *
   * @param methodType The type we wish to assign this instance to
   * @param resolvedTypes The types that have been resolved to actual values
   */
  public static void releaseTypes(Type methodType, DequeMap<TypeVariable<?>, Type> resolvedTypes) {
    SerializabilityUtil.resolveTypesWorker(methodType, resolvedTypes, false);
  }

  /**
   * Find all the actual types we can from the information in the given type,
   * and put the mapping from TypeVariable objects to actual types into the
   * resolved types map.
   *
   * The method releaseTypes should always be called after a call to this
   * method, unless the resolved types map is about to be discarded.
   *
   * @param methodType The type we wish to assign this instance to
   * @param resolvedTypes The types that have been resolved to actual values
   */
  public static void resolveTypes(Type methodType, DequeMap<TypeVariable<?>, Type> resolvedTypes) {
    SerializabilityUtil.resolveTypesWorker(methodType, resolvedTypes, true);
  }

  /**
   * Returns true if this field has an annotation named "GwtTransient".
   */
  static boolean hasGwtTransientAnnotation(Field field) {
    for (Annotation a : field.getAnnotations()) {
      if (a.annotationType().getSimpleName().equals(GwtTransient.class.getSimpleName())) {
        return true;
      }
    }
    return false;
  }

  static boolean isNotStaticTransientOrFinal(Field field) {
    /*
     * Only serialize fields that are not static, transient (including
     * @GwtTransient), or final.
     */
    int fieldModifiers = field.getModifiers();
    return !Modifier.isStatic(fieldModifiers) && !Modifier.isTransient(fieldModifiers)
        && !hasGwtTransientAnnotation(field) && !Modifier.isFinal(fieldModifiers);
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
   * @throws SerializationException if the load process encounters an unexpected
   *           problem
   */
  static CustomFieldSerializer<?> loadCustomFieldSerializer(final Class<?> customSerializerClass)
      throws SerializationException {
    CustomFieldSerializer<?> customFieldSerializer =
        CLASS_TO_SERIALIZER_INSTANCE.get(customSerializerClass);
    if (customFieldSerializer == null) {
      if (CustomFieldSerializer.class.isAssignableFrom(customSerializerClass)) {
        try {
          customFieldSerializer = (CustomFieldSerializer<?>) customSerializerClass.newInstance();
        } catch (InstantiationException e) {
          throw new SerializationException(e);

        } catch (IllegalAccessException e) {
          throw new SerializationException(e);
        }
      } else {
        customFieldSerializer = NO_SUCH_SERIALIZER;
      }
      CLASS_TO_SERIALIZER_INSTANCE.put(customSerializerClass, customFieldSerializer);
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
  private static Class<?> computeHasCustomFieldSerializer(Class<?> instanceType,
      Boolean typeChecked) {
    assert (instanceType != null);
    String qualifiedTypeName = instanceType.getName();
    /*
     * This class is called from client code running in Development Mode as well
     * as server code running in the servlet container. In Development Mode, we
     * want to load classes through the
     * CompilingClassLoader$MultiParentClassLoader, not the system classloader.
     */
    ClassLoader classLoader =
        GWT.isClient() ? SerializabilityUtil.class.getClassLoader() : Thread.currentThread()
            .getContextClassLoader();

    if (typeChecked) {
      /*
       * Look for a server-specific version of the custom field serializer.
       * Server-side versions do additional type checking before deserializing a
       * class, offering protection against certain malicious attacks on the
       * server via RPC.
       */
      String serverSerializerName = qualifiedTypeName + "_ServerCustomFieldSerializer";
      serverSerializerName = serverSerializerName.replaceFirst("client", "server");
      Class<?> serverCustomSerializer = getCustomFieldSerializer(classLoader, serverSerializerName);
      if (serverCustomSerializer != null) {
        return serverCustomSerializer;
      }

      // Try with the regular name
      serverCustomSerializer =
          getCustomFieldSerializer(classLoader, JRE_SERVER_SERIALIZER_PACKAGE + "."
              + serverSerializerName);
      if (serverCustomSerializer != null) {
        return serverCustomSerializer;
      }
    }

    // Look for client-side serializers.
    String simpleSerializerName = qualifiedTypeName + "_CustomFieldSerializer";
    Class<?> customSerializer = getCustomFieldSerializer(classLoader, simpleSerializerName);
    if (customSerializer != null) {
      return customSerializer;
    }

    // Try with the regular name
    customSerializer =
        getCustomFieldSerializer(classLoader, JRE_SERIALIZER_PACKAGE + "." + simpleSerializerName);
    if (customSerializer != null) {
      return customSerializer;
    }

    return null;
  }

  private static boolean excludeImplementationFromSerializationSignature(Class<?> instanceType) {
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

  private static boolean findExpectedInstanceClass(Class<?> instanceClass,
      Type expectedType, DequeMap<TypeVariable<?>, Type> resolvedTypes,
      Set<Class<?>> expectedInstanceClasses, Type[] expectedParameterTypes) {
    // Check for an exact match for the instance class and the expected type.
    // If found, we can return. For expected types that allow one of several
    // possible class to match (wildcards, type variables) see if this
    // instance matches any of the allowed types.
    if (expectedType instanceof TypeVariable) {
      // Every bound must have a match
      Type[] typeVariableBounds = ((TypeVariable<?>) expectedType).getBounds();
      for (Type boundType : typeVariableBounds) {
        if (!findExpectedInstanceClass(instanceClass, boundType, resolvedTypes,
            expectedInstanceClasses, expectedParameterTypes)) {
          return false;
        }
      }
      return true;
    } else if (expectedType instanceof ParameterizedType) {
      ParameterizedType paramType = (ParameterizedType) expectedType;
      if (paramType.getRawType() == instanceClass) {
        expectedInstanceClasses.add(instanceClass);
        return true;
      }
    } else if (expectedType instanceof GenericArrayType) {
      if (instanceClass.isArray()) {
        expectedInstanceClasses.add(instanceClass);
        return true;
      }
    } else if (expectedType instanceof WildcardType) {
      WildcardType wildcardType = (WildcardType) expectedType;

      Type[] lowerBounds = wildcardType.getLowerBounds();
      for (Type type : lowerBounds) {
        /* Require instance to be a superclass of type, or type itself. */
        Class<?> boundClass = getClassFromType(type, resolvedTypes);

        while (boundClass != null) {
          if (instanceClass == boundClass) {
            expectedInstanceClasses.add(boundClass);
            break;
          }
          boundClass = boundClass.getSuperclass();
        }

        // We fail if the class does not meet any bound, as we should.
        if (boundClass == null) {
          return false;
        }
      }

      Type[] upperBounds = wildcardType.getUpperBounds();
      for (Type type : upperBounds) {
        /* Require instanceClass to be a subclass of type. */
        if (!findExpectedInstanceClass(instanceClass, type, resolvedTypes,
            expectedInstanceClasses, expectedParameterTypes)) {
          return false;
        }
      }
      return true;
    } else if (((Class<?>) expectedType).getComponentType() != null) {
      // Array types just pass through here, and we catch any problems when
      // we try to deserialize the entries.
      if (((Class<?>) expectedType).isAssignableFrom(instanceClass)) {
        expectedInstanceClasses.add(instanceClass);
        return true;
      } else {
        return false;
      }
    } else if (((Class<?>) expectedType) == instanceClass) {
      expectedInstanceClasses.add(instanceClass);
      return true;
    }

    // We know that the instance class does not exactly match the expected type,
    // so try its superclass and its interfaces. At the same time, update any
    // expected types to use the superclass or interface type. We know at this
    // point that the expected type does not involve wildcards or TypeVariables,
    // so we know that the first thing to return true indicates we are done,
    // and failure of anything to return true means the instance does not meet
    // the expected type.
    if (instanceClass.getGenericSuperclass() != null) {
      Type[] localTypes = expectedParameterTypes.clone();
      if (findExpectedInstanceClassFromSuper(instanceClass.getGenericSuperclass(), expectedType,
          resolvedTypes, expectedInstanceClasses, localTypes)) {
        for (int i = 0; i < expectedParameterTypes.length; ++i) {
          expectedParameterTypes[i] = localTypes[i];
        }
        return true;
      }
    }

    Type[] interfaces = instanceClass.getGenericInterfaces();
    for (Type interfaceType : interfaces) {
      Type[] localTypes = expectedParameterTypes.clone();
      if (findExpectedInstanceClassFromSuper(interfaceType, expectedType, resolvedTypes,
          expectedInstanceClasses, localTypes)) {
        for (int i = 0; i < expectedParameterTypes.length; ++i) {
          expectedParameterTypes[i] = localTypes[i];
        }
        return true;
      }
    }

    return false;
  }

  private static boolean findExpectedInstanceClassFromSuper(
      Type superType, Type expectedType, DequeMap<TypeVariable<?>, Type> resolvedTypes,
      Set<Class<?>> expectedInstanceClasses, Type[] expectedParameterTypes) {

    if (superType instanceof GenericArrayType) {
      // Can't use array types as supertypes or interfaces
      return false;
    } else if (superType instanceof Class) {
      // No additional type info from the superclass
      return findExpectedInstanceClass((Class<?>) superType, expectedType, resolvedTypes,
          expectedInstanceClasses, expectedParameterTypes);
    } else if (superType instanceof ParameterizedType) {
      ParameterizedType paramType = (ParameterizedType) superType;
      Type rawType = paramType.getRawType();

      if (rawType instanceof Class) {
        Class<?> rawClass = (Class<?>) rawType;
        TypeVariable<?>[] classGenericTypes = rawClass.getTypeParameters();
        Type[] actualTypes = paramType.getActualTypeArguments();

        for (int i = 0; i < actualTypes.length; ++i) {
          for (int j = 0; j < expectedParameterTypes.length; ++j) {
            if (actualTypes[i] == expectedParameterTypes[j]) {
              expectedParameterTypes[j] = classGenericTypes[i];
            }
          }
        }
        return findExpectedInstanceClass(rawClass, expectedType, resolvedTypes,
            expectedInstanceClasses, expectedParameterTypes);
      }
    } else if (superType instanceof WildcardType) {
      WildcardType wildcardType = (WildcardType) superType;
      Type[] upperBounds = wildcardType.getUpperBounds();
      for (Type boundType : upperBounds) {
        if (findExpectedInstanceClassFromSuper(boundType, expectedType,
            resolvedTypes, expectedInstanceClasses, expectedParameterTypes)) {
          return true;
        }
      }
    }

    return false;
  }

    /**
   * Attempt to find known type for TypeVariable type from an instance.
   *
   * @param foundParameter The currently known parameter, which must be of type
   *          TypeVariable
   * @param instanceType The instance that we need to check for information
   *          about the type
   * @param resolvedTypes The map of known relationships between Type objects
   * @return A new value for the foundParameter, if we find one
   */
  private static Type findInstanceParameter(Type foundParameter, Type instanceType,
      DequeMap<TypeVariable<?>, Type> resolvedTypes) {
    // See what we know about the types that are matched to this type.
    if (instanceType instanceof GenericArrayType) {
      return findInstanceParameter(foundParameter, ((GenericArrayType) instanceType)
          .getGenericComponentType(), resolvedTypes);
    } else if (instanceType instanceof ParameterizedType) {
      ParameterizedType paramType = (ParameterizedType) instanceType;
      Type rawType = paramType.getRawType();

      if (rawType instanceof Class) {
        Class<?> rawClass = (Class<?>) rawType;
        TypeVariable<?>[] classGenericTypes = rawClass.getTypeParameters();
        Type[] actualTypes = paramType.getActualTypeArguments();

        for (int i = 0; i < actualTypes.length; ++i) {
          if (actualTypes[i] == foundParameter) {
            // Check if we already know about this type.
            Type capturedType = findActualType(classGenericTypes[i], resolvedTypes);
            if (capturedType != classGenericTypes[i]) {
              return capturedType;
            }

            if (rawClass.getGenericSuperclass() != null) {
              Type superParameter =
                  findInstanceParameter(classGenericTypes[i], rawClass.getGenericSuperclass(),
                      resolvedTypes);
              if (!(superParameter instanceof TypeVariable)) {
                return superParameter;
              }
            }
            Type[] rawInterfaces = rawClass.getGenericInterfaces();
            for (Type interfaceType : rawInterfaces) {
              Type interfaceParameter =
                  findInstanceParameter(classGenericTypes[i], interfaceType, resolvedTypes);
              if (!(interfaceParameter instanceof TypeVariable)) {
                return interfaceParameter;
              }
            }
          }
        }
      }
    } else if (instanceType instanceof WildcardType) {
      WildcardType wildcardType = (WildcardType) instanceType;
      Type[] upperBounds = wildcardType.getUpperBounds();
      for (Type boundType : upperBounds) {
        Type wildcardParameter = findInstanceParameter(foundParameter, boundType, resolvedTypes);
        if (!(wildcardParameter instanceof TypeVariable)) {
          return wildcardParameter;
        }
      }
    }

    return foundParameter;
  }

  /**
   * Attempt to find the actual types for the generic parameters of an instance,
   * given the types we have resolved from the method signature or class field
   * declaration.
   *
   * @param instanceClass The instance for which we want actual generic
   *          parameter types.
   * @param resolvedTypes The types that have been resolved to actual values
   * @param expectedParameterTypes An array of types representing the actual
   *         declared types for the  parameters of the instanceClass. Some may
   *         be of type TypeVariable, in which case they need to be resolved, if
   *         possible, by this method.
   */
  private static void findInstanceParameters(Class<?> instanceClass,
      DequeMap<TypeVariable<?>, Type> resolvedTypes, Type[] expectedParameterTypes) {
    TypeVariable<?>[] instanceTypes = instanceClass.getTypeParameters();

    for (int i = 0; i < expectedParameterTypes.length; ++i) {
      if (!(expectedParameterTypes[i] instanceof TypeVariable)) {
        // We already have an actual type
        continue;
      }

      // Check if we already know about this type.
      boolean haveMatch = false;
      for (int j = 0; !haveMatch && j < instanceTypes.length; ++j) {
        if (expectedParameterTypes[i] == instanceTypes[j]) {
          Type capturedType = findActualType(instanceTypes[j], resolvedTypes);
          if (!(capturedType instanceof TypeVariable)) {
            expectedParameterTypes[i] = capturedType;
            haveMatch = true;
          }
        }
      }
      if (haveMatch) {
        continue;
      }

      // Check if it is defined by superclasses
      if (instanceClass.getGenericSuperclass() != null) {
        Type superParameter = findInstanceParameter(expectedParameterTypes[i],
                instanceClass.getGenericSuperclass(), resolvedTypes);
        if (!(superParameter instanceof TypeVariable)) {
          expectedParameterTypes[i] = superParameter;
          continue;
        }
      }

      // Check if it is defined by interfaces
      Type[] interfaceTypes = instanceClass.getGenericInterfaces();
      for (Type interfaceType : interfaceTypes) {
        Type interfaceParameter =
            findInstanceParameter(expectedParameterTypes[i], interfaceType, resolvedTypes);
        if (!(interfaceParameter instanceof TypeVariable)) {
          expectedParameterTypes[i] = interfaceParameter;
          break;
        }
      }
    }
  }

  private static void generateSerializationSignature(Class<?> instanceType, CRC32 crc,
      SerializationPolicy policy) throws UnsupportedEncodingException {
    crc.update(getSerializedTypeName(instanceType).getBytes(RPCServletUtils.CHARSET_UTF8));

    if (excludeImplementationFromSerializationSignature(instanceType)) {
      return;
    }

    Class<?> customSerializer = hasCustomFieldSerializer(instanceType);
    if (customSerializer != null) {
      generateSerializationSignature(customSerializer, crc, policy);
    } else if (instanceType.isArray()) {
      generateSerializationSignature(instanceType.getComponentType(), crc, policy);
    } else if (Enum.class.isAssignableFrom(instanceType) && !Enum.class.equals(instanceType)) {
      if (!instanceType.isEnum()) {
        instanceType = instanceType.getSuperclass();
      }
      Enum<?>[] constants = instanceType.asSubclass(Enum.class).getEnumConstants();
      for (Enum<?> constant : constants) {
        crc.update(constant.name().getBytes(RPCServletUtils.CHARSET_UTF8));
      }
    } else if (!instanceType.isPrimitive()) {
      Field[] fields = applyFieldSerializationPolicy(instanceType);
      Set<String> clientFieldNames = policy.getClientFieldNamesForEnhancedClass(instanceType);
      for (Field field : fields) {
        assert (field != null);
        /**
         * If clientFieldNames is non-null, use only the fields listed there to
         * generate the signature. Otherwise, use all known fields.
         */
        if ((clientFieldNames == null) || clientFieldNames.contains(field.getName())) {
          crc.update(field.getName().getBytes(RPCServletUtils.CHARSET_UTF8));
          crc.update(getSerializedTypeName(field.getType()).getBytes(RPCServletUtils.CHARSET_UTF8));
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
      Class<?> customSerializerClass = Class.forName(qualifiedSerialzierName, false, classLoader);
      return customSerializerClass;
    } catch (ClassNotFoundException e) {
      return null;
    }
  }

  private static void resolveTypesWorker(Type methodType,
      DequeMap<TypeVariable<?>, Type> resolvedTypes, boolean addTypes) {
    if (methodType instanceof GenericArrayType) {
      SerializabilityUtil.resolveTypesWorker(((GenericArrayType) methodType)
          .getGenericComponentType(), resolvedTypes, addTypes);
    } else if (methodType instanceof ParameterizedType) {
      ParameterizedType paramType = (ParameterizedType) methodType;
      Type rawType = paramType.getRawType();
      if (rawType instanceof Class) {
        Class<?> rawClass = (Class<?>) paramType.getRawType();
        TypeVariable<?>[] classGenericTypes = rawClass.getTypeParameters();
        Type[] actualTypes = paramType.getActualTypeArguments();

        for (int i = 0; i < actualTypes.length; ++i) {
          TypeVariable<?> variableType = classGenericTypes[i];
          if (addTypes) {
            resolvedTypes.add(variableType, actualTypes[i]);
          } else {
            resolvedTypes.remove(variableType);
          }
        }

        Class<?> superClass = rawClass.getSuperclass();
        if (superClass != null) {
          Type superGenericType = rawClass.getGenericSuperclass();
          SerializabilityUtil.resolveTypesWorker(superGenericType, resolvedTypes, addTypes);
        }

        Type[] interfaceTypes = rawClass.getGenericInterfaces();
        for (Type interfaceType : interfaceTypes) {
          SerializabilityUtil.resolveTypesWorker(interfaceType, resolvedTypes, addTypes);
        }
      }
    } else if (methodType instanceof WildcardType) {
      WildcardType wildcardType = (WildcardType) methodType;
      Type[] lowerBounds = wildcardType.getLowerBounds();
      for (Type type : lowerBounds) {
        SerializabilityUtil.resolveTypesWorker(type, resolvedTypes, addTypes);
      }
      Type[] upperBounds = wildcardType.getUpperBounds();
      for (Type type : upperBounds) {
        SerializabilityUtil.resolveTypesWorker(type, resolvedTypes, addTypes);
      }
    } else if (methodType instanceof TypeVariable) {
      Type[] bounds = ((TypeVariable<?>) methodType).getBounds();
      for (Type type : bounds) {
        SerializabilityUtil.resolveTypesWorker(type, resolvedTypes, addTypes);
      }
    } else if (methodType instanceof Class) {
      Class<?> classType = (Class<?>) methodType;

      // A type that is of instance Class, with TypeParameters, must be a raw
      // class, so strip off any parameters in the map.
      TypeVariable<?>[] classParams = classType.getTypeParameters();
      for (TypeVariable<?> classParamType : classParams) {
        if (addTypes) {
          resolvedTypes.add(classParamType, classParamType);
        } else {
          resolvedTypes.remove(classParamType);
        }
      }

      Type superGenericType = classType.getGenericSuperclass();
      if (superGenericType != null) {
        SerializabilityUtil.resolveTypesWorker(superGenericType, resolvedTypes, addTypes);
      }

      Type[] interfaceTypes = classType.getGenericInterfaces();
      for (Type interfaceType : interfaceTypes) {
        SerializabilityUtil.resolveTypesWorker(interfaceType, resolvedTypes, addTypes);
      }
    }
  }
}
