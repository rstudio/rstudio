/*
 * Copyright 2014 Google Inc.
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
package com.google.gwt.dev.resource.impl;

import com.google.gwt.thirdparty.guava.common.base.Objects;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.SortedSet;

/**
 * Describes how a PathPrefixSet resolved a Resource.
 * <p>
 * Contains a record of the one most specific matching PathPrefix as well as the set of names of all
 * Modules that registered any matching PathPrefix.
 */
public class ResourceResolution {

  private PathPrefix pathPrefix;
  private SortedSet<String> sourceModuleNames;

  public boolean addSourceModuleName(String sourceModuleName) {
    return getSourceModuleNames().add(sourceModuleName);
  }

  @Override
  public boolean equals(Object object) {
    if (object instanceof ResourceResolution) {
      ResourceResolution that = (ResourceResolution) object;
      return Objects.equal(this.pathPrefix, that.pathPrefix)
          && Objects.equal(this.getSourceModuleNames(), that.getSourceModuleNames());
    }
    return false;
  }

  public PathPrefix getPathPrefix() {
    return pathPrefix;
  }

  public SortedSet<String> getSourceModuleNames() {
    // Lazy initialization to avoid the set creation cost most of the time.
    if (sourceModuleNames == null) {
      sourceModuleNames = Sets.newTreeSet();
    }
    return sourceModuleNames;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(pathPrefix, getSourceModuleNames());
  }

  public void setPathPrefix(PathPrefix pathPrefix) {
    this.pathPrefix = pathPrefix;
  }
}
