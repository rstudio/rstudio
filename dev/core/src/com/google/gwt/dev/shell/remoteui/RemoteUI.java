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
import com.google.gwt.dev.ModuleHandle;
import com.google.gwt.dev.ui.DevModeUI;
import com.google.gwt.dev.ui.DoneCallback;
import com.google.gwt.dev.ui.DoneEvent;
import com.google.gwt.dev.ui.RestartServerCallback;
import com.google.gwt.dev.ui.RestartServerEvent;

import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * An implementation of a UI for the development mode server that sends UI
 * events over the network to a remote viewer. Also receives commands from the
 * remote viewer (such as a web server restart) and forwards the requests to the
 * development mode server.
 */
public class RemoteUI extends DevModeUI implements
    MessageTransport.ErrorCallback {

  private final String clientId;
  private final DevModeServiceRequestProcessor devModeRequestProcessor;
  private final List<ModuleHandle> modules = new ArrayList<ModuleHandle>();
  private final Object modulesLock = new Object();
  private final Socket transportSocket;
  private final MessageTransport transport;
  private ViewerServiceClient viewerServiceClient = null;
  private final List<String> cachedStartupUrls = new ArrayList<String>();
  
  public RemoteUI(String host, int port, String clientId) {
    try {
      this.clientId = clientId;
      transportSocket = new Socket(host, port);
      transportSocket.setKeepAlive(true);
      transportSocket.setTcpNoDelay(true);
      devModeRequestProcessor = new DevModeServiceRequestProcessor(this);
      transport = new MessageTransport(transportSocket.getInputStream(),
          transportSocket.getOutputStream(), devModeRequestProcessor, this);
      transport.start();
    } catch (UnknownHostException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public ModuleHandle getModuleLogger(String userAgent, String remoteSocket,
      String url, String tabKey, String moduleName, String sessionKey,
      String agentTag, byte[] agentIcon, Type logLevel) {

    final int logHandle = viewerServiceClient.addModuleLog(remoteSocket, url,
        tabKey, moduleName, sessionKey, agentTag, agentIcon);
    final ViewerServiceTreeLogger moduleLogger = new ViewerServiceTreeLogger(
        viewerServiceClient);
    moduleLogger.initLogHandle(logHandle);
    moduleLogger.setMaxDetail(getLogLevel());
    ModuleHandle handle = new ModuleHandle() {
      public TreeLogger getLogger() {
        return moduleLogger;
      }

      public void unload() {
        synchronized (modulesLock) {
          if (!modules.contains(this)) {
            return;
          }
        }
        try {
          viewerServiceClient.disconnectLog(logHandle);
        } finally {
          synchronized (modulesLock) {
            modules.remove(this);
          }
        }
      }
    };
    synchronized (modulesLock) {
      modules.add(handle);
    }

    if (moduleLogger.isLoggable(TreeLogger.SPAM)) {
      if (url != null) {
        moduleLogger.log(TreeLogger.SPAM, "Top URL: " + url);
      }

      moduleLogger.log(TreeLogger.SPAM, "User agent: " + userAgent);
      moduleLogger.log(TreeLogger.SPAM, "Remote socket: " + remoteSocket);
      if (tabKey != null) {
        moduleLogger.log(TreeLogger.SPAM, "Tab key: " + tabKey);
      }
      if (sessionKey != null) {
        moduleLogger.log(TreeLogger.SPAM, "Session key: " + sessionKey);
      }
    }

    return handle;
  }

  @Override
  public TreeLogger getWebServerLogger(String serverName, byte[] serverIcon) {
    return getConsoleLogger();
  }

  @Override
  public void moduleLoadComplete(boolean success) {
    // Until the RemoteMessage protobuf's backwards compatibility issues are
    // resolved, we send the startup URLs as part of the moduleLoadComplete so
    // they are not displayed before the server is ready to serve the modules at
    // the URLs.
    viewerServiceClient = new ViewerServiceClient(transport, getTopLogger());
    viewerServiceClient.initialize(clientId, cachedStartupUrls);
    viewerServiceClient.checkCapabilities();
  }

  public void onResponseException(Exception e) {
    getTopLogger().log(TreeLogger.INFO,
        "An exception occured while attempting to send a response message.", e);
  }

  public void onTermination(Exception e) {
    getConsoleLogger().log(TreeLogger.INFO,
        "Remote UI connection terminated due to exception: " + e);
    getConsoleLogger().log(TreeLogger.INFO,
        "Shutting down development mode server.");

    try {
      // Close the transport socket
      transportSocket.close();
    } catch (IOException e1) {
      // Purposely ignored
    }

    ((DoneCallback) getCallback(DoneEvent.getType())).onDone();
  }

  public boolean restartWebServer() {
    TreeLogger webServerLogger = getConsoleLogger();
    assert (webServerLogger != null);

    RestartServerCallback callback = ((RestartServerCallback) getCallback(RestartServerEvent.getType()));
    if (callback != null) {
      callback.onRestartServer(webServerLogger);
      return true;
    } else {
      // The server is still starting up
    }

    return false;
  }

  @Override
  public void setStartupUrls(Map<String, URL> urls) {
    for (URL url : urls.values()) {
      cachedStartupUrls.add(url.toExternalForm());
    }

    super.setStartupUrls(urls);
  }
}
