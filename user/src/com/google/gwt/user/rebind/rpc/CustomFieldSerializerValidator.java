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
package com.google.gwt.user.rebind.rpc;

import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Checks that a custom serializer is valid.
 */
class CustomFieldSerializerValidator {
  private static final String NO_DESERIALIZE_METHOD = "Custom Field Serializer ''{0}'' does not define a deserialize method: ''public static void deserialize({1} reader,{2} instance)''";
  private static final String NO_INSTANTIATE_METHOD = "Custom Field Serializer ''{0}'' does not define an instantiate method: ''public static {1} instantiate({2} reader)''; but ''{1}'' is not default instantiable";
  private static final String NO_SERIALIZE_METHOD = "Custom Field Serializer ''{0}'' does not define a serialize method: ''public static void serialize({1} writer,{2} instance)''";

  /**
   * Returns a list of error messages associated with the custom field
   * serializer.
   * 
   * @param streamReaderClass
   *          {@link com.google.gwt.user.client.rpc.SerializationStreamReader SerializationStreamReader}
   * @param streamWriterClass
   *          {@link com.google.gwt.user.client.rpc.SerializationStreamWriter SerializationStreamWriter}
   * @param serializer the class which performs the serialization
   * @param serializee the class being serialized
   * @return list of error messages, if any, associated with the custom field
   *         serializer
   */
  public static List<String> validate(JClassType streamReaderClass,
      JClassType streamWriterClass, JClassType serializer, JClassType serializee) {
    List<String> reasons = new ArrayList<String>();

    if (serializee.isEnum() != null) {
      /*
       * Enumerated types cannot have custom field serializers because it would
       * introduce shared state between the client and the server via the
       * enumerated constants.
       */
      reasons.add("Enumerated types cannot have custom field serializers.");
      return reasons;
    }

    if (!hasSerializationMethod(streamReaderClass, "deserialize", serializer,
        serializee)) {
      // No valid deserialize method was found.
      reasons.add(MessageFormat.format(NO_DESERIALIZE_METHOD,
          serializer.getQualifiedSourceName(),
          streamReaderClass.getQualifiedSourceName(),
          serializee.getQualifiedSourceName()));
    }

    if (!hasSerializationMethod(streamWriterClass, "serialize", serializer,
        serializee)) {
      // No valid serialize method was found.
      reasons.add(MessageFormat.format(NO_SERIALIZE_METHOD,
          serializer.getQualifiedSourceName(),
          streamWriterClass.getQualifiedSourceName(),
          serializee.getQualifiedSourceName()));
    }

    if (!Shared.isDefaultInstantiable(serializee)) {
      if (!hasInstantiationMethod(streamReaderClass, serializer, serializee)) {
        // Not default instantiable and no instantiate method was found.
        reasons.add(MessageFormat.format(NO_INSTANTIATE_METHOD,
            serializer.getQualifiedSourceName(),
            serializee.getQualifiedSourceName(),
            streamReaderClass.getQualifiedSourceName()));
      }
    }

    return reasons;
  }

  private static boolean hasInstantiationMethod(JClassType streamReaderClass,
      JClassType serializer, JClassType serializee) {
    JMethod[] overloads = serializer.getOverloads("instantiate");
    for (JMethod overload : overloads) {
      JParameter[] parameters = overload.getParameters();

      if (parameters.length != 1) {
        // Different overload
        continue;
      }

      if (parameters[0].getType() != streamReaderClass) {
        // First param is not a stream class
        continue;
      }

      if (!isValidCustomFieldSerializerMethod(overload)) {
        continue;
      }

      JType type = overload.getReturnType();
      if (type.isPrimitive() != null) {
        // Primitives are auto serialized so this can't be the right method
        continue;
      }

      // TODO: if isArray answered yes to isClass this cast would not be
      // necessary
      JClassType clazz = (JClassType) type;
      return clazz.isAssignableFrom(serializee);
    }

    return false;
  }

  private static boolean hasSerializationMethod(JClassType streamClass,
      String methodName, JClassType serializer, JClassType serializee) {
    JMethod[] overloads = serializer.getOverloads(methodName);
    for (JMethod overload : overloads) {
      JParameter[] parameters = overload.getParameters();

      if (parameters.length != 2) {
        // Different overload
        continue;
      }

      if (parameters[0].getType() != streamClass) {
        // First param is not a stream class
        continue;
      }

      JParameter serializeeParam = parameters[1];
      JType type = serializeeParam.getType();
      if (type.isPrimitive() != null) {
        // Primitives are auto serialized so this can't be the right method
        continue;
      }

      // TODO: if isArray answered yes to isClass this cast would not be
      // necessary
      JClassType clazz = (JClassType) type;
      if (clazz.isAssignableFrom(serializee)) {
        if (isValidCustomFieldSerializerMethod(overload)
            && overload.getReturnType() == JPrimitiveType.VOID) {
          return true;
        }
      }
    }

    return false;
  }

  private static boolean isValidCustomFieldSerializerMethod(JMethod method) {
    if (method == null) {
      return false;
    }

    if (!method.isStatic()) {
      return false;
    }

    if (!method.isPublic()) {
      return false;
    }

    return true;
  }

  private CustomFieldSerializerValidator() {
  }
}
