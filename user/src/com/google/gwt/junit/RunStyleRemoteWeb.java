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
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.rmi.Naming;
import java.rmi.server.RMISocketFactory;
import java.util.HashSet;
import java.util.Set;

/**
 * Runs in Production Mode via browsers managed over RMI. This feature is
 * experimental and is not officially supported.
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

  private static class RemoteBrowser {
    /**
     * Remote browser manager.
     */
    private final BrowserManager manager;

    /**
     * RMI URLs for the remote browser server.
     */
    private final String rmiUrl;
    /**
     * Reference to the remote browser processes.
     */
    private int token;

    public RemoteBrowser(BrowserManager manager, String rmiUrl) {
      this.manager = manager;
      this.rmiUrl = rmiUrl;
    }

    public BrowserManager getManager() {
      return manager;
    }

    public String getRmiUrl() {
      return rmiUrl;
    }

    public int getToken() {
      return token;
    }

    public void setToken(int token) {
      this.token = token;
    }
  }

  /**
   * Registered as a shutdown hook to make sure that any browsers that were not
   * finished are killed.
   */
  private class ShutdownCb extends Thread {

    @Override
    public synchronized void run() {
      for (RemoteBrowser remoteBrowser : remoteBrowsers) {
        int remoteToken = remoteBrowser.getToken();
        if (remoteToken > 0) {
          try {
            remoteBrowser.getManager().killBrowser(remoteToken);
          } catch (Exception e) {
            System.err.println("Error killing remote browser during shutdown: "
                + remoteBrowser.getRmiUrl());
            e.printStackTrace();
          }
          // We've done our best to kill it. Don't try anymore.
          remoteBrowser.setToken(0);
        }
      }
    }
  }

  private static final int CONNECT_MS = 10000;
  private static final int PING_KEEPALIVE_MS = 5000;

  // Larger values when debugging the unit test framework, so you
  // don't get spurious timeouts.
  // private static final int CONNECT_MS = 1000000;
  // private static final int PING_KEEPALIVE_MS = 500000;
  // private static final int RESPONSE_TIMEOUT_MS = 1000000;

  private static final int RESPONSE_TIMEOUT_MS = 10000;

  /**
   * The list of hosts that were interrupted.
   */
  private Set<String> interruptedHosts;

  private RemoteBrowser[] remoteBrowsers;

  /**
   * A separate lock to control access to {@link #interruptedHosts}. This keeps
   * the main thread calls into {@link #getInterruptedHosts()} from having to
   * synchronized on the containing instance and potentially block on RPC calls.
   * It is okay to take the {@link #wasInterruptedLock} while locking the
   * containing instance; it is NOT okay to do the opposite or deadlock could
   * occur.
   */
  private final Object wasInterruptedLock = new Object();

  /**
   * @param shell the containing shell
   */
  public RunStyleRemoteWeb(JUnitShell shell) {
    super(shell);
  }

  @Override
  public String[] getInterruptedHosts() {
    synchronized (wasInterruptedLock) {
      if (interruptedHosts == null) {
        return null;
      }
      return interruptedHosts.toArray(new String[interruptedHosts.size()]);
    }
  }

  @Override
  public int initialize(String args) {
    if (args == null || args.length() == 0) {
      getLogger().log(TreeLogger.ERROR,
          "RemoteWeb runstyle requires comma-separated RMI URLs");
      return -1;
    }
    String[] urls = args.split(",");
    try {
      RMISocketFactoryWithTimeouts.init();
    } catch (IOException e) {
      getLogger().log(TreeLogger.ERROR,
          "RemoteWeb: Error initializing RMISocketFactory", e);
      return -1;
    }
    int numClients = urls.length;
    BrowserManager[] browserManagers = new BrowserManager[numClients];
    for (int i = 0; i < numClients; ++i) {
      long callStart = System.currentTimeMillis();
      try {
        browserManagers[i] = (BrowserManager) Naming.lookup(urls[i]);
      } catch (Exception e) {
        String message = "RemoteWeb: Error connecting to browser manager at "
            + urls[i];
        Throwable cause = e;
        if (e.getCause() instanceof SocketTimeoutException) {
          long elapsed = System.currentTimeMillis() - callStart;
          message += " - Timeout " + elapsed
              + "ms waiting to connect to browser manager.";
          cause = e.getCause();
        }
        getLogger().log(TreeLogger.ERROR, message, cause);
        return -1;
      }
    }
    synchronized (this) {
      this.remoteBrowsers = new RemoteBrowser[browserManagers.length];
      for (int i = 0; i < browserManagers.length; ++i) {
        remoteBrowsers[i] = new RemoteBrowser(browserManagers[i], urls[i]);
      }
    }
    Runtime.getRuntime().addShutdownHook(new ShutdownCb());
    return numClients;
  }

  @Override
  public synchronized void launchModule(String moduleName)
      throws UnableToCompleteException {
    String url = shell.getModuleUrl(moduleName);

    for (RemoteBrowser remoteBrowser : remoteBrowsers) {
      long callStart = System.currentTimeMillis();
      try {
        int remoteToken = remoteBrowser.getToken();
        BrowserManager mgr = remoteBrowser.getManager();
        if (remoteToken != 0) {
          mgr.killBrowser(remoteToken);
        }
        remoteToken = mgr.launchNewBrowser(url, PING_KEEPALIVE_MS);
        remoteBrowser.setToken(remoteToken);
      } catch (Exception e) {
        Throwable cause = e.getCause();
        if (cause instanceof SocketTimeoutException) {
          long elapsed = System.currentTimeMillis() - callStart;
          getLogger().log(
              TreeLogger.ERROR,
              "Timeout: " + elapsed + "ms  launching remote browser at: "
                  + remoteBrowser.getRmiUrl(), e.getCause());
          throw new UnableToCompleteException();
        }
        getLogger().log(TreeLogger.ERROR,
            "Error launching remote browser at " + remoteBrowser.getRmiUrl(), e);
        throw new UnableToCompleteException();
      }
    }

    Thread keepAliveThread = new Thread() {
      @Override
      public void run() {
        do {
          try {
            Thread.sleep(1000);
          } catch (InterruptedException ignored) {
          }
        } while (doKeepAlives());
      }
    };
    keepAliveThread.setDaemon(true);
    keepAliveThread.start();
  }

  private synchronized boolean doKeepAlives() {
    for (RemoteBrowser remoteBrowser : remoteBrowsers) {
      if (remoteBrowser.getToken() > 0) {
        long callStart = System.currentTimeMillis();
        try {
          remoteBrowser.getManager().keepAlive(remoteBrowser.getToken(),
              PING_KEEPALIVE_MS);
        } catch (Exception e) {
          Throwable cause = e.getCause();
          String rmiUrl = remoteBrowser.getRmiUrl();
          if (cause instanceof SocketTimeoutException) {
            long elapsed = System.currentTimeMillis() - callStart;
            throw new TimeoutException("Timeout: " + elapsed
                + "ms  keeping alive remote browser at: " + rmiUrl,
                e.getCause());
          } else if (e instanceof IllegalStateException) {
            if (getLogger().isLoggable(TreeLogger.INFO)) {
              getLogger().log(TreeLogger.INFO,
                  "Browser at: " + rmiUrl + " already exited.", e);
            }
          } else {
            getLogger().log(TreeLogger.ERROR,
                            "Error keeping alive remote browser at " + rmiUrl, e);
          }
          remoteBrowser.setToken(0);
          synchronized (wasInterruptedLock) {
            if (interruptedHosts == null) {
              interruptedHosts = new HashSet<String>();
            }
            interruptedHosts.add(remoteBrowser.getRmiUrl());
          }
          break;
        }
      }
    }

    synchronized (wasInterruptedLock) {
      return interruptedHosts == null;
    }
  }
}
