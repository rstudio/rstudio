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

import static com.google.gwt.dev.resource.impl.ResourceOracleImplTest.assertResourcesEqual;

/**
 * Tests {@link ResourceOracleImpl} using the real class path.
 * 
 */
public class ResourceOracleImplRealClasspathTest extends
    AbstractResourceOrientedTestBase {

  private static PathPrefix makeJunitPrefix() {
    return new PathPrefix("junit/framework/", new ResourceFilter() {
      public boolean allows(String path) {
        return path.endsWith("TestCase.class");
      }
    });
  }

  private static PathPrefix makeThisClassPrefix() {
    return new PathPrefix("com/google/gwt/dev/resource/impl/",
        new ResourceFilter() {
          public boolean allows(String path) {
            return path.endsWith("ResourceOracleImplRealClasspathTest.class");
          }
        });
  }

  private static PathPrefix makeThisClassPrefixPlus() {
    return new PathPrefix("com/google/gwt/dev/resource/impl/",
        new ResourceFilter() {
          public boolean allows(String path) {
            return path.endsWith("ResourceOracleImpl.class")
                || path.endsWith("ResourceOracleImplRealClasspathTest.class");
          }
        });
  }

  private final TreeLogger logger = createTestTreeLogger();

  private final ResourceOracleImpl resourceOracle = new ResourceOracleImpl(
      logger);

  public void testBasic() {
    PathPrefixSet pathPrefixSet = new PathPrefixSet();
    pathPrefixSet.add(makeJunitPrefix());
    pathPrefixSet.add(makeThisClassPrefix());
    resourceOracle.setPathPrefixes(pathPrefixSet);
    ResourceOracleImpl.refresh(logger, resourceOracle);
    Map<String, Resource> resourceMap = resourceOracle.getResourceMap();
    assertEquals(2, resourceMap.size());
  }

  public void testRefresh() {
    PathPrefixSet pathPrefixSet = new PathPrefixSet();
    pathPrefixSet.add(makeJunitPrefix());
    pathPrefixSet.add(makeThisClassPrefix());
    resourceOracle.setPathPrefixes(pathPrefixSet);
    ResourceOracleImpl.refresh(logger, resourceOracle);
    Map<String, Resource> resourceMap = resourceOracle.getResourceMap();
    assertEquals(2, resourceMap.size());

    // Plain refresh should have no effect.
    ResourceOracleImpl.refresh(logger, resourceOracle);
    assertResourcesEqual(resourceMap, resourceOracle.getResourceMap());

    // Setting same path entries should have no effect.
    resourceOracle.setPathPrefixes(pathPrefixSet);
    ResourceOracleImpl.refresh(logger, resourceOracle);
    assertResourcesEqual(resourceMap, resourceOracle.getResourceMap());

    // Setting identical path entries should have no effect.
    pathPrefixSet = new PathPrefixSet();
    pathPrefixSet.add(makeJunitPrefix());
    pathPrefixSet.add(makeThisClassPrefix());
    resourceOracle.setPathPrefixes(pathPrefixSet);
    ResourceOracleImpl.refresh(logger, resourceOracle);
    assertResourcesEqual(resourceMap, resourceOracle.getResourceMap());

    // Setting identical result should have no effect.
    pathPrefixSet.add(makeJunitPrefix());
    ResourceOracleImpl.refresh(logger, resourceOracle);
    assertResourcesEqual(resourceMap, resourceOracle.getResourceMap());

    // Actually change the working set.
    pathPrefixSet.add(makeThisClassPrefixPlus());
    ResourceOracleImpl.refresh(logger, resourceOracle);
    assertEquals(3, resourceOracle.getResourceMap().size());
  }
}
