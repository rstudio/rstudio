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
package com.google.gwt.junit.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * An interface to manage (possibly remote) browser windows. This class is
 * experimental and unsupported.
 */
public interface BrowserManager extends Remote {

  /**
   * Keeps the browser process represented by token alive for keepAliveMs,
   * starting now.
   * 
   * @param token an opaque token representing the browser window
   * @param keepAliveMs the number of milliseconds to let the browser process
   *          live; if roughly <code>keepAliveMs</code> milliseconds elapse
   *          without a subsequent call to this method, the browser process
   *          associated with <code>token</code> will be forceably terminated
   * @throws IllegalStateException if the process represented by token has
   *           already terminated.
   * @throws IllegalArgumentException if token does not represent a process that
   *           was returned from {@link #launchNewBrowser(String, long)}.
   * @throws IllegalArgumentException if keepAliveMs <= 0.
   * @throws RemoteException if an error occurs calling a remote implementation.
   */
  void keepAlive(int token, long keepAliveMs) throws RemoteException;

  /**
   * Forceably kills the browser process represented by <code>token</code>,
   * disregarding any previous calls to {@link #keepAlive(int, long)}. If the
   * process has already terminated, this method completes normally.
   * 
   * @param token an opaque token representing the browser window process
   * @throws IllegalArgumentException if token does not represent a process that
   *           was returned from {@link #launchNewBrowser(String, long)}.
   * @throws RemoteException if an error occurs calling a remote implementation.
   */
  void killBrowser(int token) throws RemoteException;

  /**
   * Launches a new browser window for the specified URL.
   * 
   * @param url the URL to browse to
   * @param keepAliveMs the initial number of milliseconds to let the browser
   *          process live; if roughly <code>keepAliveMs</code> milliseconds
   *          expire without a subsequent call to {@link #keepAlive(int, long)},
   *          the browser process will be forceably terminated
   * @return a positive integer that serves an an opaque token representing the
   *         new browser window.
   * @throws IllegalArgumentException if <code>url</code> is <code>null</code>.
   * @throws IllegalArgumentException if <code>keepAliveMs</code> <= 0.
   * @throws RuntimeException if an error occurs launching the browser process.
   * @throws RemoteException if an error occurs calling a remote implementation.
   */
  int launchNewBrowser(String url, long keepAliveMs) throws RemoteException;
}
