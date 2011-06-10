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
import com.google.web.bindery.requestfactory.shared.JsonRpcService;
import com.google.web.bindery.requestfactory.shared.impl.AbstractRequestContext.Dialect;

import java.util.Collections;
import java.util.List;

/**
 * Represents a service endpoint.
 */
public class ContextMethod implements AcceptsModelVisitor, HasExtraTypes {

  /**
   * Builds a {@link ContextMethod}.
   */
  public static class Builder {
    private ContextMethod toReturn = new ContextMethod();

    public ContextMethod build() {
      try {
        return toReturn;
      } finally {
        toReturn = null;
      }
    }

    public Builder setDeclaredMethod(JMethod method) {
      toReturn.methodName = method.getName();
      JClassType returnClass = method.getReturnType().isClassOrInterface();
      toReturn.interfaceName = returnClass.getQualifiedSourceName();
      toReturn.packageName = returnClass.getPackage().getName();
      toReturn.simpleSourceName = returnClass.getName().replace('.', '_') + "Impl";
      toReturn.dialect =
          returnClass.isAnnotationPresent(JsonRpcService.class) ? Dialect.JSON_RPC
              : Dialect.STANDARD;
      return this;
    }

    public Builder setExtraTypes(List<EntityProxyModel> extraTypes) {
      toReturn.extraTypes = extraTypes;
      return this;
    }

    public Builder setRequestMethods(List<RequestMethod> requestMethods) {
      toReturn.requestMethods = requestMethods;
      return this;
    }
  }

  private Dialect dialect;
  private List<EntityProxyModel> extraTypes;
  private String interfaceName;
  private String methodName;
  private String packageName;
  private List<RequestMethod> requestMethods;
  private String simpleSourceName;

  private ContextMethod() {
  }

  public void accept(ModelVisitor visitor) {
    if (visitor.visit(this)) {
      for (RequestMethod method : getRequestMethods()) {
        method.accept(visitor);
      }
    }
    visitor.endVisit(this);
  }

  public Dialect getDialect() {
    return dialect;
  }

  public List<EntityProxyModel> getExtraTypes() {
    return Collections.unmodifiableList(extraTypes);
  }

  /**
   * The qualified source name of the RequestContext sub-interface (i.e., the
   * return type of the method declaration).
   */
  public String getImplementedInterfaceQualifiedSourceName() {
    return interfaceName;
  }

  public String getMethodName() {
    return methodName;
  }

  public String getPackageName() {
    return packageName;
  }

  /**
   * The qualified source name of the implementation.
   */
  public String getQualifiedSourceName() {
    return getPackageName() + "." + getSimpleSourceName();
  }

  public List<RequestMethod> getRequestMethods() {
    return Collections.unmodifiableList(requestMethods);
  }

  public String getSimpleSourceName() {
    return simpleSourceName;
  }

  /**
   * For debugging use only.
   */
  @Override
  public String toString() {
    return getQualifiedSourceName() + " " + getMethodName() + "()";
  }
}
