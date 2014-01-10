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
import com.google.gwt.core.ext.linker.impl.StandardGeneratedResource;
import com.google.gwt.dev.cfg.Libraries.IncompatibleLibraryVersionException;
import com.google.gwt.dev.javac.CompilationUnit;
import com.google.gwt.dev.jjs.CompilerIoException;
import com.google.gwt.dev.jjs.PermutationResult;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.resource.impl.ZipFileResource;
import com.google.gwt.dev.util.ZipEntryBackedObject;
import com.google.gwt.thirdparty.guava.common.base.Splitter;
import com.google.gwt.thirdparty.guava.common.collect.LinkedHashMultimap;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Multimap;
import com.google.gwt.thirdparty.guava.common.collect.Multimaps;
import com.google.gwt.thirdparty.guava.common.collect.Sets;
import com.google.gwt.thirdparty.guava.common.io.ByteStreams;
import com.google.gwt.thirdparty.guava.common.io.CharStreams;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * A library that lazily reads and caches data from a zip file.
 */
class ZipLibrary implements Library {

  private class ZipLibraryReader {

    private long lastModified;
    private String libraryPath;
    private ZipFile zipFile;

    private ZipLibraryReader(String libraryPath) {
      this.libraryPath = libraryPath;
      try {
        File file = new File(libraryPath);
        lastModified = file.lastModified();
        zipFile = new ZipFile(file);
      } catch (IOException e) {
        throw new CompilerIoException("Failed to open zip file " + libraryPath + ".", e);
      }
    }

    private InputStream getClassFileStream(String classFilePath) {
      ZipEntry classFileEntry =
          zipFile.getEntry(Libraries.computeClassFileEntryName(classFilePath));
      return getInputStream(classFileEntry);
    }

    private InputStream getInputStream(ZipEntry zipEntry) {
      try {
        return zipFile.getInputStream(zipEntry);
      } catch (IOException e) {
        throw new CompilerIoException("Couldn't open an input stream to an entry named "
            + zipEntry.getName() + " in zip file " + zipFile.getName() + ".", e);
      }
    }

    private ZipEntryBackedObject<PermutationResult> getPermutationResultHandle() {
      return new ZipEntryBackedObject<PermutationResult>(
          zipFile, libraryPath, Libraries.PERMUTATION_RESULT_ENTRY_NAME, PermutationResult.class);
    }

    private Resource readBuildResourceByPath(String path) {
      return new ZipFileResource(
          zipFile, libraryPath, lastModified, Libraries.DIRECTORY_BUILD_RESOURCES + path);
    }

    private Set<String> readBuildResourcePaths() {
      return readStringSet(Libraries.BUILD_RESOURCE_PATHS_ENTRY_NAME);
    }

    private byte[] readBytes(String entryName) {
      ZipEntry zipEntry = zipFile.getEntry(entryName);
      try {
        return ByteStreams.toByteArray(getInputStream(zipEntry));
      } catch (IOException e) {
        throw new CompilerIoException(
            "Failed to read " + entryName + " in " + zipFile.getName() + " as bytes.", e);
      }
    }

    private CompilationUnit readCompilationUnitByTypeName(String typeName) {
      ZipEntry compilationUnitEntry =
          zipFile.getEntry(Libraries.computeCompilationUnitEntryName(typeName));
      if (compilationUnitEntry == null) {
        return null;
      }

      InputStream compilationUnitInputStream = getInputStream(compilationUnitEntry);

      CompilationUnit compilationUnit;
      try {
        ObjectInputStream objectInputStream = new ObjectInputStream(compilationUnitInputStream);
        compilationUnit = (CompilationUnit) objectInputStream.readObject();
        objectInputStream.close();
      } catch (IOException e) {
        throw new CompilerIoException(
            "Failed to read compilation unit " + typeName + " for deserialization.", e);
      } catch (ClassNotFoundException e) {
        throw new CompilerIoException("Failed to deserialize compilation unit " + typeName
            + " because of a missing referenced class.", e);
      }
      return compilationUnit;
    }

    private Set<String> readDependencyLibraryNames() {
      return readStringSet(Libraries.DEPENDENCY_LIBRARY_NAMES_ENTRY_NAME);
    }

    private ArtifactSet readGeneratedArtifacts() {
      Set<String> generatedArtifactNames =
          readStringSet(Libraries.GENERATED_ARTIFACT_NAMES_ENTRY_NAME);
      ArtifactSet artifacts = new ArtifactSet();
      for (String generatedArtifactName : generatedArtifactNames) {
        StandardGeneratedResource artifact = new StandardGeneratedResource(generatedArtifactName,
            readBytes(Libraries.DIRECTORY_GENERATED_ARTIFACTS + generatedArtifactName));
        artifacts.add(artifact);
      }
      return artifacts;
    }

    private String readLibraryName() {
      return readString(Libraries.LIBRARY_NAME_ENTRY_NAME);
    }

    private Multimap<String, String> readNewBindingPropertyValuesByName() {
      return readStringMultimap(Libraries.NEW_BINDING_PROPERTY_VALUES_BY_NAME_ENTRY_NAME);
    }

    private Multimap<String, String> readNewConfigurationPropertyValuesByName() {
      return readStringMultimap(Libraries.NEW_CONFIGURATION_PROPERTY_VALUES_BY_NAME_ENTRY_NAME);
    }

    private Resource readPublicResourceByPath(String path) {
      return new ZipFileResource(
          zipFile, libraryPath, lastModified, Libraries.DIRECTORY_PUBLIC_RESOURCES + path);
    }

    private Set<String> readPublicResourcePaths() {
      return readStringSet(Libraries.PUBLIC_RESOURCE_PATHS_ENTRY_NAME);
    }

    private Set<String> readRanGeneratorNames() {
      return readStringSet(Libraries.RAN_GENERATOR_NAMES_ENTRY_NAME);
    }

    private Set<String> readReboundTypeNames() {
      return readStringSet(Libraries.REBOUND_TYPE_NAMES_ENTRY_NAME);
    }

    private Set<String> readRegularClassFilePaths() {
      return readStringSet(Libraries.REGULAR_CLASS_FILE_PATHS_ENTRY_NAME);
    }

    private Set<String> readRegularCompilationUnitTypeNames() {
      return readStringSet(Libraries.REGULAR_COMPILATION_UNIT_TYPE_NAMES_ENTRY_NAME);
    }

    private String readString(String entryName) {
      ZipEntry zipEntry = zipFile.getEntry(entryName);
      return readToString(entryName, getInputStream(zipEntry));
    }

    private Collection<String> readStringList(String entryName) {
      ZipEntry zipEntry = zipFile.getEntry(entryName);
      InputStream entryInputStream = getInputStream(zipEntry);
      String inputString = readToString(entryName, entryInputStream);
      Iterable<String> lines =
          Splitter.on(Libraries.LINE_SEPARATOR).omitEmptyStrings().split(inputString);
      return Collections.unmodifiableSet(Sets.newLinkedHashSet(lines));
    }

    private Multimap<String, String> readStringMultimap(String entryName) {
      ZipEntry zipEntry = zipFile.getEntry(entryName);
      InputStream entryInputStream = getInputStream(zipEntry);
      String inputString = readToString(entryName, entryInputStream);
      Iterable<String> lines =
          Splitter.on(Libraries.LINE_SEPARATOR).omitEmptyStrings().split(inputString);

      Multimap<String, String> stringMultimap = LinkedHashMultimap.<String, String> create();

      for (String line : lines) {
        LinkedList<String> entry = Lists.newLinkedList(
            Splitter.on(Libraries.KEY_VALUE_SEPARATOR).omitEmptyStrings().split(line));

        String key = entry.getFirst();
        Iterable<String> values =
            Splitter.on(Libraries.VALUE_SEPARATOR).omitEmptyStrings().split(entry.getLast());
        stringMultimap.putAll(key, values);
      }
      return stringMultimap;
    }

    private Set<String> readStringSet(String entryName) {
      return Collections.unmodifiableSet(Sets.newLinkedHashSet(readStringList(entryName)));
    }

    private Set<String> readSuperSourceClassFilePaths() {
      return readStringSet(Libraries.SUPER_SOURCE_CLASS_FILE_PATHS_ENTRY_NAME);
    }

    private Set<String> readSuperSourceCompilationUnitTypeNames() {
      return readStringSet(Libraries.SUPER_SOURCE_COMPILATION_UNIT_TYPE_NAMES_ENTRY_NAME);
    }

    private String readToString(String entryName, InputStream inputStream) {
      try {
        return CharStreams.toString(new InputStreamReader(inputStream));
      } catch (IOException e) {
        throw new CompilerIoException(
            "Failed to read " + entryName + " in " + zipFile.getName() + " as a String.", e);
      }
    }

    private int readVersionNumber() {
      return Integer.parseInt(readString(Libraries.VERSION_NUMBER_ENTRY_NAME));
    }
  }

  private Set<String> buildResourcePaths;
  private Map<String, Resource> buildResourcesByPath = Maps.newHashMap();
  private Set<String> classFilePaths;
  private Map<String, CompilationUnit> compilationUnitsByTypeName = Maps.newHashMap();
  private Set<String> dependencyLibraryNames;
  private ArtifactSet generatedArtifacts;
  private String libraryName;
  private Multimap<String, String> newBindingPropertyValuesByName;
  private Multimap<String, String> newConfigurationPropertyValuesByName;
  private ZipEntryBackedObject<PermutationResult> permutationResultHandle;
  private Set<String> publicResourcePaths;
  private Map<String, Resource> publicResourcesByPath = Maps.newHashMap();
  private Set<String> ranGeneratorNames;
  private Set<String> reboundTypeNames;
  private Set<String> regularCompilationUnitTypeNames;
  private Set<String> superSourceClassFilePaths;
  private Set<String> superSourceCompilationUnitTypeNames;
  private final ZipLibraryReader zipLibraryReader;

  ZipLibrary(String fileName) throws IncompatibleLibraryVersionException {
    zipLibraryReader = new ZipLibraryReader(fileName);
    if (ZipLibraries.versionNumber != zipLibraryReader.readVersionNumber()) {
      throw new IncompatibleLibraryVersionException(
          ZipLibraries.versionNumber, zipLibraryReader.readVersionNumber());
    }
  }

  @Override
  public Resource getBuildResourceByPath(String path) {
    if (!buildResourcesByPath.containsKey(path)) {
      buildResourcesByPath.put(path, zipLibraryReader.readBuildResourceByPath(path));
    }
    return buildResourcesByPath.get(path);
  }

  @Override
  public Set<String> getBuildResourcePaths() {
    if (buildResourcePaths == null) {
      buildResourcePaths = Collections.unmodifiableSet(zipLibraryReader.readBuildResourcePaths());
    }
    return buildResourcePaths;
  }

  @Override
  public InputStream getClassFileStream(String classFilePath) {
    return zipLibraryReader.getClassFileStream(classFilePath);
  }

  @Override
  public CompilationUnit getCompilationUnitByTypeName(String typeName) {
    // If the type cache doesn't contain the type yet.
    if (!compilationUnitsByTypeName.containsKey(typeName)) {
      // and the library on disk doesn't contain the type at all.
      if (!containsCompilationUnit(typeName)) {
        // cache the fact that the type isn't available on disk.
        compilationUnitsByTypeName.put(typeName, null);
        return null;
      }
      // otherwise read and cache the type.
      compilationUnitsByTypeName.put(
          typeName, zipLibraryReader.readCompilationUnitByTypeName(typeName));
    }
    return compilationUnitsByTypeName.get(typeName);
  }

  @Override
  public Set<String> getDependencyLibraryNames() {
    if (dependencyLibraryNames == null) {
      dependencyLibraryNames =
          Collections.unmodifiableSet(zipLibraryReader.readDependencyLibraryNames());
    }
    return dependencyLibraryNames;
  }

  @Override
  public ArtifactSet getGeneratedArtifacts() {
    if (generatedArtifacts == null) {
      generatedArtifacts = zipLibraryReader.readGeneratedArtifacts();
    }
    return generatedArtifacts;
  }

  @Override
  public String getLibraryName() {
    if (libraryName == null) {
      libraryName = zipLibraryReader.readLibraryName();
    }
    return libraryName;
  }

  @Override
  public Multimap<String, String> getNewBindingPropertyValuesByName() {
    if (newBindingPropertyValuesByName == null) {
      newBindingPropertyValuesByName =
          Multimaps.unmodifiableMultimap(zipLibraryReader.readNewBindingPropertyValuesByName());
    }
    return newBindingPropertyValuesByName;
  }

  @Override
  public Multimap<String, String> getNewConfigurationPropertyValuesByName() {
    if (newConfigurationPropertyValuesByName == null) {
      newConfigurationPropertyValuesByName = Multimaps.unmodifiableMultimap(
          zipLibraryReader.readNewConfigurationPropertyValuesByName());
    }
    return newConfigurationPropertyValuesByName;
  }

  @Override
  public ZipEntryBackedObject<PermutationResult> getPermutationResultHandle() {
    if (permutationResultHandle == null) {
      permutationResultHandle = zipLibraryReader.getPermutationResultHandle();
    }
    return permutationResultHandle;
  }

  @Override
  public Resource getPublicResourceByPath(String path) {
    if (!publicResourcesByPath.containsKey(path)) {
      publicResourcesByPath.put(path, zipLibraryReader.readPublicResourceByPath(path));
    }
    return publicResourcesByPath.get(path);
  }

  @Override
  public Set<String> getPublicResourcePaths() {
    if (publicResourcePaths == null) {
      publicResourcePaths = Collections.unmodifiableSet(zipLibraryReader.readPublicResourcePaths());
    }
    return publicResourcePaths;
  }

  @Override
  public Set<String> getRanGeneratorNames() {
    if (ranGeneratorNames == null) {
      ranGeneratorNames = Collections.unmodifiableSet(zipLibraryReader.readRanGeneratorNames());
    }
    return ranGeneratorNames;
  }

  @Override
  public Set<String> getReboundTypeNames() {
    if (reboundTypeNames == null) {
      reboundTypeNames = Collections.unmodifiableSet(zipLibraryReader.readReboundTypeNames());
    }
    return reboundTypeNames;
  }

  @Override
  public Set<String> getRegularClassFilePaths() {
    if (classFilePaths == null) {
      classFilePaths = Collections.unmodifiableSet(zipLibraryReader.readRegularClassFilePaths());
    }
    return classFilePaths;
  }

  @Override
  public Set<String> getRegularCompilationUnitTypeNames() {
    if (regularCompilationUnitTypeNames == null) {
      regularCompilationUnitTypeNames =
          Collections.unmodifiableSet(zipLibraryReader.readRegularCompilationUnitTypeNames());
    }
    return regularCompilationUnitTypeNames;
  }

  @Override
  public Set<String> getSuperSourceClassFilePaths() {
    if (superSourceClassFilePaths == null) {
      superSourceClassFilePaths =
          Collections.unmodifiableSet(zipLibraryReader.readSuperSourceClassFilePaths());
    }
    return superSourceClassFilePaths;
  }

  @Override
  public Set<String> getSuperSourceCompilationUnitTypeNames() {
    if (superSourceCompilationUnitTypeNames == null) {
      superSourceCompilationUnitTypeNames =
          Collections.unmodifiableSet(zipLibraryReader.readSuperSourceCompilationUnitTypeNames());
    }
    return superSourceCompilationUnitTypeNames;
  }

  /**
   * Uses regular and super source compilation unit type name indexes to determine whether a
   * compilation unit of any kind is present that matches the given type name.
   */
  private boolean containsCompilationUnit(String typeName) {
    return getRegularCompilationUnitTypeNames().contains(typeName)
        || getSuperSourceCompilationUnitTypeNames().contains(typeName);
  }
}
