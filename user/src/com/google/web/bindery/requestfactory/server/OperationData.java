/*
 * Copyright 2011 Google Inc.
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
package com.google.web.bindery.requestfactory.server;

import com.google.gwt.dev.asm.Type;
import com.google.gwt.dev.asm.commons.Method;

/**
 * Describes operations that the client may ask the server to perform.
 */
class OperationData {
  /**
   * Creates {@link OperationData} instances.
   */
  public static class Builder {
    OperationData d = new OperationData();

    public OperationData build() {
      OperationData toReturn = d;
      d = null;

      if (toReturn.clientMethodDescriptor != null) {
        // Strip return types
        Method noReturn =
            new Method(toReturn.methodName, Type.VOID_TYPE, Type
                .getArgumentTypes(toReturn.clientMethodDescriptor));
        toReturn.clientMethodDescriptor = noReturn.getDescriptor();
      }
      if (toReturn.domainMethodDescriptor != null) {
        Method noReturn =
            new Method(toReturn.methodName, Type.VOID_TYPE, Type
                .getArgumentTypes(toReturn.domainMethodDescriptor));
        toReturn.domainMethodDescriptor = noReturn.getDescriptor();
      }

      return toReturn;
    }

    public Builder setClientMethodDescriptor(String clientMethodDescriptor) {
      d.clientMethodDescriptor = clientMethodDescriptor;
      return this;
    }

    public Builder setDomainMethodDescriptor(String domainMethodDescriptor) {
      d.domainMethodDescriptor = domainMethodDescriptor;
      return this;
    }

    public Builder setMethodName(String methodName) {
      d.methodName = methodName;
      return this;
    }

    public Builder setRequestContext(String requestContext) {
      d.requestContextBinaryName = requestContext;
      return this;
    }
  }

  private String clientMethodDescriptor;
  private String domainMethodDescriptor;
  private String methodName;
  private String requestContextBinaryName;

  OperationData() {
  }

  public String getClientMethodDescriptor() {
    return clientMethodDescriptor;
  }

  public String getDomainMethodDescriptor() {
    return domainMethodDescriptor;
  }

  public String getMethodName() {
    return methodName;
  }

  public String getRequestContext() {
    return requestContextBinaryName;
  }
}