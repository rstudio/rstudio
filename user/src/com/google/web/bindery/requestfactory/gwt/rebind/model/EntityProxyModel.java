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

import java.util.Collections;
import java.util.List;

/**
 * Represents an EntityProxy subtype.
 */
public class EntityProxyModel implements AcceptsModelVisitor, HasExtraTypes {
  /**
   * Builds {@link EntityProxyModel}.
   */
  public static class Builder {
    private EntityProxyModel toReturn = new EntityProxyModel();

    public EntityProxyModel build() {
      if (toReturn.superProxyTypes == null) {
        toReturn.superProxyTypes = Collections.emptyList();
      }
      try {
        return toReturn;
      } finally {
        toReturn = null;
      }
    }

    /**
     * Allow access to the unfinished EntityProxyModel to allow for circular
     * type dependencies.
     */
    public EntityProxyModel peek() {
      return toReturn;
    }

    public Builder setExtraTypes(List<EntityProxyModel> extraTypes) {
      toReturn.extraTypes = extraTypes;
      return this;
    }

    public Builder setQualifiedBinaryName(String qualifiedBinaryName) {
      toReturn.qualifiedBinaryName = qualifiedBinaryName;
      return this;
    }

    public Builder setQualifiedSourceName(String name) {
      assert !name.contains(" ");
      toReturn.qualifiedSourceName = name;
      return this;
    }

    public Builder setRequestMethods(List<RequestMethod> requestMethods) {
      toReturn.requestMethods = requestMethods;
      return this;
    }

    public Builder setSuperProxyTypes(List<EntityProxyModel> superTypes) {
      toReturn.superProxyTypes = superTypes;
      return this;
    }

    public Builder setType(Type type) {
      toReturn.type = type;
      return this;
    }
  }

  /**
   * The kind of proxy. This is an enum in case more proxy types are defined in
   * the future.
   */
  public enum Type {
    ENTITY, VALUE
  }

  private List<EntityProxyModel> extraTypes;
  private String qualifiedBinaryName;
  private String qualifiedSourceName;
  private List<RequestMethod> requestMethods;
  private List<EntityProxyModel> superProxyTypes;
  private Type type;

  private EntityProxyModel() {
  }

  public void accept(ModelVisitor visitor) {
    if (visitor.visit(this)) {
      for (RequestMethod method : requestMethods) {
        method.accept(visitor);
      }
    }
    visitor.endVisit(this);
  }

  public List<EntityProxyModel> getExtraTypes() {
    return Collections.unmodifiableList(extraTypes);
  }

  public String getQualifiedBinaryName() {
    return qualifiedBinaryName;
  }

  public String getQualifiedSourceName() {
    return qualifiedSourceName;
  }

  public List<RequestMethod> getRequestMethods() {
    return Collections.unmodifiableList(requestMethods);
  }

  /**
   * Returns the proxy types to which the EntityProxyModel is assignable.
   */
  public List<EntityProxyModel> getSuperProxyTypes() {
    return superProxyTypes;
  }

  public Type getType() {
    return type;
  }

  /**
   * For debugging use only.
   */
  @Override
  public String toString() {
    return qualifiedSourceName;
  }
}
