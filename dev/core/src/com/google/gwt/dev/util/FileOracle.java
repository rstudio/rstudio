/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.dev.util;

import java.net.URL;

/**
 * An abstraction for finding and retrieving a set of URLs by logical name.
 * Intuitively, it works like a jar in that each URL is uniquely located
 * somewhere in an abstract namespace. The abstract names must be constructed
 * from a series of zero or more valid Java identifiers followed the '/'
 * character and finally ending in a valid filename, for example,
 * "com/google/gwt/blah.txt". Each contained abstract path corresponds to a
 * physical URL.
 */
public abstract class FileOracle {

  /**
   * Finds a URL by abstract path.
   * 
   * @param abstractPath the abstract path of the URL to find.
   * @return the physical URL of the contained URL, or <code>null</code> the
   *         abstract path does not refer to a contained URL.
   */
  public abstract URL find(String abstractPath);

  /**
   * Gets the abstract path for every URL indexed by this FileOracle. Elements
   * of the result set can be passed into {@link #find(String)} to retrieve the
   * physical URL.
   * 
   * @return the abstract path of every URL indexed by this FileOracle
   */
  public abstract String[] getAllFiles();

  /**
   * Tests if this FileOracle has URLs.
   * 
   * @return <tt>true</tt> if this list has no elements; <tt>false</tt>
   *         otherwise.
   */
  public abstract boolean isEmpty();

}
