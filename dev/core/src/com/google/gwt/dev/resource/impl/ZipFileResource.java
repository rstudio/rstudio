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

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * Represents a resource contained in a jar or zip file.
 */
public class ZipFileResource extends AbstractResource {

  private final ZipFileClassPathEntry classPathEntry;
  private final String path;

  public ZipFileResource(ZipFileClassPathEntry classPathEntry, String path) {
    this.classPathEntry = classPathEntry;
    this.path = path;
  }

  @Override
  public ZipFileClassPathEntry getClassPathEntry() {
    return classPathEntry;
  }

  @Override
  public long getLastModified() {
    return getEntry().getTime();
  }

  @Override
  public String getLocation() {
    // CHECKSTYLE_OFF
    String proto = classPathEntry.getZipFile() instanceof JarFile ? "jar:"
        : "zip:";
    // CHECKSTYLE_ON
    return proto + classPathEntry.getLocation() + "!/" + path;
  }

  @Override
  public String getPath() {
    return path;
  }

  /**
   * Since we don't dynamically reload zips during a run, zip-based resources
   * cannot become stale.
   */
  @Override
  public boolean isStale() {
    return false;
  }

  @Override
  public InputStream openContents() {
    try {
      return classPathEntry.getZipFile().getInputStream(getEntry());
    } catch (IOException e) {
      // The spec for this method says it can return null.
      return null;
    }
  }

  @Override
  public boolean wasRerooted() {
    return false;
  }

  private ZipEntry getEntry() {
    return classPathEntry.getZipFile().getEntry(path);
  }
}
