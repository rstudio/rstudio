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
package com.google.gwt.dev.javac;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.resource.ResourceOracle;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Mocked out for now.
 */
public class CompilationStateBuilder {
  private static final CompilationStateBuilder instance = new CompilationStateBuilder();

  public static CompilationState buildFrom(TreeLogger logger,
      Set<Resource> resources) {
    return instance.doBuildFrom(logger, resources);
  }

  public static CompilationStateBuilder get() {
    return instance;
  }

  public synchronized CompilationState doBuildFrom(TreeLogger logger,
      final Set<Resource> resources) {
    final Map<String, Resource> resourceMap = new HashMap<String, Resource>();
    for (Resource resource : resources) {
      resourceMap.put(resource.getPath(), resource);
    }

    final Set<Resource> finalResources = Collections.unmodifiableSet(resources);
    final Map<String, Resource> finalMap = Collections.unmodifiableMap(resourceMap);
    ResourceOracle oracle = new ResourceOracle() {
      public void clear() {
        throw new UnsupportedOperationException();
      }

      public Set<String> getPathNames() {
        return finalMap.keySet();
      }

      public Map<String, Resource> getResourceMap() {
        return finalMap;
      }

      public Set<Resource> getResources() {
        return finalResources;
      }

    };
    return new CompilationState(logger, oracle);
  }
}
