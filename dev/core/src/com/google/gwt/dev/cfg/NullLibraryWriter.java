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
import com.google.gwt.thirdparty.guava.common.collect.ImmutableSet;
import com.google.gwt.thirdparty.guava.common.collect.Multimap;

import java.util.Set;

/**
 * A do-nothing library writer for code paths that expect a library writer but
 * which are running in a context in which no library should to be built.
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
  public void addNewBindingPropertyValuesByName(
      String propertyName, Iterable<String> propertyValues) {
  }

  @Override
  public void addNewConfigurationPropertyValuesByName(
      String propertyName, Iterable<String> propertyValues) {
  }

  @Override
  public void addPublicResource(Resource publicResource) {
  }

  @Override
  public void addRanGeneratorName(String generatorName) {
  }

  @Override
  public Multimap<String, String> getNewBindingPropertyValuesByName() {
    return null;
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
  public Set<String> getReboundTypeNames() {
    return strings;
  }

  @Override
  public void setLibraryName(String libraryName) {
  }

  @Override
  public void setReboundTypeNames(Set<String> reboundTypeNames) {
  }

  @Override
  public void write() {
  }
}
