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

/**
 * An instance of a servlet container that can be used by the shell. It is
 * assumed that this servlet container serves a web app from the directory
 * specified when this servlet container was created.
 */
public abstract class ServletContainer {

  /**
   * Returns the host on which the servlet container is running. Defaults to
   * "localhost". Used to construct a URL to reach the servlet container.
   */
  public String getHost() {
    return "localhost";
  }

  /**
   * Returns the port on which the server is running.Used to construct a URL to
   * reach the servlet container.
   */
  public abstract int getPort();

  /**
   * Causes the web app to pick up changes made within the app root dir while
   * running. This method cannot be called after {@link #stop()} has been
   * called.
   * 
   * TODO(bruce): need to determine whether all the important servlet containers
   * will let us do this (e.g. ensure they don't lock files we would need to
   * update)
   * 
   * @throws UnableToCompleteException
   */
  public abstract void refresh() throws UnableToCompleteException;

  /**
   * Stops the running servlet container. It cannot be restarted after this.
   * 
   * @throws UnableToCompleteException
   */
  public abstract void stop() throws UnableToCompleteException;
}
