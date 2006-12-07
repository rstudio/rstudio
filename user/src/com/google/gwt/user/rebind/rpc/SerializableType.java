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
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JType;

/**
 * This class represents what we know about a serializable type in the system.
 */
final class SerializableType {
  CustomSerializerInfo customSerializerInfo;
  boolean isSerializable;
  JType type;

  SerializableType(JType type, boolean isSerializable,
      CustomSerializerInfo customSerializerInfo) {
    this.type = type;
    this.customSerializerInfo = customSerializerInfo;
    this.isSerializable = isSerializable;
  }

  public JClassType getCustomSerializer() {
    if (customSerializerInfo != null) {
      return customSerializerInfo.getSerializerClass();
    }

    return null;
  }

  public JMethod getCustomSerializerInstantiateMethod() {
    if (customSerializerInfo != null) {
      return customSerializerInfo.getInstantiateMethod();
    }

    return null;
  }

  public JType getType() {
    return type;
  }

  public boolean hasCustomSerializer() {
    return customSerializerInfo != null;
  }

  public boolean isSerializable() {
    if (isSerializable) {
      return true;
    }

    if (hasCustomSerializer()) {
      return true;
    }

    if (type.isPrimitive() != null) {
      return true;
    }

    return false;
  }

  public void setSerializable(boolean serializable) {
    this.isSerializable = serializable;
  }

  public String toString() {
    String s = isSerializable() ? "Serializable " : "Unserializable ";
    s += type.toString();
    return s;
  }
}
