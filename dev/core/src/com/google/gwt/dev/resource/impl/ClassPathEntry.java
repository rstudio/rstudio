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
package com.google.gwt.dev.resource.impl;

import com.google.gwt.core.ext.TreeLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A location that acts as a starting point for finding resources
 * {@link ResourceOracleImpl}.
 */
public abstract class ClassPathEntry {

  @Override
  public final boolean equals(Object other) {
    if (other instanceof ClassPathEntry) {
      ClassPathEntry otherCpe = (ClassPathEntry) other;
      boolean ret = getLocation().equals(otherCpe.getLocation());
      // The concrete class should not differ if the location is equal
      assert (ret ? getClass() == otherCpe.getClass() : true);
      return ret;
    } else {
      return false;
    }
  }
  
  /**
   * Finds applicable resources for a list of pathPrefixSets, returning a
   * distinct answer for each set.
   * 
   * @see #findApplicableResources(TreeLogger, PathPrefixSet)
   */
  public List<Map<AbstractResource, PathPrefix>> findApplicableResources(
      TreeLogger logger, List<PathPrefixSet> pathPrefixSets) {
    List<Map<AbstractResource, PathPrefix>> results = new ArrayList<
        Map<AbstractResource, PathPrefix>>(pathPrefixSets.size());
    for (PathPrefixSet pathPrefixSet : pathPrefixSets) {
      results.add(findApplicableResources(logger, pathPrefixSet));
    }
    return results;
  }

  /**
   * Finds every resource at abstract path P within this classpath such that P
   * begins with a prefix X from the path prefix set and P is allowed by the
   * filter associated with X.
   * 
   * @return a map with key as an allowed resource and value as the PathPrefix
   *         that allows the resource; note no guarantees are made regarding the
   *         identities of the returned resource objects, and the same object
   *         may be returned across multiple calls
   */
  public abstract Map<AbstractResource, PathPrefix> findApplicableResources(
      TreeLogger logger, PathPrefixSet pathPrefixSet);

  /**
   * Gets a URL string that describes this class path entry.
   * 
   * ClassPathEntries with the same location string are considered equal.
   */
  public abstract String getLocation();
  
  @Override
  public final int hashCode() {
    return getLocation().hashCode();
  }
  
  @Override
  public String toString() {
    return getClass().getSimpleName() + ": " + getLocation();
  }
}
