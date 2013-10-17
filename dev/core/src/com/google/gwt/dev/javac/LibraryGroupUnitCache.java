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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.cfg.LibraryGroup;
import com.google.gwt.thirdparty.guava.common.base.Joiner;
import com.google.gwt.thirdparty.guava.common.base.Preconditions;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/**
 * A cache that finds compilation unit instances in a library group.<br />
 *
 * ContentId based lookups are not supported since having multiple versions of the same compilation
 * unit in a set of libraries would be an error.<br />
 *
 * Removals are not supported since libraries are immutable and the removals would only be needed if
 * library contents were invalid.
 */
public class LibraryGroupUnitCache implements UnitCache {

  private static final String JAVA_SUFFIX = ".java";

  /**
   * Translates type names to resource paths to ease lookups since the unit cache system caches
   * based on resource path but the natural mode of interaction with this cache is via type names.
   */
  public static String typeNameToResourcePath(String typeName) {
    Preconditions.checkState(!typeName.endsWith(JAVA_SUFFIX));

    // If typeName refers to a nested type using binary syntax.
    if (typeName.contains("$")) {
      typeName = typeName.split("\\$")[0];
    } else {
      // If typeName refers to a nested type using internal syntax.
      LinkedList<String> packagesAndTypes =
          Lists.newLinkedList(Arrays.asList(typeName.split("\\.")));
      String typeShortName = null;
      while (!packagesAndTypes.isEmpty()
          && Character.isUpperCase(packagesAndTypes.getLast().charAt(0))) {
        typeShortName = packagesAndTypes.removeLast();
      }

      typeName = Joiner.on(".").join(packagesAndTypes)
          + (typeShortName != null ? "." + typeShortName : "");
    }

    return typeName.replace(".", "/") + JAVA_SUFFIX;
  }

  private static String resourcePathToTypeName(String resourcePath) {
    Preconditions.checkState(resourcePath.endsWith(JAVA_SUFFIX));

    resourcePath = resourcePath.substring(0, resourcePath.length() - JAVA_SUFFIX.length());
    return resourcePath.replace("/", ".");
  }

  private Map<String, CompilationUnit> compilationUnitsByTypeName = Maps.newLinkedHashMap();
  private Set<String> knownEmptyResourcePaths = Sets.newLinkedHashSet();
  private LibraryGroup libraryGroup;

  public LibraryGroupUnitCache(LibraryGroup libraryGroup) {
    this.libraryGroup = libraryGroup;
  }

  /**
   * Adds a {@link CompilationUnit} to the cache.<br />
   *
   * Though this class is intended primarily to expose and cache compilation units from previously
   * compiled library files it must also be prepared to accept brand new compilation units resulting
   * from live compilation as this is an absolute requirement for the current compiler design.
   */
  @Override
  public void add(CompilationUnit compilationUnit) {
    String typeName = compilationUnit.getTypeName();
    if (compilationUnitsByTypeName.containsKey(typeName)) {
      return;
    }

    compilationUnitsByTypeName.put(typeName, compilationUnit);
    knownEmptyResourcePaths.remove(typeNameToResourcePath(typeName));
  }

  @Override
  public void addArchivedUnit(CompilationUnit compilationUnit) {
    throw new UnsupportedOperationException(
        "When using a library group as the source for unit caching, surfacing "
        + "other sources of previously compiled compilation units (.gwtar) is not supported.");
  }

  @Override
  public void cleanup(TreeLogger logger) {
    compilationUnitsByTypeName.clear();
    knownEmptyResourcePaths.clear();
  }

  @Override
  public CompilationUnit find(ContentId contentId) {
    throw new UnsupportedOperationException(
        "Multiple compilation unit revision retrieval is not supported.");
  }

  @Override
  public CompilationUnit find(String resourcePath) {
    String typeName = resourcePathToTypeName(resourcePath);
    if (compilationUnitsByTypeName.containsKey(typeName)) {
      return compilationUnitsByTypeName.get(typeName);
    }

    if (knownEmptyResourcePaths.contains(resourcePath)) {
      return null;
    }

    CompilationUnit compilationUnit = libraryGroup.getCompilationUnitByTypeName(typeName);
    if (compilationUnit == null) {
      knownEmptyResourcePaths.add(resourcePath);
      return null;
    }
    compilationUnitsByTypeName.put(compilationUnit.getTypeName(), compilationUnit);
    return compilationUnit;
  }

  @Override
  public void remove(CompilationUnit unit) {
    throw new UnsupportedOperationException(
        "Compilation units can not be removed from immutable libraries.");
  }
}
