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
package com.google.gwt.dev;

import com.google.gwt.core.ext.ServletContainer;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.DevModeBase.OptionPort;
import com.google.gwt.dev.DevModeBase.OptionStartupURLs;
import com.google.gwt.dev.shell.jetty.JettyLauncher;
import com.google.gwt.dev.util.BrowserLauncher;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.util.tools.ArgHandlerExtra;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An utility class for running web apps with Jetty and launching the default
 * browser.
 */
public class RunWebApp {

  interface RunWebAppOptions extends OptionStartupURLs, OptionPort {
  }

  static class RunWebAppOptionsImpl implements RunWebAppOptions {
    private int port;
    private final List<String> startupURLs = new ArrayList<String>();

    @Override
    public void addStartupURL(String url) {
      startupURLs.add(url);
    }

    @Override
    public int getPort() {
      return port;
    }

    @Override
    public List<String> getStartupURLs() {
      return Collections.unmodifiableList(startupURLs);
    }

    @Override
    public void setPort(int port) {
      this.port = port;
    }
  }

  private class ArgHandlerWar extends ArgHandlerExtra {
    @Override
    public boolean addExtraArg(String arg) {
      warFile = new File(arg);
      if (!warFile.exists()) {
        System.err.println("Could not open war file '"
            + warFile.getAbsolutePath() + "'");
        return false;
      }
      return true;
    }

    @Override
    public String getPurpose() {
      return "Specifies the location of the target .war file or war directory";
    }

    @Override
    public String[] getTagArgs() {
      return new String[] {"war"};
    }

    @Override
    public boolean isRequired() {
      return true;
    }
  }

  private class ArgProcessor extends ArgProcessorBase {
    public ArgProcessor(RunWebAppOptions options) {
      registerHandler(new DevMode.ArgHandlerStartupURLs(options));
      registerHandler(new DevModeBase.ArgHandlerPort(options));
      registerHandler(new ArgHandlerWar());
    }

    @Override
    protected String getName() {
      return RunWebApp.class.getName();
    }
  }

  public static void main(String[] args) {
    try {
      RunWebAppOptionsImpl options = new RunWebAppOptionsImpl();
      RunWebApp runWebApp = new RunWebApp(options);
      ArgProcessor argProcessor = runWebApp.new ArgProcessor(options);
      if (argProcessor.processArgs(args)) {
        runWebApp.run();
      }
    } catch (Exception e) {
      System.err.println("Unable to start Jetty server");
      e.printStackTrace();
    }
  }

  protected File warFile;

  private final RunWebAppOptions options;

  public RunWebApp(RunWebAppOptions options) {
    this.options = options;
  }

  protected void run() {
    PrintWriterTreeLogger logger = new PrintWriterTreeLogger();
    logger.setMaxDetail(TreeLogger.WARN);
    int port = options.getPort();
    try {
      ServletContainer scl = new JettyLauncher().start(logger, port, warFile);
      port = scl.getPort();
    } catch (Exception e) {
      System.err.println("Unable to start Jetty server");
      e.printStackTrace();
      return;
    }
    if (options.getStartupURLs().isEmpty()) {
      options.addStartupURL("/");
    }
    for (String startupUrl : options.getStartupURLs()) {
      startupUrl = DevModeBase.normalizeURL(startupUrl, false, port,
          "localhost");
      try {
        BrowserLauncher.browse(startupUrl);
      } catch (IOException e) {
        System.err.println("Unable to start " + startupUrl);
      } catch (URISyntaxException e) {
        System.err.println(startupUrl + " is not a valid URL");
      }
    }
  }
}
