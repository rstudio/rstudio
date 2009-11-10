/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.dev.shell.remoteui;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.dev.shell.BrowserListener;
import com.google.gwt.dev.ui.DevModeUI;
import com.google.gwt.dev.ui.DoneCallback;
import com.google.gwt.dev.ui.DoneEvent;
import com.google.gwt.dev.ui.RestartServerCallback;
import com.google.gwt.dev.ui.RestartServerEvent;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * An implementation of a UI for the development mode server that sends UI
 * events over the network to a remote viewer. Also receives commands from the
 * remote viewer (such as a web server restart) and forwards the requests to the
 * development mode server.
 */
public class RemoteUI extends DevModeUI implements
    MessageTransport.TerminationCallback {

  private final List<ModuleHandle> modules = new ArrayList<ModuleHandle>();
  private final Object modulesLock = new Object();

  private final String clientId;
  private final DevModeServiceRequestProcessor devModeRequestProcessor;
  private final MessageTransport transport;
  private ViewerServiceClient viewerServiceClient = null;
  private final int webServerPort;
  private final int browserChannelPort;

  public RemoteUI(String host, int port, String clientId, int webServerPort,
      int browserChannelPort) {
    try {
      this.clientId = clientId;
      this.browserChannelPort = browserChannelPort;
      this.webServerPort = webServerPort;

      Socket socket = new Socket(host, port);
      socket.setKeepAlive(true);
      socket.setTcpNoDelay(true);
      devModeRequestProcessor = new DevModeServiceRequestProcessor(this);
      transport = new MessageTransport(socket.getInputStream(),
          socket.getOutputStream(), devModeRequestProcessor, this);
    } catch (UnknownHostException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public TreeLogger getWebServerLogger(String serverName, byte[] serverIcon) {
    return getConsoleLogger();
  }

  @Override
  public void initialize(Type logLevel) {
    super.initialize(logLevel);
    viewerServiceClient = new ViewerServiceClient(transport);
    String devModeQueryParam = BrowserListener.getDevModeURLParams(BrowserListener.computeEndpointIdentifier(browserChannelPort));
    viewerServiceClient.initialize(clientId, devModeQueryParam, webServerPort);
    viewerServiceClient.checkCapabilities();
  }

  @Override
  public ModuleHandle loadModule(String userAgent, String remoteSocket,
      String url, String tabKey, String moduleName, String sessionKey,
      String agentTag, byte[] agentIcon, Type logLevel) {

    int logHandle;
    logHandle = viewerServiceClient.addModuleLog(remoteSocket, url, tabKey,
        moduleName, sessionKey, agentTag, agentIcon);
    final ViewerServiceTreeLogger moduleLogger = new ViewerServiceTreeLogger(
        viewerServiceClient);
    moduleLogger.setLogHandle(logHandle);
    moduleLogger.setMaxDetail(getLogLevel());
    ModuleHandle handle = new ModuleHandle() {
      public TreeLogger getLogger() {
        return moduleLogger;
      }
    };
    synchronized (modulesLock) {
      modules.add(handle);
    }
    // Copied from SwingUI.loadModule
    TreeLogger branch = moduleLogger.branch(TreeLogger.INFO, "Loading module "
        + moduleName);
    if (url != null) {
      branch.log(TreeLogger.INFO, "Top URL: " + url);
    }

    branch.log(TreeLogger.INFO, "User agent: " + userAgent);
    branch.log(TreeLogger.TRACE, "Remote socket: " + remoteSocket);
    if (tabKey != null) {
      branch.log(TreeLogger.DEBUG, "Tab key: " + tabKey);
    }
    if (sessionKey != null) {
      branch.log(TreeLogger.DEBUG, "Session key: " + sessionKey);
    }

    return handle;
  }

  public void onTermination(Exception e) {
    getTopLogger().log(
        TreeLogger.INFO,
        "Remote UI connection terminated due to exception: "
            + e.getLocalizedMessage());
    getTopLogger().log(TreeLogger.INFO,
        "Shutting down development mode server.");
    ((DoneCallback) getCallback(DoneEvent.getType())).onDone();
  }

  public boolean restartWebServer() {
    if (!supportsRestartWebServer()) {
      return false;
    }

    TreeLogger webServerLogger = getConsoleLogger();
    if (webServerLogger == null) {
      return false;
    }

    ((RestartServerCallback) getCallback(RestartServerEvent.getType())).onRestartServer(webServerLogger);
    return true;
  }

  public boolean supportsRestartWebServer() {
    return hasCallback(RestartServerEvent.getType());
  }

  @Override
  public void unloadModule(ModuleHandle module) {
    synchronized (modulesLock) {
      if (!modules.contains(module)) {
        return;
      }
    }

    ViewerServiceTreeLogger moduleLogger = (ViewerServiceTreeLogger) (module.getLogger());

    try {
      viewerServiceClient.disconnectLog(moduleLogger.getLogHandle());
    } finally {
      synchronized (modulesLock) {
        modules.remove(module);
      }
    }
  }
}
