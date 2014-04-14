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
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.shell.BrowserChannelServer.SessionHandlerServer;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * Listens for connections from OOPHM clients.
 */
public class BrowserListener {

  /**
   * Get a query parameter to be added to the URL that specifies the address
   * of this listener.
   *
   * @param address address of host to use for connections
   * @param port TCP port number to use for connection
   * @return a query parameter
   */
  public static String getDevModeURLParams(String address, int port) {
    return "gwt.codesvr=" + address + ":" + port;
  }

  private ServerSocket listenSocket;

  private Thread listenThread;

  private boolean ignoreRemoteDeath = false;

  /**
   * Listens for new connections from browsers.
   *
   * @param logger
   * @param port
   * @param handler
   */
  public BrowserListener(final TreeLogger logger, String bindAddress,
      int port, final SessionHandlerServer handler) {
    try {
      listenSocket = new ServerSocket();
      listenSocket.setReuseAddress(true);
      InetAddress address = InetAddress.getByName(bindAddress);
      listenSocket.bind(new InetSocketAddress(address, port));

      if (logger.isLoggable(TreeLogger.TRACE)) {
        logger.log(TreeLogger.TRACE, "Started code server on port "
            + listenSocket.getLocalPort(), null);
      }
      listenThread = new Thread() {
        @Override
        public void run() {
          while (true) {
            try {
              Socket sock = listenSocket.accept();
              TreeLogger branch = logger.branch(TreeLogger.TRACE,
                  "Connection received from "
                      + sock.getInetAddress().getCanonicalHostName() + ":"
                      + sock.getPort());
              try {
                sock.setTcpNoDelay(true);
                sock.setKeepAlive(true);
              } catch (SocketException e) {
                // Ignore non-critical errors.
              }

              BrowserChannelServer server = new BrowserChannelServer(branch,
                  sock, handler, ignoreRemoteDeath);
              /*
               * This object is special-cased by the SessionHandler, used for
               * methods needed by the client like hasMethod/hasProperty/etc.
               * handler is used for this object just to make sure it doesn't
               * conflict with some real object exposed to the client.
               */
              int id = server.getJavaObjectsExposedInBrowser().add(server);
              assert id == BrowserChannel.SPECIAL_SERVERMETHODS_OBJECT;
            } catch (IOException e) {
              logger.log(TreeLogger.ERROR, "Communications error", e);
            }
          }
        }
      };
      listenThread.setName("Code server listener");
      listenThread.setDaemon(true);
    } catch (BindException e) {
      logger.log(TreeLogger.ERROR, "Unable to bind socket on port " + port
          + " -- is another session active?", e);
    } catch (IOException e) {
      logger.log(TreeLogger.ERROR, "Communications error", e);
    }
  }

  /**
   * @return the port number of the listening socket.
   *
   * @throws UnableToCompleteException if the listener is not running
   */
  public int getSocketPort() throws UnableToCompleteException {
    if (listenSocket == null) {
      // If we failed to initialize our socket, just bail here.
      throw new UnableToCompleteException();
    }
    return listenSocket.getLocalPort();
  }

  /**
   * Set any created BrowserChannelServers to ignore remote deaths.
   *
   * <p>
   * This is most commonly wanted by JUnitShell.
   *
   * @param ignoreRemoteDeath
   */
  public void setIgnoreRemoteDeath(boolean ignoreRemoteDeath) {
    this.ignoreRemoteDeath = ignoreRemoteDeath;
  }

  /**
   * Start the listener thread.
   */
  public void start() {
    if (listenThread != null) {
      listenThread.start();
    }
  }
}
