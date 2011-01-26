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
 * <p>
 * Subclasses should be careful about calling any methods defined on this class
 * or else they risk failing when used with a version of GWT that did not have
 * those methods.
 */
public abstract class ServletContainerLauncher {
  /*
   * NOTE: Any new methods must have default implementations, and any users of
   * this class must be prepared to handle LinkageErrors when calling new
   * methods.
   */

  /**
   * @return byte array containing an icon (fitting into 24x24) to
   *     use for the server, or null if only the name should be used
   */
  public byte[] getIconBytes() {
    return null;
  }

  /**
   * @return a path to a 24-pixel high image file (relative to the classpath) to
   *     be used for this servlet container, or null if none.
   * @deprecated see {@link #getIconBytes} instead.
   */
  @Deprecated
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
   * Return true if this servlet container launcher is configured for secure
   * operation (ie, HTTPS).  This value is only queried after arguments, if any,
   * have been processed.
   * 
   * The default implementation just returns false.
   * 
   * @return true if HTTPS is in use
   */
  public boolean isSecure() {
    return false;
  }

  /**
   * Process any supplied arguments.
   * <p>
   * Will be called before {@link #start(TreeLogger, int, File)}, if at all.
   * 
   * @param logger logger to use for warnings/errors
   * @param arguments single string containing the arguments for this SCL, the
   *     format to be defined by the SCL
   * @return true if the arguments were processed successfully
   */
  public boolean processArguments(TreeLogger logger, String arguments) {
    logger.log(TreeLogger.ERROR, getName() + " does not accept any arguments");
    return false;
  }

  /**
   * Set the bind address for the web server socket.
   * <p>
   * Will be called before {@link #start(TreeLogger, int, File)}, if at all.
   * If not called, the SCL should listen on all addresses.
   * 
   * @param bindAddress host name or IP address, suitable for use with
   *     {@link java.net.InetAddress#getByName(String)}
   */
  public void setBindAddress(String bindAddress) {
    /*
     * By default, we do nothing, which means that old SCL implementations
     * will bind to all addresses.
     */
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
