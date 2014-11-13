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
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.collect.Lists;
import com.google.gwt.dev.util.msg.Message1String;
import com.google.gwt.thirdparty.guava.common.annotations.VisibleForTesting;
import com.google.gwt.thirdparty.guava.common.collect.Maps;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * A {@link ClassPathEntry} for a directory on the file system.
 */
public class DirectoryClassPathEntry extends ClassPathEntry {

  private static final String GWT_WATCH_FILE_CHANGES_PROPERTY = "gwt.watchFileChanges";
  private static final boolean WATCH_FILE_CHANGES =
      Boolean.parseBoolean(System.getProperty(GWT_WATCH_FILE_CHANGES_PROPERTY, "true"));

  private static class Messages {
    static final Message1String DESCENDING_INTO_DIR = new Message1String(
        TreeLogger.SPAM, "Descending into dir: $0");

    static final Message1String INCLUDING_FILE = new Message1String(
        TreeLogger.DEBUG, "Including file: $0");

    static final Message1String EXCLUDING_FILE = new Message1String(
        TreeLogger.DEBUG, "Excluding file: $0");
  }

  /**
   * Absolute directory.
   */
  private final File dir;
  private final String location;

  /**
   * A cache of previously collected Resource/Resolution pairs for a given PathPrefixSet.
   */
  private final
      Map<PathPrefixSet, Map<AbstractResource, ResourceResolution>>
      resolutionsByResourcePerPathPrefixSet =
          new IdentityHashMap<PathPrefixSet, Map<AbstractResource, ResourceResolution>>();

  /**
   * Whether changed file listening failed either to be started or at some point during execution.
   * If it has failed then no further attempts to restart should be made.
   */
  private boolean listeningFailed;

  /**
   * @param dir an absolute directory
   */
  public DirectoryClassPathEntry(File dir) {
    assert (dir.isAbsolute());
    this.dir = dir;
    this.location = dir.toURI().toString();
  }

  @Override
  public List<Map<AbstractResource, ResourceResolution>> findApplicableResources(
      TreeLogger logger, List<PathPrefixSet> pathPrefixSets) {
    List<Map<AbstractResource, ResourceResolution>> results =
        new ArrayList<Map<AbstractResource, ResourceResolution>>(pathPrefixSets.size());
    for (PathPrefixSet pathPrefixSet : pathPrefixSets) {
      results.add(new IdentityHashMap<AbstractResource, ResourceResolution>());
    }
    descendToFindResources(logger, pathPrefixSets, results, dir, "");
    return results;
  }

  @Override
  public Map<AbstractResource, ResourceResolution> findApplicableResources(TreeLogger logger,
      PathPrefixSet pathPrefixSet) {
    if (!WATCH_FILE_CHANGES) {
      return scanRecursiveDirectory(logger, pathPrefixSet);
    }

    ensureListening(logger, pathPrefixSet);

    if (listeningFailed) {
      return scanRecursiveDirectory(logger, pathPrefixSet);
    }

    boolean haveCachedResults = resolutionsByResourcePerPathPrefixSet.containsKey(pathPrefixSet);
    // If this is the first request and thus the cache is empty.
    if (!haveCachedResults) {
      // Then perform a full scan and cache the results.
      return scanRecursiveDirectory(logger, pathPrefixSet);
    } else {
      try {
        return scanChangedFiles(logger, pathPrefixSet);
      } catch (ExecutionException e) {
        listeningFailed = true;
        logger.log(TreeLogger.WARN, "The attempt to retrieve accumulated file changes in " + dir
            + " failed. Will fall back on full directory scans.");
        return scanRecursiveDirectory(logger, pathPrefixSet);
      }
    }
  }

  private void ensureListening(TreeLogger logger, PathPrefixSet pathPrefixSet) {
    if (!listeningFailed && !DirectoryPathPrefixChangeManager.isListening(this, pathPrefixSet)) {
      try {
        DirectoryPathPrefixChangeManager.ensureListening(this, pathPrefixSet);
      } catch (IOException e) {
        listeningFailed = true;
        logger.log(TreeLogger.WARN, "The attempt to start listening for file changes in " + dir
            + " failed. Will fall back on full directory scans.");
      }
    }
  }

  @VisibleForTesting
  File getDirectory() {
    return dir;
  }

  private synchronized Map<AbstractResource, ResourceResolution> scanChangedFiles(
      TreeLogger logger, PathPrefixSet pathPrefixSet) throws ExecutionException {
    // Get cached results.
    Map<AbstractResource, ResourceResolution> resolutionsByResource =
        resolutionsByResourcePerPathPrefixSet.get(pathPrefixSet);

    // Update cached results.
    Collection<File> changedFiles =
        DirectoryPathPrefixChangeManager.getAndClearChangedFiles(this, pathPrefixSet);
    for (File changedFile : changedFiles) {
      String changedRelativePath = Util.makeRelativePath(dir, changedFile);
      FileResource resource = FileResource.create(this, changedRelativePath, changedFile);

      if (!changedFile.exists()) {
        if (resolutionsByResource.containsKey(resource)) {
          resolutionsByResource.remove(resource);
        }
        continue;
      }

      ResourceResolution resourceResolution = pathPrefixSet.includesResource(changedRelativePath);
      if (resourceResolution != null) {
        Messages.INCLUDING_FILE.log(logger, changedRelativePath, null);
        resolutionsByResource.put(resource, resourceResolution);
      } else {
        Messages.EXCLUDING_FILE.log(logger, changedRelativePath, null);
      }
    }
    return resolutionsByResource;
  }

  private synchronized Map<AbstractResource, ResourceResolution> scanRecursiveDirectory(
      TreeLogger logger, PathPrefixSet pathPrefixSet) {
    Map<AbstractResource, ResourceResolution> resolutionsByResource = Maps.newHashMap();
    descendToFindResources(logger, Lists.create(pathPrefixSet), Lists.create(resolutionsByResource),
        dir, "");

    // Cache results.
    resolutionsByResourcePerPathPrefixSet.put(pathPrefixSet, resolutionsByResource);

    return resolutionsByResource;
  }

  @Override
  public String getLocation() {
    return location;
  }

  /**
   * @param logger logs progress
   * @param pathPrefixSets the sets of path prefixes to determine what resources
   *          are included
   * @param results the accumulating sets of resources (each with the
   *          corresponding pathPrefix) found
   * @param dir the file or directory to consider
   * @param dirPath the abstract path name associated with 'parent', which
   *          explicitly does not include the classpath entry in its path
   */
  @VisibleForTesting
  void descendToFindResources(TreeLogger logger,
      List<PathPrefixSet> pathPrefixSets,
      List<Map<AbstractResource, ResourceResolution>> results, File dir, String dirPath) {
    assert (dir.isDirectory()) : dir + " is not a directory";
    int len = pathPrefixSets.size();

    // Assert: this directory is included in the path prefix set.

    File[] children = dir.listFiles();
    for (File child : children) {
      String childPath = dirPath + child.getName();
      if (child.isDirectory()) {
        if (child.isHidden()) {
          // Don't look inside of intentionally hidden directories. It's just a waste of time,
          // sometimes lots of time.
          continue;
        }
        String childDirPath = childPath + "/";
        for (int i = 0; i < len; ++i) {
          if (pathPrefixSets.get(i).includesDirectory(childDirPath)) {
            Messages.DESCENDING_INTO_DIR.log(logger, child.getPath(), null);
            descendToFindResources(logger, pathPrefixSets, results, child,
                childDirPath);
            break;
          }
        }
      } else if (child.isFile()) {
        for (int i = 0; i < len; ++i) {
          ResourceResolution resourceResolution = null;
          if ((resourceResolution = pathPrefixSets.get(i).includesResource(childPath)) != null) {
            Messages.INCLUDING_FILE.log(logger, childPath, null);
            FileResource r = FileResource.create(this, childPath, child);
            results.get(i).put(r, resourceResolution);
          } else {
            Messages.EXCLUDING_FILE.log(logger, childPath, null);
          }
        }
      }
    }
  }
}
