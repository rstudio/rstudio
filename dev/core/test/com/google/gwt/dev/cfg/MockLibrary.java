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

import com.google.gwt.dev.javac.CompilationUnit;
import com.google.gwt.dev.jjs.PermutationResult;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.util.ZipEntryBackedObject;
import com.google.gwt.thirdparty.guava.common.collect.ArrayListMultimap;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Multimap;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

class MockLibrary implements Library {

  static List<Library> createRandomLibraryGraph(int libraryCount, int maxParentsPerChild) {
    Random rng = new Random();
    List<Library> libraries = Lists.newArrayList();
    libraries.add(new MockLibrary("RootLibrary"));
    for (int libraryIndex = 0; libraryIndex < libraryCount; libraryIndex++) {
      Library childLibrary = new MockLibrary("Library-" + libraryIndex);
      int parentCount = rng.nextInt(maxParentsPerChild) + 1;

      for (int parentIndex = 0; parentIndex < parentCount; parentIndex++) {
        Library parentLibrary = libraries.get(rng.nextInt(libraries.size()));
        parentLibrary.getDependencyLibraryNames().add(childLibrary.getLibraryName());
      }
      libraries.add(childLibrary);
    }
    Collections.shuffle(libraries);
    return libraries;
  }

  private Set<String> buildResourcePaths = Sets.newHashSet();
  private Set<String> dependencyLibraryNames = Sets.newLinkedHashSet();
  private String libraryName;
  private Multimap<String, String> newBindingPropertyValuesByName = ArrayListMultimap.create();
  private Set<String> ranGeneratorNames = Sets.newHashSet();
  private Set<String> reboundTypeNames = Sets.newHashSet();

  public MockLibrary(String libraryName) {
    this.libraryName = libraryName;
  }

  @Override
  public Resource getBuildResourceByPath(String path) {
    return null;
  }

  @Override
  public Set<String> getBuildResourcePaths() {
    return buildResourcePaths;
  }

  @Override
  public Set<String> getClassFilePaths() {
    return null;
  }

  @Override
  public InputStream getClassFileStream(String classFilePath) {
    return null;
  }

  @Override
  public CompilationUnit getCompilationUnitByTypeName(String typeName) {
    return null;
  }

  @Override
  public Set<String> getCompilationUnitTypeNames() {
    return null;
  }

  @Override
  public Set<String> getDependencyLibraryNames() {
    return dependencyLibraryNames;
  }

  @Override
  public String getLibraryName() {
    return libraryName;
  }

  @Override
  public Multimap<String, String> getNewBindingPropertyValuesByName() {
    return newBindingPropertyValuesByName;
  }

  @Override
  public Multimap<String, String> getNewConfigurationPropertyValuesByName() {
    return null;
  }

  @Override
  public ZipEntryBackedObject<PermutationResult> getPermutationResultHandle() {
    return null;
  }

  @Override
  public Resource getPublicResourceByPath(String path) {
    return null;
  }

  @Override
  public Set<String> getPublicResourcePaths() {
    return null;
  }

  @Override
  public Set<String> getRanGeneratorNames() {
    return ranGeneratorNames;
  }

  @Override
  public Set<String> getReboundTypeNames() {
    return reboundTypeNames;
  }

  @Override
  public Set<String> getSuperSourceClassFilePaths() {
    return null;
  }

  @Override
  public Set<String> getSuperSourceCompilationUnitTypeNames() {
    return null;
  }

  @Override
  public String toString() {
    return libraryName;
  }
}
