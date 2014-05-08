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
import com.google.gwt.dev.util.collect.Lists;
import com.google.gwt.dev.util.msg.Message1String;

import java.io.File;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link ClassPathEntry} for a directory on the file system.
 */
public class DirectoryClassPathEntry extends ClassPathEntry {

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
    for (int i = 0, c = pathPrefixSets.size(); i < c; ++i) {
      results.add(new IdentityHashMap<AbstractResource, ResourceResolution>());
    }
    descendToFindResources(logger, pathPrefixSets, results, dir, "");
    return results;
  }

  @Override
  public Map<AbstractResource, ResourceResolution> findApplicableResources(TreeLogger logger,
      PathPrefixSet pathPrefixSet) {
    Map<AbstractResource, ResourceResolution> results =
        new IdentityHashMap<AbstractResource, ResourceResolution>();
    descendToFindResources(logger, Lists.create(pathPrefixSet), Lists.create(results), dir, "");
    return results;
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
  private void descendToFindResources(TreeLogger logger,
      List<PathPrefixSet> pathPrefixSets,
      List<Map<AbstractResource, ResourceResolution>> results, File dir, String dirPath) {
    assert (dir.isDirectory()) : dir + " is not a directory";
    int len = pathPrefixSets.size();

    // Assert: this directory is included in the path prefix set.

    File[] children = dir.listFiles();
    for (File child : children) {
      String childPath = dirPath + child.getName();
      if (child.isDirectory()) {
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
            FileResource r = new FileResource(this, childPath, child);
            results.get(i).put(r, resourceResolution);
          } else {
            Messages.EXCLUDING_FILE.log(logger, childPath, null);
          }
        }
      }
    }
  }
}
