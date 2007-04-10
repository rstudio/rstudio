/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.junit;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.junit.remote.BrowserManager;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Runs remotely in web mode. This feature is experimental and is not officially
 * supported.
 */
class RunStyleRemoteWeb extends RunStyle {

  private static final int INITIAL_KEEPALIVE_MS = 5000;
  private static final int PING_KEEPALIVE_MS = 2000;

  // Larger values when debugging the unit test framework, so you
  // don't get spurious timeouts.
  // private static final int INITIAL_KEEPALIVE_MS = 500000;
  // private static final int PING_KEEPALIVE_MS = 200000;

  /**
   * Remote browser managers.
   */
  private final BrowserManager[] browserManagers;

  /**
   * References to the remote browser processes.
   */
  private int[] remoteTokens;

  /**
   * The containing shell.
   */
  private final JUnitShell shell;

  private boolean running = false;

  /**
   * @param shell the containing shell
   */
  public RunStyleRemoteWeb(JUnitShell shell, BrowserManager[] browserManagers) {
    this.shell = shell;
    this.browserManagers = browserManagers;
    this.remoteTokens = new int[ browserManagers.length ];
  }

  public void maybeLaunchModule(String moduleName, boolean forceLaunch)
      throws UnableToCompleteException {

    if (forceLaunch || !running) {
      shell.compileForWebMode(moduleName);
      String localhost;

      try {
        localhost = InetAddress.getLocalHost().getHostAddress();
      } catch (UnknownHostException e) {
        throw new RuntimeException("Unable to determine my ip address", e);
      }
      String url = "http://" + localhost + ":" + shell.getPort() + "/"
          + moduleName;

      try {
        for ( int i = 0; i < remoteTokens.length; ++i ) {
          int remoteToken = remoteTokens[ i ];
          BrowserManager mgr = browserManagers[ i ];
          if ( remoteToken != 0 ) {
            mgr.killBrowser(remoteToken);
          }
          remoteTokens[ i ] = mgr.launchNewBrowser(url, INITIAL_KEEPALIVE_MS);
        }
      } catch (Exception e) {
        shell.getTopLogger().log(TreeLogger.ERROR,
            "Error launching remote browser", e);
        throw new UnableToCompleteException();
      }

      running = true;
    }
  }

  public boolean wasInterrupted() {
    for ( int i = 0; i < remoteTokens.length; ++i ) {
      int remoteToken = remoteTokens[ i ];
      BrowserManager mgr = browserManagers[ i ];
      if (remoteToken > 0) {
        try {
          mgr.keepAlive(remoteToken, PING_KEEPALIVE_MS);
        } catch (Exception e) {
          // TODO(tobyr): We're failing on the first exception, rather than
          //  collecting them, but that's probably OK for now.
          shell.getTopLogger().log(TreeLogger.WARN,
              "Unexpected exception keeping remote browser alive", e);
          return true;
        }
      }
    }
    return false;
  }

}