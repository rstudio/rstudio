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

import com.google.gwt.dev.javac.testing.impl.JavaResourceBase;
import com.google.gwt.dev.javac.testing.impl.MockResourceOracle;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import junit.framework.TestCase;

import java.util.List;

/**
 * Tests for CombinedResourceOracle.
 */
public class CombinedResourceOracleTest extends TestCase {

  private static LibraryGroupBuildResourceOracle createLibraryGroupBuildResourceOracle() {
    // Create random libraries with some build resources.
    List<MockLibrary> libraries = MockLibrary.createRandomLibraryGraph(4, 1);
    libraries.get(0).getBuildResourcePaths().add("User.gwt.xml");
    libraries.get(0).getBuildResourcePaths().add("Tab.ui.xml");
    libraries.get(3).getBuildResourcePaths().add("Core.gwt.xml");

    // Wrap them up into a library group.
    LibraryGroup libraryGroup = LibraryGroup.fromLibraries(libraries, true);

    // Put a resource oracle facade in front of that library group.
    return new LibraryGroupBuildResourceOracle(libraryGroup);
  }

  public void testRetrieveBuildResource() {
    // Create some independent resource oracles.
    LibraryGroupBuildResourceOracle buildResourceOracle = createLibraryGroupBuildResourceOracle();
    MockResourceOracle mockResourceOracle = new MockResourceOracle(JavaResourceBase.FOO);

    // Wrap them up in a combined resource oracle.
    CombinedResourceOracle combinedResourceOracle =
        new CombinedResourceOracle(buildResourceOracle, mockResourceOracle);

    // Verify that the combined resource oracle sees all of the build resource paths in the
    // libraries.
    assertEquals(Sets.newHashSet("test/Foo.java", "Tab.ui.xml", "User.gwt.xml", "Core.gwt.xml"),
        combinedResourceOracle.getPathNames());
  }
}
