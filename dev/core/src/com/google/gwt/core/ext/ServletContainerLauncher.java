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
package com.google.gwt.core.ext;

import java.io.File;
import java.net.BindException;

/**
 * Defines the service provider interface for launching servlet containers that
 * can be used by the GWT development mode.
 */
public abstract class ServletContainerLauncher {

  /**
   * @return a path to a 24-pixel high image file (relative to the classpath) to
   *     be used for this servlet container, or null if none.
   */
  public String getIconPath() {
    return null;
  }

  /**
   * @return a short human-readable name of this servlet container, or null
   *     if no name should be displayed.
   */
  public String getName() {
    return "Web Server";
  }

  /**
   * Start an embedded HTTP servlet container.
   * 
   * @param logger the server logger
   * @param port the TCP port to serve on; if 0 is requested, a port should be
   *          automatically selected
   * @param appRootDir the base WAR directory
   * @return the launched servlet container
   * @throws BindException if the requested port is already in use
   * @throws Exception if the server fails to start for any other reason
   */
  public abstract ServletContainer start(TreeLogger logger, int port,
      File appRootDir) throws BindException, Exception;
}
