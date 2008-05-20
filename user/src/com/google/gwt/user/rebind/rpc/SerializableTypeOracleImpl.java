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

import com.google.gwt.core.ext.typeinfo.JArrayType;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.javac.TypeOracleMediator;
import com.google.gwt.dev.util.Util;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.CRC32;

final class SerializableTypeOracleImpl implements SerializableTypeOracle {

  private static final Comparator<JField> FIELD_COMPARATOR = new Comparator<JField>() {
    public int compare(JField f1, JField f2) {
      return f1.getName().compareTo(f2.getName());
    }
  };

  private static final String GENERATED_FIELD_SERIALIZER_SUFFIX = "_FieldSerializer";
  private static final String TYPE_SERIALIZER_SUFFIX = "_TypeSerializer";
  private static final Set<String> TYPES_WHOSE_IMPLEMENTATION_IS_EXCLUDED_FROM_SIGNATURES = new HashSet<String>();

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
  }

  private final Set<JClassType> serializableTypesSet;
  private final TypeOracle typeOracle;
  private final Set<JClassType> possiblyInstantiatedTypes;

  public SerializableTypeOracleImpl(TypeOracle typeOracle,
      Set<JClassType> serializableTypes,
      Set<JClassType> possiblyInstantiatedTypes) {

    serializableTypesSet = serializableTypes;
    this.typeOracle = typeOracle;

    this.possiblyInstantiatedTypes = possiblyInstantiatedTypes;
  }

  public String getFieldSerializerName(JType type) {
    if (!isSerializable(type)) {
      return null;
    }

    JClassType customSerializer = hasCustomFieldSerializer(type);
    if (customSerializer != null) {
      return customSerializer.getQualifiedSourceName();
    }

    assert (type.isClassOrInterface() != null || type.isArray() != null);
    JClassType classType = (JClassType) type;
    String[] name = Shared.synthesizeTopLevelClassName(classType,
        GENERATED_FIELD_SERIALIZER_SUFFIX);
    if (name[0].length() > 0) {
      String serializerName = name[0] + "." + name[1];
      if (SerializableTypeOracleBuilder.isInStandardJavaPackage(type.getQualifiedSourceName())) {
        /*
         * Don't generate code into java packages. If you do hosted mode
         * CompilingClassLoader will fail to resolve references to the generated
         * code.
         */
        serializerName = "com.google.gwt.user.client.rpc.core."
            + serializerName;
      }

      return serializerName;
    } else {
      return name[1];
    }
  }

  /**
   * Returns the fields which qualify for serialization.
   */
  public JField[] getSerializableFields(JClassType classType) {
    assert (classType != null);

    List<JField> fields = new ArrayList<JField>();
    JField[] declFields = classType.getFields();
    assert (declFields != null);
    for (JField field : declFields) {
      // TODO(mmendez): this is shared with the serializable type oracle
      // builder, join with that
      if (field.isStatic() || field.isTransient() || field.isFinal()) {
        continue;
      }

      fields.add(field);
    }

    Collections.sort(fields, FIELD_COMPARATOR);
    return fields.toArray(new JField[fields.size()]);
  }

  /**
   * 
   */
  public JType[] getSerializableTypes() {
    return serializableTypesSet.toArray(new JType[serializableTypesSet.size()]);
  }

  public String getSerializationSignature(JType type) {
    CRC32 crc = new CRC32();

    try {
      generateSerializationSignature(type, crc);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(
          "Could not compute the serialization signature", e);
    }

    return Long.toString(crc.getValue());
  }

  public String getSerializedTypeName(JType type) {
    return TypeOracleMediator.computeBinaryClassName(type);
  }

  public String getTypeSerializerQualifiedName(JClassType serviceIntf) {
    if (serviceIntf.isInterface() == null) {
      throw new IllegalArgumentException(serviceIntf.getQualifiedSourceName()
          + " is not a service interface");
    }

    String[] name = Shared.synthesizeTopLevelClassName(serviceIntf,
        TYPE_SERIALIZER_SUFFIX);
    if (name[0].length() > 0) {
      return name[0] + "." + name[1];
    } else {
      return name[1];
    }
  }

  public String getTypeSerializerSimpleName(JClassType serviceIntf) {
    if (serviceIntf.isInterface() == null) {
      throw new IllegalArgumentException(serviceIntf.getQualifiedSourceName()
          + " is not a service interface");
    }

    String[] name = Shared.synthesizeTopLevelClassName(serviceIntf,
        TYPE_SERIALIZER_SUFFIX);
    return name[1];
  }

  public JClassType hasCustomFieldSerializer(JType type) {
    return SerializableTypeOracleBuilder.findCustomFieldSerializer(typeOracle,
        type);
  }

  /**
   * Returns <code>true</code> if the type is serializable.
   */
  public boolean isSerializable(JType type) {
    return serializableTypesSet.contains(type);
  }

  public boolean maybeInstantiated(JType type) {
    return possiblyInstantiatedTypes.contains(type);
  }

  private boolean excludeImplementationFromSerializationSignature(
      JType instanceType) {
    if (TYPES_WHOSE_IMPLEMENTATION_IS_EXCLUDED_FROM_SIGNATURES.contains(instanceType.getQualifiedSourceName())) {
      return true;
    }

    return false;
  }

  private void generateSerializationSignature(JType type, CRC32 crc)
      throws UnsupportedEncodingException {
    JParameterizedType parameterizedType = type.isParameterized();
    if (parameterizedType != null) {
      generateSerializationSignature(parameterizedType.getRawType(), crc);

      return;
    }

    String serializedTypeName = getSerializedTypeName(type);
    crc.update(serializedTypeName.getBytes(Util.DEFAULT_ENCODING));

    if (excludeImplementationFromSerializationSignature(type)) {
      return;
    }

    JClassType customSerializer = hasCustomFieldSerializer(type);
    if (customSerializer != null) {
      generateSerializationSignature(customSerializer, crc);
    } else if (type.isArray() != null) {
      JArrayType isArray = type.isArray();
      generateSerializationSignature(isArray.getComponentType(), crc);
    } else if (type.isClassOrInterface() != null) {
      JClassType isClassOrInterface = type.isClassOrInterface();
      JField[] fields = getSerializableFields(isClassOrInterface);
      for (JField field : fields) {
        assert (field != null);

        crc.update(field.getName().getBytes(Util.DEFAULT_ENCODING));
        crc.update(getSerializedTypeName(field.getType()).getBytes(
            Util.DEFAULT_ENCODING));
      }

      JClassType superClass = isClassOrInterface.getSuperclass();
      if (superClass != null) {
        generateSerializationSignature(superClass, crc);
      }
    }
  }
}
