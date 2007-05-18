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
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JType;

/**
 * Interface implemented by any class that wants to answer questions about
 * serializable types for a given
 * {@link com.google.gwt.user.client.rpc.RemoteService RemoteService}.
 */
public interface SerializableTypeOracle {
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
   * Returns the set of fields that are serializable for a given class type.
   * This method does not consider any superclass fields.
   * 
   * @param classType the class for which we want serializable fields
   * @return array of fields that meet the serialization criteria.
   */
  JField[] getSerializableFields(JClassType classType);

  /**
   * Returns the list of all types that are considered serializable.
   * 
   * @return array of serializable types
   */
  JType[] getSerializableTypes();

  /**
   * Returns the serialization signature for a type.
   * 
   * @param instanceType
   * @return a string representing the serialization signature of a type
   */
  String getSerializationSignature(JType instanceType);

  /**
   * Returns the serialized name of a type.  The serialized name of a type is
   * the name that would be returned by {@link Class#getName()}.
   * 
   * @param type
   * @return serialized name of a type
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
   * interface.
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
