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
import com.google.web.bindery.requestfactory.vm.impl.OperationKey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

/**
 * Provides access to payload deobfuscation services.
 */
class Deobfuscator {
  public static class Builder {
    private Deobfuscator d = new Deobfuscator();
    {
      d.domainToClientType = new HashMap<String, List<String>>();
      d.operationData = new HashMap<OperationKey, OperationData>();
      d.typeTokens = new HashMap<String, String>();
    }

    public Builder addClientToDomainMapping(String domainBinaryName, SortedSet<Type> value) {
      List<String> clientBinaryNames;
      switch (value.size()) {
        case 0:
          clientBinaryNames = Collections.emptyList();
          break;
        case 1:
          clientBinaryNames = Collections.singletonList(value.first().getClassName());
          break;
        default:
          clientBinaryNames = new ArrayList<String>(value.size());
          for (Type t : value) {
            clientBinaryNames.add(t.getClassName());
          }
          clientBinaryNames = Collections.unmodifiableList(clientBinaryNames);
      }
      d.domainToClientType.put(domainBinaryName, clientBinaryNames);
      return this;
    }

    public Builder addClientToDomainMappings(Map<Type, SortedSet<Type>> data) {
      for (Map.Entry<Type, SortedSet<Type>> entry : data.entrySet()) {
        String domainBinaryName = entry.getKey().getClassName();
        SortedSet<Type> value = entry.getValue();
        addClientToDomainMapping(domainBinaryName, value);
      }
      return this;
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
      d.typeTokens.put(token, binaryName);
      return this;
    }

    public Builder addRawTypeTokens(Map<String, String> typeTokens) {
      for (Map.Entry<String, String> entry : typeTokens.entrySet()) {
        addRawTypeToken(entry.getKey(), entry.getValue());
      }
      return this;
    }

    public Builder addTypeTokens(Map<String, Type> typeTokens) {
      for (Map.Entry<String, Type> entry : typeTokens.entrySet()) {
        d.typeTokens.put(entry.getKey(), entry.getValue().getClassName());
      }
      return this;
    }

    public Deobfuscator build() {
      Deobfuscator toReturn = d;
      toReturn.domainToClientType = Collections.unmodifiableMap(toReturn.domainToClientType);
      toReturn.operationData = Collections.unmodifiableMap(toReturn.operationData);
      toReturn.typeTokens = Collections.unmodifiableMap(toReturn.typeTokens);
      d = null;
      return toReturn;
    }

    public Builder merge(Deobfuscator deobfuscator) {
      d.domainToClientType.putAll(deobfuscator.domainToClientType);
      d.operationData.putAll(deobfuscator.operationData);
      d.typeTokens.putAll(deobfuscator.typeTokens);
      return this;
    }
  }

  /**
   * Maps domain types (e.g Foo) to client proxy types (e.g. FooAProxy,
   * FooBProxy).
   */
  private Map<String, List<String>> domainToClientType;
  private Map<OperationKey, OperationData> operationData;
  /**
   * Map of obfuscated ids to binary class names.
   */
  private Map<String, String> typeTokens;

  Deobfuscator() {
  }

  /**
   * Returns the client proxy types whose {@code @ProxyFor} is exactly
   * {@code binaryTypeName}. Ordered such that the most-derived types will be
   * iterated over first.
   */
  public List<String> getClientProxies(String binaryTypeName) {
    return domainToClientType.get(binaryTypeName);
  }

  /**
   * Returns a method descriptor that should be invoked on the service object.
   */
  public String getDomainMethodDescriptor(String operation) {
    OperationData data = getData(operation);
    return data == null ? null : data.getDomainMethodDescriptor();
  }

  public String getRequestContext(String operation) {
    OperationData data = getData(operation);
    return data == null ? null : data.getRequestContext();
  }

  public String getRequestContextMethodDescriptor(String operation) {
    OperationData data = getData(operation);
    return data == null ? null : data.getClientMethodDescriptor();
  }

  public String getRequestContextMethodName(String operation) {
    OperationData data = getData(operation);
    return data == null ? null : data.getMethodName();
  }

  /**
   * Returns a type's binary name based on an obfuscated token.
   */
  public String getTypeFromToken(String token) {
    return typeTokens.get(token);
  }

  private OperationData getData(String operation) {
    OperationData data = operationData.get(new OperationKey(operation));
    return data;
  }
}