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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.dev.cfg.Libraries.IncompatibleLibraryVersionException;
import com.google.gwt.dev.javac.CompilationUnit;
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.PermutationResult;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.util.PersistenceBackedObject;
import com.google.gwt.thirdparty.guava.common.annotations.VisibleForTesting;
import com.google.gwt.thirdparty.guava.common.base.Predicates;
import com.google.gwt.thirdparty.guava.common.collect.HashMultimap;
import com.google.gwt.thirdparty.guava.common.collect.LinkedHashMultimap;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Multimap;
import com.google.gwt.thirdparty.guava.common.collect.SetMultimap;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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

  private static final Comparator<Library> LIBRARY_NAME_COMPARATOR = new Comparator<Library>() {
    @Override
    public int compare(Library thisLibrary, Library thatLibrary) {
      return thisLibrary.getLibraryName().compareTo(thatLibrary.getLibraryName());
    }
  };

  @VisibleForTesting
  public static String formatDuplicateCompilationUnitMessage(String compilationUnitTypeSourceName,
      String oldLibraryName, String newLibraryName) {
    return String.format("Compilation units must be unique but '%s' is being "
        + "provided by both the '%s' and '%s' library.", compilationUnitTypeSourceName,
        oldLibraryName, newLibraryName);
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
  private SetMultimap<String, String> processedReboundTypeSourceNamesByGenerator =
      HashMultimap.create();
  private Set<String> reboundTypeSourceNames;
  private List<Library> rootLibraries;
  private Set<String> superSourceCompilationUnitTypeSourceNames;

  protected LibraryGroup() {
    // Private to force class construction via one of the public factory functions.
  }

  /**
   * Closes all read streams.
   */
  public void close() {
    for (Library library : libraries) {
      library.close();
    }
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
   * Returns a list of source names of types which have already been rebound by the given generator.
   */
  public Set<String> getProcessedReboundTypeSourceNames(String generatorName) {
    if (!processedReboundTypeSourceNamesByGenerator.containsKey(generatorName)) {
      Set<String> processedReboundTypeSourceNames = Sets.newHashSet();
      for (Library library : libraries) {
        processedReboundTypeSourceNames.addAll(
            library.getProcessedReboundTypeSourceNamesByGenerator().get(generatorName));
      }
      processedReboundTypeSourceNamesByGenerator.putAll(generatorName,
          Collections.unmodifiableSet(processedReboundTypeSourceNames));
    }
    return processedReboundTypeSourceNamesByGenerator.get(generatorName);
  }

  /**
   * Returns the resource referenced by name if present or null.
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
      reboundTypeSourceNames =
          Collections.unmodifiableSet(reboundTypeSourceNames);
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

  public void verify(TreeLogger logger) throws UnableToCompleteException {
    try {
      // Forces the building of the mapping from source names of compilation unit types to the
      // library that provides them, so that there is an opportunity to notice and reject duplicate
      // providing libraries.
      getLibrariesByCompilationUnitTypeSourceName();
    } catch (CollidingCompilationUnitException e) {
      logger.log(TreeLogger.ERROR, e.getMessage());
      throw new UnableToCompleteException();
    }
  }

  @VisibleForTesting
  List<Library> getLibraries() {
    return libraries;
  }

  @VisibleForTesting
  List<Library> getLibraries(Collection<String> libraryNames) {
    return Lists.newArrayList(
        Maps.filterKeys(librariesByName, Predicates.in(libraryNames)).values());
  }

  private void buildLibraryIndexes(boolean verifyLibraryReferences) {
    // Start processing with a consistent library order to ensure consistently ordered output.
    Collections.sort(libraries, LIBRARY_NAME_COMPARATOR);

    librariesByName = Maps.newLinkedHashMap();
    for (Library library : libraries) {
      if (librariesByName.containsKey(library.getLibraryName())) {
        throw new DuplicateLibraryNameException("More than one library is claiming the name \""
            + library.getLibraryName() + "\", thus making library references ambiguous. "
            + "Compilation can not proceed.");
      }
      librariesByName.put(library.getLibraryName(), library);
    }

    Multimap<Library, Library> parentLibrariesByChildLibrary = LinkedHashMultimap.create();
    Multimap<Library, Library> childLibrariesByParentLibrary = LinkedHashMultimap.create();
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
   * Throws an exception if the referenced compilation unit (which is being provided by the
   * referenced new library) is already being provided by some older library.
   */
  private void assertUniquelyProvided(Library newLibrary, String compilationUnitTypeSourceName) {
    if (librariesByCompilationUnitTypeSourceName.containsKey(compilationUnitTypeSourceName)) {
      Library oldLibrary =
          librariesByCompilationUnitTypeSourceName.get(compilationUnitTypeSourceName);
      throw new CollidingCompilationUnitException(formatDuplicateCompilationUnitMessage(
          compilationUnitTypeSourceName, oldLibrary.getLibraryName(), newLibrary.getLibraryName()));
    }
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

  private Map<String, Library> getLibrariesByCompilationUnitTypeSourceName() {
    if (librariesByCompilationUnitTypeSourceName == null) {
      librariesByCompilationUnitTypeSourceName = Maps.newLinkedHashMap();

      // Record regular compilation units first.
      for (Library library : libraries) {
        for (String compilationUnitTypeSourceName :
            library.getRegularCompilationUnitTypeSourceNames()) {
          assertUniquelyProvided(library, compilationUnitTypeSourceName);
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
          assertUniquelyProvided(library, superSourceCompilationUnitTypeSourceName);
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
