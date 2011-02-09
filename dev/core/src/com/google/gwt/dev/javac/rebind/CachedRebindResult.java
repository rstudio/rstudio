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
package com.google.gwt.dev.javac.rebind;

import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.dev.javac.GeneratedUnit;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A class to represent the results from a rebind operation.  This can be
 * cached and presented to subsequent rebind operations, providing the generator
 * information needed to decide whether full or partial re-generation is required.
 */
public class CachedRebindResult implements Serializable {
  private final ArtifactSet artifacts;
  private final Map<String, GeneratedUnit> generatedUnitMap;
  private final String returnedTypeName;
  private final long timeGenerated;
  private final CachedClientDataMap clientDataMap;

  public CachedRebindResult(String resultTypeName, ArtifactSet artifacts, 
      Map<String, GeneratedUnit> generatedUnitMap, 
      long timeGenerated, CachedClientDataMap clientDataMap) {
    this.returnedTypeName = resultTypeName;
    this.artifacts = new ArtifactSet(artifacts);
    this.generatedUnitMap = new HashMap<String, GeneratedUnit>(generatedUnitMap);
    this.timeGenerated = timeGenerated;
    this.clientDataMap = clientDataMap;
  }
  
  public CachedRebindResult(String resultTypeName, ArtifactSet artifacts, 
      Map<String, GeneratedUnit> generatedUnitMap, long timeGenerated) {
    this(resultTypeName, artifacts, generatedUnitMap, timeGenerated, null);
  }
  
  public ArtifactSet getArtifacts() {
    return artifacts;
  }
  
  public Object getClientData(String key) {
    if (clientDataMap == null) {
      return null;
    } else {
      return clientDataMap.get(key);
    }
  }
  
  public CachedClientDataMap getClientDataMap() {
    return clientDataMap;
  }
  
  public GeneratedUnit getGeneratedUnit(String typeName) {
    return generatedUnitMap.get(typeName);
  }
  
  public Collection<GeneratedUnit> getGeneratedUnits() {
    return generatedUnitMap.values();
  }
  
  public String getReturnedTypeName() {
    return returnedTypeName;
  }
  
  public long getTimeGenerated() {
    return timeGenerated;
  }
  
  public boolean isTypeCached(String typeName) {
    return generatedUnitMap.containsKey(typeName);
  }
}