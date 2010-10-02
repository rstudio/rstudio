/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.requestfactory.rebind.model;

import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;

/**
 * Represents a method declaration that causes data to be transported. This can
 * be a method declared in a RequestContext or a getter or setter on an
 * EntityProxy.
 */
public class RequestMethod {

  /**
   * Builds a {@link ContextMethod}.
   */
  public static class Builder {
    private RequestMethod toReturn = new RequestMethod();

    public RequestMethod build() {
      try {
        return toReturn;
      } finally {
        toReturn = null;
      }
    }

    public void setCollectionElementType(JClassType elementType) {
      toReturn.collectionElementType = elementType;
    }

    public void setCollectionType(CollectionType collectionType) {
      toReturn.collectionType = collectionType;
    }

    public void setDataType(JClassType dataType) {
      toReturn.dataType = dataType;
    }

    public void setDeclarationMethod(JMethod declarationMethod) {
      toReturn.declarationMethod = declarationMethod;
    }

    public void setEntityType(EntityProxyModel entityType) {
      toReturn.entityType = entityType;
    }

    public void setInstanceType(EntityProxyModel instanceType) {
      toReturn.instanceType = instanceType;
    }

    public void setValueType(boolean valueType) {
      toReturn.valueType = valueType;
    }
  }

  /**
   * Indicates the type of collection that a Request will return.
   */
  public enum CollectionType {
    // NB: Intended to be extended with a MAP value
    LIST, SET
  }

  private JClassType collectionElementType;
  private CollectionType collectionType;
  private JMethod declarationMethod;
  private EntityProxyModel entityType;
  private EntityProxyModel instanceType;
  private JClassType dataType;
  private boolean valueType;

  private RequestMethod() {
  }

  /**
   * If the method returns a collection, this method will return the element
   * type.
   * 
   * @return
   */
  public JClassType getCollectionElementType() {
    return collectionElementType;
  }

  public CollectionType getCollectionType() {
    return collectionType;
  }

  public JClassType getDataType() {
    return dataType;
  }

  public JMethod getDeclarationMethod() {
    return declarationMethod;
  }

  /**
   * If the type returned from {@link #getDataType()} refers to an EntityProxy
   * subtype, or a collection of EntityProxy subtypes, returns the
   * EntityProxyModel describing the entity.
   */
  public EntityProxyModel getEntityType() {
    return entityType;
  }

  /**
   * If the method is intended to be invoked on an instance of an EntityProxy,
   * returns the EntityProxyModel describing that type.
   */
  public EntityProxyModel getInstanceType() {
    return instanceType;
  }

  public boolean isCollectionType() {
    return collectionType != null;
  }

  public boolean isEntityType() {
    return entityType != null;
  }

  public boolean isInstance() {
    return instanceType != null;
  }

  public boolean isValueType() {
    return valueType;
  }

  /**
   * For debugging use only.
   */
  @Override
  public String toString() {
    return getDeclarationMethod().toString();
  }
}
