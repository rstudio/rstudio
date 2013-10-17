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
 * A {@link ResourceOracle} for finding public resources in a library group.
 */
public class LibraryGroupPublicResourceOracle implements ResourceOracle {

  private final LibraryGroup libraryGroup;
  private Set<String> pathNames;
  private Set<Resource> publicResources;
  private Map<String, Resource> publicResourcesByPath;

  public LibraryGroupPublicResourceOracle(LibraryGroup libraryGroup) {
    this.libraryGroup = libraryGroup;
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException("A clear/refresh life cycle is not supported.");
  }

  @Override
  public Set<String> getPathNames() {
    if (pathNames == null) {
      pathNames = ImmutableSet.<String> copyOf(libraryGroup.getPublicResourcePaths());
    }
    return pathNames;
  }

  @Override
  public Map<String, Resource> getResourceMap() {
    if (publicResourcesByPath == null) {
      publicResourcesByPath = Maps.newHashMap();
      for (String path : getPathNames()) {
        publicResourcesByPath.put(path, libraryGroup.getPublicResourceByPath(path));
      }
      publicResourcesByPath = Collections.unmodifiableMap(publicResourcesByPath);
    }
    return publicResourcesByPath;
  }

  @Override
  public Set<Resource> getResources() {
    if (publicResources == null) {
      Set<String> paths = getPathNames();
      publicResources = Sets.newHashSet();
      for (String path : paths) {
        publicResources.add(libraryGroup.getPublicResourceByPath(path));
      }
      publicResources = Collections.unmodifiableSet(publicResources);
    }
    return publicResources;
  }
}
