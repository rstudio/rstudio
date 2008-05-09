/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.dev.resource.impl;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.util.msg.Message1String;

import java.io.File;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * A classpath entry that is a jar file.
 */
public class JarFileClassPathEntry extends ClassPathEntry {

  /**
   * Logger messages related to this class.
   */
  private static class Messages {
    static final Message1String BUILDING_INDEX = new Message1String(
        TreeLogger.TRACE, "Indexing jar file: $0");

    static final Message1String EXCLUDING_RESOURCE = new Message1String(
        TreeLogger.DEBUG, "Excluding $0");

    static final Message1String FINDING_INCLUDED_RESOURCES = new Message1String(
        TreeLogger.DEBUG, "Searching for included resources in $0");

    static final Message1String INCLUDING_RESOURCE = new Message1String(
        TreeLogger.DEBUG, "Including $0");

    static final Message1String READ_JAR_ENTRY = new Message1String(
        TreeLogger.DEBUG, "$0");
  }

  private Set<JarFileResource> allJarFileResources;
  private Set<AbstractResource> cachedAnswers;
  private final JarFile jarFile;
  private PathPrefixSet lastPrefixSet;

  public JarFileClassPathEntry(JarFile jarFile) {
    this.jarFile = jarFile;
  }

  /**
   * Indexes the jar file on-demand, and only once over the life of the process.
   */
  @Override
  public Set<AbstractResource> findApplicableResources(TreeLogger logger,
      PathPrefixSet pathPrefixSet) {
    // Never re-index.
    if (allJarFileResources == null) {
      allJarFileResources = buildIndex(logger);
    }

    if (cachedAnswers == null || lastPrefixSet != pathPrefixSet
        || lastPrefixSet.getModCount() != pathPrefixSet.getModCount()) {
      cachedAnswers = computeApplicableResources(logger, pathPrefixSet);
    }

    return cachedAnswers;
  }

  public JarFile getJarFile() {
    return jarFile;
  }

  @Override
  public String getLocation() {
    return new File(jarFile.getName()).toURI().toString();
  }

  private Set<JarFileResource> buildIndex(TreeLogger logger) {
    logger = Messages.BUILDING_INDEX.branch(logger, jarFile.getName(), null);

    HashSet<JarFileResource> results = new HashSet<JarFileResource>();
    Enumeration<JarEntry> e = jarFile.entries();
    while (e.hasMoreElements()) {
      JarEntry jarEntry = e.nextElement();
      if (jarEntry.isDirectory()) {
        // Skip directories.
        continue;
      }
      if (jarEntry.getName().startsWith("META-INF/")) {
        // Skip META-INF since classloaders normally make this invisible.
        continue;
      }
      JarFileResource jarResource = new JarFileResource(this, jarEntry);
      results.add(jarResource);
      Messages.READ_JAR_ENTRY.log(logger, jarEntry.getName(), null);
    }
    return results;
  }

  private Set<AbstractResource> computeApplicableResources(TreeLogger logger,
      PathPrefixSet pathPrefixSet) {
    logger = Messages.FINDING_INCLUDED_RESOURCES.branch(logger,
        jarFile.getName(), null);

    Set<AbstractResource> results = new HashSet<AbstractResource>();
    for (JarFileResource r : allJarFileResources) {
      String path = r.getPath();
      if (pathPrefixSet.includesResource(path)) {
        Messages.INCLUDING_RESOURCE.log(logger, path, null);
        results.add(r);
      } else {
        Messages.EXCLUDING_RESOURCE.log(logger, path, null);
      }
    }
    return results;
  }
}
