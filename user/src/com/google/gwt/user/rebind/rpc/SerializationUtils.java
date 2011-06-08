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
package com.google.gwt.user.rebind.rpc;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.typeinfo.JArrayType;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.util.Util;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.CRC32;

/**
 * Utilities used for implementing serialization.
 */
public class SerializationUtils {
  static final Comparator<JField> FIELD_COMPARATOR = new Comparator<JField>() {
    public int compare(JField f1, JField f2) {
      return f1.getName().compareTo(f2.getName());
    }
  };
  static final String GENERATED_FIELD_SERIALIZER_SUFFIX = "_FieldSerializer";
  static final String TYPE_SERIALIZER_SUFFIX = "_TypeSerializer";
  static final Set<String> TYPES_WHOSE_IMPLEMENTATION_IS_EXCLUDED_FROM_SIGNATURES =
      new HashSet<String>();

  static {
    TYPES_WHOSE_IMPLEMENTATION_IS_EXCLUDED_FROM_SIGNATURES.add("java.lang.Boolean");
    TYPES_WHOSE_IMPLEMENTATION_IS_EXCLUDED_FROM_SIGNATURES.add("java.lang.Byte");
    TYPES_WHOSE_IMPLEMENTATION_IS_EXCLUDED_FROM_SIGNATURES.add("java.lang.Character");
    TYPES_WHOSE_IMPLEMENTATION_IS_EXCLUDED_FROM_SIGNATURES.add("java.lang.Double");
    TYPES_WHOSE_IMPLEMENTATION_IS_EXCLUDED_FROM_SIGNATURES.add("java.lang.Exception");
    TYPES_WHOSE_IMPLEMENTATION_IS_EXCLUDED_FROM_SIGNATURES.add("java.lang.Float");
    TYPES_WHOSE_IMPLEMENTATION_IS_EXCLUDED_FROM_SIGNATURES.add("java.lang.Integer");
    TYPES_WHOSE_IMPLEMENTATION_IS_EXCLUDED_FROM_SIGNATURES.add("java.lang.Long");
    TYPES_WHOSE_IMPLEMENTATION_IS_EXCLUDED_FROM_SIGNATURES.add("java.lang.Object");
    TYPES_WHOSE_IMPLEMENTATION_IS_EXCLUDED_FROM_SIGNATURES.add("java.lang.Short");
    TYPES_WHOSE_IMPLEMENTATION_IS_EXCLUDED_FROM_SIGNATURES.add("java.lang.String");
    TYPES_WHOSE_IMPLEMENTATION_IS_EXCLUDED_FROM_SIGNATURES.add("java.lang.Throwable");

    /*
     * Work around for incompatible type hierarchy (and therefore signature)
     * between JUnit3 and JUnit4.
     */
    TYPES_WHOSE_IMPLEMENTATION_IS_EXCLUDED_FROM_SIGNATURES
        .add("junit.framework.AssertionFailedError");
  }

  /**
   * Returns the binary name of a type. This is the same name that would be
   * returned by {@link Class#getName()} for this type.
   * 
   * @param type TypeOracle type to get the name for
   * @return binary name for a type
   */
  public static String getRpcTypeName(JType type) {
    JPrimitiveType primitiveType = type.isPrimitive();
    if (primitiveType != null) {
      return primitiveType.getJNISignature();
    }

    JArrayType arrayType = type.isArray();
    if (arrayType != null) {
      JType component = arrayType.getComponentType();
      if (component.isClassOrInterface() != null) {
        return "[L" + getRpcTypeName(arrayType.getComponentType()) + ";";
      } else {
        return "[" + getRpcTypeName(arrayType.getComponentType());
      }
    }

    JParameterizedType parameterizedType = type.isParameterized();
    if (parameterizedType != null) {
      return getRpcTypeName(parameterizedType.getBaseType());
    }

    JClassType classType = type.isClassOrInterface();
    assert (classType != null);

    JClassType enclosingType = classType.getEnclosingType();
    if (enclosingType != null) {
      return getRpcTypeName(enclosingType) + "$" + classType.getSimpleSourceName();
    }

    return classType.getQualifiedSourceName();
  }

  /**
   * Returns the set of fields that are serializable for a given class type.
   * This method does not consider any superclass fields.
   * 
   * @param typeOracle the type oracle
   * @param classType the class for which we want serializable fields
   * @return array of fields that meet the serialization criteria
   */
  public static JField[] getSerializableFields(TypeOracle typeOracle, JClassType classType) {
    assert (classType != null);

    List<JField> fields = new ArrayList<JField>();
    JField[] declFields = classType.getFields();
    assert (declFields != null);
    for (JField field : declFields) {
      if (SerializableTypeOracleBuilder
          .shouldConsiderForSerialization(TreeLogger.NULL, true, field)) {
        fields.add(field);
      }
    }

    Collections.sort(fields, FIELD_COMPARATOR);
    return fields.toArray(new JField[fields.size()]);
  }

  /**
   * Returns the name of the field serializer for a particular type. This name
   * can be either the name of a custom field serializer or that of a generated
   * field serializer. If the type is not serializable then it can return null.
   * 
   * @param type the type that is going to be serialized
   * @return the fully qualified name of the field serializer for the given type
   */
  static String getFieldSerializerName(TypeOracle typeOracle, JType type) {
    JClassType customSerializer =
        SerializableTypeOracleBuilder.findCustomFieldSerializer(typeOracle, type);
    if (customSerializer != null) {
      return customSerializer.getQualifiedSourceName();
    }

    assert (type.isClassOrInterface() != null || type.isArray() != null);
    JClassType classType = (JClassType) type;
    return getStandardSerializerName(classType);
  }

  /**
   * Returns the serialization signature for a type.
   * 
   * @param instanceType
   * @return a string representing the serialization signature of a type
   */
  static String getSerializationSignature(TypeOracle typeOracle, JType type)
      throws RuntimeException {
    CRC32 crc = new CRC32();

    try {
      generateSerializationSignature(typeOracle, type, crc);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("Could not compute the serialization signature", e);
    }

    return Long.toString(crc.getValue());
  }

  /**
   * Returns the name of the generated field serializer.
   */
  static String getStandardSerializerName(JClassType classType) {
    String[] name =
        Shared.synthesizeTopLevelClassName(classType,
            SerializationUtils.GENERATED_FIELD_SERIALIZER_SUFFIX);
    if (name[0].length() > 0) {
      String serializerName = name[0] + "." + name[1];
      if (SerializableTypeOracleBuilder.isInStandardJavaPackage(classType.getQualifiedSourceName())) {
        /*
         * Don't generate code into java packages. If you do Development Mode
         * CompilingClassLoader will fail to resolve references to the generated
         * code.
         */
        serializerName = "com.google.gwt.user.client.rpc.core." + serializerName;
      }

      return serializerName;
    } else {
      return name[1];
    }
  }

  /**
   * Returns the qualified name of the type serializer class for the given
   * service interface.
   * 
   * @param serviceIntf service interface
   * @return name of the type serializer that handles the service interface
   */
  static String getTypeSerializerQualifiedName(JClassType serviceIntf)
      throws IllegalArgumentException {
    if (serviceIntf.isInterface() == null) {
      throw new IllegalArgumentException(serviceIntf.getQualifiedSourceName()
          + " is not a service interface");
    }

    String[] name = Shared.synthesizeTopLevelClassName(serviceIntf, TYPE_SERIALIZER_SUFFIX);
    if (name[0].length() > 0) {
      return name[0] + "." + name[1];
    } else {
      return name[1];
    }
  }

  /**
   * Returns the simple name of the type serializer class for the given service
   * interface.
   * 
   * @param serviceIntf service interface
   * @return the simple name of the type serializer class
   */
  static String getTypeSerializerSimpleName(JClassType serviceIntf) throws IllegalArgumentException {
    if (serviceIntf.isInterface() == null) {
      throw new IllegalArgumentException(serviceIntf.getQualifiedSourceName()
          + " is not a service interface");
    }

    String[] name = Shared.synthesizeTopLevelClassName(serviceIntf, TYPE_SERIALIZER_SUFFIX);
    return name[1];
  }

  private static boolean excludeImplementationFromSerializationSignature(JType instanceType) {
    if (TYPES_WHOSE_IMPLEMENTATION_IS_EXCLUDED_FROM_SIGNATURES.contains(instanceType
        .getQualifiedSourceName())) {
      return true;
    }

    return false;
  }

  private static void generateSerializationSignature(TypeOracle typeOracle, JType type, CRC32 crc)
      throws UnsupportedEncodingException {
    JParameterizedType parameterizedType = type.isParameterized();
    if (parameterizedType != null) {
      generateSerializationSignature(typeOracle, parameterizedType.getRawType(), crc);

      return;
    }

    String serializedTypeName = getRpcTypeName(type);
    crc.update(serializedTypeName.getBytes(Util.DEFAULT_ENCODING));

    if (excludeImplementationFromSerializationSignature(type)) {
      return;
    }

    JClassType customSerializer =
        SerializableTypeOracleBuilder.findCustomFieldSerializer(typeOracle, type);
    if (customSerializer != null) {
      generateSerializationSignature(typeOracle, customSerializer, crc);
    } else if (type.isArray() != null) {
      JArrayType isArray = type.isArray();
      generateSerializationSignature(typeOracle, isArray.getComponentType(), crc);
    } else if (type.isClassOrInterface() != null) {
      JClassType isClassOrInterface = type.isClassOrInterface();
      JField[] fields = getSerializableFields(typeOracle, isClassOrInterface);
      for (JField field : fields) {
        assert (field != null);

        crc.update(field.getName().getBytes(Util.DEFAULT_ENCODING));
        crc.update(getRpcTypeName(field.getType()).getBytes(Util.DEFAULT_ENCODING));
      }

      JClassType superClass = isClassOrInterface.getSuperclass();
      if (superClass != null) {
        generateSerializationSignature(typeOracle, superClass, crc);
      }
    }
  }
}
