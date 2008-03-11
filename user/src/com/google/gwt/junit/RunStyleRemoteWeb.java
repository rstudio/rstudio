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
package com.google.gwt.junit;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.junit.client.TimeoutException;
import com.google.gwt.junit.remote.BrowserManager;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.server.RMISocketFactory;

/**
 * Runs remotely in web mode. This feature is experimental and is not officially
 * supported.
 */
class RunStyleRemoteWeb extends RunStyle {

  static class RMISocketFactoryWithTimeouts extends RMISocketFactory {
    private static boolean initialized;

    public static void init() throws IOException {
      if (!initialized) {
        RMISocketFactory.setSocketFactory(new RMISocketFactoryWithTimeouts());
        initialized = true;
      }
    }

    @Override
    public ServerSocket createServerSocket(int port) throws IOException {
      return RMISocketFactory.getDefaultSocketFactory().createServerSocket(port);
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
      Socket socket = new Socket();
      socket.connect(new InetSocketAddress(host, port), CONNECT_MS);
      socket.setSoTimeout(RESPONSE_TIMEOUT_MS);
      return socket;
    }
  }

  private static final int CONNECT_MS = 10000;
  private static final int INITIAL_KEEPALIVE_MS = 5000;
  private static final int PING_KEEPALIVE_MS = 2000;
  private static final int RESPONSE_TIMEOUT_MS = 10000;

  // Larger values when debugging the unit test framework, so you
  // don't get spurious timeouts.
  // private static final int CONNECT_MS = 1000000;
  // private static final int INITIAL_KEEPALIVE_MS = 500000;
  // private static final int PING_KEEPALIVE_MS = 200000;
  // private static final int RESPONSE_TIMEOUT_MS = 1000000;

  public static RunStyle create(JUnitShell shell, String[] urls) {
    try {
      RMISocketFactoryWithTimeouts.init();
    } catch (IOException e) {
      System.err.println("Error initializing RMISocketFactory");
      e.printStackTrace();
      System.exit(1);
    }
    int numClients = urls.length;
    BrowserManager[] browserManagers = new BrowserManager[numClients];
    for (int i = 0; i < numClients; ++i) {
      long callStart = System.currentTimeMillis();
      try {
        browserManagers[i] = (BrowserManager) Naming.lookup(urls[i]);
      } catch (Exception e) {
        if (e.getCause() instanceof SocketTimeoutException) {
          long elapsed = System.currentTimeMillis() - callStart;
          System.err.println("Timeout " + elapsed
              + "ms waiting for browser manager server to connect at "
              + urls[i]);
          e.getCause().printStackTrace();
        } else {
          System.err.println("Error connecting to browser manager at "
              + urls[i]);
          e.printStackTrace();
        }
        System.exit(1);
      }
    }
    return new RunStyleRemoteWeb(shell, browserManagers, urls);
  }

  /**
   * Remote browser managers.
   */
  private final BrowserManager[] browserManagers;

  /**
   * RMI URLs for each remote browser server (using the same index as
   * browserManagers[] and remoteTokens[]).
   */
  private String remoteBmsUrls[];

  /**
   * References to the remote browser processes.
   */
  private int[] remoteTokens;

  private boolean running = false;

  /**
   * The containing shell.
   */
  private final JUnitShell shell;

  /**
   * @param shell the containing shell
   * @param browserManagers a populated array of RMI remote interfaces to each
   *          remote BrowserManagerServer
   * @param urls the URLs for each BrowserManager - used for error reporting
   *          only
   */
  private RunStyleRemoteWeb(JUnitShell shell, BrowserManager[] browserManagers,
      String[] urls) {
    this.shell = shell;
    this.browserManagers = browserManagers;
    this.remoteTokens = new int[browserManagers.length];
    this.remoteBmsUrls = urls;
  }

  @Override
  public void maybeLaunchModule(String moduleName, boolean forceLaunch)
      throws UnableToCompleteException {

    if (forceLaunch || !running) {
      shell.compileForWebMode(moduleName, null);
      String localhost;

      try {
        localhost = InetAddress.getLocalHost().getHostAddress();
      } catch (UnknownHostException e) {
        throw new RuntimeException("Unable to determine my ip address", e);
      }
      String url = "http://" + localhost + ":" + shell.getPort() + "/"
          + getUrlSuffix(moduleName);

      for (int i = 0; i < remoteTokens.length; ++i) {
        long callStart = System.currentTimeMillis();
        try {
          int remoteToken = remoteTokens[i];
          BrowserManager mgr = browserManagers[i];
          if (remoteToken != 0) {
            mgr.killBrowser(remoteToken);
          }
          remoteTokens[i] = mgr.launchNewBrowser(url, INITIAL_KEEPALIVE_MS);
        } catch (Exception e) {
          Throwable cause = e.getCause();
          if (cause instanceof SocketTimeoutException) {
            long elapsed = System.currentTimeMillis()
                - callStart;
            shell.getTopLogger().log(
                TreeLogger.ERROR,
                "Timeout: " + elapsed + "ms  launching remote browser at: "
                    + remoteBmsUrls[i], e.getCause());
            throw new UnableToCompleteException();
          }
          shell.getTopLogger().log(TreeLogger.ERROR,
              "Error launching remote browser at " + remoteBmsUrls[i], e);
          throw new UnableToCompleteException();
        }
      }
      running = true;
    }
  }

  @Override
  public boolean wasInterrupted() {
    for (int i = 0; i < remoteTokens.length; ++i) {
      int remoteToken = remoteTokens[i];
      BrowserManager mgr = browserManagers[i];
      if (remoteToken > 0) {

        long callStart = System.currentTimeMillis();
        try {
          mgr.keepAlive(remoteToken, PING_KEEPALIVE_MS);
        } catch (Exception e) {
          Throwable cause = e.getCause();
          if (cause instanceof SocketTimeoutException) {
            long elapsed = System.currentTimeMillis()
                - callStart;
            throw new TimeoutException("Timeout: " + elapsed
                + "ms  keeping alive remote browser at: " + remoteBmsUrls[i], 
                e.getCause());
          } else if (e instanceof IllegalStateException) {
            shell.getTopLogger().log(TreeLogger.INFO,
                "Browser at: " + remoteBmsUrls[i] + " already exited.", e);
          } else {
            shell.getTopLogger().log(TreeLogger.ERROR,
                "Error keeping alive remote browser at " + remoteBmsUrls[i], e);
          }
          return true;
        }
      }
    }
    return false;
  }
}
