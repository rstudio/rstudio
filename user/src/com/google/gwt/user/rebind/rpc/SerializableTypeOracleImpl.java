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
import com.google.gwt.core.ext.typeinfo.JType;

import java.util.Set;

final class SerializableTypeOracleImpl implements SerializableTypeOracle {

  private final Set<JClassType> possiblyInstantiatedTypes;
  private final Set<JClassType> serializableTypesSet;

  public SerializableTypeOracleImpl(Set<JClassType> serializableTypes,
      Set<JClassType> possiblyInstantiatedTypes) {

    serializableTypesSet = serializableTypes;
    this.possiblyInstantiatedTypes = possiblyInstantiatedTypes;
  }

  public JType[] getSerializableTypes() {
    return serializableTypesSet.toArray(new JType[serializableTypesSet.size()]);
  }

  /**
   * Returns <code>true</code> if the type's fields can be serializede.
   */
  public boolean isSerializable(JType type) {
    return serializableTypesSet.contains(type);
  }

  /**
   * Returns <code>true</code> if the type can be serialized and then
   * instantiated on the other side.
   */
  public boolean maybeInstantiated(JType type) {
    return possiblyInstantiatedTypes.contains(type);
  }
}
