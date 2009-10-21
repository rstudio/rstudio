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
import com.google.gwt.dev.ui.DevModeUI;
import com.google.gwt.dev.ui.RestartServerCallback;
import com.google.gwt.dev.ui.RestartServerEvent;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * TODO: Implement me.
 */
public class RemoteUI extends DevModeUI {

  private final List<ModuleHandle> modules = new ArrayList<ModuleHandle>();
  private final Object modulesLock = new Object();

  private final DevModeServiceRequestProcessor devModeRequestProcessor;
  private final MessageTransport transport;
  private ViewerServiceTreeLogger webServerLogger = null;
  private ViewerServiceTreeLogger mainLogger = null;
  private ViewerServiceClient viewerServiceClient = null;

  public RemoteUI(String host, int port) {
    try {
      Socket socket = new Socket(host, port);
      devModeRequestProcessor = new DevModeServiceRequestProcessor(this);
      transport = new MessageTransport(socket.getInputStream(),
          socket.getOutputStream(), devModeRequestProcessor);
    } catch (UnknownHostException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public TreeLogger getTopLogger() {
    if (mainLogger != null) {
      return mainLogger;
    }

    mainLogger = new ViewerServiceTreeLogger(viewerServiceClient);
    int topLoggerHandle = viewerServiceClient.addMainLog();
    mainLogger.setLogHandle(topLoggerHandle);
    mainLogger.setMaxDetail(getLogLevel());
    return mainLogger;
  }

  @Override
  public TreeLogger getWebServerLogger(String serverName, byte[] serverIcon) {
    if (webServerLogger != null) {
      return webServerLogger;
    }

    webServerLogger = new ViewerServiceTreeLogger(viewerServiceClient);
    int webServerLoggerHandle = viewerServiceClient.addServerLog(serverName,
        serverIcon);
    webServerLogger.setLogHandle(webServerLoggerHandle);
    webServerLogger.setMaxDetail(getLogLevel());
    return webServerLogger;
  }

  @Override
  public void initialize(Type logLevel) {
    super.initialize(logLevel);
    viewerServiceClient = new ViewerServiceClient(transport);
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

  public void restartWebServer() {
    if (supportsRestartWebServer() && webServerLogger != null) {
      ((RestartServerCallback) getCallback(RestartServerEvent.getType())).onRestartServer(webServerLogger);
    }
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
