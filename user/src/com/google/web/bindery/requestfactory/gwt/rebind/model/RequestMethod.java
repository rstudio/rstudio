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
package com.google.web.bindery.requestfactory.gwt.rebind.model;

import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.web.bindery.requestfactory.shared.JsonRpcWireName;
import com.google.web.bindery.requestfactory.vm.impl.OperationKey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a method declaration that causes data to be transported. This can
 * be a method declared in a RequestContext or a getter or setter on an
 * EntityProxy.
 */
public class RequestMethod implements AcceptsModelVisitor {

  /**
   * Builds a {@link ContextMethod}.
   */
  public static class Builder {
    private RequestMethod toReturn = new RequestMethod();

    public void addExtraSetter(JMethod method) {
      if (toReturn.extraSetters == null) {
        toReturn.extraSetters = new ArrayList<JMethod>();
      }
      toReturn.extraSetters.add(method);
    }

    public RequestMethod build() {
      if (toReturn.extraSetters == null) {
        toReturn.extraSetters = Collections.emptyList();
      } else {
        toReturn.extraSetters = Collections.unmodifiableList(toReturn.extraSetters);
      }
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

    public void setDeclarationMethod(JClassType contextType, JMethod declarationMethod) {
      toReturn.declarationMethod = declarationMethod;

      JClassType returnClass = declarationMethod.getReturnType().isClassOrInterface();
      JsonRpcWireName annotation = returnClass == null ? null
          : returnClass.getAnnotation(JsonRpcWireName.class);
      if (annotation == null) {
        StringBuilder sb = new StringBuilder("(");
        for (JType type : declarationMethod.getParameterTypes()) {
          sb.append(type.getJNISignature());
        }
        // Return type ignored
        sb.append(")V");
        toReturn.operation =
            new OperationKey(contextType.getQualifiedBinaryName(), declarationMethod.getName(), sb
                .toString()).get();
      } else {
        toReturn.operation = annotation.value();
        toReturn.apiVersion = annotation.version();
      }
    }

    public void setEntityType(EntityProxyModel entityType) {
      toReturn.entityType = entityType;
    }

    public void setInstanceType(EntityProxyModel instanceType) {
      toReturn.instanceType = instanceType;
    }

    public void setMapKeyType(JClassType elementType) {
      toReturn.mapKeyType = elementType;
    }

    public void setMapValueType(JClassType elementType) {
      toReturn.mapValueType = elementType;
    }

    public void setValueType(boolean valueType) {
      toReturn.valueType = valueType;
    }
  }

  /**
   * Indicates the type of collection that a Request will return.
   */
  public enum CollectionType {
    LIST, SET, MAP
  }

  private String apiVersion;
  private JClassType collectionElementType;
  private CollectionType collectionType;
  private JClassType dataType;
  private JMethod declarationMethod;
  private EntityProxyModel entityType;
  private List<JMethod> extraSetters = new ArrayList<JMethod>();
  private EntityProxyModel instanceType;
  private String operation;
  private JClassType mapValueType;
  private JClassType mapKeyType;
  private boolean valueType;

  private RequestMethod() {
  }

  public void accept(ModelVisitor visitor) {
    if (visitor.visit(this)) {
      // Empty
    }
    visitor.endVisit(this);
  }

  public String getApiVersion() {
    return apiVersion;
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

  public List<JMethod> getExtraSetters() {
    return extraSetters;
  }

  /**
   * If the method is intended to be invoked on an instance of an EntityProxy,
   * returns the EntityProxyModel describing that type.
   */
  public EntityProxyModel getInstanceType() {
    return instanceType;
  }

  public JClassType getMapKeyType() {
    return mapKeyType;
  }

  public JClassType getMapValueType() {
    return mapValueType;
  }

  public String getOperation() {
    return operation;
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
