/*
 * Copyright 2011 Google Inc.
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
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.javac.CompilationStateBuilder;
import com.google.gwt.dev.javac.CompilationUnitArchive;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles loading archived modules into the CompilationState for the Compiler
 * and DevMode.
 */
public class ArchivePreloader {
  static final boolean ARCHIVES_ENABLED = Boolean.valueOf(System.getProperty("gwt.usearchives",
      "true"));
  private static Map<String, Long> alreadyLoaded = new HashMap<String, Long>();

  /**
   * Load any .gwtar files into the cache before building CompilationState.
   */
  static void preloadArchives(TreeLogger logger, ModuleDef module) {
    if (!ArchivePreloader.ARCHIVES_ENABLED) {
      return;
    }

    logger.log(TreeLogger.TRACE,
        "Looking for precompiled archives.  To disable, use -Dgwt.usearchives=false");

    SpeedTracerLogger.Event loadArchive = SpeedTracerLogger.start(CompilerEventType.LOAD_ARCHIVE);
    try {
      Collection<URL> archiveURLs = module.getAllCompilationUnitArchiveURLs();

      for (URL archiveURL : archiveURLs) {
        Long lastModifiedTime = Util.getResourceModifiedTime(archiveURL);
        String toLoad = archiveURL.toExternalForm();
        Long previousLoadTime = alreadyLoaded.get(toLoad);
        if (previousLoadTime == null || !previousLoadTime.equals(lastModifiedTime)) {
          logger.log(TreeLogger.TRACE, "Loading archived module: " + archiveURL);
          try {
            CompilationUnitArchive archive = CompilationUnitArchive.createFromURL(archiveURL);
            // Pre-populate CompilationStateBuilder with .gwtar files
            CompilationStateBuilder.addArchive(archive);
          } catch (IOException ex) {
            logger.log(TreeLogger.WARN, "Unable to read: " + archiveURL + ". Skipping: " + ex);
          } catch (ClassNotFoundException ex) {
            logger.log(TreeLogger.WARN, "Incompatible archived module: " + archiveURL
                + ". Skipping: " + ex);
          }
          // Mark it loaded whether or not it worked. We don't want to continue
          // to try and fail.
          alreadyLoaded.put(toLoad, lastModifiedTime);
        } else {
          logger.log(TreeLogger.TRACE, "Skipping already loaded archive: " + archiveURL);
        }
      }
    } finally {
      loadArchive.end();
    }
  }
}
