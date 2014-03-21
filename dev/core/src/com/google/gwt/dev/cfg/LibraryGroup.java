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
import com.google.gwt.dev.cfg.Libraries.IncompatibleLibraryVersionException;
import com.google.gwt.dev.javac.CompilationUnit;
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.PermutationResult;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.util.PersistenceBackedObject;
import com.google.gwt.thirdparty.guava.common.base.Function;
import com.google.gwt.thirdparty.guava.common.base.Predicates;
import com.google.gwt.thirdparty.guava.common.collect.HashMultimap;
import com.google.gwt.thirdparty.guava.common.collect.Iterables;
import com.google.gwt.thirdparty.guava.common.collect.LinkedHashMultimap;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Multimap;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A convenience wrapper around a set of libraries.<br />
 *
 * Indexes library contents to enable single step retrieval of resources by name.<br />
 *
 * Analyzes the library dependency tree to provide link ordering and property and rebound type
 * change analysis for partial generator execution.<br />
 *
 * Combines library resource lists for easy iteration over the complete set.<br />
 *
 * Is mostly immutable, its contained set of libraries can only be set at time of construction.
 */
public class LibraryGroup {

  /**
   * An exception that indicates that a single Compilation Unit is being provided by more than one
   * library.
   */
  public static class CollidingCompilationUnitException extends InternalCompilerException {

    public CollidingCompilationUnitException(String message) {
      super(message);
    }
  }

  /**
   * An exception that indicates that more than one library has the same name, thus making name
   * based references ambiguous.
   */
  public static class DuplicateLibraryNameException extends InternalCompilerException {

    public DuplicateLibraryNameException(String message) {
      super(message);
    }
  }

  /**
   * An exception that indicates that some library was referenced as a dependency but was not
   * provided to the compiler.
   */
  public static class UnresolvedLibraryException extends InternalCompilerException {

    public UnresolvedLibraryException(String message) {
      super(message);
    }
  }

  /**
   * Factory function that constructs and returns a library group from a list of libraries.
   */
  public static LibraryGroup fromLibraries(
      List<? extends Library> libraries, boolean verifyLibraryReferences) {
    LibraryGroup libraryGroup = new LibraryGroup();
    for (Library library : libraries) {
      libraryGroup.libraries.add(library);
    }
    libraryGroup.buildLibraryIndexes(verifyLibraryReferences);
    return libraryGroup;
  }

  /**
   * Factory function that constructs and returns a library group from a list of zip library paths.
   */
  public static LibraryGroup fromZipPaths(List<String> zipLibraryPaths)
      throws IncompatibleLibraryVersionException {
    List<Library> zipLibraries = Lists.newArrayList();
    for (String zipLibraryPath : zipLibraryPaths) {
      zipLibraries.add(new ZipLibrary(zipLibraryPath));
    }
    return fromLibraries(zipLibraries, true);
  }

  private Set<String> compilationUnitTypeSourceNames;
  private ArtifactSet generatedArtifacts;
  private List<Library> libraries = Lists.newArrayList();
  private Map<String, Library> librariesByBuildResourcePath;
  private Map<String, Library> librariesByClassFilePath;
  private Map<String, Library> librariesByCompilationUnitTypeBinaryName;
  private Map<String, Library> librariesByCompilationUnitTypeSourceName;
  private Map<String, Library> librariesByName;
  private Map<String, Library> librariesByPublicResourcePath;
  private List<PersistenceBackedObject<PermutationResult>> permutationResultHandles;
  private Set<String> reboundTypeSourceNames;
  private List<Library> rootLibraries;

  private Set<String> superSourceCompilationUnitTypeSourceNames;

  protected LibraryGroup() {
    // Private to force class construction via one of the public factory functions.
  }

  /**
   * Returns whether a build resource is is available at the given path.
   */
  public boolean containsBuildResource(String buildResourcePath) {
    return getLibrariesByBuildResourcePath().containsKey(buildResourcePath);
  }

  /**
   * Create and return a library group of just the given libraries referenced by name. Useful for
   * taking an overly broad library group of all libraries in a dependency tree and trimming it down
   * to a restricted set of strict top level dependencies.
   */
  public LibraryGroup createSubgroup(List<String> libraryNames) {
    return fromLibraries(getLibraries(libraryNames), false);
  }

  /**
   * Walks the parts of the library dependency graph that have not run the given generator
   * referenced by name and accumulates and returns a map from binding property name to newly legal
   * values that were declared in those libraries.
   */
  public Multimap<String, String> gatherNewBindingPropertyValuesForGenerator(String generatorName) {
    Multimap<String, String> newBindingPropertyValuesByName = LinkedHashMultimap.create();
    for (Library libraryPendingGeneratorRun : gatherLibrariesForGenerator(generatorName, false)) {
      newBindingPropertyValuesByName.putAll(
          libraryPendingGeneratorRun.getNewBindingPropertyValuesByName());
    }
    return newBindingPropertyValuesByName;
  }

  /**
   * Walks the parts of the library dependency graph that have not run the given generator
   * referenced by name and accumulates and returns a map from configuration property name to newly
   * set values that were declared in those libraries.
   */
  public Multimap<String, String> gatherNewConfigurationPropertyValuesForGenerator(
      String generatorName) {
    Multimap<String, String> newConfigurationPropertyValuesByName = LinkedHashMultimap.create();
    for (Library libraryPendingGeneratorRun : gatherLibrariesForGenerator(generatorName, false)) {
      newConfigurationPropertyValuesByName.putAll(
          libraryPendingGeneratorRun.getNewConfigurationPropertyValuesByName());
    }
    return newConfigurationPropertyValuesByName;
  }

  /**
   * Walks the parts of the library dependency graph that have not run the given generator
   * referenced by name and accumulates and returns a set of newly rebound type names.
   */
  public Set<String> gatherNewReboundTypeSourceNamesForGenerator(String generatorName) {
    Set<String> newReboundTypeSourceNames = Sets.newHashSet();
    List<Library> unprocessedLibraries = gatherLibrariesForGenerator(generatorName, false);
    for (Library unprocessedLibrary : unprocessedLibraries) {
      newReboundTypeSourceNames.addAll(unprocessedLibrary.getReboundTypeSourceNames());
    }
    return newReboundTypeSourceNames;
  }

  /**
   * Walks the parts of the library dependency graph that have already run the given generator
   * referenced by name and accumulates and returns the set of old rebound type names.
   */
  public Set<String> gatherOldReboundTypeSourceNamesForGenerator(String generatorName) {
    Set<String> oldReboundTypeSourceNames = Sets.newHashSet();
    List<Library> processedLibraries = gatherLibrariesForGenerator(generatorName, true);
    for (Library processedLibrary : processedLibraries) {
      oldReboundTypeSourceNames.addAll(processedLibrary.getReboundTypeSourceNames());
    }
    return oldReboundTypeSourceNames;
  }

  /**
   * Returns the resource referenced by name if present or null;
   */
  public Resource getBuildResourceByPath(String buildResourcePath) {
    if (!getLibrariesByBuildResourcePath().containsKey(buildResourcePath)) {
      return null;
    }
    Library library = getLibrariesByBuildResourcePath().get(buildResourcePath);
    return library.getBuildResourceByPath(buildResourcePath);
  }

  /**
   * Returns the set of all build resource paths.
   */
  public Set<String> getBuildResourcePaths() {
    return getLibrariesByBuildResourcePath().keySet();
  }

  /**
   * Opens and returns an input stream for the given class file path if present or null.
   */
  public InputStream getClassFileStream(String classFilePath) {
    if (!getLibrariesByClassFilePath().containsKey(Libraries.computeClassFileName(classFilePath))) {
      return null;
    }
    Library library =
        getLibrariesByClassFilePath().get(Libraries.computeClassFileName(classFilePath));
    return library.getClassFileStream(classFilePath);
  }

  /**
   * Returns the compilation unit for the given compilation unit type name if present or null.
   */
  public CompilationUnit getCompilationUnitByTypeBinaryName(String typeBinaryName) {
    if (!getLibrariesByCompilationUnitTypeBinaryName().containsKey(typeBinaryName)) {
      return null;
    }
    Library library = getLibrariesByCompilationUnitTypeBinaryName().get(typeBinaryName);
    return library.getCompilationUnitByTypeBinaryName(typeBinaryName);
  }

  /**
   * Returns the compilation unit for the given compilation unit type name if present or null.
   */
  public CompilationUnit getCompilationUnitByTypeSourceName(String typeSourceName) {
    if (!getLibrariesByCompilationUnitTypeSourceName().containsKey(typeSourceName)) {
      return null;
    }
    Library library = getLibrariesByCompilationUnitTypeSourceName().get(typeSourceName);
    return library.getCompilationUnitByTypeSourceName(typeSourceName);
  }

  /**
   * Returns the set of all compilation unit type source names (both regular and super sourced).
   * Useful to be able to force loading of all known types to make subtype queries accurate for
   * example when doing global generator execution.
   */
  public Set<String> getCompilationUnitTypeSourceNames() {
    if (compilationUnitTypeSourceNames == null) {
      compilationUnitTypeSourceNames = Sets.newLinkedHashSet();
      for (Library library : libraries) {
        compilationUnitTypeSourceNames.addAll(library.getRegularCompilationUnitTypeSourceNames());
        compilationUnitTypeSourceNames.addAll(
            library.getSuperSourceCompilationUnitTypeSourceNames());
      }
      compilationUnitTypeSourceNames = Collections.unmodifiableSet(compilationUnitTypeSourceNames);
    }
    return compilationUnitTypeSourceNames;
  }

  public ArtifactSet getGeneratedArtifacts() {
    if (generatedArtifacts == null) {
      generatedArtifacts = new ArtifactSet();
      for (Library library : libraries) {
        generatedArtifacts.addAll(library.getGeneratedArtifacts());
      }
    }
    return generatedArtifacts;
  }

  /**
   * Returns the list of all permutation result handles (one per library) in library link order.
   */
  public List<PersistenceBackedObject<PermutationResult>> getPermutationResultHandlesInLinkOrder() {
    if (permutationResultHandles == null) {
      permutationResultHandles = Lists.newArrayList();
      for (Library library : libraries) {
        permutationResultHandles.add(library.getPermutationResultHandle());
      }
      permutationResultHandles = Collections.unmodifiableList(permutationResultHandles);
    }
    return permutationResultHandles;
  }

  /**
   * Returns the resource referenced by name if present or null;
   */
  public Resource getPublicResourceByPath(String path) {
    if (!getLibrariesByPublicResourcePath().containsKey(path)) {
      return null;
    }
    Library library = getLibrariesByPublicResourcePath().get(path);
    return library.getPublicResourceByPath(path);
  }

  /**
   * Returns the set of all public resource paths.
   */
  public Set<String> getPublicResourcePaths() {
    return getLibrariesByPublicResourcePath().keySet();
  }

  /**
   * Returns the set of names of types which are the subject of GWT.create() calls in source code in
   * any of the contained libraries.
   */
  public Set<String> getReboundTypeSourceNames() {
    if (reboundTypeSourceNames == null) {
      reboundTypeSourceNames = Sets.newLinkedHashSet();
      for (Library library : libraries) {
        reboundTypeSourceNames.addAll(library.getReboundTypeSourceNames());
      }
      reboundTypeSourceNames = Collections.unmodifiableSet(reboundTypeSourceNames);
    }
    return reboundTypeSourceNames;
  }

  /**
   * Returns the set of compilation unit type source names for all contained super source
   * compilation units.
   */
  public Set<String> getSuperSourceCompilationUnitTypeSourceNames() {
    if (superSourceCompilationUnitTypeSourceNames == null) {
      superSourceCompilationUnitTypeSourceNames = Sets.newLinkedHashSet();
      for (Library library : libraries) {
        superSourceCompilationUnitTypeSourceNames.addAll(
            library.getSuperSourceCompilationUnitTypeSourceNames());
      }
      superSourceCompilationUnitTypeSourceNames =
          Collections.unmodifiableSet(superSourceCompilationUnitTypeSourceNames);
    }
    return superSourceCompilationUnitTypeSourceNames;
  }

  // VisibleForTesting
  List<Library> getLibraries() {
    return libraries;
  }

  // VisibleForTesting
  List<Library> getLibraries(Collection<String> libraryNames) {
    return Lists.newArrayList(
        Maps.filterKeys(librariesByName, Predicates.in(libraryNames)).values());
  }

  private Iterable<Library> asLibraries(Set<String> libraryNames) {
    return Iterables.transform(libraryNames, new Function<String, Library>() {
        @Override
      public Library apply(String libraryName) {
        return librariesByName.get(libraryName);
      }
    });
  }

  private void buildLibraryIndexes(boolean verifyLibraryReferences) {
    librariesByName = Maps.newLinkedHashMap();
    for (Library library : libraries) {
      if (librariesByName.containsKey(library.getLibraryName())) {
        throw new DuplicateLibraryNameException("More than one library is claiming the name \""
            + library.getLibraryName() + "\", thus making library references ambiguous. "
            + "Compilation can not proceed.");
      }
      librariesByName.put(library.getLibraryName(), library);
    }

    Multimap<Library, Library> parentLibrariesByChildLibrary = HashMultimap.create();
    Multimap<Library, Library> childLibrariesByParentLibrary = HashMultimap.create();
    for (Library parentLibrary : libraries) {
      for (String childLibraryName : parentLibrary.getDependencyLibraryNames()) {
        Library childLibrary = librariesByName.get(childLibraryName);
        boolean libraryIsMissing = childLibrary == null;
        if (libraryIsMissing && verifyLibraryReferences) {
          throw new UnresolvedLibraryException("Library " + parentLibrary.getLibraryName()
              + " references library " + childLibraryName + " but it is not available.");
        }
        parentLibrariesByChildLibrary.put(childLibrary, parentLibrary);
        childLibrariesByParentLibrary.put(parentLibrary, childLibrary);
      }
    }

    rootLibraries = Lists.newArrayList();
    for (Library library : libraries) {
      boolean libraryHasParents = parentLibrariesByChildLibrary.containsKey(library);
      if (libraryHasParents) {
        continue;
      }
      rootLibraries.add(library);
    }

    List<Library> librariesInLinkOrder = Lists.newArrayList();
    Set<Library> maybeLeafLibraries = Sets.newLinkedHashSet(libraries);
    while (!maybeLeafLibraries.isEmpty()) {
      List<Library> leafLibraries = Lists.newArrayList();
      for (Library maybeLeafLibrary : maybeLeafLibraries) {
        if (childLibrariesByParentLibrary.get(maybeLeafLibrary).isEmpty()) {
          leafLibraries.add(maybeLeafLibrary);
        }
      }
      librariesInLinkOrder.addAll(leafLibraries);

      maybeLeafLibraries.clear();
      for (Library leafLibrary : leafLibraries) {
        Collection<Library> parentLibraries = parentLibrariesByChildLibrary.removeAll(leafLibrary);
        maybeLeafLibraries.addAll(parentLibraries);
        for (Library parentLibrary : parentLibraries) {
          childLibrariesByParentLibrary.remove(parentLibrary, leafLibrary);
        }
      }
    }

    libraries = librariesInLinkOrder;
  }

  /**
   * Walks the library dependency graph and collects a list of libraries that either have or have
   * not run the given generator depending on the given gatherNotProcessed boolean.
   */
  private List<Library> gatherLibrariesForGenerator(
      String generatorName, boolean gatherLibrariesThatHaveAlreadyRunThisGenerator) {
    Set<Library> exploredLibraries = Sets.newHashSet();
    LinkedList<Library> unexploredLibraries = Lists.newLinkedList();
    List<Library> librariesForGenerator = Lists.newArrayList();

    unexploredLibraries.addAll(rootLibraries);
    while (!unexploredLibraries.isEmpty()) {
      Library library = unexploredLibraries.removeFirst();
      exploredLibraries.add(library);

      boolean libraryHasAlreadyRunThisGenerator =
          library.getRanGeneratorNames().contains(generatorName);
      if (!gatherLibrariesThatHaveAlreadyRunThisGenerator && libraryHasAlreadyRunThisGenerator) {
        // don't gather this one
        continue;
      }

      // gather this library
      librariesForGenerator.add(library);
      for (Library dependencyLibrary : asLibraries(library.getDependencyLibraryNames())) {
        if (exploredLibraries.contains(dependencyLibrary)) {
          continue;
        }
        unexploredLibraries.add(dependencyLibrary);
      }
    }

    return librariesForGenerator;
  }

  private Map<String, Library> getLibrariesByBuildResourcePath() {
    if (librariesByBuildResourcePath == null) {
      librariesByBuildResourcePath = Maps.newLinkedHashMap();
      for (Library library : libraries) {
        Set<String> buildResourcePaths = library.getBuildResourcePaths();
        for (String buildResourcePath : buildResourcePaths) {
          librariesByBuildResourcePath.put(buildResourcePath, library);
        }
      }
      librariesByBuildResourcePath = Collections.unmodifiableMap(librariesByBuildResourcePath);
    }
    return librariesByBuildResourcePath;
  }

  // TODO(stalcup): throw an error if more than one version of a type is provided.
  private Map<String, Library> getLibrariesByClassFilePath() {
    if (librariesByClassFilePath == null) {
      librariesByClassFilePath = Maps.newLinkedHashMap();

      // Record regular class files first.
      for (Library library : libraries) {
        Set<String> classFilePaths = library.getRegularClassFilePaths();
        for (String classFilePath : classFilePaths) {
          librariesByClassFilePath.put(classFilePath, library);
        }
      }

      // Overwrite with superSource class files, so they have higher priority.
      for (Library library : libraries) {
        Set<String> superSourceClassFilePaths = library.getSuperSourceClassFilePaths();
        for (String superSourceClassFilePath : superSourceClassFilePaths) {
          librariesByClassFilePath.put(superSourceClassFilePath, library);
        }
      }

      librariesByClassFilePath = Collections.unmodifiableMap(librariesByClassFilePath);
    }
    return librariesByClassFilePath;
  }

  // TODO(stalcup): throw an error if more than one version of a type is provided.
  private Map<String, Library> getLibrariesByCompilationUnitTypeBinaryName() {
    if (librariesByCompilationUnitTypeBinaryName == null) {
      librariesByCompilationUnitTypeBinaryName = Maps.newLinkedHashMap();

      // Record regular compilation units first.
      for (Library library : libraries) {
        for (String compilationUnitTypeSourceName :
            library.getRegularCompilationUnitTypeSourceNames()) {
          librariesByCompilationUnitTypeSourceName.put(compilationUnitTypeSourceName, library);

          Collection<String> nestedTypeBinaryNames = library
              .getNestedBinaryNamesByCompilationUnitName().get(compilationUnitTypeSourceName);
          for (String nestedTypeBinaryName : nestedTypeBinaryNames) {
            librariesByCompilationUnitTypeBinaryName.put(nestedTypeBinaryName, library);
          }
        }
      }

      // Overwrite with superSource compilation units, so they have higher priority.
      for (Library library : libraries) {
        for (String superSourceCompilationUnitTypeSourceName :
            library.getSuperSourceCompilationUnitTypeSourceNames()) {
          librariesByCompilationUnitTypeSourceName.put(superSourceCompilationUnitTypeSourceName,
              library);

          Collection<String> nestedTypeBinaryNames = library
              .getNestedBinaryNamesByCompilationUnitName().get(
              superSourceCompilationUnitTypeSourceName);
          for (String nestedTypeBinaryName : nestedTypeBinaryNames) {
            librariesByCompilationUnitTypeBinaryName.put(nestedTypeBinaryName, library);
          }
        }
      }
    }
    return Collections.unmodifiableMap(librariesByCompilationUnitTypeBinaryName);
  }

  // TODO(stalcup): throw an error if more than one version of a type is provided.
  private Map<String, Library> getLibrariesByCompilationUnitTypeSourceName() {
    if (librariesByCompilationUnitTypeSourceName == null) {
      librariesByCompilationUnitTypeSourceName = Maps.newLinkedHashMap();

      // Record regular compilation units first.
      for (Library library : libraries) {
        for (String compilationUnitTypeSourceName :
            library.getRegularCompilationUnitTypeSourceNames()) {
          checkCompilationUnitUnique(library, compilationUnitTypeSourceName);
          librariesByCompilationUnitTypeSourceName.put(compilationUnitTypeSourceName, library);

          Collection<String> nestedTypeSourceNames = library
              .getNestedSourceNamesByCompilationUnitName().get(compilationUnitTypeSourceName);
          for (String nestedTypeSourceName : nestedTypeSourceNames) {
            librariesByCompilationUnitTypeSourceName.put(nestedTypeSourceName, library);
          }
        }
      }

      // Overwrite with superSource compilation units, so they have higher priority.
      for (Library library : libraries) {
        for (String superSourceCompilationUnitTypeSourceName :
            library.getSuperSourceCompilationUnitTypeSourceNames()) {
          checkCompilationUnitUnique(library, superSourceCompilationUnitTypeSourceName);
          librariesByCompilationUnitTypeSourceName.put(superSourceCompilationUnitTypeSourceName,
              library);

          Collection<String> nestedTypeSourceNames = library
              .getNestedSourceNamesByCompilationUnitName().get(
              superSourceCompilationUnitTypeSourceName);
          for (String nestedTypeSourceName : nestedTypeSourceNames) {
            librariesByCompilationUnitTypeSourceName.put(nestedTypeSourceName, library);
          }
        }
      }
    }
    return Collections.unmodifiableMap(librariesByCompilationUnitTypeSourceName);
  }

  private void checkCompilationUnitUnique(Library newLibrary,
      String compilationUnitTypeSourceName) {
    if (librariesByCompilationUnitTypeSourceName.containsKey(compilationUnitTypeSourceName)) {
      Library oldLibrary =
          librariesByCompilationUnitTypeSourceName.get(compilationUnitTypeSourceName);
      throw new CollidingCompilationUnitException(String.format(
          "Compilation units must be unique but '%s' is being "
          + "provided by both the '%s' and '%s' library.", compilationUnitTypeSourceName,
          oldLibrary.getLibraryName(), newLibrary.getLibraryName()));
    }
  }

  private Map<String, Library> getLibrariesByPublicResourcePath() {
    if (librariesByPublicResourcePath == null) {
      librariesByPublicResourcePath = Maps.newLinkedHashMap();
      for (Library library : libraries) {
        Set<String> publicResourcePaths = library.getPublicResourcePaths();
        for (String publicResourcePath : publicResourcePaths) {
          librariesByPublicResourcePath.put(publicResourcePath, library);
        }
      }

      librariesByPublicResourcePath = Collections.unmodifiableMap(librariesByPublicResourcePath);
    }
    return librariesByPublicResourcePath;
  }
}
