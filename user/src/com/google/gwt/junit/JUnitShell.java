/*
 * Copyright 2006 Google Inc.
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
import com.google.gwt.dev.GWTShell;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.ModuleDefLoader;
import com.google.gwt.dev.shell.BrowserWidgetHost;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.junit.client.TimeoutException;
import com.google.gwt.junit.remote.BrowserManager;
import com.google.gwt.util.tools.ArgHandlerFlag;
import com.google.gwt.util.tools.ArgHandlerString;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import junit.framework.TestResult;

import java.rmi.Naming;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is responsible for hosting JUnit test case execution. There are
 * three main pieces to the JUnit system.
 * 
 * <ul>
 * <li>Test environment</li>
 * <li>Client classes</li>
 * <li>Server classes</li>
 * </ul>
 * 
 * <p>
 * The test environment consists of this class and the non-translatable version
 * of {@link com.google.gwt.junit.client.GWTTestCase}. These two classes
 * integrate directly into the real JUnit test process.
 * </p>
 * 
 * <p>
 * The client classes consist of the translatable version of
 * {@link com.google.gwt.junit.client.GWTTestCase}, translatable JUnit classes,
 * and the user's own {@link com.google.gwt.junit.client.GWTTestCase}-derived
 * class. The client communicates to the server via RPC.
 * </p>
 * 
 * <p>
 * The server consists of {@link com.google.gwt.junit.server.JUnitHostImpl}, an
 * RPC servlet which communicates back to the test environment through a
 * {@link JUnitMessageQueue}, thus closing the loop.
 * </p>
 */
public class JUnitShell extends GWTShell {

  /**
   * This is a system property that, when set, emulates command line arguments.
   */
  private static final String PROP_GWT_ARGS = "gwt.args";

  /**
   * This legacy system property, when set, causes us to run in web mode.
   * (Superceded by passing "-web" into gwt.args).
   */
  private static final String PROP_JUNIT_HYBRID_MODE = "gwt.hybrid";

  /**
   * Wait a long time for the client to contact the server and begin running the
   * test.
   */
  private static final int TEST_BEGIN_TIMEOUT_MILLIS = 20000;

  /**
   * Singleton object for hosting unit tests. All test case instances executed
   * by the TestRunner will use the single unitTestShell.
   */
  private static JUnitShell unitTestShell;

  static {
    ModuleDefLoader.forceInherit("com.google.gwt.junit.JUnit");
    ModuleDefLoader.setEnableCachingModules(true);
  }

  /**
   * Called by {@link com.google.gwt.junit.server.JUnitHostImpl} to get an
   * interface into the test process.
   * 
   * @return The {@link JUnitMessageQueue} interface that belongs to the
   *         singleton {@link JUnitShell}, or <code>null</code> if no such
   *         singleton exists.
   */
  public static JUnitMessageQueue getMessageQueue() {
    if (unitTestShell == null) {
      return null;
    }
    return unitTestShell.messageQueue;
  }

  /**
   * Entry point for {@link com.google.gwt.junit.client.GWTTestCase}. Gets or
   * creates the singleton {@link JUnitShell} and invokes its
   * {@link #runTestImpl(String, TestCase, TestResult)}.
   */
  public static void runTest(String moduleName, TestCase testCase,
      TestResult testResult) throws UnableToCompleteException {
    getUnitTestShell().runTestImpl(moduleName, testCase, testResult);
  }

  /**
   * Lazily initialize the singleton JUnitShell.
   */
  private static JUnitShell getUnitTestShell() {
    if (unitTestShell == null) {
      JUnitShell shell = new JUnitShell();
      String[] args = shell.synthesizeArgs();
      if (!shell.processArgs(args)) {
        System.exit(1);
      }
      if (!shell.startUp()) {
        return null;
      }
      unitTestShell = shell;
    }

    return unitTestShell;
  }

  /**
   * When headless, all logging goes to the console.
   */
  private PrintWriterTreeLogger consoleLogger;

  /**
   * Portal to interact with the servlet.
   */
  private JUnitMessageQueue messageQueue = new JUnitMessageQueue();

  /**
   * What type of test we're running; Local hosted, local web, or remote web.
   */
  private RunStyle runStyle = new RunStyleLocalHosted(this);

  /**
   * The time at which the current test will fail if the client has not yet
   * started the test.
   */
  private long testBeginTimout;

  /**
   * Class name of the current/last test case to run.
   */
  private String testCaseClassName;

  /**
   * Enforce the singleton pattern. The call to {@link GWTShell}'s ctor forces
   * server mode and disables processing extra arguments as URLs to be shown.
   */
  private JUnitShell() {
    super(true, true);

    registerHandler(new ArgHandlerFlag() {

      public String getPurpose() {
        return "Causes your test to run in web (compiled) mode (defaults to hosted mode)";
      }

      public String getTag() {
        return "-web";
      }

      public boolean setFlag() {
        runStyle = new RunStyleLocalWeb(JUnitShell.this);
        return true;
      }

    });

    registerHandler(new ArgHandlerString() {

      public String getPurpose() {
        return "Runs web mode via RMI to a BrowserManagerServer; e.g. rmi://localhost/ie6";
      }

      public String getTag() {
        return "-remoteweb";
      }

      public String[] getTagArgs() {
        return new String[] {"rmiUrl"};
      }

      public boolean isUndocumented() {
        return true;
      }

      public boolean setString(String str) {
        try {
          BrowserManager browserManager = (BrowserManager) Naming.lookup(str);
          runStyle = new RunStyleRemoteWeb(JUnitShell.this, browserManager);
        } catch (Exception e) {
          System.err.println("Error connecting to browser manager at " + str);
          e.printStackTrace();
          System.exit(1);
          return false;
        }
        return true;
      }
    });

    registerHandler(new ArgHandlerFlag() {

      public String getPurpose() {
        return "Causes the log window and browser windows to be displayed; useful for debugging";
      }

      public String getTag() {
        return "-notHeadless";
      }

      public boolean setFlag() {
        setHeadless(false);
        return true;
      }
    });

    setRunTomcat(true);
    setHeadless(true);

    // Legacy: -Dgwt.hybrid runs web mode
    if (System.getProperty(PROP_JUNIT_HYBRID_MODE) != null) {
      runStyle = new RunStyleLocalWeb(this);
    }
  }

  public TreeLogger getTopLogger() {
    if (consoleLogger != null) {
      return consoleLogger;
    } else {
      return super.getTopLogger();
    }
  }

  protected String doGetDefaultLogLevel() {
    return "WARN";
  }

  /**
   * Overrides the default module loading behavior. Clears any existing entry
   * points and adds an entry point for the class being tested. This test class
   * is then rebound to a generated subclass.
   */
  protected ModuleDef doLoadModule(TreeLogger logger, String moduleName)
      throws UnableToCompleteException {

    ModuleDef module = super.doLoadModule(logger, moduleName);

    // Tweak the module for JUnit support
    //
    module.clearEntryPoints();
    module.addEntryPointTypeName(testCaseClassName);
    return module;
  }

  /**
   * Never check for updates in JUnit mode.
   */
  protected boolean doShouldCheckForUpdates() {
    return false;
  }

  protected void initializeLogger() {
    if (isHeadless()) {
      consoleLogger = new PrintWriterTreeLogger();
      consoleLogger.setMaxDetail(getLogLevel());
    } else {
      super.initializeLogger();
    }
  }

  /**
   * Overrides {@link GWTShell#notDone()} to wait for the currently-running test
   * to complete.
   */
  protected boolean notDone() {
    if (messageQueue.hasNextTestName(testCaseClassName)
        && testBeginTimout < System.currentTimeMillis()) {
      throw new TimeoutException(
          "The browser did not contact the server within "
              + TEST_BEGIN_TIMEOUT_MILLIS + "ms.");
    }

    if (messageQueue.hasResult(testCaseClassName)) {
      return false;
    }

    return !runStyle.wasInterrupted();
  }

  void compileForWebMode(String moduleName) throws UnableToCompleteException {
    BrowserWidgetHost browserHost = getBrowserHost();
    assert (browserHost != null);
    browserHost.compile(new String[] {moduleName});
  }

  /**
   * Runs a particular test case.
   */
  private void runTestImpl(String moduleName, TestCase testCase,
      TestResult testResult) throws UnableToCompleteException {

    String newTestCaseClassName = testCase.getClass().getName();
    messageQueue.setNextTestName(newTestCaseClassName, testCase.getName());

    boolean forceLaunch = !newTestCaseClassName.equals(testCaseClassName);
    testCaseClassName = newTestCaseClassName;
    runStyle.maybeLaunchModule(moduleName, forceLaunch);

    // Wait for test to complete
    try {
      // Set a timeout period to automatically fail if the servlet hasn't been
      // contacted; something probably went wrong (the module failed to load?)
      testBeginTimout = System.currentTimeMillis() + TEST_BEGIN_TIMEOUT_MILLIS;
      pumpEventLoop();
    } catch (TimeoutException e) {
      testResult.addError(testCase, e);
      return;
    }

    Throwable result = messageQueue.getResult(testCaseClassName);
    if (result instanceof AssertionFailedError) {
      testResult.addFailure(testCase, (AssertionFailedError) result);
    } else if (result != null) {
      testResult.addError(testCase, result);
    }
  }

  /**
   * Synthesize command line arguments from a system property.
   */
  private String[] synthesizeArgs() {
    ArrayList argList = new ArrayList();

    String args = System.getProperty(PROP_GWT_ARGS);
    if (args != null) {
      // Match either a non-whitespace, non start of quoted string, or a
      // quoted string that can have embedded, escaped quoting characters
      //
      Pattern pattern = Pattern.compile("[^\\s\"]+|\"[^\"\\\\]*(\\\\.[^\"\\\\]*)*\"");
      Matcher matcher = pattern.matcher(args);
      while (matcher.find()) {
        argList.add(matcher.group());
      }
    }

    return (String[]) argList.toArray(new String[argList.size()]);
  }
}
