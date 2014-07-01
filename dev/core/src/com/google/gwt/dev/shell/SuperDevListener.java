/*
 * Copyright 2014 Google Inc.
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
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.impl.StandardLinkerContext;
import com.google.gwt.dev.DevMode.HostedModeOptions;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.util.tools.Utility;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Starts a superdev-mode codeserver.
 */
public class SuperDevListener implements CodeServerListener {

  private final Thread listenThread;
  private final TreeLogger logger;
  private final HostedModeOptions options;
  private final int codeServerPort;

  /**
   * Listens for new connections from browsers.
   */
  public SuperDevListener(TreeLogger treeLogger, HostedModeOptions options) {
    List<String> args = new ArrayList<String>();
    this.logger = treeLogger;
    this.options = options;

    int port = options.getCodeServerPort();
    if (port < 0 || port == 9997) {
      port = 9876;
    } else if (port == 0) {
      try {
        ServerSocket serverSocket = new ServerSocket(0);
        port = serverSocket.getLocalPort();
        serverSocket.close();
      } catch (IOException e) {
        logger.log(TreeLogger.ERROR, "Unable to get an unnused port.");
        throw new RuntimeException(e);
      }
    }
    codeServerPort = port;

    args.add("-noprecompile");
    args.add("-port");
    args.add(String.valueOf(codeServerPort));
    args.add("-sourceLevel");
    args.add(String.valueOf(options.getSourceLevel()));
    if (options.getBindAddress() != null) {
      args.add("-bindAddress");
      args.add(options.getBindAddress());
    }
    if (options.getWorkDir() != null) {
      args.add("-workDir");
      args.add(String.valueOf(options.getWorkDir()));
    }
    if (options.getLogLevel() != null) {
      args.add("-logLevel");
      args.add(String.valueOf(options.getLogLevel()));
    }
    for (String mod : options.getModuleNames()) {
      args.add(mod);
    }

    final String[] codeServerArgs = args.toArray(new String[0]);

    logger.log(Type.INFO, "Runing CodeServer with parameters: " + args);

    // Using reflection so as we don't create a circular dependency between
    // dev.jar && codeserver.jar
    final Method mainMethod;
    try {
      Class<?> clazz = Class.forName("com.google.gwt.dev.codeserver.CodeServer");
      mainMethod = clazz.getMethod("main", String[].class);
    } catch (ClassNotFoundException e) {
      logger.log(TreeLogger.ERROR, "Unable to find main() method for Super Dev Mode "
          + "code server. Hint: verify that gwt-codeserver.jar is in your classpath.");
      throw new RuntimeException(e);
    } catch (NoSuchMethodException e) {
      logger.log(TreeLogger.ERROR, "Unable to run superdev codeServer.", e);
      throw new RuntimeException(e);
    }

    listenThread = new Thread() {
      public void run() {
        try {
          mainMethod.invoke(null, new Object[] {codeServerArgs});
        } catch (Exception e) {
          logger.log(TreeLogger.ERROR, "Unable to run superdev codeServer.", e);
        }
      }
    };
    listenThread.setName("SuperDevMode code server listener");
    listenThread.setDaemon(true);
  }

  @Override
  public int getSocketPort() {
    return codeServerPort;
  }

  @Override
  public URL makeStartupUrl(String url) throws UnableToCompleteException {
    try {
      return new URL(url);
    } catch (MalformedURLException e) {
      logger.log(TreeLogger.ERROR, "Invalid URL " + url, e);
      throw new UnableToCompleteException();
    }
  }

  @Override
  public void writeCompilerOutput(StandardLinkerContext linkerStack, ArtifactSet artifacts,
      ModuleDef module, boolean isRelink) throws UnableToCompleteException {
    try {
      String computeScriptBase =
          Utility.getFileFromClassPath("com/google/gwt/dev/codeserver/computeScriptBase.js");
      String contents =
          Utility.getFileFromClassPath("com/google/gwt/dev/codeserver/stub.nocache.js");

      File file =
          new File(options.getWarDir() + "/" + module.getName() + "/" + module.getName()
              + ".nocache.js");

      file.deleteOnExit();

      file.getParentFile().mkdirs();

      Map<String, String> replacements = new HashMap<String, String>();
      replacements.put("__COMPUTE_SCRIPT_BASE__", computeScriptBase);
      // must go after script base
      replacements.put("__MODULE_NAME__", module.getName());
      replacements.put("__SUPERDEV_PORT__", "" + codeServerPort);

      Utility.writeTemplateFile(file, contents, replacements);

      // TODO(manolo): make codeserver accept multiple user.agent comma separated
      // although sourcemaps would not work, and an empty _callback parameter
      // returning no wrapped json response.
      String recompileUrl =
          "http://" + options.getConnectAddress() + ":" + codeServerPort + "/recompile/"
              + module.getName() + "?_calback=bar&user.agent=safari";

      System.out.println("To compile the module '" + module.getName()
          + "' , visit:\n " + recompileUrl);

    } catch (IOException e) {
      logger.log(TreeLogger.ERROR, "Unable to create nocache script ", e);
      throw new UnableToCompleteException();
    }
  }

  @Override
  public void setIgnoreRemoteDeath(boolean b) {
  }

  @Override
  public void start() {
    listenThread.start();
  }
}
