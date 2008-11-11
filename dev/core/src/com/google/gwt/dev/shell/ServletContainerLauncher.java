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
package com.google.gwt.dev.shell;

import com.google.gwt.core.ext.TreeLogger;

import java.io.File;
import java.net.BindException;

import javax.servlet.Filter;

/**
 * Defines the service provider interface for launching servlet containers that
 * can be used by the shell.
 */
public interface ServletContainerLauncher {

  /**
   * Start an embedded HTTP server.
   * 
   * @param logger the server logger
   * @param port the TCP port to serve on
   * @param appRootDir the base WAR directory
   * @param filter a servlet filter that must be installed on the root path to
   *          serve generated files
   * @return the launch servlet contained
   * @throws BindException if the requested port is already in use
   * @throws Exception if the server fails to start for any other reason
   */
  ServletContainer start(TreeLogger logger, int port, File appRootDir,
      Filter filter) throws BindException, Exception;
}
