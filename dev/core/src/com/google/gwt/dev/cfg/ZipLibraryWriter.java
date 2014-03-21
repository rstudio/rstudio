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
import com.google.gwt.core.ext.linker.GeneratedResource;
import com.google.gwt.dev.javac.CompilationUnit;
import com.google.gwt.dev.javac.CompiledClass;
import com.google.gwt.dev.jjs.CompilerIoException;
import com.google.gwt.dev.jjs.PermutationResult;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.util.Name.InternalName;
import com.google.gwt.dev.util.ZipEntryBackedObject;
import com.google.gwt.thirdparty.guava.common.base.Joiner;
import com.google.gwt.thirdparty.guava.common.collect.LinkedHashMultimap;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Multimap;
import com.google.gwt.thirdparty.guava.common.collect.Multimaps;
import com.google.gwt.thirdparty.guava.common.collect.Sets;
import com.google.gwt.thirdparty.guava.common.io.ByteStreams;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * A library builder that writes contents to a zip file.
 */
// TODO(stalcup): the compiler currently uses an inefficient mixture of java, protobuf, and
// custom serialization. unify all serialization on protobuf (either ascii or binary format
// depending on human-readability constraints).
public class ZipLibraryWriter implements LibraryWriter {

  private static String keyValueSeparatorString = Libraries.KEY_VALUE_SEPARATOR + "";
  private static String valueSeparatorString = Libraries.VALUE_SEPARATOR + "";
  private static String lineSeparatorString = Libraries.LINE_SEPARATOR + "";
  private static byte[] keyValueSeparatorBytes = keyValueSeparatorString.getBytes();
  private static byte[] valueSeparatorBytes = valueSeparatorString.getBytes();
  private static byte[] lineSeparatorBytes = lineSeparatorString.getBytes();

  private class ZipWriter {

    private boolean fileReady;
    private final File zipFile;
    private ZipOutputStream zipOutputStream;

    private ZipWriter(String zipFileName) {
      zipFile = new File(zipFileName);
    }

    private void createFileIfMissing() {
      if (!zipFile.exists()) {
        try {
          zipFile.createNewFile();
          if (!zipFile.canWrite()) {
            throw new CompilerIoException(
                "Created new library file " + zipFile.getPath() + " but am unable to write to it.");
          }
        } catch (IOException e) {
          throw new CompilerIoException(
              "Failed to create new library file " + zipFile.getPath() + ".", e);
        }
      }
    }

    private void createZipEntry(String entryName) {
      ZipEntry zipEntry = new ZipEntry(entryName);
      try {
        zipOutputStream.putNextEntry(zipEntry);
      } catch (Exception e) {
        throw new CompilerIoException("Failed to create zip entry " + entryName + ".", e);
      }
    }

    private synchronized void ensureFileReady() {
      if (fileReady) {
        return;
      }
      fileReady = true;

      ensureParentDirectoryExists();
      createFileIfMissing();
      try {
        zipOutputStream =
            new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)));
        zipOutputStream.setLevel(Deflater.BEST_SPEED);
      } catch (FileNotFoundException e) {
        throw new CompilerIoException(
            "Failed to open new library file " + zipFile.getPath() + " as a stream.", e);
      }
    }

    private void ensureParentDirectoryExists() {
      zipFile.getParentFile().mkdirs();
    }

    private ZipEntryBackedObject<PermutationResult> getPermutationResultHandle() {
      return new ZipEntryBackedObject<PermutationResult>(zipOutputStream, zipFile.getPath(),
          Libraries.PERMUTATION_RESULT_ENTRY_NAME, PermutationResult.class);
    }

    private void startEntry(String entryName) {
      ZipEntry zipEntry = new ZipEntry(entryName);
      try {
        zipOutputStream.putNextEntry(zipEntry);
      } catch (IOException e) {
        throw new CompilerIoException("Failed to create entry " + entryName
            + " in new library file " + zipFile.getPath() + ".", e);
      }
    }

    private void write() {
      ensureFileReady();

      try {
        // Header
        writeLibraryName();
        writeVersionNumber();

        // Dependency tree structure
        writeDependencyLibraryNames();

        // Precompiled sources
        writeClassFilePaths();
        writeCompilationUnitTypeSourceNames();
        writeNestedNamesByCompilationUnitName();

        // Resources
        writeBuildResources();
        writeBuildResourcePaths();
        writePublicResources();
        writePublicResourcePaths();

        // Generator related
        writeNewBindingPropertyValuesByName();
        writeNewConfigurationPropertyValuesByName();
        writeReboundTypeSourceNames();
        writeRanGeneratorNames();
        writeGeneratedArtifactPaths();
        writeGeneratedArtifacts();
      } finally {
        try {
          zipOutputStream.close();
        } catch (IOException e) {
          throw new CompilerIoException(
              "Failed to close new library file " + zipFile.getPath() + ".", e);
        }
      }
    }

    private void writeBuildResourcePaths() {
      writeStringSet(Libraries.BUILD_RESOURCE_PATHS_ENTRY_NAME, buildResourcesByPath.keySet());
    }

    private void writeBuildResources() {
      writeResources("build", Libraries.DIRECTORY_BUILD_RESOURCES, buildResourcesByPath);
    }

    private void writeClassFile(String classFilePath, byte[] classBytes) {
      try {
        ensureFileReady();

        startEntry(Libraries.computeClassFileEntryName(classFilePath));
        zipOutputStream.write(classBytes);
      } catch (IOException e) {
        throw new CompilerIoException("Failed to write class file " + classFilePath
            + " to new library file " + zipFile.getPath() + ".", e);
      }
    }

    private void writeClassFilePaths() {
      writeStringSet(Libraries.REGULAR_CLASS_FILE_PATHS_ENTRY_NAME, regularClassFilePaths);
      writeStringSet(Libraries.SUPER_SOURCE_CLASS_FILE_PATHS_ENTRY_NAME, superSourceClassFilePaths);
    }

    private void writeCompilationUnitFile(CompilationUnit compilationUnit) {
      ensureFileReady();

      startEntry(Libraries.computeCompilationUnitEntryName(compilationUnit.getTypeName()));
      try {
        ObjectOutputStream out = new ObjectOutputStream(zipOutputStream);
        out.writeObject(compilationUnit);
      } catch (IOException e) {
        throw new CompilerIoException("Failed to serialize compilation unit "
            + compilationUnit.getTypeName() + " in new library " + zipFile.getPath() + ".", e);
      }
    }

    private void writeCompilationUnitTypeSourceNames() {
      writeStringSet(Libraries.REGULAR_COMPILATION_UNIT_TYPE_SOURCE_NAMES_ENTRY_NAME,
          regularCompilationUnitTypeSourceNames);
      writeStringSet(Libraries.SUPER_SOURCE_COMPILATION_UNIT_TYPE_SOURCE_NAMES_ENTRY_NAME,
          superSourceCompilationUnitTypeSourceNames);
    }

    private void writeDependencyLibraryNames() {
      writeStringSet(Libraries.DEPENDENCY_LIBRARY_NAMES_ENTRY_NAME, dependencyLibraryNames);
    }

    private void writeGeneratedArtifactPaths() {
      Set<String> generatedArtifactNames = Sets.newHashSet();
      for (GeneratedResource generatedArtifact : generatedArtifacts.find(GeneratedResource.class)) {
        generatedArtifactNames.add(generatedArtifact.getPartialPath());
      }
      writeStringSet(Libraries.GENERATED_ARTIFACT_NAMES_ENTRY_NAME, generatedArtifactNames);
    }

    private void writeGeneratedArtifacts() {
      for (GeneratedResource generatedArtifact : generatedArtifacts.find(GeneratedResource.class)) {
        startEntry(Libraries.DIRECTORY_GENERATED_ARTIFACTS + generatedArtifact.getPartialPath());
        try {
          generatedArtifact.writeTo(TreeLogger.NULL, zipOutputStream);
        } catch (UnableToCompleteException e) {
          throw new CompilerIoException("Failed to read generated artifact "
              + generatedArtifact.getPartialPath() + " to write into new library file "
              + zipFile.getPath() + ".", e);
        }
      }
    }

    private void writeLibraryName() {
      writeString(Libraries.LIBRARY_NAME_ENTRY_NAME, libraryName);
    }

    private void writeNestedNamesByCompilationUnitName() {
      writeStringMultimap(Libraries.NESTED_SOURCE_NAMES_BY_ENCLOSING_NAME_ENTRY_NAME,
          nestedSourceNamesByCompilationUnitName);
      writeStringMultimap(Libraries.NESTED_BINARY_NAMES_BY_ENCLOSING_NAME_ENTRY_NAME,
          nestedBinaryNamesByCompilationUnitName);
    }

    private void writeNewBindingPropertyValuesByName() {
      writeStringMultimap(
          Libraries.NEW_BINDING_PROPERTY_VALUES_BY_NAME_ENTRY_NAME, newBindingPropertyValuesByName);
    }

    private void writeNewConfigurationPropertyValuesByName() {
      writeStringMultimap(Libraries.NEW_CONFIGURATION_PROPERTY_VALUES_BY_NAME_ENTRY_NAME,
          newConfigurationPropertyValuesByName);
    }

    private void writePublicResourcePaths() {
      writeStringSet(Libraries.PUBLIC_RESOURCE_PATHS_ENTRY_NAME, publicResourcesByPath.keySet());
    }

    private void writePublicResources() {
      writeResources("public", Libraries.DIRECTORY_PUBLIC_RESOURCES, publicResourcesByPath);
    }

    private void writeRanGeneratorNames() {
      writeStringSet(Libraries.RAN_GENERATOR_NAMES_ENTRY_NAME, ranGeneratorNames);
    }

    private void writeReboundTypeSourceNames() {
      writeStringSet(Libraries.REBOUND_TYPE_SOURCE_NAMES_ENTRY_NAME, reboundTypeSourceNames);
    }

    private void writeResources(
        String typeSourceName, String directory, Map<String, Resource> resourcesByPath) {
      for (Resource resource : resourcesByPath.values()) {
        startEntry(directory + resource.getPath());
        try {
          ByteStreams.copy(resource.openContents(), zipOutputStream);
        } catch (IOException e) {
          throw new CompilerIoException("Failed to copy " + typeSourceName + " resource "
              + resource.getPath() + " into new library file " + zipFile.getPath() + ".", e);
        }
      }
    }

    private void writeString(String entryName, String string) {
      createZipEntry(entryName);
      try {
        zipOutputStream.write(string.getBytes());
      } catch (IOException e) {
        throw new CompilerIoException("Failed to write " + entryName + " as a String.", e);
      }
    }

    private void writeStringMultimap(String entryName, Multimap<String, String> stringMultimap) {
      Map<String, Collection<String>> stringListsByString = stringMultimap.asMap();

      createZipEntry(entryName);

      Iterator<Entry<String, Collection<String>>> entryIterator =
          stringListsByString.entrySet().iterator();

      try {
        while (entryIterator.hasNext()) {
          Entry<String, Collection<String>> entry = entryIterator.next();
          String key = encode(entry.getKey());
          zipOutputStream.write(key.getBytes());

          boolean first = true;
          Collection<String> values = entry.getValue();
          for (String value : values) {
            if (first) {
              first = false;
              zipOutputStream.write(keyValueSeparatorBytes);
            } else {
              zipOutputStream.write(valueSeparatorBytes);
            }
            zipOutputStream.write(encode(value).getBytes());
          }

          if (entryIterator.hasNext()) {
            zipOutputStream.write(lineSeparatorBytes);
          }
        }
      } catch (IOException e) {
        throw new CompilerIoException("Failed to write " + entryName + " as a String multimap.", e);
      }
    }

    private String encode(String string) {
      string = encodeCharacter(string, Libraries.ENCODE_PREFIX);
      string = encodeCharacter(string, Libraries.KEY_VALUE_SEPARATOR);
      string = encodeCharacter(string, Libraries.LINE_SEPARATOR);
      string = encodeCharacter(string, Libraries.VALUE_SEPARATOR);
      return string;
    }

    private String encodeCharacter(String string, char character) {
      return string.replace(character + "", Libraries.ENCODE_PREFIX + Integer.toString(character));
    }

    private void writeStringSet(String entryName, Set<String> stringSet) {
      createZipEntry(entryName);
      Set<String> encodedStringSet = Sets.newHashSet();
      for (String string : stringSet) {
        encodedStringSet.add(encode(string));
      }
      try {
        zipOutputStream.write(
            Joiner.on(Libraries.LINE_SEPARATOR).join(encodedStringSet).getBytes());
      } catch (IOException e) {
        throw new CompilerIoException("Failed to write " + entryName + " as a String set.", e);
      }
    }

    private void writeVersionNumber() {
      writeString(
          Libraries.VERSION_NUMBER_ENTRY_NAME, Integer.toString(ZipLibraries.versionNumber));
    }
  }

  private Map<String, Resource> buildResourcesByPath = Maps.newHashMap();
  private Map<String, CompilationUnit> compilationUnitsByTypeSourceName = Maps.newHashMap();
  private Set<String> dependencyLibraryNames = Sets.newHashSet();
  private ArtifactSet generatedArtifacts = new ArtifactSet();
  private String libraryName;
  private Multimap<String, String> nestedBinaryNamesByCompilationUnitName =
      LinkedHashMultimap.create();
  private Multimap<String, String> nestedSourceNamesByCompilationUnitName =
      LinkedHashMultimap.create();
  private Multimap<String, String> newBindingPropertyValuesByName = LinkedHashMultimap.create();
  private Multimap<String, String> newConfigurationPropertyValuesByName =
      LinkedHashMultimap.create();
  private ZipEntryBackedObject<PermutationResult> permutationResultHandle;
  private Map<String, Resource> publicResourcesByPath = Maps.newHashMap();
  private Set<String> ranGeneratorNames = Sets.newHashSet();
  private Set<String> reboundTypeSourceNames = Sets.newHashSet();
  private Set<String> regularClassFilePaths = Sets.newHashSet();
  private Set<String> regularCompilationUnitTypeSourceNames = Sets.newLinkedHashSet();
  private Set<String> superSourceClassFilePaths = Sets.newHashSet();
  private Set<String> superSourceCompilationUnitTypeSourceNames = Sets.newLinkedHashSet();
  private ZipWriter zipWriter;

  public ZipLibraryWriter(String fileName) {
    zipWriter = new ZipWriter(fileName);
  }

  @Override
  public void addBuildResource(Resource buildResource) {
    buildResourcesByPath.put(buildResource.getPath(), buildResource);
  }

  @Override
  public void addCompilationUnit(CompilationUnit compilationUnit) {
    assert !compilationUnit.isError() : "Invalid units should be pruned before writing.";
    assert !compilationUnitsByTypeSourceName.containsKey(
        compilationUnit.getTypeName()) : "Units should be deduped before writing.";

    if (compilationUnit.isSuperSource()) {
      superSourceCompilationUnitTypeSourceNames.add(compilationUnit.getTypeName());
    } else {
      regularCompilationUnitTypeSourceNames.add(compilationUnit.getTypeName());
    }
    compilationUnitsByTypeSourceName.put(compilationUnit.getTypeName(), compilationUnit);

    nestedSourceNamesByCompilationUnitName.removeAll(compilationUnit.getTypeName());
    nestedBinaryNamesByCompilationUnitName.removeAll(compilationUnit.getTypeName());
    Collection<CompiledClass> compiledClasses = compilationUnit.getCompiledClasses();
    for (CompiledClass compiledClass : compiledClasses) {
      nestedSourceNamesByCompilationUnitName.put(compilationUnit.getTypeName(),
          compiledClass.getSourceName());
      nestedBinaryNamesByCompilationUnitName.put(compilationUnit.getTypeName(),
          InternalName.toBinaryName(compiledClass.getInternalName()));
    }

    for (CompiledClass compiledClass : compilationUnit.getCompiledClasses()) {
      if (compilationUnit.isSuperSource()) {
        String classFilePath = compiledClass.getInternalName();
        superSourceClassFilePaths.add(Libraries.computeClassFileName(classFilePath));
        zipWriter.writeClassFile(classFilePath, compiledClass.getBytes());
      } else {
        String classFilePath = compiledClass.getInternalName();
        regularClassFilePaths.add(Libraries.computeClassFileName(classFilePath));
        zipWriter.writeClassFile(classFilePath, compiledClass.getBytes());
      }
    }

    zipWriter.writeCompilationUnitFile(compilationUnit);
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
    this.generatedArtifacts.addAll(generatedArtifacts);
  }

  @Override
  public void addNewBindingPropertyValuesByName(
      String propertyName, Iterable<String> propertyValues) {
    newBindingPropertyValuesByName.putAll(propertyName, propertyValues);
  }

  @Override
  public void addNewConfigurationPropertyValuesByName(
      String propertyName, Iterable<String> propertyValues) {
    newConfigurationPropertyValuesByName.putAll(propertyName, propertyValues);
  }

  @Override
  public void addPublicResource(Resource publicResource) {
    publicResourcesByPath.put(publicResource.getPath(), publicResource);
  }

  @Override
  public void addRanGeneratorName(String generatorName) {
    ranGeneratorNames.add(generatorName);
  }

  @Override
  public Multimap<String, String> getNewBindingPropertyValuesByName() {
    return Multimaps.unmodifiableMultimap(newBindingPropertyValuesByName);
  }

  @Override
  public Multimap<String, String> getNewConfigurationPropertyValuesByName() {
    return Multimaps.unmodifiableMultimap(newConfigurationPropertyValuesByName);
  }

  @Override
  public ZipEntryBackedObject<PermutationResult> getPermutationResultHandle() {
    if (permutationResultHandle == null) {
      permutationResultHandle = zipWriter.getPermutationResultHandle();
    }
    return permutationResultHandle;
  }

  @Override
  public Set<String> getReboundTypeSourceNames() {
    return Collections.unmodifiableSet(reboundTypeSourceNames);
  }

  @Override
  public void setLibraryName(String libraryName) {
    this.libraryName = libraryName;
  }

  @Override
  public void setReboundTypeSourceNames(Set<String> reboundTypeSourceNames) {
    this.reboundTypeSourceNames = reboundTypeSourceNames;
  }

  @Override
  public void write() {
    zipWriter.write();
  }
}
