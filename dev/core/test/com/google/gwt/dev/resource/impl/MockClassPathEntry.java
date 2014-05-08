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
import com.google.gwt.dev.resource.Resource;

import junit.framework.Assert;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Mock for {@link ClassPathEntry}.
 */
public class MockClassPathEntry extends ClassPathEntry {

  final String pathRoot;
  private final Map<String, MockAbstractResource> resourceMap =
      new HashMap<String, MockAbstractResource>();

  /**
   * By default, MockClassPathEntry has an all-inclusive path prefix. Tests may
   * change it by calling {@link #setPathPrefixes(PathPrefixSet)}.
   */
  public MockClassPathEntry(String pathRoot) {
    this.pathRoot = pathRoot;
  }

  public void addResource(String resourcePath) {
    Resource old = resourceMap.get(resourcePath);
    Assert.assertNull(
        "resource already exists; use updateResource() to replace", old);
    resourceMap.put(resourcePath, createMockResource(resourcePath));
  }

  @Override
  public Map<AbstractResource, ResourceResolution> findApplicableResources(
      TreeLogger logger, PathPrefixSet pathPrefixes) {
    // Only include resources that have the prefix and pass its filter.
    Map<AbstractResource, ResourceResolution> results =
        new IdentityHashMap<AbstractResource, ResourceResolution>();
    for (Map.Entry<String, MockAbstractResource> entry : resourceMap.entrySet()) {
      String path = entry.getKey();
      ResourceResolution resourceResolution = null;
      if ((resourceResolution = pathPrefixes.includesResource(path)) != null) {
        results.put(entry.getValue(), resourceResolution);
      }
    }

    return results;
  }

  @Override
  public String getLocation() {
    return pathRoot;
  }

  public void removeResource(String resourcePath) {
    Resource old = resourceMap.get(resourcePath);
    Assert.assertNotNull(
        "resource does not already exists; use addResource() to add it first",
        old);
    resourceMap.remove(resourcePath);
  }

  public void updateResource(String resourcePath) {
    MockAbstractResource old = resourceMap.get(resourcePath);
    Assert.assertNotNull(
        "resource does not already exists; use addResource() if you were trying to add", old);
    resourceMap.put(resourcePath, createMockResource(resourcePath));
  }

  private MockAbstractResource createMockResource(final String path) {
    return new MockAbstractResource(this, path);
  }

}
