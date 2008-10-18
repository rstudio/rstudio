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

import java.util.Set;

/**
 * A location that acts as a starting point for finding resources
 * {@link ResourceOracleImpl}.
 */
public abstract class ClassPathEntry {

  /**
   * Finds every resource at abstract path P within this classpath such that P
   * begins with a prefix X from the path prefix set and P is allowed by the
   * filter associated with X.
   * 
   * @return a set of zero or more resources; note no guarantees are made
   *         regarding the identities of the returned resource objects, and the
   *         same object may be returned across multiple calls
   */
  public abstract Set<AbstractResource> findApplicableResources(
      TreeLogger logger, PathPrefixSet pathPrefixSet);

  /**
   * Gets a URL string that describes this class path entry.
   */
  public abstract String getLocation();

  @Override
  public String toString() {
    return getClass().getSimpleName() + ": " + getLocation();
  }
}
