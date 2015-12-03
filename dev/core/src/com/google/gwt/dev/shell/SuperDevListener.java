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
import com.google.gwt.dev.util.arg.OptionJsInteropMode;
import com.google.gwt.dev.util.arg.OptionMethodNameDisplayMode;
import com.google.gwt.thirdparty.guava.common.base.Stopwatch;
import com.google.gwt.thirdparty.guava.common.collect.ListMultimap;
import com.google.gwt.thirdparty.guava.common.collect.Lists;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Starts a superdev-mode codeserver.
 */
public class SuperDevListener implements CodeServerListener {

  private final TreeLogger logger;
  private final int codeServerPort;
  private List<String> codeServerArgs;

  /**
   * Listens for new connections from browsers.
   */
  public SuperDevListener(TreeLogger treeLogger, HostedModeOptions options) {
    this.logger = treeLogger;
    this.codeServerPort = chooseCodeServerPort(treeLogger, options);

    // This directory must exist when the Code Server starts.
    ensureModuleBaseDir(options);

    codeServerArgs = makeCodeServerArgs(options, codeServerPort);
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
    // The code server will do this.
  }

  @Override
  public void setIgnoreRemoteDeath(boolean b) {
  }

  @Override
  public void start() {
    try {
      Stopwatch watch = Stopwatch.createStarted();
      logger.log(Type.INFO, "Runing CodeServer with parameters: " + codeServerArgs);
      runCodeServer(codeServerArgs.toArray(new String[0]));
      logger.log(Type.INFO, "Code server started in " + watch + " ms");
    } catch (Exception e) {
      logger.log(Type.INFO, "Unable to start Code server");
      throw new RuntimeException(e);
    }
  }

  private void runCodeServer(String[] mainArgs) throws Exception {
    // Using reflection so as we don't create a circular dependency between
    // dev.jar && codeserver.jar
    Method mainMethod;
    try {
      Class<?> clazz = Class.forName("com.google.gwt.dev.codeserver.CodeServer");
      mainMethod = clazz.getMethod("main", String[].class);
    } catch (ClassNotFoundException e) {
      logger.log(TreeLogger.ERROR, "Unable to find main() method for Super Dev Mode "
          + "code server. Hint: verify that gwt-codeserver.jar is in your classpath.");
      throw e;
    }

    mainMethod.invoke(null, new Object[] {mainArgs});
  }

  private static int chooseCodeServerPort(TreeLogger logger, HostedModeOptions options) {
    int port = options.getCodeServerPort();
    if (port == 0) {
      // Automatically choose an unused port.
      try {
        ServerSocket serverSocket = new ServerSocket(0);
        port = serverSocket.getLocalPort();
        serverSocket.close();
        return port;
      } catch (IOException e) {
        logger.log(TreeLogger.ERROR, "Unable to get an unnused port.");
        throw new RuntimeException(e);
      }
    } else if (port < 0 || port == 9997) {
      // 9997 is the default non-SuperDevMode port from DevModeBase. TODO: use constant.
      return 9876; // Default Super Dev Mode port
    } else {
      return port; // User-specified port
    }
  }

  private static void ensureModuleBaseDir(HostedModeOptions options) {
    File dir = options.getModuleBaseDir();
    if (!dir.isDirectory()) {
      dir.mkdirs();
      if (!dir.isDirectory()) {
        throw new RuntimeException("unable to create module base directory: " +
            dir.getAbsolutePath());
      }
    }
  }

  private static List<String> makeCodeServerArgs(HostedModeOptions options, int port) {
    List<String> args = new ArrayList<String>();
    args.add("-noprecompile");
    args.add("-port");
    args.add(String.valueOf(port));
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
    args.add("-launcherDir");
    args.add(options.getModuleBaseDir().getAbsolutePath());
    if (options.getLogLevel() != null) {
      args.add("-logLevel");
      args.add(String.valueOf(options.getLogLevel()));
    }
    if (options.getJsInteropMode() != OptionJsInteropMode.Mode.JS_RC) {
      args.add("-XjsInteropMode");
      args.add(options.getJsInteropMode().name());
    }
    if (options.shouldGenerateJsInteropExports()) {
      args.add("-generateJsInteropExports");
    }
    if (!options.isIncrementalCompileEnabled()) {
      args.add("-noincremental");
    }
    if (options.getMethodNameDisplayMode() != OptionMethodNameDisplayMode.Mode.NONE) {
      args.add("-XmethodNameDisplayMode");
      args.add(options.getMethodNameDisplayMode().name());
    }

    args.add("-style");
    args.add(options.getOutput().name());

    if (options.getProperties().size() > 0) {
      args.addAll(makeSetPropertyArgs(options.getProperties()));
    }
    for (String mod : options.getModuleNames()) {
      args.add(mod);
    }
    return args;
  }

  private static List<String> makeSetPropertyArgs(
      ListMultimap<String, String> properties) {
    List<String> propertyArgs = Lists.newArrayList();
    for (String propertyName : properties.keySet()) {
      propertyArgs.add("-setProperty");
      StringBuilder nameValues = new StringBuilder(propertyName + "=");
      for (String propertyValue : properties.get(propertyName)) {
        nameValues.append(propertyValue + ",");
      }
      propertyArgs.add(nameValues.substring(0, nameValues.length() - 1));
    }
    return propertyArgs;
  }
}
