/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.resource.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Exposes a URL as a resource.<br />
 *
 * Useful when copying data that was not loaded via the normal resource system (for example .gwt.xml
 * files) into systems that expect a resource object (for example a library builder).
 */
public class UrlResource extends AbstractResource {

  private final long lastModified;
  private final String path;
  private final URL url;

  public UrlResource(URL url, String path, long lastModified) {
    this.url = url;
    this.path = path;
    this.lastModified = lastModified;
  }

  @Override
  public ClassPathEntry getClassPathEntry() {
    throw new UnsupportedOperationException("UrlResources do not come from the class path.");
  }

  @Override
  public long getLastModified() {
    return lastModified;
  }

  @Override
  public String getLocation() {
    return url.toExternalForm();
  }

  @Override
  public String getPath() {
    return path;
  }

  @Override
  public InputStream openContents() throws IOException {
    return url.openStream();
  }
}
