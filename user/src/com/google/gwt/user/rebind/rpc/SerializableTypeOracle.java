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

import com.google.gwt.core.ext.typeinfo.JType;

/**
 * Interface implemented by any class that wants to answer questions about
 * serializable types for a given
 * {@link com.google.gwt.user.client.rpc.RemoteService RemoteService}.
 */
public interface SerializableTypeOracle {

  /**
   * Returns the list of all types that are considered serializable.
   * 
   * @return array of serializable types
   */
  JType[] getSerializableTypes();

  /**
   * Returns true if the type is serializable. If a type is serializable then
   * there is a secondary type called a FieldSerializer that provides the
   * behavior necessary to serialize or deserialize the fields of an instance.
   * 
   * @param type the type that maybe serializable
   * @return true if the type is serializable
   */
  boolean isSerializable(JType type);

  /**
   * Returns <code>true</code> if the type might be instantiated as part of
   * deserialization or serialization.
   * 
   * @param type the type to test
   * @return <code>true</code> if the type might be instantiated
   */
  boolean maybeInstantiated(JType type);
}
