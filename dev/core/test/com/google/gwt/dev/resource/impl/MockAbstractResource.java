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

import junit.framework.Assert;

import java.io.InputStream;

public final class MockAbstractResource extends AbstractResource {
  private boolean isStale;
  private final MockClassPathEntry mockClassPathEntry;
  private final String path;

  public MockAbstractResource(MockClassPathEntry mockClassPathEntry, String path) {
    this.mockClassPathEntry = mockClassPathEntry;
    this.path = path;
  }

  @Override
  public ClassPathEntry getClassPathEntry() {
    return this.mockClassPathEntry;
  }

  @Override
  public long getLastModified() {
    return 0;
  }

  @Override
  public String getLocation() {
    return this.mockClassPathEntry.pathRoot + "/" + path;
  }

  @Override
  public String getPath() {
    return path;
  }

  @Override
  public InputStream openContents() {
    Assert.fail("Not implemented");
    return null;
  }

  public void setStale(boolean isStale) {
    this.isStale = isStale;
  }

  @Override
  public boolean wasRerooted() {
    return false;
  }
}