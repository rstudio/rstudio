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

import com.google.gwt.thirdparty.guava.common.collect.Maps;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Map;

/**
 * Represents a resource contained in a directory on a file system.
 * <p>
 * The class is immutable and automatically interned.
 */
public class FileResource extends AbstractResource {

  private static Map<String, FileResource> fileResourceByKey = Maps.newHashMap();

  public static FileResource create(DirectoryClassPathEntry classPathEntry, String abstractPathName,
      File file) {
    // Intern the file resource.
    String classPathLocation = classPathEntry != null ? classPathEntry.getLocation() : "null";
    String key = classPathLocation + File.pathSeparator + abstractPathName + File.pathSeparator
        + file.getAbsolutePath();
    FileResource fileResource = fileResourceByKey.get(key);
    if (fileResource == null) {
      fileResource = new FileResource(classPathEntry, abstractPathName, file);
      fileResourceByKey.put(key, fileResource);
    }

    return fileResource;
  }

  private final String abstractPathName;
  private final WeakReference<DirectoryClassPathEntry> classPathEntryReference;
  private final File file;

  private FileResource(DirectoryClassPathEntry classPathEntry, String abstractPathName, File file) {
    assert (file.isFile()) : file + " is not a file.";
    this.classPathEntryReference = new WeakReference<DirectoryClassPathEntry>(classPathEntry);
    this.abstractPathName = abstractPathName;
    this.file = file;
  }

  @Override
  public DirectoryClassPathEntry getClassPathEntry() {
    DirectoryClassPathEntry classPathEntry = classPathEntryReference.get();
    assert classPathEntry != null;
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
}
