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
import com.google.gwt.dev.util.PersistenceBackedObject;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableSet;

import java.util.Set;

/**
 * A do-nothing library writer for code paths that expect a library writer but which are running in
 * a context in which no library should to be built.
 */
public class NullLibraryWriter implements LibraryWriter {

  private Set<String> strings = ImmutableSet.of();

  @Override
  public void addBuildResource(Resource buildResource) {
  }

  @Override
  public void addCompilationUnit(CompilationUnit compilationUnit) {
  }

  @Override
  public void addDependencyLibraryName(String libraryName) {
  }

  @Override
  public void addDependencyLibraryNames(Set<String> dependencyLibraryNames) {
  }

  @Override
  public void addGeneratedArtifacts(ArtifactSet generatedArtifacts) {
  }

  @Override
  public void addPublicResource(Resource publicResource) {
  }

  @Override
  public PersistenceBackedObject<PermutationResult> getPermutationResultHandle() {
    return null;
  }

  @Override
  public Set<String> getProcessedReboundTypeSourceNames(String generatorName) {
    return null;
  }

  @Override
  public Set<String> getReboundTypeSourceNames() {
    return strings;
  }

  @Override
  public void markReboundTypeProcessed(String processedReboundTypeSourceName,
      String generatorName) {
  }

  @Override
  public void markReboundTypesProcessed(Set<String> reboundTypeSourceNames) {
  }

  @Override
  public void setLibraryName(String libraryName) {
  }

  @Override
  public void write() {
  }
}
