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

import com.google.gwt.thirdparty.guava.common.collect.Sets;

import junit.framework.TestCase;

import java.util.List;

/**
 * Tests for LibraryGroupBuildResourceOracle.
 */
public class LibraryGroupBuildResourceOracleTest extends TestCase {

  public void testRetrieveBuildResource() {
    // Create random libraries with some build resources.
    List<MockLibrary> libraries = MockLibrary.createRandomLibraryGraph(4, 1);
    libraries.get(0).getBuildResourcePaths().add("User.gwt.xml");
    libraries.get(0).getBuildResourcePaths().add("Tab.ui.xml");
    libraries.get(3).getBuildResourcePaths().add("Core.gwt.xml");

    // Wrap them up into a library group.
    LibraryGroup libraryGroup = LibraryGroup.fromLibraries(libraries, true);

    // Put a resource oracle facade in front of that library group.
    LibraryGroupBuildResourceOracle buildResourceOracle =
        new LibraryGroupBuildResourceOracle(libraryGroup);

    // Verify that the build resource oracle sees all of the build resource paths in the libraries.
    assertEquals(Sets.newHashSet("Tab.ui.xml", "User.gwt.xml", "Core.gwt.xml"),
        buildResourceOracle.getPathNames());
  }
}
