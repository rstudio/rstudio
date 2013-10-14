/*
 * Copyright 2013 Google Inc.
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
package com.google.gwt.dev.cfg;

import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.resource.ResourceOracle;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableSet;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * A {@link ResourceOracle} for finding build resources in a library group.
 */
public class LibraryGroupBuildResourceOracle implements ResourceOracle {

  private Set<Resource> buildResources;
  private Map<String, Resource> buildResourcesByPath;
  private final LibraryGroup libraryGroup;
  private Set<String> pathNames;

  public LibraryGroupBuildResourceOracle(LibraryGroup libraryGroup) {
    this.libraryGroup = libraryGroup;
  }

  @Override
  public void clear() {
    pathNames = null;
    buildResources = null;
    buildResourcesByPath = null;
  }

  @Override
  public Set<String> getPathNames() {
    if (pathNames == null) {
      pathNames = ImmutableSet.<String> copyOf(libraryGroup.getBuildResourcePaths());
    }
    return pathNames;
  }

  @Override
  public Map<String, Resource> getResourceMap() {
    if (buildResourcesByPath == null) {
      buildResourcesByPath = Maps.newHashMap();
      for (String path : getPathNames()) {
        buildResourcesByPath.put(path, libraryGroup.getBuildResourceByPath(path));
      }
      buildResourcesByPath = Collections.unmodifiableMap(buildResourcesByPath);
    }
    return buildResourcesByPath;
  }

  @Override
  public Set<Resource> getResources() {
    if (buildResources == null) {
      Set<String> paths = getPathNames();
      buildResources = Sets.newHashSet();
      for (String path : paths) {
        buildResources.add(libraryGroup.getBuildResourceByPath(path));
      }
      buildResources = Collections.unmodifiableSet(buildResources);
    }
    return buildResources;
  }
}
