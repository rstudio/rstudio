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
package com.google.gwt.dev.resource;

import java.io.InputStream;
import java.net.URL;

/**
 * Provides information about a file-like resource.
 */
public abstract class Resource {

  /**
   * Overridden to finalize; always returns object identity.
   */
  @Override
  public final boolean equals(Object obj) {
    return super.equals(obj);
  }

  /**
   * Returns the last modified time of the compilation unit.
   */
  public abstract long getLastModified();

  /**
   * Returns the user-relevant location of the resource. No programmatic
   * assumptions should be made about the return value.
   */
  public abstract String getLocation();

  /**
   * Returns the full abstract path of the resource.
   */
  public abstract String getPath();

  /**
   * Returns a URL for this resource; this URL will only be valid for resources
   * based off the file system.
   * 
   * TODO: get rid of this method?
   */
  public abstract URL getURL();

  /**
   * Overridden to finalize; always returns identity hash code.
   */
  @Override
  public final int hashCode() {
    return super.hashCode();
  }

  /**
   * Returns the contents of the resource. May return <code>null</code> if this
   * {@link Resource} has been invalidated by its containing
   * {@link ResourceOracle}. The caller is responsible for closing the stream.
   */
  public abstract InputStream openContents();

  /**
   * Overridden to finalize; always returns {@link #getLocation()}.
   */
  @Override
  public final String toString() {
    return getLocation();
  }

  public abstract boolean wasRerooted();

}
