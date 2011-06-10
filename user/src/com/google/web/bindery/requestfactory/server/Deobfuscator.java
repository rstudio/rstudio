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
import com.google.gwt.dev.util.Name.BinaryName;
import com.google.web.bindery.requestfactory.vm.impl.OperationKey;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides access to payload deobfuscation services.
 */
class Deobfuscator {
  public static class Builder {
    private Deobfuscator d = new Deobfuscator();
    {
      d.operationData = new HashMap<OperationKey, OperationData>();
      d.typeTokens = new HashMap<String, Type>();
    }

    public Builder addOperation(OperationKey key, OperationData data) {
      d.operationData.put(key, data);
      return this;
    }

    public Builder addOperationData(Map<OperationKey, OperationData> operationData) {
      d.operationData.putAll(operationData);
      return this;
    }

    public Builder addRawTypeToken(String token, String binaryName) {
      d.typeTokens.put(token, Type.getObjectType(BinaryName.toInternalName(binaryName)));
      return this;
    }

    public Builder addRawTypeTokens(Map<String, String> typeTokens) {
      for (Map.Entry<String, String> entry : typeTokens.entrySet()) {
        addRawTypeToken(entry.getKey(), entry.getValue());
      }
      return this;
    }

    public Deobfuscator build() {
      Deobfuscator toReturn = d;
      toReturn.operationData = Collections.unmodifiableMap(toReturn.operationData);
      toReturn.typeTokens = Collections.unmodifiableMap(toReturn.typeTokens);
      d = null;
      return toReturn;
    }

    /**
     * This method should be removed in favor of having a map of RequestFactory
     * to Deobfuscators in ResolverServiceLayer and getting rid of the static
     * validator instance.
     */
    public Builder setOperationData(Map<OperationKey, OperationData> operationData) {
      d.operationData = operationData;
      return this;
    }

    /**
     * To be removed as well.
     */
    public Builder setTypeTokens(Map<String, Type> typeTokens) {
      d.typeTokens = typeTokens;
      return this;
    }
  }

  private Map<OperationKey, OperationData> operationData;

  /**
   * Map of obfuscated ids to binary class names.
   */
  private Map<String, Type> typeTokens;

  Deobfuscator() {
  }

  /**
   * Returns a method descriptor that should be invoked on the service object.
   */
  public String getDomainMethodDescriptor(String operation) {
    OperationData data = operationData.get(new OperationKey(operation));
    return data == null ? null : data.getDomainMethodDescriptor();
  }

  public String getRequestContext(String operation) {
    OperationData data = operationData.get(new OperationKey(operation));
    return data == null ? null : data.getRequestContext();
  }

  public String getRequestContextMethodDescriptor(String operation) {
    OperationData data = operationData.get(new OperationKey(operation));
    return data == null ? null : data.getClientMethodDescriptor();
  }

  public String getRequestContextMethodName(String operation) {
    OperationData data = operationData.get(new OperationKey(operation));
    return data == null ? null : data.getMethodName();
  }

  /**
   * Returns a type's binary name based on an obfuscated token.
   */
  public String getTypeFromToken(String token) {
    Type type = typeTokens.get(token);
    return type == null ? null : type.getClassName();
  }
}