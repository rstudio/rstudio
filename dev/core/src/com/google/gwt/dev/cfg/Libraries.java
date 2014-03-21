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

import com.google.gwt.dev.util.Name;

/**
 * Utility functions and constants for reading and writing library files.
 */
public class Libraries {

  /**
   * Indicates that an attempt to load a library failed because the version in the provided data did
   * not match the version required by current code.
   */
  public static class IncompatibleLibraryVersionException extends Exception {

    private int actualVersionNumber;
    private int requiredVersionNumber;

    public IncompatibleLibraryVersionException(int requiredVersionNumber, int actualVersionNumber) {
      this.requiredVersionNumber = requiredVersionNumber;
      this.actualVersionNumber = actualVersionNumber;
    }

    @Override
    public String getMessage() {
      return "An attempt to load a library failed because the version in the provided data ("
          + actualVersionNumber + ") did" + " not match the version required by current code ("
          + requiredVersionNumber + ".";
    }
  }

  public static final String BUILD_RESOURCE_PATHS_ENTRY_NAME = "buildResourcePaths.txt";
  public static final String DEPENDENCY_LIBRARY_NAMES_ENTRY_NAME = "dependencyLibraryNames.txt";
  public static final String DIRECTORY_BUILD_RESOURCES = "buildResources/";
  public static final String DIRECTORY_BYTECODE = "bytecode/";
  public static final String DIRECTORY_COMPILATION_UNITS = "compilationUnits/";
  public static final String DIRECTORY_GENERATED_ARTIFACTS = "generatedArtifacts/";
  public static final String DIRECTORY_PUBLIC_RESOURCES = "publicResources/";
  public static final String EXTENSION_CLASS_FILE = ".class";
  public static final String EXTENSION_COMPILATION_UNITS = ".compilationUnit";
  public static final String GENERATED_ARTIFACT_NAMES_ENTRY_NAME = "generatedArtifactNames.txt";
  public static final char ENCODE_PREFIX = '%';
  public static final char KEY_VALUE_SEPARATOR = ':';
  public static final String LIBRARY_NAME_ENTRY_NAME = "libraryName.txt";
  public static final char LINE_SEPARATOR = '\n';
  public static final String NESTED_BINARY_NAMES_BY_ENCLOSING_NAME_ENTRY_NAME =
      "nestedBinaryNamesByEnclosingName.txt";
  public static final String NESTED_SOURCE_NAMES_BY_ENCLOSING_NAME_ENTRY_NAME =
      "nestedSourceNamesByEnclosingName.txt";
  public static final String NEW_BINDING_PROPERTY_VALUES_BY_NAME_ENTRY_NAME =
      "newBindingPropertyValuesByName.txt";
  public static final String NEW_CONFIGURATION_PROPERTY_VALUES_BY_NAME_ENTRY_NAME =
      "newConfigurationPropertyValuesByName.txt";
  public static final String PERMUTATION_RESULT_ENTRY_NAME = "permutationResult.ser";
  public static final String PUBLIC_RESOURCE_PATHS_ENTRY_NAME = "publicResourcePaths.txt";
  public static final String RAN_GENERATOR_NAMES_ENTRY_NAME = "ranGeneratorNames.txt";
  public static final String REBOUND_TYPE_SOURCE_NAMES_ENTRY_NAME = "reboundTypeSourceNames.txt";
  public static final String REGULAR_CLASS_FILE_PATHS_ENTRY_NAME = "regularClassFilePaths.txt";
  public static final String REGULAR_COMPILATION_UNIT_TYPE_SOURCE_NAMES_ENTRY_NAME =
      "regularCompilationUnitTypeSourceNames.txt";
  public static final String SUPER_SOURCE_CLASS_FILE_PATHS_ENTRY_NAME =
      "superSourceClassFilePaths.txt";
  public static final String SUPER_SOURCE_COMPILATION_UNIT_TYPE_SOURCE_NAMES_ENTRY_NAME =
      "superSourceCompilationUnitTypeSourceNames.txt";
  public static final char VALUE_SEPARATOR = ',';
  public static final String VERSION_NUMBER_ENTRY_NAME = "versionNumber.txt";

  /**
   * Computes a consistent full path with extension at which to store a given class file path.
   */
  public static String computeClassFileEntryName(String classFilePath) {
    return DIRECTORY_BYTECODE + classFilePath + EXTENSION_CLASS_FILE;
  }

  /**
   * Adds class file extension to provided class file path in a consistent way.
   */
  public static String computeClassFileName(String classFilePath) {
    return classFilePath + EXTENSION_CLASS_FILE;
  }

  /**
   * Computes a consistent full path, escaped and with extension at which to store a given
   * compilation unit path.
   */
  public static String computeCompilationUnitEntryName(String compilationUnitTypeSourceName) {
    assert Name.isSourceName(compilationUnitTypeSourceName);
    return DIRECTORY_COMPILATION_UNITS + compilationUnitTypeSourceName.replace(".", "/")
        + EXTENSION_COMPILATION_UNITS;
  }
}
