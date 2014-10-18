/*
 * Copyright 2014 Google Inc.
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
package com.google.gwt.core.ext.impl;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.resource.ResourceOracle;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Temporary internal utility functions for locating Resources.
 */
public class ResourceLocatorImpl {

  private static final int CLASS_LOADER_LOAD_REPORT_LIMIT = 10;

  private static int classLoaderLoadCount;

  public static void resetClassLoaderLoadWarningCount() {
    classLoaderLoadCount = 0;
  }

  /**
   * Returns an InputStream for the given resource as found in the ResourceOracle.
   * <p>
   * Returns null if a resource is unavailable or the stream fails to open. Matching the
   * ClassLoader.getResourceAsStream() semantic.
   * <p>
   * For backwards compatibility it will fallback on finding via ClassLoader and warn the user about
   * the problems this can cause in per-file compiles.
   * <p>
   * Should only be used by internal Generators and only till ClassLoader fallback is deprecated.
   */
  // TODO(stalcup): migrate internal Generators away from needing ClassLoader fallback.
  public static InputStream tryFindResourceAsStream(TreeLogger logger,
      ResourceOracle resourceOracle, String resourceName) {
    URL url = ResourceLocatorImpl.tryFindResourceUrl(logger, resourceOracle, resourceName);
    if (url == null) {
      return null;
    }
    try {
      return url.openStream();
    } catch (IOException e) {
      return null;
    }
  }

  /**
   * Returns a URL for the given resource as found in the ResourceOracle.
   * <p>
   * For backwards compatibility it will fallback on finding via ClassLoader and warn the user about
   * the problems this can cause in per-file compiles.
   * <p>
   * Should only be used by internal Generators and only till ClassLoader fallback is deprecated.
   */
  // TODO(stalcup): migrate internal Generators away from needing ClassLoader fallback.
  public static URL tryFindResourceUrl(TreeLogger logger, ResourceOracle resourceOracle,
      String resourceName) {
    // Also associates the requested resource with the currently being rebound type (via
    // RecordingResourceOracle).
    Resource resource = resourceOracle.getResource(resourceName);
    if (resource != null) {
      return resource.getURL();
    }

    // Fall back on loading resources via ClassLoader. This is needed for backwards compatibility
    // but should be avoided in favor of ResourceOracle loads since ResourceOracle loads so that
    // Resource modifications can be noticed and reacted to in per-file compilation recompiles.
    URL resourceUrl = Thread.currentThread().getContextClassLoader().getResource(resourceName);
    if (resourceUrl != null) {
      if (classLoaderLoadCount++ < CLASS_LOADER_LOAD_REPORT_LIMIT) {
        logger.log(TreeLogger.WARN, "Resource '" + resourceName
            + "' was located via ClassLoader. As a result changes in that resource will not be "
            + "reflected in per-file recompiles. It should be registered via  <source /> or "
            + "<resource /> entry in your .gwt.xml. In a future version of GWT, we will remove "
            + "this fallback and your application will stop compiling");
        if (classLoaderLoadCount == CLASS_LOADER_LOAD_REPORT_LIMIT) {
          logger.log(TreeLogger.WARN, "Suppressing further ClassLoader resource load warnings.");
        }
      }
      return resourceUrl;
    }

    return null;
  }
}
