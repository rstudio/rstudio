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

import com.google.gwt.core.ext.UnableToCompleteException;

/**
 * An instance of a servlet container that can be used by the shell. It is
 * assumed that this servlet container serves a web app from the root directory
 * specified by a call to
 * {@link ServletContainerLauncher#setAppRootDir(java.io.File)}.
 */
public interface ServletContainer {

  /**
   * Provides the port on which the server is actually running, which can be
   * useful when automatic port selection was requested.
   */
  int getPort();

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
  void refresh() throws UnableToCompleteException;

  /**
   * Stops the running servlet container. It cannot be restarted after this.
   * 
   * @throws UnableToCompleteException
   */
  void stop() throws UnableToCompleteException;
}
