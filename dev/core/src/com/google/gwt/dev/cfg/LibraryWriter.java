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
import com.google.gwt.thirdparty.guava.common.collect.Multimap;

import java.util.Set;

/**
 * A builder which collects and writes the contents of a library.<br />
 *
 * It also provides access to a very small subset of properties for LibraryGroup access.
 */
public interface LibraryWriter {

  /**
   * Adds a build resource. Build resources are any resources provided in source paths but which are
   * not themselves source code. For example *.gwt.xml and *.ui.xml files.
   */
  void addBuildResource(Resource buildResource);

  /**
   * Adds a previously compiled (but not yet unified) compilation unit.
   */
  void addCompilationUnit(CompilationUnit compilationUnit);

  /**
   * Registers a dependency on some other library.
   */
  void addDependencyLibraryName(String dependencyLibraryName);

  /**
   * Registers dependency on a set of other libraries.
   */
  void addDependencyLibraryNames(Set<String> dependencyLibraryNames);

  /**
   * Adds a generated artifact. Artifacts created by generators need to be collected so that they
   * can be provided to the final linker.
   */
  void addGeneratedArtifacts(ArtifactSet generatedArtifacts);

  /**
   * Registers newly legal values on some binding property.
   */
  void addNewBindingPropertyValuesByName(String propertyName, Iterable<String> propertyValues);

  /**
   * Sets the newly current value(s) on some configuration property.
   */
  void addNewConfigurationPropertyValuesByName(
      String propertyName, Iterable<String> propertyValues);

  /**
   * Adds a public resource (such as a html, css, or png file).
   */
  void addPublicResource(Resource publicResource);

  /**
   * Marks a generator as having been executed for this library and all sub libraries.
   */
  void addRanGeneratorName(String generatorName);

  /**
   * Returns the map of binding property names to newly legal values.
   */
  Multimap<String, String> getNewBindingPropertyValuesByName();

  /**
   * Returns a map of configuration property names to newly set value(s).
   */
  Multimap<String, String> getNewConfigurationPropertyValuesByName();

  /**
   * Returns a handle to the permutation result object that was constructed as part of the
   * compilation for this library.
   */
  PersistenceBackedObject<PermutationResult> getPermutationResultHandle();

  /**
   * Returns the set of names of types which are the subject of GWT.create() calls in source code
   * for this library.
   */
  Set<String> getReboundTypeNames();

  /**
   * Records the library name.<br />
   *
   * Library names are the way that libraries reference one another as dependencies and should be
   * unique within the build tree.
   */
  void setLibraryName(String libraryName);

  /**
   * Records the set of names of types which are the subject of GWT.create() calls in source code
   * for this library.
   */
  void setReboundTypeNames(Set<String> reboundTypeNames);

  /**
   * Finishes writing all library contents and closes the library.
   */
  void write();
}
