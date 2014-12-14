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
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.EmittedArtifact.Visibility;
import com.google.gwt.core.ext.linker.impl.StandardLinkerContext;
import com.google.gwt.dev.DevMode.HostedModeOptions;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.shell.BrowserChannelServer.SessionHandlerServer;
import com.google.gwt.dev.util.NullOutputFileSet;
import com.google.gwt.dev.util.OutputFileSet;
import com.google.gwt.dev.util.OutputFileSetOnDirectory;

import java.io.File;
import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;

/**
 * Listens for connections from OOPHM clients.
 */
public class BrowserListener implements CodeServerListener {

  /**
   * Get a query parameter to be added to the URL that specifies the address of this listener.
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

  private HostedModeOptions options;

  private TreeLogger logger;

  /**
   * Listens for new connections from browsers.
   */
  public BrowserListener(TreeLogger treeLogger, HostedModeOptions options,
      final SessionHandlerServer handler) {
    try {
      this.options = options;
      this.logger = treeLogger;
      listenSocket = new ServerSocket();
      listenSocket.setReuseAddress(true);
      InetAddress address = InetAddress.getByName(options.getBindAddress());
      listenSocket.bind(new InetSocketAddress(address, options.getCodeServerPort()));

      if (logger.isLoggable(TreeLogger.TRACE)) {
        logger.log(TreeLogger.TRACE, "Started code server on port " + listenSocket.getLocalPort(),
            null);
      }
      listenThread = new Thread() {
        @Override
        public void run() {
          while (true) {
            try {
              Socket sock = listenSocket.accept();
              TreeLogger branch =
                  logger.branch(TreeLogger.TRACE, "Connection received from "
                      + sock.getInetAddress().getCanonicalHostName() + ":" + sock.getPort());
              try {
                sock.setTcpNoDelay(true);
                sock.setKeepAlive(true);
              } catch (SocketException e) {
                // Ignore non-critical errors.
              }

              BrowserChannelServer server =
                  new BrowserChannelServer(branch, sock, handler, ignoreRemoteDeath);
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
      logger.log(TreeLogger.ERROR, "Unable to bind socket on port " + options.getPort()
          + " -- is another session active?", e);
    } catch (IOException e) {
      logger.log(TreeLogger.ERROR, "Communications error", e);
    }
  }

  @Override
  public int getSocketPort() {
    return listenSocket.getLocalPort();
  }

  @Override
  public URL makeStartupUrl(String url) throws UnableToCompleteException {
    URL parsedUrl = null;
    try {
      parsedUrl = new URL(url);
      String path = parsedUrl.getPath();
      String query = parsedUrl.getQuery();
      String hash = parsedUrl.getRef();
      String hostedParam =
          BrowserListener.getDevModeURLParams(options.getConnectAddress(), getSocketPort());
      if (query == null) {
        query = hostedParam;
      } else {
        query += '&' + hostedParam;
      }
      path += '?' + query;
      if (hash != null) {
        path += '#' + hash;
      }
      parsedUrl = new URL(parsedUrl.getProtocol(), parsedUrl.getHost(), parsedUrl.getPort(), path);
      url = parsedUrl.toExternalForm();
    } catch (MalformedURLException e) {
      logger.log(TreeLogger.ERROR, "Invalid URL " + url, e);
      throw new UnableToCompleteException();
    }
    return parsedUrl;
  }

  @Override
  public synchronized void writeCompilerOutput(StandardLinkerContext linkerStack,
      ArtifactSet artifacts, ModuleDef module, boolean isRelink) throws UnableToCompleteException {
    TreeLogger linkLogger =
        logger.branch(TreeLogger.DEBUG, "Linking module '" + module.getName() + "'");

    OutputFileSetOnDirectory outFileSet =
        new OutputFileSetOnDirectory(options.getModuleBaseDir(), module.getName() + "/");
    OutputFileSetOnDirectory deployFileSet =
        new OutputFileSetOnDirectory(options.getDeployDir(), module.getName() + "/");
    OutputFileSet extraFileSet = new NullOutputFileSet();
    if (options.getExtraDir() != null) {
      extraFileSet = new OutputFileSetOnDirectory(options.getExtraDir(), module.getName() + "/");
    }

    linkerStack.produceOutput(linkLogger, artifacts, Visibility.Public, outFileSet);
    linkerStack.produceOutput(linkLogger, artifacts, Visibility.Deploy, deployFileSet);
    linkerStack.produceOutput(linkLogger, artifacts, Visibility.Private, extraFileSet);

    outFileSet.close();
    deployFileSet.close();
    try {
      extraFileSet.close();
    } catch (IOException e) {
      linkLogger.log(TreeLogger.ERROR, "Error emiting extra files", e);
      throw new UnableToCompleteException();
    }

    // Update the timestamp for files that Super Dev Mode might previously have touched.
    // The .nocache.js file produced by devmode has identical timestamp than the bootstrap
    // html page, hence the browser uses superdevmode cached file instead of refreshing it
    // with the new devmode version, setting ts to current time fixes the issue.
    new File(options.getModuleBaseDir() + "/" + module.getName() + "/" + module.getName() + ".nocache.js")
        .setLastModified(System.currentTimeMillis());
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

  @Override
  public void start() {
    if (listenThread != null) {
      listenThread.start();
    }
  }
}
