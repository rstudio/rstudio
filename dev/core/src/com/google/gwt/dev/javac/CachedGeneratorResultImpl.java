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
package com.google.gwt.dev.javac;

import com.google.gwt.core.ext.CachedGeneratorResult;
import com.google.gwt.core.ext.linker.ArtifactSet;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * An implementation class to represent the cached results from a previous
 * generator invocation.
 */
public class CachedGeneratorResultImpl implements CachedGeneratorResult, Serializable {
  private final ArtifactSet artifacts;
  private final Map<String, GeneratedUnit> generatedUnitMap;
  private final String resultTypeName;
  private final long timeGenerated;
  private final Map<String, Serializable> clientDataMap;

  public CachedGeneratorResultImpl(String resultTypeName, ArtifactSet artifacts,
      Map<String, GeneratedUnit> generatedUnitMap, long timeGenerated,
      Map<String, Serializable> clientDataMap) {
    this.resultTypeName = resultTypeName;
    this.artifacts = new ArtifactSet(artifacts);
    this.generatedUnitMap = new HashMap<String, GeneratedUnit>(generatedUnitMap);
    this.timeGenerated = timeGenerated;
    assert clientDataMap instanceof Serializable;
    this.clientDataMap = clientDataMap;
  }

  public CachedGeneratorResultImpl(String resultTypeName, ArtifactSet artifacts,
      Map<String, GeneratedUnit> generatedUnitMap, long timeGenerated) {
    this(resultTypeName, artifacts, generatedUnitMap, timeGenerated, null);
  }

  public ArtifactSet getArtifacts() {
    return artifacts;
  }

  @Override
  public Object getClientData(String key) {
    if (clientDataMap == null) {
      return null;
    } else {
      return clientDataMap.get(key);
    }
  }

  public GeneratedUnit getGeneratedUnit(String typeName) {
    return generatedUnitMap.get(typeName);
  }

  public Collection<GeneratedUnit> getGeneratedUnits() {
    return generatedUnitMap.values();
  }

  @Override
  public String getResultTypeName() {
    return resultTypeName;
  }

  @Override
  public long getTimeGenerated() {
    return timeGenerated;
  }

  @Override
  public boolean isTypeCached(String typeName) {
    return generatedUnitMap.containsKey(typeName);
  }
}