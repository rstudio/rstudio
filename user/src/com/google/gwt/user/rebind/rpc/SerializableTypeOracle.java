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

import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JType;

/**
 * Interface implemented by any class that wants to answer questions about
 * serializable types.
 */
public interface SerializableTypeOracle {
  /**
   * Returns the set of fields that are serializable for a given class type.
   * This method does not consider any superclass fields.
   * 
   * @param classType the class for which we want serializable fields
   * @return array of fields that meet the serialization criteria.
   */
  JField[] applyFieldSerializationPolicy(JClassType classType);

  /**
   * Creates a string that represents a serialized instance reference from a
   * qualified type name.
   * 
   * @param instanceType
   * @return string containing a serialized instance reference
   */
  String encodeSerializedInstanceReference(JType instanceType);

  /**
   * Returns the instantiate method on a custom field serializer if there is one
   * and it is valid or null if there is not.
   * 
   * @param type
   * @return reference to a valid custom field serializer instantiation method
   *         or null
   */
  JMethod getCustomFieldSerializerInstantiateMethodForType(JType type);

  /**
   * Returns the name of the field serializer for a particular type. This name
   * can be either the name of a custom field serializer or that of a generated
   * field serializer. If the type is not serializable then it can return null.
   * 
   * @param type the type that is going to be serialized
   * @return the fully qualified name of the field serializer for the given type
   */
  String getFieldSerializerName(JType type);

  /**
   * Returns the list of all types that are considered serializable.
   * 
   * @return array of serializable types
   */
  JType[] getSerializableTypes();

  /**
   * Get the set of serializable types that are assignable to the requested
   * class, interface, primitive, parameterized, or array type. The set of types
   * for arrays will always be the inner most component type if it is
   * serializable or for parameterized types it will be the set of types
   * assignable to the raw parameterized type.
   * 
   * @param type
   * @return array of serializable types that are assignable to the given type
   */
  JType[] getSerializableTypesAssignableTo(JType type);

  /**
   * Returns the serialization signature for a type.
   * 
   * @param instanceType
   * @return a string representing the serialization signature of a type
   */
  String getSerializationSignature(JType instanceType);

  /**
   * Returns the serialized name of a type.
   * 
   * The following table describes the encoding of a serialized type name.
   * 
   * <table>
   * <tr>
   * <th>Element Type
   * <th>Serialized Name
   * <tr>
   * <td>Array
   * <td>[<i>Serialized Type Name of Component Type</i>
   * <tr>
   * <td>boolean
   * <td>Z
   * <tr>
   * <td>byte
   * <td>B
   * <tr>
   * <td>char
   * <td>C
   * <tr>
   * <td>class or interface
   * <td><i>Binary name of class or interface</i> see Class.getName()
   * <tr>
   * <td>double
   * <td>D
   * <tr>
   * <td>float
   * <td>F
   * <tr>
   * <td>int
   * <td>I
   * <tr>
   * <td>long
   * <td>J
   * <tr>
   * <td>short
   * <td>S </table>
   * 
   * @param type
   * @return the serialized type name
   */
  String getSerializedTypeName(JType type);

  /**
   * Returns the qualified name of the type serializer class for the given
   * service interface.
   * 
   * @param serviceIntf service interface
   * @return name of the type serializer that handles the service interface
   */
  String getTypeSerializerQualifiedName(JClassType serviceIntf);

  /**
   * Returns the simple name of the type serializer class for the given service
   * interface interface.
   * 
   * @param serviceIntf service interface
   * @return the simple name of the type serializer class
   */
  String getTypeSerializerSimpleName(JClassType serviceIntf);

  /**
   * Returns the custom field serializer associated with the given type. If
   * there is none, null is returned.
   * 
   * @param type type that may have a custom field serializer
   * @return custom field serializer or null if there is none
   */
  JClassType hasCustomFieldSerializer(JType type);

  /**
   * Returns true if the type is serializable. If a type is serializable then
   * there is a secondary type called a FieldSerializer that provides the
   * behavior necessary to serialize or deserialize the fields of an instance.
   * 
   * @param type the type that maybe serializable
   * @return true if the type is serializable
   */
  boolean isSerializable(JType type);
}
