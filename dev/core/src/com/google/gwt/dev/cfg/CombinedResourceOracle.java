/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.cfg;

import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.resource.ResourceOracle;
import com.google.gwt.dev.resource.impl.AbstractResourceOracle;
import com.google.gwt.thirdparty.guava.common.collect.Sets;
import com.google.gwt.thirdparty.guava.common.io.Files;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Combines multiple resource oracles into a single queryable oracle.<br />
 *
 * Useful when the resource oracles being combined are very different in nature. For example one
 * that surfaces resources on disk and one that surfaces resources in a set of precompiled
 * libraries.
 */
public class CombinedResourceOracle extends AbstractResourceOracle {

  private Set<Resource> buildResources;
  private Set<String> pathNames;
  private List<ResourceOracle> resourceOracles;

  public CombinedResourceOracle(ResourceOracle... resourceOracles) {
    this.resourceOracles = Arrays.asList(resourceOracles);
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException("A clear/refresh life cycle is not supported.");
  }

  @Override
  public Set<String> getPathNames() {
    if (pathNames == null) {
      pathNames = Sets.newHashSet();
      for (ResourceOracle resourceOracle : resourceOracles) {
        pathNames.addAll(resourceOracle.getPathNames());
      }
      pathNames = Collections.unmodifiableSet(pathNames);
    }
    return pathNames;
  }

  @Override
  public Set<Resource> getResources() {
    if (buildResources == null) {
      buildResources = Sets.newHashSet();
      for (ResourceOracle resourceOracle : resourceOracles) {
        buildResources.addAll(resourceOracle.getResources());
      }
      buildResources = Collections.unmodifiableSet(buildResources);
    }
    return buildResources;
  }

  @Override
  public Resource getResource(String pathName) {
    pathName = Files.simplifyPath(pathName);
    for (ResourceOracle resourceOracle : resourceOracles) {
      Resource resource = resourceOracle.getResource(pathName);
      if (resource != null) {
        return resource;
      }
    }
    return null;
  }
}
