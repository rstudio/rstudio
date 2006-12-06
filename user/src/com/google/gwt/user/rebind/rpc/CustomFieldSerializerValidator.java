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
package com.google.gwt.user.rebind.rpc;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

import java.util.HashMap;
import java.util.Map;

/**
 * Checks that a custom serializer is valid.
 */
class CustomFieldSerializerValidator {

  private static final String TYPE_SERIALIZER_DESERIALIZE_METHOD_NAME = "deserialize";

  private static final String TYPE_SERIALIZER_INSTANTIATE_METHOD_NAME = "instantiate";

  private static final String TYPE_SERIALIZER_SERIALIZE_METHOD_NAME = "serialize";

  /**
   * Checks that a custom serializer is valid.
   * 
   * @param logger
   * @param typeOracle
   * @param serializee class being serialized
   * @param serializer custom serializer class
   * @return true if the custom serializer is valid
   */
  public static boolean isValid(TreeLogger logger, TypeOracle typeOracle,
      JClassType serializee, JClassType serializer) {
    assert (logger != null);
    assert (serializee != null);
    assert (serializer != null);
    assert (typeOracle != null);

    logger = logger.branch(TreeLogger.SPAM,
      "Validating Custom Field Serializer '"
        + serializer.getQualifiedSourceName() + "' for class '"
        + serializee.getQualifiedSourceName() + "'", null);

    boolean isValidDeserialize = checkDeserializeMethod(logger, typeOracle,
      serializee, serializer);

    boolean isValidSerialize = checkSerializeMethod(logger, typeOracle,
      serializee, serializer);

    // This just emits a warning for now
    checkInstantiateMethod(logger, typeOracle, serializee, serializer);

    boolean isValid = isValidDeserialize && isValidSerialize;
    if (isValid) {
      logger = logger.branch(TreeLogger.SPAM, "Custom Field Serializer '"
        + serializer.getQualifiedSourceName() + "' is valid.", null);
    }

    return isValid;
  }

  /**
   * Check that the method is static, has one parameter of type
   * SerializationStreamReader and that it's return type matches the instance
   * type.
   * 
   * @param method
   * @param instanceType
   * @return true for a valid instantiate method, false otherwise
   */
  public static boolean isValidInstantiateMethod(JMethod method,
      JType instanceType) {
    if (method == null) {
      return false;
    }

    if (!method.getName().equals(
      CustomFieldSerializerValidator.TYPE_SERIALIZER_INSTANTIATE_METHOD_NAME)) {
      return false;
    }

    JParameter[] params = method.getParameters();
    if (params.length != 1) {
      return false;
    }
    assert (params[0] != null);
    JType paramType = params[0].getType();
    assert (paramType != null);
    if (!paramType.getQualifiedSourceName().equals(
      SerializationStreamReader.class.getName())) {
      return false;
    }

    if (!isValidCustomSerializerMethod(method)) {
      return false;
    }

    JType returnType = method.getReturnType();
    if (returnType == null) {
      return false;
    }

    if (returnType != instanceType) {
      return false;
    }

    return true;
  }

  /**
   * Get the types that can be serialized by the custom serializer.
   * 
   * @param logger
   * @param typeOracle
   * @param serializers
   * @return types
   */
  public static Map validateCustomFieldSerializers(TreeLogger logger,
      TypeOracle typeOracle, JClassType[] serializers) {
    assert (logger != null);
    assert (serializers != null);
    assert (typeOracle != null);

    boolean failed = false;
    Map customSerializerMap = new HashMap();
    for (int i = 0; i < serializers.length; ++i) {
      JClassType serializer = serializers[i];

      JMethod[] methods = serializer.getMethods();
      for (int j = 0; j < methods.length; ++j) {
        JMethod method = methods[j];

        JType serializee;
        if ((serializee = isSerializeMethod(method)) != null) {
          CustomSerializerInfo csi = getCustomSerializerEntry(
            customSerializerMap, serializee);

          failed = csi.setSerializerClass(logger, serializer) ? failed : true;
          failed = csi.setSerializeMethod(logger, method) ? failed : true;
        } else if ((serializee = isDeserializeMethod(method)) != null) {
          CustomSerializerInfo csi = getCustomSerializerEntry(
            customSerializerMap, serializee);
          failed = csi.setSerializerClass(logger, serializer) ? failed : true;
          failed = csi.setDeserializeMethod(logger, method) ? failed : true;
        } else if ((serializee = isInstantiateMethod(method)) != null) {
          CustomSerializerInfo csi = getCustomSerializerEntry(
            customSerializerMap, serializee);
          failed = csi.setSerializerClass(logger, serializer) ? failed : true;
          failed = csi.setInstantiateMethod(logger, method) ? failed : true;
        } else {
          logger.branch(TreeLogger.SPAM, "Method '"
            + method.getReadableDeclaration()
            + "' is not a serialization related method", null);
        }
      }
    }

    if (failed) {
      return null;
    }

    return customSerializerMap;
  }

  /*
   * @return true for a valid deserialize method
   */
  private static boolean checkDeserializeMethod(TreeLogger logger,
      TypeOracle typeOracle, JClassType serializee, JClassType serializer) {
    // Check for a deserialize method
    //
    JMethod deserialize = serializer.findMethod(
      CustomFieldSerializerValidator.TYPE_SERIALIZER_DESERIALIZE_METHOD_NAME,
      getSerializationParams(typeOracle,
        SerializationStreamReader.class.getName(), serializee));

    boolean isValid = isValidCustomSerializerMethod(deserialize);
    if (!isValid) {
      logger.branch(
        TreeLogger.ERROR,
        "Custom serializer '"
          + serializer.getQualifiedSourceName()
          + "' is not valid because it does not define a deserialize method whose signature is 'void deserialize("
          + SerializationStreamReader.class.getName() + " streamReader, "
          + serializee.getQualifiedSourceName() + " instance)'", null);
    }

    return isValid;
  }

  /**
   * Check to see whether a valid instantiate method is declared for this custom
   * field serialzer.
   * 
   * @param logger
   * @param typeOracle
   * @param serializee
   * @param serializer
   * @return returns true if the method is not defined or it is defined and
   *         matches what we expect, false otherwise.
   */
  private static boolean checkInstantiateMethod(TreeLogger logger,
      TypeOracle typeOracle, JClassType serializee, JClassType serializer) {
    JMethod instantiate = serializer.findMethod(
      CustomFieldSerializerValidator.TYPE_SERIALIZER_INSTANTIATE_METHOD_NAME
        + serializee.getSimpleSourceName(), getInstantiationParams(typeOracle));
    if (instantiate == null) {
      return true;
    }

    boolean isValid = isValidInstantiateMethod(instantiate, serializee);
    if (!isValid) {
      logger.branch(
        TreeLogger.WARN,
        "Custom serializer '"
          + serializer.getQualifiedSourceName()
          + "' defines an instantiate method whose signature does not match 'public static "
          + serializee.getQualifiedSourceName() + " instantiate("
          + SerializationStreamReader.class.getName() + " streamReader)'", null);
    }

    return isValid;
  }

  /*
   * @return true for a valid serialize method
   */
  private static boolean checkSerializeMethod(TreeLogger logger,
      TypeOracle typeOracle, JClassType serializee, JClassType serializer) {
    // Check for a serialize method
    //

    JMethod serialize = serializer.findMethod(
      CustomFieldSerializerValidator.TYPE_SERIALIZER_SERIALIZE_METHOD_NAME,
      getSerializationParams(typeOracle,
        SerializationStreamWriter.class.getName(), serializee));
    boolean isValid = isValidCustomSerializerMethod(serialize);
    if (!isValid) {
      logger.branch(
        TreeLogger.ERROR,
        "Custom serializer '"
          + serializer.getQualifiedSourceName()
          + "' is not valid because it does not define a serialize method whose signature is 'public static void serialize("
          + SerializationStreamWriter.class.getName() + " streamWriter, "
          + serializee.getQualifiedSourceName() + " instance)'", null);
    }

    return isValid;
  }

  private static CustomSerializerInfo getCustomSerializerEntry(
      Map customSerializerMap, JType serializee) {
    CustomSerializerInfo csm = (CustomSerializerInfo) customSerializerMap.get(serializee);
    if (csm == null) {
      csm = new CustomSerializerInfo();
      customSerializerMap.put(serializee, csm);
    }

    return csm;
  }

  private static JType[] getInstantiationParams(TypeOracle typeOracle) {
    JType streamReaderType = typeOracle.findType(SerializationStreamReader.class.getName());
    if (streamReaderType == null) {
      throw new RuntimeException("Could not find a definition for "
        + SerializationStreamReader.class.getName());
    }

    return new JType[]{streamReaderType};
  }

  private static JType[] getSerializationParams(TypeOracle typeOracle,
      String streamTypeName, JClassType serializee) {
    JType streamReaderType = typeOracle.findType(streamTypeName);
    if (streamReaderType == null) {
      throw new RuntimeException("Could not find a definition for "
        + streamTypeName);
    }

    return new JType[]{streamReaderType, serializee};
  }

  /**
   * Returns the type that this method serializes if the method actually is a
   * serialization method on a custom field serializer.
   * 
   * @param method method that maybe a serialization method
   * @param methodName the expected, well-known name for a serialization method
   * @param param0TypeName the expected, well-known type name for the first
   *        parameter to a serialization method
   * @return the type that this method serializes if it is a serialization
   *         method or null otherwise
   */
  private static JType isCustomSerializerMethod(JMethod method,
      String methodName, String param0TypeName) {
    if (!method.getName().equals(methodName)) {
      return null;
    }

    JType returnType = method.getReturnType();
    if (returnType == null) {
      return null;
    }

    if (returnType != JPrimitiveType.VOID) {
      return null;
    }

    JParameter[] params = method.getParameters();
    if (params.length != 2) {
      return null;
    }

    JType streamType = params[0].getType();
    if (!streamType.getQualifiedSourceName().equals(param0TypeName)) {
      return null;
    }

    JType serializee = params[1].getType();
    return serializee;
  }

  /**
   * Returns the type that this method deserializes if it is the deserialize
   * method of a custom field serializer.
   * 
   * @param method the method that maybe a deserializer method
   * @return the type that this method deserializes or null if it is not a
   *         deserialization method
   */
  private static JType isDeserializeMethod(JMethod method) {
    return isCustomSerializerMethod(method,
      CustomFieldSerializerValidator.TYPE_SERIALIZER_DESERIALIZE_METHOD_NAME,
      SerializationStreamReader.class.getName());
  }

  /**
   * Returns the type that this method instantiates if it is the instantiate
   * method of a custom field serializer.
   * 
   * @param method the method that maybe an instantiate method
   * @return the type that the method instantiates or null if it is not an
   *         instantiation method
   */
  private static JType isInstantiateMethod(JMethod method) {
    assert (method != null);

    JType returnType = method.getReturnType();
    if (returnType == null) {
      return null;
    }

    if (!method.getName().startsWith(
      CustomFieldSerializerValidator.TYPE_SERIALIZER_INSTANTIATE_METHOD_NAME)) {
      return null;
    }

    JParameter[] params = method.getParameters();
    if (params.length != 1) {
      return null;
    }

    if (!params[0].getType().getQualifiedSourceName().equals(
      SerializationStreamReader.class.getName())) {
      return null;
    }

    return returnType;
  }

  /**
   * Returns the type that this method serializes if it is the serialize method
   * of a custom field serializer.
   * 
   * @param method the method that maybe a serializer method
   * @return the type that the method serializes or null if it is not a
   *         serialization method
   */
  private static JType isSerializeMethod(JMethod method) {
    return isCustomSerializerMethod(method,
      CustomFieldSerializerValidator.TYPE_SERIALIZER_SERIALIZE_METHOD_NAME,
      SerializationStreamWriter.class.getName());
  }

  /**
   * Returns true if the method is a valid method for a custom field serializer.
   * Right now the only constraint it that it be non-null and static.
   * 
   * @param method
   * @return is the method valid?
   */
  private static boolean isValidCustomSerializerMethod(JMethod method) {
    if (method == null) {
      return false;
    }

    if (!method.isStatic()) {
      return false;
    }

    return true;
  }

  private CustomFieldSerializerValidator() {
  }
}
