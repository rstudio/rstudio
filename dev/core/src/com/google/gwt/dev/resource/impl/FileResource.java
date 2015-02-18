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

import com.google.gwt.thirdparty.guava.common.collect.MapMaker;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentMap;

/**
 * Represents a resource contained in a directory on a file system.
 * <p>The class is immutable and automatically interned.
 */
public class FileResource extends AbstractResource {
  /*
   * The #equals and #hashCode is locked to work on identities by the base class Resource.
   * Without interning, it would be impractical to use this class as a key in a map.
   */

  private static final ConcurrentMap<String, FileResource> canonicalFileResources =
      new MapMaker().weakValues().makeMap();

  public static FileResource of(String abstractPathName, File file) {
    String key = abstractPathName + "@" + file.getAbsolutePath();
    FileResource sample = new FileResource(abstractPathName, file);
    FileResource canonical = canonicalFileResources.putIfAbsent(key, sample);
    return (canonical == null) ? sample : canonical;
  }

  private final String abstractPathName;
  private final File file;

  private FileResource(String abstractPathName, File file) {
    this.abstractPathName = abstractPathName;
    this.file = file;
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
