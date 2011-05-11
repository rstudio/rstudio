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

package com.google.gwt.dev.shell;

/**
 * Represents a session between devmode and a browser plugin. A session is
 * essentially a socket connection, established by the plugin when a GWT module
 * is started in devmode and used to communicate invocations of Java from
 * JavaScript and vice-versa.
 */
public class DevModeSession {

  private static final ThreadLocal<DevModeSession> sessionForCurrentThread =
      new ThreadLocal<DevModeSession>();

  /**
   * Gets the devmode session for the current thread. If a thread is not
   * associated with a devmode session, via
   * <code>setSessionForCurrentThread()</code>, then this will return null.
   */
  public static DevModeSession getSessionForCurrentThread() {
    return sessionForCurrentThread.get();
  }

  /**
   * Sets the devmode session for the current thread.
   */
  static void setSessionForCurrentThread(DevModeSession session) {
    sessionForCurrentThread.set(session);
  }

  private String moduleName;
  private String userAgent;

  /**
   * Creates a new instance. It is public only for unit test purposes and is
   * not meant to be used outside of this package.
   * 
   * @param moduleName the name of the GWT module for this session
   * @param userAgent the User agent field provided by the browser for this
   *          session
   */
  public DevModeSession(String moduleName, String userAgent) {
    this.moduleName = moduleName;
    this.userAgent = userAgent;
  }

  public String getModuleName() {
    return moduleName;
  }

  public String getUserAgent() {
    return userAgent;
  }
}
