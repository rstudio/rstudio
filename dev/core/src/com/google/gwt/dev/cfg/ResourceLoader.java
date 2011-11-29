/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.dev.cfg;

import java.net.URL;
import java.util.List;

/**
 * A classpath-like way of loading files.
 * (Must implement equals and hashCode to work as a key in a HashMap.)
 */
public interface ResourceLoader {

  /**
   * Returns the URLs that will be searched in order for files.
   */
  List<URL> getClassPath();

  /**
   * Returns a URL that may be used to load the resource, or null if the
   * resource can't be found.
   *
   * <p> (The API is the same as {@link ClassLoader#getResource(String)}.) </p>
   */
  URL getResource(String resourceName);
}
