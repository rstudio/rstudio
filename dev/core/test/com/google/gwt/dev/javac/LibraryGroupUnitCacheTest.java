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
package com.google.gwt.dev.javac;

import com.google.gwt.dev.cfg.LibraryGroup;
import com.google.gwt.dev.cfg.MockLibrary;

import junit.framework.TestCase;

import java.util.List;

/**
 * Unit test for {@link LibraryGroupUnitCache}.
 */
public class LibraryGroupUnitCacheTest extends TestCase {

  private CompilationUnit barCompilationUnit =
      new MockCompilationUnit("com.example.Bar", "source1");
  private CompilationUnit bazCompilationUnit =
      new MockCompilationUnit("com.example.Baz", "source2");
  private CompilationUnit fooCompilationUnit =
      new MockCompilationUnit("com.example.Foo", "source3");

  public void testFindCompilationUnit() {
    String fooResourcePath =
        LibraryGroupUnitCache.typeNameToResourcePath(fooCompilationUnit.getTypeName());

    // Create random libraries with some compilation units.
    List<MockLibrary> libraries = MockLibrary.createRandomLibraryGraph(4, 1);
    libraries.get(0).addCompilationUnit(fooCompilationUnit);
    libraries.get(0).addSuperSourceCompilationUnit(barCompilationUnit);
    libraries.get(3).addCompilationUnit(bazCompilationUnit);

    // Wrap them up into a library group unit cache.
    LibraryGroupUnitCache libraryGroupUnitCache =
        new LibraryGroupUnitCache(LibraryGroup.fromLibraries(libraries, true));

    // Finds regular and super sourced compilation units using both resource path and content id
    // lookups.
    assertEquals(fooCompilationUnit, libraryGroupUnitCache.find(fooResourcePath));
    assertEquals(barCompilationUnit,
        libraryGroupUnitCache.find(new ContentId(barCompilationUnit.getTypeName(), "someHash")));
  }

  public void testNoSuchEntry() {
    String barResourcePath =
        LibraryGroupUnitCache.typeNameToResourcePath(barCompilationUnit.getTypeName());

    // Create random libraries with some compilation units.
    List<MockLibrary> libraries = MockLibrary.createRandomLibraryGraph(4, 1);
    libraries.get(0).addCompilationUnit(fooCompilationUnit);

    // Wrap them up into a library group unit cache.
    LibraryGroupUnitCache libraryGroupUnitCache =
        new LibraryGroupUnitCache(LibraryGroup.fromLibraries(libraries, true));

    // Verify that invalid lookups return a null.
    assertEquals(null, libraryGroupUnitCache.find(barResourcePath));
  }
}
