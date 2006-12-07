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

/**
 * Customized Seralizer information.
 * 
 */
public class CustomSerializerInfo {
  JClassType serializeeClass;
  JClassType serializerClass;
  JMethod deserializeMethod;
  JMethod instantiateMethod;
  JMethod serializeMethod;

  public JMethod getDeserializeMethod() {
    return deserializeMethod;
  }

  public JMethod getInstantiateMethod() {
    return instantiateMethod;
  }

  public JClassType getSerializeeClass() {
    return serializeeClass;
  }

  public JMethod getSerializeMethod() {
    return serializeMethod;
  }

  public JClassType getSerializerClass() {
    return serializerClass;
  }

  public boolean setDeserializeMethod(TreeLogger logger,
      JMethod deserializeMethod) {

    if (this.deserializeMethod != null
        && this.deserializeMethod != deserializeMethod) {
      logger.log(TreeLogger.ERROR, "Type "
          + serializeeClass.getQualifiedSourceName()
          + " has more than one custom field serializer deserialize method, "
          + this.deserializeMethod.getReadableDeclaration() + " and "
          + deserializeMethod.getReadableDeclaration(), null);
      return false;
    }
    this.deserializeMethod = deserializeMethod;
    return true;
  }

  public boolean setInstantiateMethod(TreeLogger logger,
      JMethod instantiateMethod) {
    if (this.instantiateMethod != null
        && this.instantiateMethod != instantiateMethod) {
      logger.log(TreeLogger.ERROR, "Type "
          + serializeeClass.getQualifiedSourceName()
          + " has more than one custom field serializer instantiate method, "
          + this.instantiateMethod.getReadableDeclaration() + " and "
          + instantiateMethod.getReadableDeclaration(), null);
      return false;
    }

    this.instantiateMethod = instantiateMethod;
    return true;
  }

  public void setSerializeeClass(JClassType serializeeClass) {
    this.serializeeClass = serializeeClass;
  }

  public boolean setSerializeMethod(TreeLogger logger, JMethod serializeMethod) {
    if (this.serializeMethod != null && this.serializeMethod != serializeMethod) {
      logger.log(TreeLogger.ERROR, "Type "
          + serializeeClass.getQualifiedSourceName()
          + " has more than one custom field serializer deserialize method, "
          + this.serializeMethod.getReadableDeclaration() + " and "
          + serializeMethod.getReadableDeclaration(), null);
      return false;
    }

    this.serializeMethod = serializeMethod;
    return true;
  }

  public boolean setSerializerClass(TreeLogger logger,
      JClassType serializerClass) {
    if (this.serializerClass != null && this.serializerClass != serializerClass) {
      logger.log(TreeLogger.ERROR, "Type "
          + serializeeClass.getQualifiedSourceName()
          + " has more than one custom field serializer, "
          + this.serializerClass.getQualifiedSourceName() + " and "
          + serializerClass.getQualifiedSourceName(), null);
      return false;
    }
    this.serializerClass = serializerClass;
    return true;
  }
}