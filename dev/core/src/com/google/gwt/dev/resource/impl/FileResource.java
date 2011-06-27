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
import java.io.IOException;
import java.io.InputStream;

/**
 * Represents a resource contained in directory on a file system.
 */
public class FileResource extends AbstractResource {

  private final String abstractPathName;
  private final DirectoryClassPathEntry classPathEntry;
  private final File file;

  public FileResource(DirectoryClassPathEntry classPathEntry, String abstractPathName, File file) {
    assert (file.isFile()) : file + " is not a file.";
    this.classPathEntry = classPathEntry;
    this.abstractPathName = abstractPathName;
    this.file = file;
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
    return file.toURI().toString();
  }

  @Override
  public String getPath() {
    return abstractPathName;
  }

  @Override
  public InputStream openContents() throws IOException {
    return new FileInputStream(file);
  }

  @Override
  public boolean wasRerooted() {
    return false;
  }
}
