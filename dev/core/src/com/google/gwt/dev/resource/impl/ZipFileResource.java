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

import com.google.gwt.dev.util.StringInterner;
import com.google.gwt.dev.util.Strings;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;

/**
 * Represents a resource contained in a jar or zip file.
 */
public class ZipFileResource extends AbstractResource {

  private final ZipFileClassPathEntry classPathEntry;
  private final String path;
  private final String[] pathParts;

  public ZipFileResource(ZipFileClassPathEntry classPathEntry, String path) {
    this.classPathEntry = classPathEntry;
    this.path = StringInterner.get().intern(path);
    this.pathParts = Strings.splitPath(path);
  }

  @Override
  public ZipFileClassPathEntry getClassPathEntry() {
    return classPathEntry;
  }

  /**
   * Returns the lastModified time of the zip file itself.  Some build environments contain zip file
   * entries that are not time synchronized, causing problems with staleness calculations.
   */
  @Override
  public long getLastModified() {
    return classPathEntry.lastModified();
  }

  @Override
  public String getLocation() {
    // CHECKSTYLE_OFF
    return "jar:" + classPathEntry.getLocation() + "!/" + path;
    // CHECKSTYLE_ON
  }

  @Override
  public String getPath() {
    return path;
  }

  /**
   * @return components of {@link #getPath()}.
   */
  public String[] getPathParts() {
    return pathParts;
  }

  @Override
  public InputStream openContents() throws IOException {
    return classPathEntry.getZipFile().getInputStream(new ZipEntry(path));
  }

  @Override
  public boolean wasRerooted() {
    return false;
  }
}
