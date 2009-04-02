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

import java.util.Map;
import java.util.Set;

/**
 * An abstraction for finding and retrieving {@link Resource}s by abstract path
 * name. Intuitively, it works like a jar in that each URL is uniquely located
 * somewhere in an abstract namespace. The abstract names must be constructed
 * from a series of zero or more valid Java identifiers followed by the '/'
 * character and finally ending in a valid filename, for example,
 * <code>com/google/gwt/blah.txt</code>.
 * 
 * <p>
 * The identity of the returned sets and maps will change exactly when the
 * underlying module is refreshed.
 * </p>
 * 
 * <p>
 * Even when the identity of a returned set changes, the identity of any
 * contained {@link Resource} values is guaranteed to differ from a previous
 * result exactly when that particular resource becomes invalid.
 * </p>
 * 
 * <p>
 * A resource could become invalid for various reasons, including:
 * <ul>
 * <li>the underlying file was deleted or modified</li>
 * <li>another file with the same logical name superceded it on the classpath</li>
 * <li>the underlying module changed to exclude this file or supercede it with
 * another file</li>
 * </ul>
 * </p>
 * 
 * <p>
 * After a refresh, a client can reliably detect changes by checking which of
 * its cached resource is still contained in the new result of
 * {@link #getResources()}.
 * </p>
 */
public interface ResourceOracle {

  /**
   * Frees up all existing resources and transient internal state. All returned
   * collections will be empty after this call until this ResoruceOracle is
   * refreshed.
   */
  void clear();

  /**
   * Returns an unmodifiable set of unique abstract path names with constant
   * lookup time.
   */
  Set<String> getPathNames();

  /**
   * Returns an unmodifiable map of abstract path name to resource.
   */
  Map<String, Resource> getResourceMap();

  /**
   * Returns an unmodifiable set of unique resources with constant lookup time.
   */
  Set<Resource> getResources();
}
