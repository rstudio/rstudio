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

/**
 * Represents a resource contained in a jar file.
 */
public class JarFileResource extends AbstractResource {

  private final JarFileClassPathEntry classPathEntry;
  private final JarEntry jarEntry;

  public JarFileResource(JarFileClassPathEntry classPathEntry, JarEntry jarEntry) {
    this.classPathEntry = classPathEntry;
    this.jarEntry = jarEntry;
  }

  @Override
  public JarFileClassPathEntry getClassPathEntry() {
    return classPathEntry;
  }

  public JarEntry getJarEntry() {
    return jarEntry;
  }

  @Override
  public String getLocation() {
    return "jar:" + classPathEntry.getLocation() + "!/" + getPath();
  }

  @Override
  public String getPath() {
    return jarEntry.getName();
  }

  @Override
  public URL getURL() {
    try {
      return new URL(getLocation());
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Since we don't dynamically reload jars during a run, jar-based resources
   * cannot become stale.
   */
  @Override
  public boolean isStale() {
    return false;
  }

  @Override
  public InputStream openContents() {
    try {
      return classPathEntry.getJarFile().getInputStream(jarEntry);
    } catch (IOException e) {
      // The spec for this method says it can return null.
      return null;
    }
  }
}
