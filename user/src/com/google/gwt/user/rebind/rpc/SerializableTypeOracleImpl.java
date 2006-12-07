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

import com.google.gwt.core.ext.typeinfo.JArrayType;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPackage;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.zip.CRC32;

final class SerializableTypeOracleImpl implements SerializableTypeOracle {

  private static final String DEFAULT_BUILTIN_CUSTOM_SERIALIZER_PACKAGE_NAME = "com.google.gwt.user.client.rpc.core.java.lang";
  private static final String GENERATED_FIELD_SERIALIZER_SUFFIX = "_FieldSerializer";
  private static final Map PRIMITIVE_TYPE_BINARY_NAMES = new HashMap();
  private static final String TYPE_SERIALIZER_SUFFIX = "_TypeSerializer";

  private static final Set TYPES_WHOSE_IMPLEMENTATION_IS_EXCLUDED_FROM_SIGNATURES = new HashSet();

  static {
    PRIMITIVE_TYPE_BINARY_NAMES.put("boolean", "Z");
    PRIMITIVE_TYPE_BINARY_NAMES.put("byte", "B");
    PRIMITIVE_TYPE_BINARY_NAMES.put("char", "C");
    PRIMITIVE_TYPE_BINARY_NAMES.put("double", "D");
    PRIMITIVE_TYPE_BINARY_NAMES.put("float", "F");
    PRIMITIVE_TYPE_BINARY_NAMES.put("int", "I");
    PRIMITIVE_TYPE_BINARY_NAMES.put("long", "J");
    PRIMITIVE_TYPE_BINARY_NAMES.put("short", "S");

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

  private Map assignableTypeCache;

  private JType objectType;

  private Map reachableTypes;

  private TypeOracle typeOracle;

  public SerializableTypeOracleImpl(TypeOracle typeOracle, Map reachableTypes)
      throws NotFoundException {
    this.reachableTypes = reachableTypes;
    this.typeOracle = typeOracle;
    assignableTypeCache = new IdentityHashMap();
    objectType = typeOracle.getType("java.lang.Object");
  }

  public JField[] applyFieldSerializationPolicy(JClassType classType) {
    ArrayList fields = new ArrayList();
    JField[] declFields = classType.getFields();
    for (int iField = 0; iField < declFields.length; ++iField) {
      JField field = declFields[iField];
      // TODO(mmendez): this is shared with the serializable type oracle
      // builder, join with that
      if (field.isStatic() || field.isTransient() || field.isFinal()) {
        continue;
      }

      fields.add(field);
    }

    Comparator fieldComp = new Comparator() {
      public int compare(Object o1, Object o2) {
        JField f1 = (JField) o1;
        JField f2 = (JField) o2;

        return f1.getName().compareTo(f2.getName());
      }
    };

    // TODO(mmendez): clean this up
    JField[] fieldsArray = (JField[]) fields.toArray(new JField[fields.size()]);
    Arrays.sort(fieldsArray, fieldComp);
    return fieldsArray;
  }

  public String encodeSerializedInstanceReference(JType instanceType) {
    return getSerializedTypeName(instanceType) + "/"
        + getSerializationSignature(instanceType);
  }

  public String encodeSerializedInstanceReference(String qualifiedTypeName)
      throws ClassNotFoundException {
    JClassType type = typeOracle.findType(qualifiedTypeName);
    if (type == null) {
      throw new ClassNotFoundException();
    }

    return encodeSerializedInstanceReference(type);
  }

  /**
   * For a given type find it's custom field serializer, if it has one, and then
   * try to find a valid instantiation method.
   */
  public JMethod getCustomFieldSerializerInstantiateMethodForType(JType type) {
    SerializableType reachableType = (SerializableType) reachableTypes.get(type);
    if (reachableType == null) {
      return null;
    }

    return reachableType.getCustomSerializerInstantiateMethod();
  }

  public String getFieldSerializerName(JType type) {
    if (!isSerializable(type)) {
      return null;
    }

    JClassType customSerializer = hasCustomFieldSerializer(type);
    if (customSerializer != null) {
      return customSerializer.getQualifiedSourceName();
    }

    JClassType classType = type.isClassOrInterface();
    if (classType != null) {
      String[] name = Shared.synthesizeTopLevelClassName(classType,
          GENERATED_FIELD_SERIALIZER_SUFFIX);
      if (name[0].length() > 0) {
        return name[0] + "." + name[1];
      } else {
        return name[1];
      }
    } else {
      // TODO(mmendez): is this branch ever needed; if not, tighten param type
      return type.getQualifiedSourceName() + GENERATED_FIELD_SERIALIZER_SUFFIX;
    }
  }

  public JType[] getSerializableTypes() {
    ArrayList list = new ArrayList();
    Set entrySet = reachableTypes.entrySet();
    assert (entrySet != null);

    Iterator iter = entrySet.iterator();
    while (iter.hasNext()) {
      Entry entry = (Entry) iter.next();
      SerializableType reachableType = (SerializableType) entry.getValue();
      assert (reachableType != null);

      if (reachableType.getType().isPrimitive() != null) {
        continue;
      }

      if (reachableType.isSerializable()) {
        list.add(reachableType.getType());
      }
    }

    return (JType[]) list.toArray(new JType[list.size()]);
  }

  public JType[] getSerializableTypesAssignableTo(JType type) {
    // Object is treated specially since it cannot be the root
    // of serializable subtypes
    //
    if (type == objectType) {
      return new JType[0];
    }

    JPrimitiveType primitive = type.isPrimitive();
    if (primitive != null) {
      // Primitives are always serializable and assignable to themselves.
      return new JType[] {primitive};
    }

    // Order is important here since Parameterized types
    JParameterizedType parameterizedType = type.isParameterized();
    if (parameterizedType != null) {
      return getSerializableTypesAssignableTo(parameterizedType.getRawType());
    }

    JArrayType arrayType = type.isArray();
    if (arrayType != null) {
      return getSerializableTypesAssignableTo(arrayType.getLeafType());
    }

    JClassType classOrInterface = type.isClassOrInterface();
    if (classOrInterface != null) {
      return getSerializableTypesAssignableTo(classOrInterface);
    }

    // Otherwise return the empty array
    return new JType[0];
  }

  public String getSerializationSignature(JType type) {
    CRC32 crc = new CRC32();

    generateSerializationSignature(type, crc);

    return Long.toString(crc.getValue());
  }

  public String getSerializedInstanceReference(JType type) {
    return getSerializedTypeName(type, false);
  }

  public String getSerializedTypeName(JType type) {
    JPrimitiveType primitiveType = type.isPrimitive();
    if (primitiveType != null) {
      Object val = PRIMITIVE_TYPE_BINARY_NAMES.get(primitiveType.getSimpleSourceName());
      if (val == null) {
        throw new RuntimeException("Unexpected primitive type '"
            + primitiveType.getQualifiedSourceName() + "'");
      }

      return (String) val;
    }

    JArrayType arrayType = type.isArray();
    if (arrayType != null) {
      JType componentType = arrayType.getComponentType();
      boolean isClassOrInterface = (componentType.isArray() == null)
          && (componentType.isPrimitive() == null);
      return "[" + (isClassOrInterface ? "L" : "")
          + getSerializedTypeName(arrayType.getComponentType())
          + (isClassOrInterface ? ";" : "");
    }

    JParameterizedType parameterizedType = type.isParameterized();
    if (parameterizedType != null) {
      return getSerializedTypeName(parameterizedType.getRawType());
    }

    JClassType classType = type.isClassOrInterface();
    assert (classType != null);

    JClassType enclosingType = classType.getEnclosingType();
    if (enclosingType != null) {
      return getSerializedTypeName(enclosingType) + "$"
          + classType.getSimpleSourceName();
    }

    JPackage pkg = classType.getPackage();
    if (pkg != null) {
      return pkg.getName() + "." + classType.getSimpleSourceName();
    }

    return classType.getSimpleSourceName();
  }

  /**
   * 
   */
  public String getSerializedTypeName(JType type, boolean addTypeSignature) {
    if (type.isPrimitive() != null) {
      Object val = PRIMITIVE_TYPE_BINARY_NAMES.get(type.getSimpleSourceName());
      if (val == null) {
        throw new RuntimeException("Unexpected primitive type '"
            + type.getQualifiedSourceName() + "'");
      }

      return (String) val;
    }

    JArrayType arrayType = type.isArray();
    if (arrayType != null) {
      return "["
          + getSerializedTypeName(arrayType.getComponentType(),
              addTypeSignature);
    }

    JParameterizedType parameterizedType = type.isParameterized();
    if (parameterizedType != null) {
      return getSerializedTypeName(parameterizedType.getRawType(),
          addTypeSignature);
    }

    JClassType classType = type.isClassOrInterface();
    assert (classType != null);

    String serializedTypeName;
    JClassType enclosingType = classType.getEnclosingType();
    if (enclosingType != null) {
      serializedTypeName = getSerializedTypeName(enclosingType, false) + "$";
    } else {
      JPackage pkg = classType.getPackage();
      if (pkg != null) {
        serializedTypeName = pkg.getName() + ".";
      } else {
        serializedTypeName = "";
      }
    }

    serializedTypeName += classType.getSimpleSourceName();

    if (addTypeSignature) {
      serializedTypeName += "/" + getSerializationSignature(classType);
      JClassType customSerializer = hasCustomFieldSerializer(classType);
      if (customSerializer != null) {
        serializedTypeName += "/"
            + getSerializedInstanceReference(customSerializer);
      }
    }

    return serializedTypeName;
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
    SerializableType reachableType = (SerializableType) reachableTypes.get(type);
    if (reachableType == null) {
      return null;
    }

    JClassType customSerializer = reachableType.getCustomSerializer();
    if (customSerializer != null) {
      return customSerializer;
    }

    if (!isSerializable(type)) {
      return null;
    }

    JArrayType arrayType = type.isArray();
    if (arrayType == null) {
      return null;
    }

    JType componentType = arrayType.getComponentType();
    JPrimitiveType primitiveType = componentType.isPrimitive();
    String qualifiedSerializerName = DEFAULT_BUILTIN_CUSTOM_SERIALIZER_PACKAGE_NAME
        + ".";
    if (primitiveType != null) {
      qualifiedSerializerName += primitiveType.getSimpleSourceName();
    } else {
      qualifiedSerializerName += typeOracle.getJavaLangObject().getSimpleSourceName();
    }
    qualifiedSerializerName += "_Array_CustomFieldSerializer";

    return typeOracle.findType(qualifiedSerializerName);
  }

  public boolean isSerializable(JType type) {
    SerializableType reachableType = (SerializableType) reachableTypes.get(type);
    if (reachableType == null) {
      return false;
    }

    return reachableType.isSerializable();
  }

  private boolean excludeImplementationFromSerializationSignature(
      JType instanceType) {
    if (TYPES_WHOSE_IMPLEMENTATION_IS_EXCLUDED_FROM_SIGNATURES.contains(instanceType.getQualifiedSourceName())) {
      return true;
    }

    return false;
  }

  private void generateSerializationSignature(JType type, CRC32 crc) {
    JParameterizedType parameterizedType = type.isParameterized();
    if (parameterizedType != null) {
      generateSerializationSignature(parameterizedType.getRawType(), crc);

      return;
    }

    String serializedTypeName = getSerializedTypeName(type);
    crc.update(serializedTypeName.getBytes());

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
      JField[] fields = applyFieldSerializationPolicy(isClassOrInterface);
      for (int i = 0; i < fields.length; ++i) {
        JField field = fields[i];
        assert (field != null);

        crc.update(field.getName().getBytes());
        crc.update(getSerializedTypeName(field.getType()).getBytes());
      }

      JClassType superClass = isClassOrInterface.getSuperclass();
      if (superClass != null) {
        generateSerializationSignature(superClass, crc);
      }
    }
  }

  /**
   * Get all serializable types that can be assigned to the requested type.
   */
  private JType[] getSerializableTypesAssignableTo(JClassType classOrInterface) {
    HashSet assignableTypes = (HashSet) assignableTypeCache.get(classOrInterface);
    if (assignableTypes != null) {
      return (JType[]) assignableTypes.toArray(new JType[assignableTypes.size()]);
    }

    assignableTypes = new HashSet();
    boolean isSerializable = isSerializable(classOrInterface);
    if (isSerializable) {
      assignableTypes.add(classOrInterface);
    }

    JClassType[] subTypes = classOrInterface.getSubtypes();
    for (int index = 0; index < subTypes.length; ++index) {
      JClassType subType = subTypes[index];

      if (isSerializable(subType)) {
        assignableTypes.add(subType);
      }
    }

    // Cache the results in case we need it later
    //
    assignableTypeCache.put(classOrInterface, assignableTypes);

    return (JType[]) assignableTypes.toArray(new JType[assignableTypes.size()]);
  }
}
