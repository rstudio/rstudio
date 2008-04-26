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
package com.google.gwt.junit;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.dev.BootStrapPlatform;
import com.google.gwt.dev.GWTShell;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.ModuleDefLoader;
import com.google.gwt.dev.cfg.Properties;
import com.google.gwt.dev.cfg.Property;
import com.google.gwt.dev.shell.BrowserWidgetHost;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.junit.client.TimeoutException;
import com.google.gwt.junit.client.impl.GWTRunner;
import com.google.gwt.junit.client.impl.JUnitResult;
import com.google.gwt.junit.client.impl.JUnitHost.TestInfo;
import com.google.gwt.util.tools.ArgHandler;
import com.google.gwt.util.tools.ArgHandlerFlag;
import com.google.gwt.util.tools.ArgHandlerString;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import junit.framework.TestResult;

import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
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
 * The client classes consist of the translatable version of {@link
 * com.google.gwt.junit.client.GWTTestCase}, translatable JUnit classes, and the
 * user's own {@link com.google.gwt.junit.client.GWTTestCase}-derived class.
 * The client communicates to the server via RPC.
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
   * A strategy for running the test.
   */
  public interface Strategy {
    String getModuleInherit();

    String getSyntheticModuleExtension();

    void processResult(TestCase testCase, JUnitResult result);
  }

  private static class JUnitStrategy implements Strategy {
    public String getModuleInherit() {
      return "com.google.gwt.junit.JUnit";
    }

    public String getSyntheticModuleExtension() {
      return "JUnit";
    }

    public void processResult(TestCase testCase, JUnitResult result) {
    }
  }

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
   * The amount of time to wait for all clients to have contacted the server and
   * begun running the test.
   */
  private static final int TEST_BEGIN_TIMEOUT_MILLIS = 60000;

  /**
   * Singleton object for hosting unit tests. All test case instances executed
   * by the TestRunner will use the single unitTestShell.
   */
  private static JUnitShell unitTestShell;

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
   * creates the singleton {@link JUnitShell} and invokes its {@link
   * #runTestImpl(String, TestCase, TestResult, Strategy)}.
   */
  public static void runTest(String moduleName, TestCase testCase,
      TestResult testResult) throws UnableToCompleteException {
    getUnitTestShell().runTestImpl(moduleName, testCase, testResult,
        new JUnitStrategy());
  }

  public static void runTest(String moduleName, TestCase testCase,
      TestResult testResult, Strategy strategy)
      throws UnableToCompleteException {
    getUnitTestShell().runTestImpl(moduleName, testCase, testResult, strategy);
  }

  /**
   * Retrieves the JUnitShell. This should only be invoked during TestRunner
   * execution of JUnit tests.
   */
  private static JUnitShell getUnitTestShell() {
    if (unitTestShell == null) {
      BootStrapPlatform.init();
      BootStrapPlatform.applyPlatformHacks();
      JUnitShell shell = new JUnitShell();
      String[] args = shell.synthesizeArgs();
      if (!shell.processArgs(args)) {
        throw new RuntimeException("Invalid shell arguments");
      }

      shell.messageQueue = new JUnitMessageQueue(shell.numClients);

      if (!shell.startUp()) {
        throw new RuntimeException("Shell failed to start");
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
   * Name of the module containing the current/last module to run.
   */
  private ModuleDef currentModule;

  /**
   * If true, no launches have yet been successful.
   */
  private boolean firstLaunch = true;

  /**
   * If true, the last attempt to launch failed.
   */
  private boolean lastLaunchFailed;

  /**
   * Portal to interact with the servlet.
   */
  private JUnitMessageQueue messageQueue;

  /**
   * The number of test clients executing in parallel. With -remoteweb, users
   * can specify a number of parallel test clients, but by default we only have
   * 1.
   */
  private int numClients = 1;

  /**
   * What type of test we're running; Local hosted, local web, or remote web.
   */
  private RunStyle runStyle = new RunStyleLocalHosted(this);

  /**
   * The time the test actually began.
   */
  private long testBeginTime;

  /**
   * The time at which the current test will fail if the client has not yet
   * started the test.
   */
  private long testBeginTimeout;

  /**
   * Enforce the singleton pattern. The call to {@link GWTShell}'s ctor forces
   * server mode and disables processing extra arguments as URLs to be shown.
   */
  private JUnitShell() {
    super(true, true);

    registerHandler(new ArgHandlerFlag() {

      @Override
      public String getPurpose() {
        return "Causes your test to run in web (compiled) mode (defaults to hosted mode)";
      }

      @Override
      public String getTag() {
        return "-web";
      }

      @Override
      public boolean setFlag() {
        runStyle = new RunStyleLocalWeb(JUnitShell.this);
        numClients = 1;
        return true;
      }

    });

    registerHandler(new ArgHandlerString() {

      @Override
      public String getPurpose() {
        return "Runs web mode via RMI to a BrowserManagerServer; e.g. rmi://localhost/ie6";
      }

      @Override
      public String getTag() {
        return "-remoteweb";
      }

      @Override
      public String[] getTagArgs() {
        return new String[] {"rmiUrl"};
      }

      @Override
      public boolean isUndocumented() {
        return true;
      }

      @Override
      public boolean setString(String str) {
        String[] urls = str.split(",");
        runStyle = RunStyleRemoteWeb.create(JUnitShell.this, urls);
        numClients = urls.length;
        return runStyle != null;
      }
    });

    registerHandler(new ArgHandler() {

      @Override
      public String[] getDefaultArgs() {
        return null;
      }

      @Override
      public String getPurpose() {
        return "Causes the system to wait for a remote browser to connect";
      }

      @Override
      public String getTag() {
        return "-wait";
      }

      @Override
      public String[] getTagArgs() {
        return new String[] {"[numClients]"};
      }

      @Override
      public int handle(String[] args, int tagIndex) {
        int value = 1;
        if (tagIndex + 1 < args.length) {
          try {
            // See if the next item is an integer.
            value = Integer.parseInt(args[tagIndex + 1]);
            if (value >= 1) {
              setInt(value);
              return 1;
            }
          } catch (NumberFormatException e) {
            // fall-through
          }
        }
        setInt(1);
        return 0;
      }

      public void setInt(int value) {
        runStyle = new RunStyleWait(JUnitShell.this, value);
        numClients = value;
      }

    });

    registerHandler(new ArgHandlerFlag() {

      @Override
      public String getPurpose() {
        return "Causes the log window and browser windows to be displayed; useful for debugging";
      }

      @Override
      public String getTag() {
        return "-notHeadless";
      }

      @Override
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

  @Override
  public TreeLogger getTopLogger() {
    if (consoleLogger != null) {
      return consoleLogger;
    } else {
      return super.getTopLogger();
    }
  }

  @Override
  protected Type doGetDefaultLogLevel() {
    return Type.WARN;
  }

  /**
   * Never check for updates in JUnit mode.
   */
  @Override
  protected boolean doShouldCheckForUpdates() {
    return false;
  }

  @Override
  protected ArgHandlerPort getArgHandlerPort() {
    return new ArgHandlerPort() {
      @Override
      public String[] getDefaultArgs() {
        return new String[] {"-port", "auto"};
      }
    };
  }

  @Override
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
  @Override
  protected boolean notDone() {
    int activeClients = messageQueue.getNumClientsRetrievedCurrentTest();
    if (firstLaunch && runStyle instanceof RunStyleWait) {
      String[] newClients = messageQueue.getNewClients();
      int printIndex = activeClients - newClients.length + 1;
      for (String newClient : newClients) {
        System.out.println(printIndex + " - " + newClient);
        ++printIndex;
      }
      if (activeClients == this.numClients) {
        System.out.println("Starting tests");
      } else {
        // Wait forever for first contact; user-driven.
        return true;
      }
    }

    long currentTimeMillis = System.currentTimeMillis();
    if (activeClients == numClients) {
      firstLaunch = false;
    } else if (testBeginTimeout < currentTimeMillis) {
      double elapsed = (currentTimeMillis - testBeginTime) / 1000.0;
      throw new TimeoutException(
          "The browser did not contact the server within "
              + TEST_BEGIN_TIMEOUT_MILLIS + "ms.\n"
              + messageQueue.getUnretrievedClients()
              + "\n Actual time elapsed: " + elapsed + " seconds.\n");
    }

    if (runStyle.wasInterrupted()) {
      throw new TimeoutException("A remote browser died a mysterious death.");
    }

    return !messageQueue.hasResult();
  }

  @Override
  protected void sleep() {
    if (runStyle.isLocal()) {
      super.sleep();
    } else {
      messageQueue.waitForResults(1000);
    }
  }

  void compileForWebMode(String moduleName, String userAgentString)
      throws UnableToCompleteException {
    ModuleDef module = doLoadModule(getTopLogger(), moduleName);
    if (userAgentString != null) {
      Properties props = module.getProperties();
      Property userAgent = props.find("user.agent");
      if (userAgent != null) {
        userAgent.setActiveValue(userAgentString);
      }
    }
    BrowserWidgetHost browserHost = getBrowserHost();
    assert (browserHost != null);
    browserHost.compile(module);
  }

  /**
   * Runs a particular test case.
   */
  private void runTestImpl(String moduleName, TestCase testCase,
      TestResult testResult, Strategy strategy)
      throws UnableToCompleteException {

    if (lastLaunchFailed) {
      throw new UnableToCompleteException();
    }

    String syntheticModuleName = moduleName + "."
        + strategy.getSyntheticModuleExtension();
    boolean sameTest = (currentModule != null)
        && syntheticModuleName.equals(currentModule.getName());
    if (sameTest && lastLaunchFailed) {
      throw new UnableToCompleteException();
    }

    if (!sameTest) {
      /*
       * Synthesize a synthetic module that derives from the user-specified
       * module but also includes JUnit support.
       */
      currentModule = ModuleDefLoader.createSyntheticModule(getTopLogger(),
          syntheticModuleName, new String[] {
              moduleName, strategy.getModuleInherit()}, true);
      // Replace any user entry points with our test runner.
      currentModule.clearEntryPoints();
      currentModule.addEntryPointTypeName(GWTRunner.class.getName());
      // Squirrel away the name of the active module for GWTRunnerGenerator
      Property moduleNameProp = currentModule.getProperties().create(
          "junit.moduleName");
      moduleNameProp.addKnownValue(moduleName);
      moduleNameProp.setActiveValue(moduleName);
      runStyle.maybeCompileModule(syntheticModuleName);
    }

    messageQueue.setNextTest(new TestInfo(currentModule.getName(),
        testCase.getClass().getName(), testCase.getName()));

    try {
      if (firstLaunch) {
        runStyle.launchModule(currentModule.getName());
      }
    } catch (UnableToCompleteException e) {
      lastLaunchFailed = true;
      testResult.addError(testCase, new JUnitFatalLaunchException(e));
      return;
    }

    // Wait for test to complete
    try {
      // Set a timeout period to automatically fail if the servlet hasn't been
      // contacted; something probably went wrong (the module failed to load?)
      testBeginTime = System.currentTimeMillis();
      testBeginTimeout = testBeginTime + TEST_BEGIN_TIMEOUT_MILLIS;
      pumpEventLoop();
    } catch (TimeoutException e) {
      lastLaunchFailed = true;
      testResult.addError(testCase, e);
      return;
    }

    assert (messageQueue.hasResult());
    Map<String, JUnitResult> results = messageQueue.getResults();

    boolean parallelTesting = numClients > 1;

    for (Entry<String, JUnitResult> entry : results.entrySet()) {
      String clientId = entry.getKey();
      JUnitResult result = entry.getValue();
      Throwable exception = result.getException();

      // In the case that we're running multiple clients at once, we need to
      // let the user know the browser in which the failure happened
      if (parallelTesting && exception != null) {
        String msg = "Remote test failed at " + clientId;
        if (exception instanceof AssertionFailedError) {
          AssertionFailedError newException = new AssertionFailedError(msg
              + "\n" + exception.getMessage());
          newException.setStackTrace(exception.getStackTrace());
          exception = newException;
        } else {
          exception = new RuntimeException(msg, exception);
        }
      }

      // A "successful" failure
      if (exception instanceof AssertionFailedError) {
        testResult.addFailure(testCase, (AssertionFailedError) exception);
      } else if (exception != null) {
        // A real failure
        if (exception instanceof JUnitFatalLaunchException) {
          lastLaunchFailed = true;
        }
        testResult.addError(testCase, exception);
      }

      strategy.processResult(testCase, result);
    }
  }

  /**
   * Synthesize command line arguments from a system property.
   */
  private String[] synthesizeArgs() {
    ArrayList<String> argList = new ArrayList<String>();

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

    return argList.toArray(new String[argList.size()]);
  }
}
