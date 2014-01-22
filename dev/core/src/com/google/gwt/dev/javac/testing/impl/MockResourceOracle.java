/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.dev.javac.testing.impl;

import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.resource.ResourceOracle;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A simple {@link ResourceOracle} for testing.
 */
public class MockResourceOracle implements ResourceOracle {

  private Map<String, Resource> exportedMap = Collections.emptyMap();
  private Set<Resource> exportedValues = Collections.emptySet();

  public MockResourceOracle(Resource... resources) {
    add(resources);
  }

  public void add(Resource... resources) {
    Map<String, Resource> newMap = new HashMap<String, Resource>(exportedMap);
    for (Resource resource : resources) {
      String path = resource.getPath();
      if (newMap.containsKey(path)) {
        throw new IllegalArgumentException(String.format(
            "Encountered two resources with the same path [%s]", path));
      }
      newMap.put(path, resource);
    }
    export(newMap);
  }

  public void addOrReplace(Resource... resources) {
    Map<String, Resource> newMap = new HashMap<String, Resource>(exportedMap);
    for (Resource resource : resources) {
      newMap.put(resource.getPath(), resource);
    }
    export(newMap);
  }

  public void clear() {
  }

  public Set<String> getPathNames() {
    return exportedMap.keySet();
  }

  public Map<String, Resource> getResourceMap() {
    return exportedMap;
  }

  public Set<Resource> getResources() {
    return exportedValues;
  }

  public void remove(String... paths) {
    Map<String, Resource> newMap = new HashMap<String, Resource>(exportedMap);
    for (String path : paths) {
      Resource oldValue = newMap.remove(path);
      if (oldValue == null) {
        throw new IllegalArgumentException(String.format(
            "Attempted to remove non-existing resource with path [%s]", path));
      }
    }
    export(newMap);
  }

  public void replace(Resource... resources) {
    Map<String, Resource> newMap = new HashMap<String, Resource>(exportedMap);
    for (Resource resource : resources) {
      String path = resource.getPath();
      if (!newMap.containsKey(path)) {
        throw new IllegalArgumentException(String.format(
            "Attempted to replace non-existing resource with path [%s]", path));
      }
      newMap.put(path, resource);
    }
    export(newMap);
  }

  private void export(Map<String, Resource> newMap) {
    exportedMap = Collections.unmodifiableMap(newMap);
    // Make a new hash set for constant lookup.
    exportedValues = Collections.unmodifiableSet(new HashSet<Resource>(
        exportedMap.values()));
  }

}