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

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipFile;

/**
 * Represents a resource contained in a jar or zip file.
 */
public class ZipFileResource extends AbstractResource {

  private ZipFileClassPathEntry classPathEntry;
  private final String entryName;
  private String filePath;
  private final long lastModified;
  private final String[] pathParts;
  private final ZipFile zipFile;

  /**
   * Constructs a ZipFileResource from a jar class path entry and a zip entry name.<br />
   */
  public ZipFileResource(ZipFileClassPathEntry classPathEntry, String entryName) {
    this.classPathEntry = classPathEntry;
    this.entryName = StringInterner.get().intern(entryName);
    this.pathParts = entryName.split("/");

    this.zipFile = classPathEntry.getZipFile();
    this.filePath = classPathEntry.getLocation();
    assert filePath.startsWith("file:/");
    this.lastModified = classPathEntry.lastModified();
  }

  @Override
  public long getLastModified() {
    return lastModified;
  }

  @Override
  public String getLocation() {
    return "jar:" + filePath + "!/" + entryName;
  }

  @Override
  public String getPath() {
    return entryName;
  }

  /**
   * Returns the components of {@link #getPath()}.
   */
  public String[] getPathParts() {
    return pathParts;
  }

  @Override
  public InputStream openContents() throws IOException {
    return zipFile.getInputStream(zipFile.getEntry(entryName));
  }
}
