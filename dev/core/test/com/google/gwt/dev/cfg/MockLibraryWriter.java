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
import com.google.gwt.dev.jjs.PermutationResult;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.util.ZipEntryBackedObject;
import com.google.gwt.thirdparty.guava.common.collect.HashMultimap;
import com.google.gwt.thirdparty.guava.common.collect.SetMultimap;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.Set;

/**
 * A mock and in memory library writer for setting up test situations.
 */
public class MockLibraryWriter implements LibraryWriter {

  private Set<String> buildResourcePaths = Sets.newHashSet();
  private Set<String> dependencyLibraryNames = Sets.newHashSet();
  private String libraryName;
  private SetMultimap<String, String> processedReboundTypeSourceNamesByGenerator =
      HashMultimap.create();
  private Set<String> reboundTypeNames;

  @Override
  public void addBuildResource(Resource buildResource) {
    buildResourcePaths.add(buildResource.getPath());
  }

  @Override
  public void addCompilationUnit(CompilationUnit compilationUnit) {
  }

  @Override
  public void addDependencyLibraryName(String libraryName) {
    dependencyLibraryNames.add(libraryName);
  }

  @Override
  public void addDependencyLibraryNames(Set<String> dependencyLibraryNames) {
    this.dependencyLibraryNames.addAll(dependencyLibraryNames);
  }

  @Override
  public void addGeneratedArtifacts(ArtifactSet generatedArtifacts) {
  }

  @Override
  public void addPublicResource(Resource publicResource) {
  }

  public Set<String> getBuildResourcePaths() {
    return buildResourcePaths;
  }

  public Set<String> getDependencyLibraryNames() {
    return dependencyLibraryNames;
  }

  public String getLibraryName() {
    return libraryName;
  }

  @Override
  public ZipEntryBackedObject<PermutationResult> getPermutationResultHandle() {
    return null;
  }

  @Override
  public Set<String> getProcessedReboundTypeSourceNames(String generatorName) {
    return processedReboundTypeSourceNamesByGenerator.get(generatorName);
  }

  @Override
  public Set<String> getReboundTypeSourceNames() {
    return reboundTypeNames;
  }

  @Override
  public void markReboundTypeProcessed(String processedReboundTypeSourceName,
      String generatorName) {
    processedReboundTypeSourceNamesByGenerator.put(generatorName, processedReboundTypeSourceName);
  }

  @Override
  public void markReboundTypesProcessed(Set<String> reboundTypeSourceNames) {
    this.reboundTypeNames = reboundTypeSourceNames;
  }

  @Override
  public void setLibraryName(String libraryName) {
    this.libraryName = libraryName;
  }

  @Override
  public void write() {
  }
}
