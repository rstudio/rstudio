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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.zip.ZipEntry;

/**
 * Represents a resource contained in a jar or zip file.
 */
public class ZipFileResource extends AbstractResource {

  private final ZipFileClassPathEntry classPathEntry;
  private final ZipEntry zipEntry;

  public ZipFileResource(ZipFileClassPathEntry classPathEntry, ZipEntry zipEntry) {
    this.classPathEntry = classPathEntry;
    this.zipEntry = zipEntry;
  }

  @Override
  public ZipFileClassPathEntry getClassPathEntry() {
    return classPathEntry;
  }

  @Override
  public String getLocation() {
    String proto = zipEntry instanceof JarEntry ? "jar:" : "zip:";
    return proto + classPathEntry.getLocation() + "!/" + getPath();
  }

  @Override
  public String getPath() {
    return zipEntry.getName();
  }

  @Override
  public URL getURL() {
    try {
      return new URL(getLocation());
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  public ZipEntry getZipEntry() {
    return zipEntry;
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
      return classPathEntry.getZipFile().getInputStream(zipEntry);
    } catch (IOException e) {
      // The spec for this method says it can return null.
      return null;
    }
  }
}
