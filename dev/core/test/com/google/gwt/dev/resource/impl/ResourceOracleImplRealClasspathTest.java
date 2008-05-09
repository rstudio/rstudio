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

import java.util.Map;

/**
 * Tests {@link ResourceOracleImpl} using the real class path.
 * 
 */
public class ResourceOracleImplRealClasspathTest extends
    AbstractResourceOrientedTestBase {

  private static final PathPrefix JUNIT_PREFIX = new PathPrefix(
      "junit/framework/", new ResourceFilter() {
        public boolean allows(String path) {
          return path.endsWith("TestCase.class");
        }
      });
  private static final PathPrefix JUNIT_PREFIX_DUP = new PathPrefix(
      "junit/framework/", new ResourceFilter() {
        public boolean allows(String path) {
          return path.endsWith("TestCase.class");
        }
      });

  private static final PathPrefix THIS_CLASS_PREFIX = new PathPrefix(
      "com/google/gwt/dev/resource/impl/", new ResourceFilter() {
        public boolean allows(String path) {
          return path.endsWith("ResourceOracleImplRealClasspathTest.class");
        }
      });

  private static final PathPrefix THIS_CLASS_PREFIX_PLUS = new PathPrefix(
      "com/google/gwt/dev/resource/impl/", new ResourceFilter() {
        public boolean allows(String path) {
          return path.endsWith("ResourceOracleImpl.class")
              || path.endsWith("ResourceOracleImplRealClasspathTest.class");
        }
      });

  private final TreeLogger logger = createTestTreeLogger();
  private final ResourceOracleImpl resourceOracle = new ResourceOracleImpl(
      logger);

  public void testBasic() {
    PathPrefixSet pathPrefixSet = new PathPrefixSet();
    pathPrefixSet.add(JUNIT_PREFIX);
    pathPrefixSet.add(THIS_CLASS_PREFIX);
    resourceOracle.setPathPrefixes(pathPrefixSet);
    resourceOracle.refresh(logger);
    Map<String, Resource> resourceMap = resourceOracle.getResourceMap();
    assertEquals(2, resourceMap.size());
  }

  public void testRefresh() {
    PathPrefixSet pathPrefixSet = new PathPrefixSet();
    pathPrefixSet.add(JUNIT_PREFIX);
    pathPrefixSet.add(THIS_CLASS_PREFIX);
    resourceOracle.setPathPrefixes(pathPrefixSet);
    resourceOracle.refresh(logger);
    Map<String, Resource> resourceMap = resourceOracle.getResourceMap();
    assertEquals(2, resourceMap.size());

    // Plain refresh should have no effect.
    resourceOracle.refresh(logger);
    assertSame(resourceMap, resourceOracle.getResourceMap());

    // Setting same path entries should have no effect.
    resourceOracle.setPathPrefixes(pathPrefixSet);
    resourceOracle.refresh(logger);
    assertSame(resourceMap, resourceOracle.getResourceMap());

    // Setting identical path entries should have no effect.
    pathPrefixSet = new PathPrefixSet();
    pathPrefixSet.add(JUNIT_PREFIX);
    pathPrefixSet.add(THIS_CLASS_PREFIX);
    resourceOracle.setPathPrefixes(pathPrefixSet);
    resourceOracle.refresh(logger);
    assertSame(resourceMap, resourceOracle.getResourceMap());

    // Setting identical result should have no effect.
    pathPrefixSet.add(JUNIT_PREFIX_DUP);
    resourceOracle.refresh(logger);
    assertSame(resourceMap, resourceOracle.getResourceMap());

    // Actually change the working set.
    pathPrefixSet.add(THIS_CLASS_PREFIX_PLUS);
    resourceOracle.refresh(logger);
    Map<String, Resource> newResourceMap = resourceOracle.getResourceMap();
    assertNotSame(resourceMap, newResourceMap);
    assertEquals(3, newResourceMap.size());
  }
}
