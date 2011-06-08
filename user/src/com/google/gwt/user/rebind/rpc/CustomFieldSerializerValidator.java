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
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Checks that a custom serializer is valid.
 */
public class CustomFieldSerializerValidator {
  private static final String NO_DESERIALIZE_METHOD =
      "Custom Field Serializer ''{0}'' does not define a deserialize method: ''public static void deserialize({1} reader,{2} instance)''";
  private static final String NO_INSTANTIATE_METHOD =
      "Custom Field Serializer ''{0}'' does not define an instantiate method: ''public static {1} instantiate({2} reader)''; but ''{1}'' is not default instantiable";
  private static final String NO_SERIALIZE_METHOD =
      "Custom Field Serializer ''{0}'' does not define a serialize method: ''public static void serialize({1} writer,{2} instance)''";
  private static final String TOO_MANY_METHODS =
      "Custom Field Serializer ''{0}'' defines too many methods named ''{1}''; please define only one method with that name";
  private static final String WRONG_CONCRETE_TYPE_RETURN =
      "Custom Field Serializer ''{0}'' returns the wrong type from ''concreteType''; return type must be ''java.lang.String''";

  public static JMethod getConcreteTypeMethod(JClassType serializer) {
    return serializer.findMethod("concreteType", new JType[0]);
  }

  public static JMethod getDeserializationMethod(JClassType serializer, JClassType serializee) {
    return getMethod("deserialize", SerializationStreamReader.class.getName(), serializer,
        serializee);
  }

  public static JMethod getInstantiationMethod(JClassType serializer, JClassType serializee) {
    JMethod[] overloads = serializer.getOverloads("instantiate");
    for (JMethod overload : overloads) {
      JParameter[] parameters = overload.getParameters();

      if (parameters.length != 1) {
        // Different overload
        continue;
      }

      if (!parameters[0].getType().getQualifiedSourceName().equals(
          SerializationStreamReader.class.getName())) {
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
      if (clazz.isAssignableFrom(serializee)) {
        return overload;
      }
    }

    return null;
  }

  public static JMethod getSerializationMethod(JClassType serializer, JClassType serializee) {
    return getMethod("serialize", SerializationStreamWriter.class.getName(), serializer, serializee);
  }

  public static boolean hasDeserializationMethod(JClassType serializer, JClassType serializee) {
    return getDeserializationMethod(serializer, serializee) != null;
  }

  public static boolean hasInstantiationMethod(JClassType serializer, JClassType serializee) {
    return getInstantiationMethod(serializer, serializee) != null;
  }

  public static boolean hasSerializationMethod(JClassType serializer, JClassType serializee) {
    return getSerializationMethod(serializer, serializee) != null;
  }

  /**
   * Returns a list of error messages associated with the custom field
   * serializer.
   * 
   * @param serializer the class which performs the serialization
   * @param serializee the class being serialized
   * @return list of error messages, if any, associated with the custom field
   *         serializer
   */
  public static List<String> validate(JClassType serializer, JClassType serializee) {
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

    if (!hasDeserializationMethod(serializer, serializee)) {
      // No valid deserialize method was found.
      reasons.add(MessageFormat.format(NO_DESERIALIZE_METHOD, serializer.getQualifiedSourceName(),
          SerializationStreamReader.class.getName(), serializee.getQualifiedSourceName()));
    } else {
      checkTooMany("deserialize", serializer, reasons);
    }

    if (!hasSerializationMethod(serializer, serializee)) {
      // No valid serialize method was found.
      reasons.add(MessageFormat.format(NO_SERIALIZE_METHOD, serializer.getQualifiedSourceName(),
          SerializationStreamWriter.class.getName(), serializee.getQualifiedSourceName()));
    } else {
      checkTooMany("serialize", serializer, reasons);
    }

    if (!hasInstantiationMethod(serializer, serializee)) {
      if (!serializee.isDefaultInstantiable() && !serializee.isAbstract()) {
        // Not default instantiable and no instantiate method was found.
        reasons.add(MessageFormat.format(NO_INSTANTIATE_METHOD,
            serializer.getQualifiedSourceName(), serializee.getQualifiedSourceName(),
            SerializationStreamReader.class.getName()));
      }
    } else {
      checkTooMany("instantiate", serializer, reasons);
    }

    JMethod concreteTypeMethod = getConcreteTypeMethod(serializer);
    if (concreteTypeMethod != null) {
      if (!"java.lang.String".equals(concreteTypeMethod.getReturnType().getQualifiedSourceName())) {
        // Wrong return type.
        reasons.add(MessageFormat.format(WRONG_CONCRETE_TYPE_RETURN, serializer
            .getQualifiedSourceName()));
      } else {
        checkTooMany("concreteType", serializer, reasons);
      }
    }

    return reasons;
  }

  private static void checkTooMany(String methodName, JClassType serializer, List<String> reasons) {
    JMethod[] overloads = serializer.getOverloads(methodName);
    if (overloads.length > 1) {
      reasons.add(MessageFormat.format(TOO_MANY_METHODS, serializer.getQualifiedSourceName(),
          methodName));
    }
  }

  private static JMethod getMethod(String methodName, String streamClassName,
      JClassType serializer, JClassType serializee) {
    JMethod[] overloads = serializer.getOverloads(methodName);
    for (JMethod overload : overloads) {
      JParameter[] parameters = overload.getParameters();

      if (parameters.length != 2) {
        // Different overload
        continue;
      }

      if (!parameters[0].getType().getQualifiedSourceName().equals(streamClassName)) {
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
          return overload;
        }
      }
    }

    return null;
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
