/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.rpc.linker;

import com.google.gwt.core.ext.linker.Artifact;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This artifact allows the RpcProxyCreator class to communicate with the
 * ClientOracleLinker.
 */
public class RpcDataArtifact extends Artifact<RpcDataArtifact> {

  private final String rpcServiceName;
  private final Map<String, List<String>> fieldsByClassName = new HashMap<String, List<String>>();

  public RpcDataArtifact(String rpcServiceName) {
    super(ClientOracleLinker.class);
    this.rpcServiceName = rpcServiceName;
  }

  public Map<String, List<String>> getOperableFields() {
    return fieldsByClassName;
  }

  @Override
  public int hashCode() {
    return rpcServiceName.hashCode();
  }

  public void setFields(String className, List<String> fields) {
    fieldsByClassName.put(className, fields);
  }

  @Override
  protected int compareToComparableArtifact(RpcDataArtifact o) {
    return rpcServiceName.compareTo(o.rpcServiceName);
  }

  @Override
  protected Class<RpcDataArtifact> getComparableArtifactType() {
    return RpcDataArtifact.class;
  }

}
