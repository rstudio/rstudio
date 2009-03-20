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
import com.google.gwt.dev.HostedModeBase.OptionPort;
import com.google.gwt.dev.HostedModeBase.OptionStartupURLs;
import com.google.gwt.dev.shell.BrowserWidget;
import com.google.gwt.dev.shell.jetty.JettyLauncher;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.util.tools.ArgHandlerExtra;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An experimental class for running web apps.
 */
public class RunWebApp {

  interface RunWebAppOptions extends OptionStartupURLs, OptionPort {
  }

  static class RunWebAppOptionsImpl implements RunWebAppOptions {
    private int port;
    private final List<String> startupURLs = new ArrayList<String>();

    public void addStartupURL(String url) {
      startupURLs.add(url);
    }

    public int getPort() {
      return port;
    }

    public List<String> getStartupURLs() {
      return Collections.unmodifiableList(startupURLs);
    }

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
      registerHandler(new HostedMode.ArgHandlerStartupURLs(options));
      registerHandler(new HostedModeBase.ArgHandlerPort(options));
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
      startupUrl = HostedModeBase.normalizeURL(startupUrl, port, "localhost");
      BrowserWidget.launchExternalBrowser(logger, startupUrl);
    }
  }
}
