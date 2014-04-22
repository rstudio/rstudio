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

import java.io.InputStream;
import java.util.Set;

/**
 * Encapsulates the data resulting from compilation of a single module, with the intention of
 * facilitating efficient separate compilation.<br />
 *
 * The contained data is a combination of raw output as well as indexes that represent compiler
 * state which would be expensive to recalculate.<br />
 *
 * Implementers are encouraged to load their contents lazily to minimize work.
 */
// TODO(stalcup): refactor UnifyAst to only perform binary name based lookups so that libraries
// don't need to index compilation units by both source and binary name
public interface Library {

  /**
   * Closes all read streams.
   */
  void close();

  /**
   * Returns a resource handle or null for the provided path.
   */
  Resource getBuildResourceByPath(String buildResourcePath);

  /**
   * Returns the set of paths of build resources. Facilitates LibraryGroup's fast single resource
   * retrieval across large groups of provided libraries.
   */
  Set<String> getBuildResourcePaths();

  /**
   * Returns a class file input stream or null for the provided path.
   */
  InputStream getClassFileStream(String classFilePath);

  /**
   * Returns the compilation unit containing the type with the given binary name. The returned
   * compilation unit might be regular or might be super sourced depending on which was stored
   * during library construction.
   */
  CompilationUnit getCompilationUnitByTypeBinaryName(String typeBinaryName);

  /**
   * Returns the compilation unit containing the type with the given source name. The returned
   * compilation unit might be regular or might be super sourced depending on which was stored
   * during library construction.
   */
  CompilationUnit getCompilationUnitByTypeSourceName(String typeSourceName);

  /**
   * Returns the set of names of dependency libraries. Facilitates LibraryGroup's library tree
   * analysis for link ordering and partial generator execution.
   */
  Set<String> getDependencyLibraryNames();

  /**
   * Returns the set of artifacts that were created by generators when compiling this library.
   */
  ArtifactSet getGeneratedArtifacts();

  /**
   * Returns the name of the library. Should be unique within the library dependency tree.
   */
  String getLibraryName();

  /**
   * Returns a mapping from compilation unit type source name to a list of nested type binary names.
   */
  Multimap<String, String> getNestedBinaryNamesByCompilationUnitName();

  /**
   * Returns a mapping from compilation unit type source name to a list of nested type source names.
   */
  Multimap<String, String> getNestedSourceNamesByCompilationUnitName();

  /**
   * Returns a handle to the serialized permutation result of this library. Final linking relies on
   * the permutation result contents.
   */
  // TODO(stalcup): refactor PersistenceBackedObject name to PersistedObjectHandle or remove it
  // completely
  PersistenceBackedObject<PermutationResult> getPermutationResultHandle();

  /**
   * Returns a mapping from generator name to the set of source names of types that have been
   * processed by that generator in this library.
   */
  Multimap<String, String> getProcessedReboundTypeSourceNamesByGenerator();

  /**
   * Returns a resource handle or null for the provided path.
   */
  Resource getPublicResourceByPath(String path);

  /**
   * Returns the set of paths of public resources. Facilitates LibraryGroup's fast single resource
   * retrieval across large groups of provided libraries.
   */
  Set<String> getPublicResourcePaths();

  /**
   * Returns the set of source names of types which are the subject of GWT.create() calls in source
   * code for this library. This list of types is needed for generator execution and reconstructing
   * this list from source would be very costly.
   */
  Set<String> getReboundTypeSourceNames();

  /**
   * Returns the set of regular (non-super-source) class file paths. Facilitates LibraryGroup's fast
   * single class file retrieval across large groups of provided libraries.
   */
  Set<String> getRegularClassFilePaths();

  /**
   * Returns the set of regular (non-super-source) compilation unit type source names. Facilitates
   * LibraryGroup's fast single compilation unit retrieval across large groups of provided
   * libraries.
   */
  Set<String> getRegularCompilationUnitTypeSourceNames();

  /**
   * Returns the set of super source class file paths. Facilitates LibraryGroup's fast single class
   * file retrieval across large groups of provided libraries and makes possible the prioritization
   * of super source over regular class files.
   */
  Set<String> getSuperSourceClassFilePaths();

  /**
   * Returns the set of super source compilation unit type source names. Facilitates LibraryGroup's
   * fast compilation unit retrieval across large groups of provided libraries and makes possible
   * the prioritization of super source over regular compilation units.
   */
  Set<String> getSuperSourceCompilationUnitTypeSourceNames();
}
