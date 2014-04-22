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

import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.dev.javac.CompilationUnit;
import com.google.gwt.dev.javac.CompiledClass;
import com.google.gwt.dev.jjs.PermutationResult;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.util.Name.InternalName;
import com.google.gwt.dev.util.ZipEntryBackedObject;
import com.google.gwt.thirdparty.guava.common.collect.HashMultimap;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Multimap;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * A mock and in memory library for setting up test situations.
 */
public class MockLibrary implements Library {

  public static List<MockLibrary> createRandomLibraryGraph(
      int libraryCount, int maxParentsPerChild) {
    Random rng = new Random();
    List<MockLibrary> libraries = Lists.newArrayList();
    libraries.add(new MockLibrary("RootLibrary"));
    for (int libraryIndex = 0; libraryIndex < libraryCount; libraryIndex++) {
      MockLibrary childLibrary = new MockLibrary("Library-" + libraryIndex);
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
  private Multimap<String, String> compilationUnitNamesByNestedBinaryName = HashMultimap.create();
  private Multimap<String, String> compilationUnitNamesByNestedSourceName = HashMultimap.create();
  private Map<String, CompilationUnit> compilationUnitsByTypeName = Maps.newHashMap();
  private Set<String> compilationUnitTypeNames = Sets.newHashSet();
  private Set<String> dependencyLibraryNames = Sets.newLinkedHashSet();
  private String libraryName;
  private Multimap<String, String> nestedBinaryNamesByCompilationUnitName = HashMultimap.create();
  private Multimap<String, String> nestedSourceNamesByCompilationUnitName = HashMultimap.create();
  private Multimap<String, String> processedReboundTypeSourceNamesByGenerator =
      HashMultimap.create();
  private Set<String> reboundTypeNames = Sets.newHashSet();
  private Set<String> superSourceCompilationUnitTypeNames = Sets.newHashSet();

  public MockLibrary(String libraryName) {
    this.libraryName = libraryName;
  }

  public void addCompilationUnit(CompilationUnit compilationUnit) {
    String compilationUnitTypeSourceName = compilationUnit.getTypeName();
    compilationUnitsByTypeName.put(compilationUnitTypeSourceName, compilationUnit);
    compilationUnitTypeNames.add(compilationUnitTypeSourceName);

    Collection<CompiledClass> compiledClasses = compilationUnit.getCompiledClasses();
    for (CompiledClass compiledClass : compiledClasses) {
      String sourceName = compiledClass.getSourceName();
      String binaryName = InternalName.toBinaryName(compiledClass.getInternalName());
      nestedSourceNamesByCompilationUnitName.put(compilationUnitTypeSourceName, sourceName);
      nestedBinaryNamesByCompilationUnitName.put(compilationUnitTypeSourceName, binaryName);
      compilationUnitNamesByNestedSourceName.put(sourceName, compilationUnitTypeSourceName);
      compilationUnitNamesByNestedBinaryName.put(binaryName, compilationUnitTypeSourceName);
    }
  }

  public void addSuperSourceCompilationUnit(CompilationUnit superSourceCompilationUnit) {
    String superSourceCompilationUnitTypeSourceName = superSourceCompilationUnit.getTypeName();
    compilationUnitsByTypeName.put(superSourceCompilationUnitTypeSourceName,
        superSourceCompilationUnit);
    compilationUnitTypeNames.add(superSourceCompilationUnitTypeSourceName);

    Collection<CompiledClass> compiledClasses = superSourceCompilationUnit.getCompiledClasses();
    for (CompiledClass compiledClass : compiledClasses) {
      String sourceName = compiledClass.getSourceName();
      String binaryName = InternalName.toBinaryName(compiledClass.getInternalName());
      nestedSourceNamesByCompilationUnitName.put(superSourceCompilationUnitTypeSourceName,
          sourceName);
      nestedBinaryNamesByCompilationUnitName.put(superSourceCompilationUnitTypeSourceName,
          binaryName);
      compilationUnitNamesByNestedSourceName.put(sourceName,
          superSourceCompilationUnitTypeSourceName);
      compilationUnitNamesByNestedBinaryName.put(binaryName,
          superSourceCompilationUnitTypeSourceName);
    }
  }

  @Override
  public void close() {
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
  public InputStream getClassFileStream(String classFilePath) {
    return null;
  }

  @Override
  public CompilationUnit getCompilationUnitByTypeBinaryName(String typeBinaryName) {
    // Convert nested binary name to enclosing type source name.
    String typeSourceName =
        compilationUnitNamesByNestedBinaryName.get(typeBinaryName).iterator().next();
    return compilationUnitsByTypeName.get(typeSourceName);
  }

  @Override
  public CompilationUnit getCompilationUnitByTypeSourceName(String typeSourceName) {
    // Convert nested source name to enclosing type source name.
    typeSourceName = compilationUnitNamesByNestedSourceName.get(typeSourceName).iterator().next();
    return compilationUnitsByTypeName.get(typeSourceName);
  }

  @Override
  public Set<String> getDependencyLibraryNames() {
    return dependencyLibraryNames;
  }

  @Override
  public ArtifactSet getGeneratedArtifacts() {
    return null;
  }

  @Override
  public String getLibraryName() {
    return libraryName;
  }

  @Override
  public Multimap<String, String> getNestedBinaryNamesByCompilationUnitName() {
    return nestedBinaryNamesByCompilationUnitName;
  }

  @Override
  public Multimap<String, String> getNestedSourceNamesByCompilationUnitName() {
    return nestedSourceNamesByCompilationUnitName;
  }

  @Override
  public ZipEntryBackedObject<PermutationResult> getPermutationResultHandle() {
    return null;
  }

  @Override
  public Multimap<String, String> getProcessedReboundTypeSourceNamesByGenerator() {
    return processedReboundTypeSourceNamesByGenerator;
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
  public Set<String> getRegularClassFilePaths() {
    return null;
  }

  @Override
  public Set<String> getRegularCompilationUnitTypeSourceNames() {
    return compilationUnitTypeNames;
  }

  @Override
  public Set<String> getReboundTypeSourceNames() {
    return reboundTypeNames;
  }

  @Override
  public Set<String> getSuperSourceClassFilePaths() {
    return null;
  }

  @Override
  public Set<String> getSuperSourceCompilationUnitTypeSourceNames() {
    return superSourceCompilationUnitTypeNames;
  }

  @Override
  public String toString() {
    return libraryName;
  }
}
