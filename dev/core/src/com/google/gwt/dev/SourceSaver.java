/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.EmittedArtifact;
import com.google.gwt.core.ext.linker.EmittedArtifact.Visibility;
import com.google.gwt.core.linker.SymbolMapsLinker.SourceMapArtifact;
import com.google.gwt.dev.Link.LinkOptions;
import com.google.gwt.dev.cfg.ResourceLoader;
import com.google.gwt.dev.json.JsonArray;
import com.google.gwt.dev.json.JsonException;
import com.google.gwt.dev.json.JsonObject;
import com.google.gwt.dev.util.OutputFileSet;
import com.google.gwt.thirdparty.guava.common.collect.Sets;
import com.google.gwt.thirdparty.guava.common.io.ByteStreams;
import com.google.gwt.thirdparty.guava.common.io.Resources;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * SourceSaver writes source code useful for a debugger.
 */
class SourceSaver {

  /**
   * Copies all source files referenced in at least one sourcemap to the appropriate destination.
   *
   * @param artifacts contains the sourcemaps we need source code for
   *   and also generated source code to copy.
   * @param loader contains the non-generated source code
   * @param modulePrefix a prefix to add to a source file's path when writing it.
   */
  static void save(TreeLogger logger, ArtifactSet artifacts, ResourceLoader loader,
      LinkOptions options, String modulePrefix, OutputFileSet extraFileSet)
      throws IOException, UnableToCompleteException {

    // First scan artifacts for useful files.

    Set<EmittedArtifact> genFiles = new LinkedHashSet<EmittedArtifact>();
    Set<EmittedArtifact> sourceMaps = new LinkedHashSet<EmittedArtifact>();
    for (EmittedArtifact candidate : artifacts.find(EmittedArtifact.class)) {

      if (candidate.getVisibility() == Visibility.Source) {
        genFiles.add(candidate);
        continue;
      }

      boolean isSourceMap =
          SourceMapArtifact.isSourceMapFile.matcher(candidate.getPartialPath()).find();
      if (isSourceMap) {
        sourceMaps.add(candidate);
      }
    }

    if (sourceMaps.isEmpty()) {
      logger.log(Type.WARN, "Not saving source because sourcemaps weren't generated. " +
          "Hint: set compiler.useSourceMaps.");
      return;
    }

    boolean saveInExtras = options.getSaveSourceOutput() == null ||
        options.getSaveSourceOutput().equals(options.getExtraDir());

    OutputFileSet out;
    if (saveInExtras) {
      out = extraFileSet;
      logger.log(Type.INFO, "Saving source with extras");
    } else {
      out = Link.chooseOutputFileSet(options.getSaveSourceOutput(), modulePrefix);
      logger.log(Type.INFO, "Saving source to " + options.getSaveSourceOutput());
    }

    try {
      copySources(logger, sourceMaps, genFiles, loader, out, "src/");
    } finally {
      if (!saveInExtras) {
        out.close();
      }
    }
  }

  private static void copySources(TreeLogger logger,
      Set<EmittedArtifact> sourceMaps,
      Set<EmittedArtifact> genFiles, ResourceLoader loader,
      OutputFileSet dest, String destPrefix)
      throws UnableToCompleteException {

    Set<String> filesInSourceMap = getSourcePaths(logger, sourceMaps);

    // All files in the source map should be either be resources (input source files) or
    // in the ArtifactSet (generated files). Try both places and log a warning if we
    // can't find it anywhere.

    // First, copy input source files..
    Set<String> remainingFiles = Sets.newLinkedHashSet();
    for (String path : filesInSourceMap) {
      try {
        if (!copySourceFile(path, loader, dest, destPrefix)) {
          remainingFiles.add(path);
        }
      } catch (IOException e) {
        logger.log(Type.ERROR, "Unable to copy source file: " + path, e);
        throw new UnableToCompleteException();
      }
    }

    // Next, copy generated files.
    for (EmittedArtifact candidate : genFiles) {
      if (!remainingFiles.contains(candidate.getPartialPath())) {
        // This file was generated but not used according to the sourcemap.
        // Perhaps an interface? Anyway we don't need it for debugging.
        continue;
      }
      copyGeneratedFile(logger, candidate, dest, destPrefix);
      remainingFiles.remove(candidate.getPartialPath());
    }

    // Nothing should be left. If there is, log a warning.
    if (!remainingFiles.isEmpty()) {
      logger.log(Type.WARN, "Unable to find all source code needed by debuggers. " +
          remainingFiles.size() + " files from sourcemaps weren't found.");
      if (logger.isLoggable(Type.DEBUG)) {
        TreeLogger missing = logger.branch(Type.DEBUG, "Missing files:");
        int filesPrinted = 0;
        for (String path : remainingFiles) {
          if (filesPrinted >= 100) {
            missing.log(Type.DEBUG, "(truncated)");
            break;
          }
          missing.log(Type.DEBUG, path);
          filesPrinted++;
        }
      }
    }
  }

  /**
   * Finds the path of each source file that contributed to at least one sourcemap.
   */
  private static Set<String> getSourcePaths(TreeLogger logger, Set<EmittedArtifact> sourceMaps)
      throws UnableToCompleteException {
    Set<String> sourceFiles = new LinkedHashSet<String>();
    for (EmittedArtifact map : sourceMaps) {
      // TODO maybe improve performance by not re-reading the sourcemap files.
      // (We'd need another way for SourceMapRecorder to pass the list of files here.)
      JsonObject json = loadSourceMap(logger, map);
      JsonArray sources = json.get("sources").asArray();
      for (int i = 0; i < sources.getLength(); i++) {
        sourceFiles.add(sources.get(i).asString().getString());
      }
    }
    return sourceFiles;
  }

  /**
   * Reads a sourcemap as a JSON object.
   */
  private static JsonObject loadSourceMap(TreeLogger logger, EmittedArtifact sourceMap)
      throws UnableToCompleteException {
    JsonObject json;
    try {
      InputStream bytes = sourceMap.getContents(logger);
      try {
        json = JsonObject.parse(new InputStreamReader(bytes));
      } finally {
        bytes.close();
      }
    } catch (JsonException e) {
      logger.log(Type.ERROR, "Unable to parse sourcemap: " + sourceMap.getPartialPath(), e);
      throw new UnableToCompleteException();
    } catch (IOException e) {
      logger.log(Type.ERROR, "Unable to read sourcemap: " + sourceMap.getPartialPath(), e);
      throw new UnableToCompleteException();
    }
    return json;
  }

  /**
   * Copies a source file from the module to a directory or jar.
   * Returns false if the source file wasn't found.
   */
  private static boolean copySourceFile(String path, ResourceLoader loader,
      OutputFileSet dest, String destPrefix) throws IOException {

    URL resource = loader.getResource(path);
    if (resource == null) {
      return false;
    }

    try (InputStream resourceAsStream = Resources.asByteSource(resource).openStream();
        OutputStream out = dest.openForWrite(destPrefix + path);) {
      ByteStreams.copy(resourceAsStream, out);
    }

    return true;
  }

  private static void copyGeneratedFile(TreeLogger log, EmittedArtifact src,
      OutputFileSet dest, String destPrefix) throws UnableToCompleteException {

    String newPath = destPrefix + src.getPartialPath();
    try {
      OutputStream out = dest.openForWrite(newPath, src.getLastModified());
      try {
        src.writeTo(log, out);
      } finally {
        out.close();
      }
    } catch (IOException e) {
      log.log(TreeLogger.WARN, "Error emitting artifact: " + newPath, e);
    }
  }
}
