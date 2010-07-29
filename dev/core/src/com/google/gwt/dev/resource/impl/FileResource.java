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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Represents a resource contained in directory on a file system.
 */
public class FileResource extends AbstractResource {

  private final String abstractPathName;
  private final DirectoryClassPathEntry classPathEntry;
  private final File file;
  private final long modificationSeconds;

  public FileResource(DirectoryClassPathEntry classPathEntry,
      String abstractPathName, File file) {
    assert (file.isFile());
    this.classPathEntry = classPathEntry;
    this.abstractPathName = abstractPathName;
    this.file = file;
    this.modificationSeconds = lastModifiedSeconds(file);
  }

  @Override
  public DirectoryClassPathEntry getClassPathEntry() {
    return classPathEntry;
  }

  @Override
  public long getLastModified() {
    return file.lastModified();
  }

  @Override
  public String getLocation() {
    return file.getAbsoluteFile().toURI().toString();
  }

  @Override
  public String getPath() {
    return abstractPathName;
  }

  @Override
  public boolean isStale() {
    if (!file.exists()) {
      // File was deleted. Always stale.
      return true;
    }

    long currentModificationSeconds = lastModifiedSeconds(file);
    /*
     * We use != instead of > because the point is to reflect what's actually on
     * the file system, not to worry about freshness per se.
     */
    return (currentModificationSeconds != modificationSeconds);
  }

  @Override
  public InputStream openContents() {
    try {
      return new FileInputStream(file);
    } catch (FileNotFoundException e) {
      return null;
    }
  }

  @Override
  public boolean wasRerooted() {
    return false;
  }

  private long lastModifiedSeconds(File file) {
    return file.lastModified() / 1000;
  }

}
