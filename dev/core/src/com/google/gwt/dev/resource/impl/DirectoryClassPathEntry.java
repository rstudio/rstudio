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
import com.google.gwt.thirdparty.guava.common.collect.Maps;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * A {@link ClassPathEntry} for a directory on the file system.
 */
public class DirectoryClassPathEntry extends ClassPathEntry {

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
  public Map<AbstractResource, ResourceResolution> findApplicableResources(TreeLogger logger,
      PathPrefixSet pathPrefixSet) {
    try {
      return ResourceAccumulatorManager.getResources(this, pathPrefixSet);
    } catch (IOException e) {
      // Not using logger because there are plenty of places in the compiler that uses
      // TreeLogger.NULL that causes this problem to be silently ignored.
      System.err.println("The attempt to retrieve files in " + dir + " failed.");
      e.printStackTrace();
      return Maps.newLinkedHashMap();
    }
  }

  public File getDirectory() {
    return dir;
  }

  @Override
  public String getLocation() {
    return location;
  }
}
