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

    JMethod deserialize = serializer.findMethod("deserialize", new JType[] {
        streamReaderClass, serializee});
    if (!isValidCustomFieldSerializerMethod(deserialize, JPrimitiveType.VOID)) {
      reasons.add(MessageFormat.format(NO_DESERIALIZE_METHOD,
          serializer.getQualifiedSourceName(),
          streamReaderClass.getQualifiedSourceName(),
          serializee.getQualifiedSourceName()));
    }

    JMethod serialize = serializer.findMethod("serialize", new JType[] {
        streamWriterClass, serializee});
    if (!isValidCustomFieldSerializerMethod(serialize, JPrimitiveType.VOID)) {
      reasons.add(MessageFormat.format(NO_SERIALIZE_METHOD,
          serializer.getQualifiedSourceName(),
          streamWriterClass.getQualifiedSourceName(),
          serializee.getQualifiedSourceName()));
    }

    if (!serializee.isAbstract() && !serializee.isDefaultInstantiable()) {
      JMethod instantiate = serializer.findMethod("instantiate",
          new JType[] {streamReaderClass});
      if (!isValidCustomFieldSerializerMethod(instantiate, serializee)) {
        reasons.add(MessageFormat.format(NO_INSTANTIATE_METHOD,
            serializer.getQualifiedSourceName(),
            serializee.getQualifiedSourceName(),
            streamReaderClass.getQualifiedSourceName()));
      }
    }

    return reasons;
  }

  private static boolean isValidCustomFieldSerializerMethod(JMethod method,
      JType returnType) {
    if (method == null || method.getReturnType() != returnType
        || !method.isPublic() || !method.isStatic()) {
      return false;
    }

    return true;
  }

  private CustomFieldSerializerValidator() {
  }
}
