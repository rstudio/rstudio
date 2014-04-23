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

import com.google.gwt.core.ext.Linker;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.impl.StandardLinkerContext;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.core.shared.SerializableThrowable;
import com.google.gwt.dev.ArgProcessorBase;
import com.google.gwt.dev.Compiler;
import com.google.gwt.dev.CompilerContext;
import com.google.gwt.dev.CompilerOptions;
import com.google.gwt.dev.DevMode;
import com.google.gwt.dev.cfg.BindingProperty;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.Properties;
import com.google.gwt.dev.cfg.Property;
import com.google.gwt.dev.javac.CompilationProblemReporter;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.CompilationUnit;
import com.google.gwt.dev.shell.CheckForUpdates;
import com.google.gwt.dev.shell.jetty.JettyLauncher;
import com.google.gwt.dev.util.arg.ArgHandlerDeployDir;
import com.google.gwt.dev.util.arg.ArgHandlerDisableAggressiveOptimization;
import com.google.gwt.dev.util.arg.ArgHandlerDisableCastChecking;
import com.google.gwt.dev.util.arg.ArgHandlerDisableClassMetadata;
import com.google.gwt.dev.util.arg.ArgHandlerDisableClusterSimilarFunctions;
import com.google.gwt.dev.util.arg.ArgHandlerDisableInlineLiteralParameters;
import com.google.gwt.dev.util.arg.ArgHandlerDisableOptimizeDataflow;
import com.google.gwt.dev.util.arg.ArgHandlerDisableOrdinalizeEnums;
import com.google.gwt.dev.util.arg.ArgHandlerDisableRemoveDuplicateFunctions;
import com.google.gwt.dev.util.arg.ArgHandlerDisableRunAsync;
import com.google.gwt.dev.util.arg.ArgHandlerDisableUpdateCheck;
import com.google.gwt.dev.util.arg.ArgHandlerDraftCompile;
import com.google.gwt.dev.util.arg.ArgHandlerEnableAssertions;
import com.google.gwt.dev.util.arg.ArgHandlerExtraDir;
import com.google.gwt.dev.util.arg.ArgHandlerGenDir;
import com.google.gwt.dev.util.arg.ArgHandlerLocalWorkers;
import com.google.gwt.dev.util.arg.ArgHandlerLogLevel;
import com.google.gwt.dev.util.arg.ArgHandlerMaxPermsPerPrecompile;
import com.google.gwt.dev.util.arg.ArgHandlerNamespace;
import com.google.gwt.dev.util.arg.ArgHandlerOptimize;
import com.google.gwt.dev.util.arg.ArgHandlerScriptStyle;
import com.google.gwt.dev.util.arg.ArgHandlerSourceLevel;
import com.google.gwt.dev.util.arg.ArgHandlerWarDir;
import com.google.gwt.dev.util.arg.ArgHandlerWorkDirOptional;
import com.google.gwt.junit.JUnitMessageQueue.ClientStatus;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.junit.client.TimeoutException;
import com.google.gwt.junit.client.impl.JUnitHost.TestInfo;
import com.google.gwt.junit.client.impl.JUnitResult;
import com.google.gwt.thirdparty.guava.common.base.Splitter;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableSet;
import com.google.gwt.util.tools.ArgHandlerFlag;
import com.google.gwt.util.tools.ArgHandlerInt;
import com.google.gwt.util.tools.ArgHandlerString;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import junit.framework.TestResult;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Servlet;

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
public class JUnitShell extends DevMode {

  /**
   * A strategy for running the test.
   */
  public interface Strategy {
    String getSyntheticModuleExtension();

    void processModule(ModuleDef module);
  }

  private static class ArgHandlerRunCompiledJavascript extends ArgHandlerFlag {

    private JUnitShell shell;

    public ArgHandlerRunCompiledJavascript(JUnitShell shell) {
      this.shell = shell;

      addTagValue("-web", false);
      addTagValue("-prod", false);
    }

    @Override
    public String getPurposeSnippet() {
      return "Runs tests in Development Mode, using the Java virtual machine.";
    }

    @Override
    public String getLabel() {
      return "devMode";
    }

    @Override
    public boolean setFlag(boolean enabled) {
      shell.developmentMode = enabled;
      return true;
    }

    @Override
    public boolean getDefaultValue() {
      return shell.developmentMode;
    }
  }

  private static class ArgHandlerShowWindows extends ArgHandlerFlag {

    private JUnitShell shell;

    public ArgHandlerShowWindows(JUnitShell shell) {
      this.shell = shell;

      addTagValue("-notHeadless", true);
    }

    @Override
    public String getPurposeSnippet() {
      return "Causes the log window and browser windows to be displayed; useful for debugging.";
    }

    @Override
    public String getLabel() {
      return "showUi";
    }

    @Override
    public boolean setFlag(boolean enabled) {
      shell.setHeadless(!enabled);
      return true;
    }

    @Override
    public boolean getDefaultValue() {
      return !shell.isHeadless();
    }
  }

  private static class ArgHandlerRunInStandardsMode extends ArgHandlerFlag {

    private JUnitShell shell;

    public ArgHandlerRunInStandardsMode(JUnitShell shell) {
      this.shell = shell;

      addTagValue("-standardsMode", true);
      addTagValue("-quirksMode", false);
    }

    @Override
    public String getPurposeSnippet() {
      return "Run each test using an HTML document in standards mode (rather than quirks mode).";
    }

    @Override
    public String getLabel() {
      return "runStandardsMode";
    }

    @Override
    public boolean setFlag(boolean enabled) {
      shell.setStandardsMode(enabled);
      return true;
    }

    @Override
    public boolean getDefaultValue() {
      return shell.standardsMode;
    }
  }

  static class ArgProcessor extends ArgProcessorBase {

    @SuppressWarnings("deprecation")
    public ArgProcessor(final JUnitShell shell) {
      final HostedModeOptionsImpl options = shell.options;
      /*
       * ----- Options from DevModeBase -------
       */
      // DISABLE: ArgHandlerNoServerFlag.
      registerHandler(new ArgHandlerPort(options) {
        @Override
        public String[] getDefaultArgs() {
          // Override port to auto by default.
          return new String[]{"-port", "auto"};
        }
      });
      registerHandler(new ArgHandlerWhitelist());
      registerHandler(new ArgHandlerBlacklist());
      registerHandler(new ArgHandlerLogDir(options));
      registerHandler(new ArgHandlerLogLevel(options));
      registerHandler(new ArgHandlerGenDir(options));
      // DISABLE: ArgHandlerBindAddress.
      registerHandler(new ArgHandlerCodeServerPort(options) {
        @Override
        public String[] getDefaultArgs() {
          // Override code server port to auto by default.
          return new String[]{this.getTag(), "auto"};
        }
      });
      // DISABLE: ArgHandlerRemoteUI.

      /*
       * ----- Options from DevMode -------
       */
      // Hard code the server.
      options.setServletContainerLauncher(shell.new MyJettyLauncher());
      // DISABLE: ArgHandlerStartupURLs
      registerHandler(new ArgHandlerWarDir(options) {
        private static final String OUT_TAG = "-out";

        @Override
        public String[] getTags() {
          return new String[] {getTag(), OUT_TAG};
        }

        @Override
        public int handle(String[] args, int tagIndex) {
          if (OUT_TAG.equals(args[tagIndex])) {
            // -out is deprecated. Print a warning message
            System.err.println("The -out option is deprecated. This option will be removed in " +
                "future GWT release and will throw an error if it is still used. Please use -war " +
                "option instead.");
          }
          return super.handle(args, tagIndex);
        }
      });

      registerHandler(new ArgHandlerDeployDir(options));
      registerHandler(new ArgHandlerExtraDir(options));
      registerHandler(new ArgHandlerWorkDirOptional(options));
      registerHandler(new ArgHandlerSourceLevel(options));

      // DISABLE: ArgHandlerModuleName

      /*
       * ----- Additional options from Compiler not already included -------
       */
      registerHandler(new ArgHandlerScriptStyle(options));
      registerHandler(new ArgHandlerEnableAssertions(options));
      registerHandler(new ArgHandlerDisableAggressiveOptimization(options));
      registerHandler(new ArgHandlerDisableCastChecking(options));
      registerHandler(new ArgHandlerDisableClassMetadata(options));
      registerHandler(new ArgHandlerDisableClusterSimilarFunctions(options));
      registerHandler(new ArgHandlerDisableInlineLiteralParameters(options));
      registerHandler(new ArgHandlerDisableOptimizeDataflow(options));
      registerHandler(new ArgHandlerDisableOrdinalizeEnums(options));
      registerHandler(new ArgHandlerDisableRemoveDuplicateFunctions(options));
      registerHandler(new ArgHandlerDisableRunAsync(options));
      registerHandler(new ArgHandlerDisableUpdateCheck(options));
      registerHandler(new ArgHandlerDraftCompile(options));
      registerHandler(new ArgHandlerMaxPermsPerPrecompile(options));
      registerHandler(new ArgHandlerLocalWorkers(options));
      registerHandler(new ArgHandlerNamespace(options));
      registerHandler(new ArgHandlerOptimize(options));

      /*
       * ----- Options specific to JUnitShell -----
       */

      // Override log level to set WARN by default..
      registerHandler(new ArgHandlerLogLevel(options) {
        @Override
        protected Type getDefaultLogLevel() {
          return TreeLogger.WARN;
        }
      });

      registerHandler(new ArgHandlerRunCompiledJavascript(shell));

      registerHandler(new ArgHandlerInt() {

        @Override
        public String[] getDefaultArgs() {
          return new String[]{getTag(), "5"};
        }

        @Override
        public String getPurpose() {
          return "Set the test method timeout, in minutes";
        }

        @Override
        public String getTag() {
          return "-testMethodTimeout";
        }

        @Override
        public String[] getTagArgs() {
          return new String[]{"minutes"};
        }

        @Override
        public boolean isUndocumented() {
          return false;
        }

        @Override
        public void setInt(int minutes) {
          shell.baseTestMethodTimeoutMillis = minutes * 60 * 1000;
        }
      });

      registerHandler(new ArgHandlerInt() {
        @Override
        public String[] getDefaultArgs() {
          return new String[]{getTag(), String.valueOf(DEFAULT_BEGIN_TIMEOUT_MINUTES)};
        }

        @Override
        public String getPurpose() {
          return "Set the test begin timeout (time for clients to contact "
              + "server), in minutes";
        }

        @Override
        public String getTag() {
          return "-testBeginTimeout";
        }

        @Override
        public String[] getTagArgs() {
          return new String[]{"minutes"};
        }

        @Override
        public boolean isUndocumented() {
          return false;
        }

        @Override
        public void setInt(int minutes) {
          shell.baseTestBeginTimeoutMillis = minutes * 60 * 1000;
        }
      });

      registerHandler(new ArgHandlerString() {
        @Override
        public String getPurpose() {
          return "Selects the runstyle to use for this test.  The name is "
              + "a suffix of com.google.gwt.junit.RunStyle or is a fully "
              + "qualified class name, and may be followed with a colon and "
              + "an argument for this runstyle.  The specified class must"
              + "extend RunStyle.";
        }

        @Override
        public String getTag() {
          return "-runStyle";
        }

        @Override
        public String[] getTagArgs() {
          return new String[]{"runstyle[:args]"};
        }

        @Override
        public boolean isUndocumented() {
          return false;
        }

        @Override
        public boolean setString(String runStyleArg) {
          shell.runStyleName = runStyleArg;
          return true;
        }
      });

      registerHandler(new ArgHandlerString() {
        @Override
        public String getPurpose() {
          return "Configure batch execution of tests";
        }

        @Override
        public String getTag() {
          return "-batch";
        }

        @Override
        public String[] getTagArgs() {
          return new String[]{"none|class|module"};
        }

        @Override
        public boolean isUndocumented() {
          return true;
        }

        @Override
        public boolean setString(String str) {
          if (str.equals("none")) {
            shell.batchingStrategy = new NoBatchingStrategy();
          } else if (str.equals("class")) {
            shell.batchingStrategy = new ClassBatchingStrategy();
          } else if (str.equals("module")) {
            shell.batchingStrategy = new ModuleBatchingStrategy();
          } else {
            return false;
          }
          return true;
        }
      });

      registerHandler(new ArgHandlerShowWindows(shell));

      registerHandler(new ArgHandlerString() {

        @Override
        public String getPurpose() {
          return "Precompile modules as tests are running (speeds up remote tests but requires more memory)";
        }

        @Override
        public String getTag() {
          return "-precompile";
        }

        @Override
        public String[] getTagArgs() {
          return new String[]{"simple|all|parallel"};
        }

        @Override
        public boolean isUndocumented() {
          return true;
        }

        @Override
        public boolean setString(String str) {
          if (str.equals("simple")) {
            shell.compileStrategy = new SimpleCompileStrategy(shell);
          } else if (str.equals("all")) {
            shell.compileStrategy = new PreCompileStrategy(shell);
          } else if (str.equals("parallel")) {
            shell.compileStrategy = new ParallelCompileStrategy(shell);
          } else {
            return false;
          }
          return true;
        }
      });

      registerHandler(new ArgHandlerRunInStandardsMode(shell));

      registerHandler(new ArgHandlerInt() {

        @Override
        public String getPurpose() {
          return "EXPERIMENTAL: Sets the maximum number of attempts for running each test method";
        }

        @Override
        public String getTag() {
          return "-Xtries";
        }

        @Override
        public String[] getTagArgs() {
          return new String[]{"1"};
        }

        @Override
        public boolean isRequired() {
          return false;
        }

        @Override
        public boolean isUndocumented() {
          return false;
        }

        @Override
        public void setInt(int value) {
          shell.tries = value;
        }

        @Override
        public boolean isExperimental() {
          return true;
        }
      });

      registerHandler(new ArgHandlerString() {

        @Override
        public String getPurpose() {
          return "Specify the user agents to reduce the number of permutations for remote browser tests;"
              + " e.g. ie8,safari,gecko1_8";
        }

        @Override
        public String getTag() {
          return "-userAgents";
        }

        @Override
        public String[] getTagArgs() {
          return new String[]{"userAgents"};
        }

        @Override
        public boolean setString(String str) {
          Splitter splitter = Splitter.on(",").omitEmptyStrings().trimResults();
          shell.userAgentsOpt = ImmutableSet.copyOf(splitter.split(str));
          return true;
        }
      });
    }

    @Override
    protected String getName() {
      return JUnitShell.class.getName();
    }
  }

  private final class MyJettyLauncher extends JettyLauncher {

    /**
     * Adds in special JUnit stuff.
     */
    @Override
    protected JettyServletContainer createServletContainer(TreeLogger logger,
        File appRootDir, Server server, WebAppContext wac, int localPort) {
      // Don't bother shutting down cleanly.
      server.setStopAtShutdown(false);
      // Save off the Context so we can add our own servlets later.
      JUnitShell.this.wac = wac;
      return super.createServletContainer(logger, appRootDir, server, wac,
          localPort);
    }

    /**
     * Ignore DevMode's normal WEB-INF classloader rules and just allow the
     * system classloader to dominate. This makes JUnitHostImpl live in the
     * right classloader (mine).
     */
    @SuppressWarnings("unchecked")
    @Override
    protected WebAppContext createWebAppContext(TreeLogger logger,
        File appRootDir) {
      return new WebAppContext(appRootDir.getAbsolutePath(), "/") {
        {
          // Prevent file locking on Windows; pick up file changes.
          getInitParams().put(
              "org.eclipse.jetty.servlet.Default.useFileMappedBuffer", "false");

          // Prefer the parent class loader so that JUnit works.
          setParentLoaderPriority(true);
        }
      };
    }
  }

  /**
   * How many minutes to wait for the browser to contact the test system.
   */
  private static final int DEFAULT_BEGIN_TIMEOUT_MINUTES = 1;

  /**
   * This is a system property that, when set, emulates command line arguments.
   */
  private static final String PROP_GWT_ARGS = "gwt.args";

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
   * Get the set of remote user agents to compile.
   *
   * @return the set of remote user agents
   */
  public static Set<String> getRemoteUserAgents() {
    if (unitTestShell == null) {
      return null;
    }
    return unitTestShell.runStyle.getUserAgents();
  }

  /**
   * Get the compiler options
   *
   * @return the compiler options that have been set.
   */
  public static CompilerOptions getCompilerOptions() {
    if (unitTestShell == null) {
      return null;
    }
    return unitTestShell.options;
  }

  /**
   * Checks if a testCase should not be executed. Currently, a test is either
   * executed on all clients (mentioned in this test) or on no clients.
   *
   * @param testInfo the test info to check
   * @return true iff the test should not be executed on any of the specified
   *         clients.
   */
  public static boolean mustNotExecuteTest(TestInfo testInfo) {
    if (unitTestShell == null) {
      throw new IllegalStateException(
          "mustNotExecuteTest cannot be called before runTest()");
    }
    try {
      Class<?> testClass = TestCase.class.getClassLoader().loadClass(
          testInfo.getTestClass());
      return unitTestShell.mustNotExecuteTest(getBannedPlatforms(testClass,
          testInfo.getTestMethod()));
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException("Could not load test class: "
          + testInfo.getTestClass());
    }
  }

  /**
   * Entry point for {@link com.google.gwt.junit.client.GWTTestCase}. Gets or
   * creates the singleton {@link JUnitShell} and invokes its
   * {@link #runTestImpl(GWTTestCase, TestResult)}.
   */
  public static void runTest(GWTTestCase testCase, TestResult testResult)
      throws UnableToCompleteException {
    getUnitTestShell().runTestImpl(testCase, testResult);
  }

  /**
   * Retrieves the JUnitShell. This should only be invoked during TestRunner
   * execution of JUnit tests.
   */
  static JUnitShell getUnitTestShell() {
    if (unitTestShell == null) {
      unitTestShell = new JUnitShell();
      unitTestShell.lastLaunchFailed = true;
      String[] args = unitTestShell.synthesizeArgs();
      ArgProcessor argProcessor = new ArgProcessor(unitTestShell);
      if (!argProcessor.processArgs(args)) {
        throw new JUnitFatalLaunchException("Error processing shell arguments");
      }
      // Always bind to the wildcard address and substitute the host address in
      // URLs. Note that connectAddress isn't actually used here, as we
      // override it from the runsStyle in getModuleUrl, but we set it to match
      // what will actually be used anyway to avoid confusion.
      unitTestShell.options.setBindAddress("0.0.0.0");
      try {
        unitTestShell.options.setConnectAddress(InetAddress.getLocalHost().getHostAddress());
      } catch (UnknownHostException e) {
        throw new JUnitFatalLaunchException("Unable to resolve my address", e);
      }
      if (!unitTestShell.startUp()) {
        throw new JUnitFatalLaunchException("Shell failed to start");
      }
      // TODO: install a shutdown hook? Not necessary with GWTShell.
      unitTestShell.lastLaunchFailed = false;
    }
    unitTestShell.checkArgs();
    return unitTestShell;
  }

  /**
   * Sanity check; if the type we're trying to run did not actually wind up in
   * the type oracle, there's no way this test can possibly run. Bail early
   * instead of failing on the client.
   */
  private static JUnitFatalLaunchException checkTestClassInCurrentModule(TreeLogger logger,
      CompilationState compilationState, String moduleName, TestCase testCase) {
    TypeOracle typeOracle = compilationState.getTypeOracle();
    String typeName = testCase.getClass().getName();
    typeName = typeName.replace('$', '.');
    JClassType foundType = typeOracle.findType(typeName);
    if (foundType != null) {
      return null;
    }
    Map<String, CompilationUnit> unitMap = compilationState.getCompilationUnitMap();
    CompilationUnit unit = unitMap.get(typeName);
    String errMsg;
    if (unit == null) {
      errMsg = "The test class '" + typeName + "' was not found in module '"
          + moduleName + "'; no compilation unit for that type was seen";
    } else {
      CompilationProblemReporter.logMissingTypeErrorWithHints(logger, typeName, compilationState);
      errMsg = "The test class '" + typeName
          + "' had compile errors; check log for details";
    }
    return new JUnitFatalLaunchException(errMsg);
  }

  /**
   * Returns the set of banned {@code Platform} for a test method.
   *
   * @param testClass the testClass
   * @param methodName the name of the test method
   */
  private static Set<Platform> getBannedPlatforms(Class<?> testClass,
      String methodName) {
    Set<Platform> bannedSet = EnumSet.noneOf(Platform.class);
    if (testClass.isAnnotationPresent(DoNotRunWith.class)) {
      bannedSet.addAll(Arrays.asList(testClass.getAnnotation(DoNotRunWith.class).value()));
    }
    try {
      Method testMethod = testClass.getMethod(methodName);
      if (testMethod.isAnnotationPresent(DoNotRunWith.class)) {
        bannedSet.addAll(Arrays.asList(testMethod.getAnnotation(
            DoNotRunWith.class).value()));
      }
    } catch (SecurityException e) {
      // should not happen
      e.printStackTrace();
    } catch (NoSuchMethodException e) {
      // should not happen
      e.printStackTrace();
    }
    return bannedSet;
  }

  /**
   * Our server's web app context; used to dynamically add servlets.
   */
  WebAppContext wac;

  /**
   * The amount of time to wait for all clients to have contacted the server and
   * begin running the test. "Contacted" does not necessarily mean "the test has
   * begun," e.g. for linker errors stopping the test initialization.
   */
  private long baseTestBeginTimeoutMillis;

  /**
   * The amount of time to wait for all clients to complete a single test
   * method, in milliseconds, measured from when the <i>last</i> client connects
   * (and thus starts the test). Set by the -testMethodTimeout argument.
   */
  private long baseTestMethodTimeoutMillis;

  /**
   * Determines how to batch up tests for execution.
   */
  private BatchingStrategy batchingStrategy = new NoBatchingStrategy();

  /**
   * Determines how modules are compiled.
   */
  private CompileStrategy compileStrategy = new SimpleCompileStrategy(
      JUnitShell.this);

  /**
   * A type oracle for the current module, used to validate class existence.
   */
  private CompilationState currentCompilationState;

  /**
   * Name of the module containing the current/last module to run.
   */
  private ModuleDef currentModule;

  /**
   * The name of the current test case being run.
   */
  private TestInfo currentTestInfo;

  /**
   * True if we are running the test in development mode.
   */
  private boolean developmentMode = true;

  /**
   * Used to make sure we don't start the runStyle more than once.
   */
  private boolean runStyleStarted;

  /**
   * If true, we haven't started all the clients yet. (Used for manual mode.)
   */
  private boolean waitingForClients = true;

  /**
   * If true, the last attempt to launch failed.
   */
  private boolean lastLaunchFailed;

  /**
   * We need to keep a hard reference to the last module that was launched until
   * all client browsers have successfully transitioned to the current module.
   * Failure to do so allows the last module to be GC'd, which transitively
   * kills the {@link com.google.gwt.junit.server.JUnitHostImpl} servlet. If the
   * servlet dies, the client browsers will be unable to transition.
   */
  @SuppressWarnings("unused")
  private ModuleDef lastModule;

  /**
   * Records what servlets have been loaded at which paths.
   */
  private final Map<String, String> loadedServletsByPath = new HashMap<String, String>();

  /**
   * Portal to interact with the servlet.
   */
  private JUnitMessageQueue messageQueue;

  /**
   * An exception that should by fired the next time runTestImpl runs.
   */
  private UnableToCompleteException pendingException;

  /**
   * The remote user agents so we can limit permutations for remote tests.
   */
  Set<String> userAgentsOpt; // Visible for testing

  /**
   * What type of test we're running; Local development, local production, or
   * remote production.
   */
  private RunStyle runStyle = null;

  /**
   * The argument passed to -runStyle. This is parsed later so we can pass in a
   * logger.
   */
  private String runStyleName = "HtmlUnit";

  private boolean standardsMode = true;

  /**
   * Test method timeout as modified by the batching strategy.
   */
  private long testBatchingMethodTimeoutMillis;

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
   * Timeout for individual test method. If System.currentTimeMillis() is later
   * than this timestamp, then we need to pack up and go home. Zero for "not yet
   * set" (at the start of a test). This interval begins when the
   * testBeginTimeout interval is done.
   */
  private long testMethodTimeout;

  /**
   * Max number of times a test method must be tried.
   */
  private int tries;

  /**
   * Visible for testing only. (See {@link #getUnitTestShell}.)
   */
  JUnitShell() {
    setRunTomcat(true);
    setHeadless(true);
  }

  public String getModuleUrl(String moduleName) {
    // TODO(jat): consider using DevModeBase.processUrl instead
    String localhost = runStyle.getLocalHostName();
    return getModuleUrl(localhost, getPort(), moduleName, codeServerPort);
  }

  public CompilerContext getCompilerContext() {
    return compilerContext;
  }

  /**
   * Check for updates once a minute.
   */
  @Override
  protected long checkForUpdatesInterval() {
    return CheckForUpdates.ONE_MINUTE;
  }

  @Override
  protected boolean doStartup() {
    if (!super.doStartup()) {
      return false;
    }
    int numClients = createRunStyle(runStyleName);
    if (numClients < 0) {
      // RunStyle already logged reasons for its failure
      return false;
    }
    messageQueue = new JUnitMessageQueue(numClients);

    if (tries >= 1) {
      runStyle.setTries(tries);
    }

    if (userAgentsOpt != null) {
      runStyle.setUserAgents(userAgentsOpt);
    }

    if (!runStyle.setupMode(getTopLogger(), developmentMode)) {
      getTopLogger().log(
          TreeLogger.ERROR,
          "Run style does not support "
              + (developmentMode ? "development" : "production") + " mode");
      return false;
    }
    return true;
  }

  @Override
  protected void ensureCodeServerListener() {
    if (developmentMode) {
      super.ensureCodeServerListener();
      listener.setIgnoreRemoteDeath(true);
    }
  }

  @Override
  protected void inferStartupUrls() {
    // do nothing -- JUnitShell isn't expected to have startup URLs
  }

  @Override
  protected ModuleDef loadModule(TreeLogger logger, String moduleName,
      boolean refresh) throws UnableToCompleteException {
    // Never refresh modules in JUnit.
    return super.loadModule(logger, moduleName, false);
  }

  /**
   * Checks to see if this test run is complete.
   */
  protected boolean notDone() {
    int activeClients = messageQueue.getNumClientsRetrievedTest(currentTestInfo);
    int expectedClients = messageQueue.getNumClients();
    if (runStyle instanceof RunStyleManual && waitingForClients) {
      String[] newClients = messageQueue.getNewClients();
      int printIndex = activeClients - newClients.length + 1;
      for (String newClient : newClients) {
        System.out.println(printIndex + " - " + newClient);
        ++printIndex;
      }
      if (activeClients < expectedClients) {
        // Wait forever for first contact; user-driven.
        return true;
      }
      waitingForClients = false;
    }

    long currentTimeMillis = System.currentTimeMillis();
    if (activeClients >= expectedClients) {
      if (activeClients > expectedClients) {
        getTopLogger().log(
            TreeLogger.WARN,
            "Too many clients: expected " + expectedClients + ", found "
                + activeClients);
      }

      /*
       * It's now safe to release any reference to the last module since all
       * clients have transitioned to the current module.
       */
      lastModule = currentModule;
      if (testMethodTimeout == 0) {
        testMethodTimeout = currentTimeMillis + testBatchingMethodTimeoutMillis;
      } else if (testMethodTimeout < currentTimeMillis) {
        double elapsed = (currentTimeMillis - testBeginTime) / 1000.0;
        throw new TimeoutException(
            "The browser did not complete the test method "
                + currentTestInfo.toString() + " in "
                + testBatchingMethodTimeoutMillis
                + "ms.\n  We have no results from:\n"
                + messageQueue.getWorkingClients(currentTestInfo)
                + "Actual time elapsed: " + elapsed + " seconds.\n"
                + "Try increasing this timeout using the '-testMethodTimeout minutes' option\n");
      }
    } else if (testBeginTimeout < currentTimeMillis) {
      double elapsed = (currentTimeMillis - testBeginTime) / 1000.0;
      throw new TimeoutException(
          "The browser did not contact the server within "
              + baseTestBeginTimeoutMillis + "ms.\n"
              + messageQueue.getUnretrievedClients(currentTestInfo)
              + "\n Actual time elapsed: " + elapsed + " seconds.\n"
              + "Try increasing this timeout using the '-testBeginTimeout minutes' option\n"
              + "The default value of minutes is 1, i.e., the server waits 1 minute or 60 seconds.\n");
    }

    // Check that we haven't lost communication with a remote host.
    String[] interruptedHosts = runStyle.getInterruptedHosts();
    if (interruptedHosts != null) {
      StringBuilder msg = new StringBuilder();
      msg.append("A remote browser died a mysterious death.\n");
      msg.append("  We lost communication with:");
      for (String host : interruptedHosts) {
        msg.append("\n  ").append(host);
      }
      throw new TimeoutException(msg.toString());
    }

    if (messageQueue.hasResults(currentTestInfo)) {
      return false;
    } else if (pendingException == null) {
      // Instead of waiting around for results, try to compile the next module.
      try {
        compileStrategy.maybeCompileAhead();
      } catch (UnableToCompleteException e) {
        pendingException = e;
      }
    }
    return true;
  }

  @Override
  protected void warnAboutNoStartupUrls() {
    // do nothing -- JUnitShell isn't expected to have startup URLs
  }

  void compileForWebMode(ModuleDef module, Set<String> userAgents)
      throws UnableToCompleteException {
    if (userAgents != null && !userAgents.isEmpty()) {
      Properties props = module.getProperties();
      Property userAgent = props.find("user.agent");
      if (userAgent instanceof BindingProperty) {
        BindingProperty bindingProperty = (BindingProperty) userAgent;
        bindingProperty.setAllowedValues(bindingProperty.getRootCondition(),
            userAgents.toArray(new String[0]));
      }
    }
    if (!new Compiler(options).run(getTopLogger(), module)) {
      throw new UnableToCompleteException();
    }
    // TODO(scottb): prepopulate currentCompilationState somehow?
  }

  String getModuleUrl(String hostName, int port, String moduleName, int codeServerPort) {
    String url = "http://" + hostName + ":" + port + "/" + moduleName
        + (standardsMode ? "/junit-standards.html" : "/junit.html");
    if (developmentMode) {
      url += "?gwt.codesvr=" + hostName + ":" + codeServerPort;
    }
    return url;
  }

  void maybeCompileForWebMode(ModuleDef module, Set<String> userAgents)
      throws UnableToCompleteException {
    compilerContext = compilerContextBuilder.module(module).build();
    // Load any declared servlets.
    for (String path : module.getServletPaths()) {
      String servletClass = module.findServletForPath(path);
      path = '/' + module.getName() + path;
      if (!servletClass.equals(loadedServletsByPath.get(path))) {
        try {
          // We should load the class ourselves because otherwise if Jetty tries and fails to load
          // by itself, it will be left in a broken state (looks like this is fixed in Jetty 9).
          Class<? extends Servlet> clazz = wac.loadClass(servletClass).asSubclass(Servlet.class);
          wac.addServlet(clazz, path);
          loadedServletsByPath.put(path, servletClass);
        } catch (ClassNotFoundException e) {
          getTopLogger().log(
              TreeLogger.WARN,
              "Failed to load servlet class '" + servletClass
                  + "' declared in '" + module.getName() + "'", e);
        }
      }
    }
    if (developmentMode) {
      // BACKWARDS COMPATIBILITY: many linkers currently fail in dev mode.
      try {
        Linker l = module.getActivePrimaryLinker().newInstance();
        StandardLinkerContext context = new StandardLinkerContext(
            getTopLogger(), module, compilerContext.getPublicResourceOracle(), null);
        if (!l.supportsDevModeInJunit(context)) {
          if (module.getLinker("std") != null) {
            // TODO: unfortunately, this could be race condition between dev/prod
            module.addLinker("std");
          }
        }
      } catch (Exception e) {
        getTopLogger().log(TreeLogger.WARN, "Failed to instantiate linker: " + e);
      }
      super.link(getTopLogger(), module);
    } else {
      compileForWebMode(module, userAgents);
    }
  }

  void setStandardsMode(boolean standardsMode) {
    this.standardsMode = standardsMode;
  }

  private void checkArgs() {
    if (runStyle.getTries() > 1
        && !(batchingStrategy instanceof NoBatchingStrategy)) {
      throw new JUnitFatalLaunchException(
          "Batching does not work with tries > 1");
    }
  }

  /**
   * Create the specified (or default) runStyle.
   *
   * @param runStyleName the argument passed to -runStyle
   * @return the number of clients, or -1 if initialization was unsuccessful
   */
  private int createRunStyle(String runStyleName) {
    String args = null;
    String name = runStyleName;
    int colon = name.indexOf(':');
    if (colon >= 0) {
      args = name.substring(colon + 1);
      name = name.substring(0, colon);
    }
    if (name.indexOf('.') < 0) {
      name = RunStyle.class.getName() + name;
    }
    Throwable caught = null;
    try {
      Class<?> clazz = Class.forName(name);
      Class<? extends RunStyle> runStyleClass = clazz.asSubclass(RunStyle.class);
      Constructor<? extends RunStyle> ctor = runStyleClass.getConstructor(JUnitShell.class);
      runStyle = ctor.newInstance(JUnitShell.this);
      return runStyle.initialize(args);
    } catch (ClassNotFoundException e) {
      // special error message for CNFE since it is likely a typo
      String msg = "Unable to create runStyle \"" + runStyleName + "\"";
      if (runStyleName.indexOf('.') < 0 && runStyleName.length() > 0
          && Character.isLowerCase(runStyleName.charAt(0))) {
        // apparently using a built-in runstyle with an initial lowercase letter
        msg += " - did you mean \""
            + Character.toUpperCase(runStyleName.charAt(0))
            + runStyleName.substring(1) + "\"?";
      } else {
        msg += " -- is it spelled correctly?";
      }
      getTopLogger().log(TreeLogger.ERROR, msg);
      return -1;
    } catch (SecurityException e) {
      caught = e;
    } catch (NoSuchMethodException e) {
      caught = e;
    } catch (IllegalArgumentException e) {
      caught = e;
    } catch (InstantiationException e) {
      caught = e;
    } catch (IllegalAccessException e) {
      caught = e;
    } catch (InvocationTargetException e) {
      caught = e;
    }
    getTopLogger().log(TreeLogger.ERROR,
        "Unable to create runStyle \"" + runStyleName + "\"", caught);
    return -1;
  }

  private boolean mustNotExecuteTest(Set<Platform> bannedPlatforms) {
    if (!Collections.disjoint(bannedPlatforms, runStyle.getPlatforms())) {
      return true;
    }

    if (developmentMode) {
      if (bannedPlatforms.contains(Platform.Devel)) {
        return true;
      }
    } else {
      // Prod mode
      if (bannedPlatforms.contains(Platform.Prod)) {
        return true;
      }
    }

    return false;
  }

  private boolean mustRetry(int numTries) {
    if (numTries >= runStyle.getTries()) {
      return false;
    }
    assert (batchingStrategy instanceof NoBatchingStrategy);
    // checked in {@code checkArgs()}
    /*
     * If a batching strategy is being used, the client will already have moved
     * passed the failed test case. The whole block could be re-executed, but it
     * would be more complicated.
     */
    return true;
  }

  private void processTestResult(TestCase testCase, TestResult testResult) {

    Map<ClientStatus, JUnitResult> results = messageQueue.getResults(currentTestInfo);
    assert results != null;
    assert results.size() == messageQueue.getNumClients() : results.size()
        + " != " + messageQueue.getNumClients();

    for (Entry<ClientStatus, JUnitResult> entry : results.entrySet()) {
      JUnitResult result = entry.getValue();
      assert (result != null);

      if (result.isAnyException()) {
        if (result.isExceptionOf(AssertionFailedError.class)) {
          testResult.addFailure(testCase, toAssertionFailedError(result.getException()));
        } else {
          testResult.addError(testCase, result.getException());
        }
      }
    }
  }

  private AssertionFailedError toAssertionFailedError(SerializableThrowable thrown) {
    AssertionFailedError error = new AssertionFailedError(thrown.getMessage());
    error.setStackTrace(thrown.getStackTrace());
    if (thrown.getCause() != null) {
      error.initCause(thrown.getCause());
    }
    return error;
  }

  private void runTestImpl(GWTTestCase testCase, TestResult testResult)
      throws UnableToCompleteException {
    runTestImpl(testCase, testResult, 0);
  }

  /**
   * Runs a particular test case.
   */
  private void runTestImpl(GWTTestCase testCase, TestResult testResult,
      int numTries) throws UnableToCompleteException {

    testBatchingMethodTimeoutMillis = batchingStrategy.getTimeoutMultiplier()
        * baseTestMethodTimeoutMillis;
    if (mustNotExecuteTest(getBannedPlatforms(testCase.getClass(),
        testCase.getName()))) {
      return;
    }

    if (lastLaunchFailed) {
      throw new UnableToCompleteException();
    }

    String moduleName = testCase.getModuleName();
    String syntheticModuleName = testCase.getSyntheticModuleName();
    Strategy strategy = testCase.getStrategy();
    boolean sameTest = (currentModule != null)
        && syntheticModuleName.equals(currentModule.getName());
    if (sameTest && lastLaunchFailed) {
      throw new UnableToCompleteException();
    }

    // Get the module definition for the current test.
    if (!sameTest) {
      try {
        currentModule = compileStrategy.maybeCompileModule(moduleName,
            syntheticModuleName, strategy, batchingStrategy, getTopLogger());
        compilerContext = compilerContextBuilder.module(currentModule).build();
        currentCompilationState = currentModule.getCompilationState(getTopLogger(),
            compilerContext);
      } catch (UnableToCompleteException e) {
        lastLaunchFailed = true;
        throw e;
      }
    }
    assert (currentModule != null);

    JUnitFatalLaunchException launchException = checkTestClassInCurrentModule(getTopLogger(),
        currentCompilationState, moduleName, testCase);
    if (launchException != null) {
      testResult.addError(testCase, launchException);
      return;
    }

    currentTestInfo = new TestInfo(currentModule.getName(),
        testCase.getClass().getName(), testCase.getName());
    numTries++;
    if (messageQueue.hasResults(currentTestInfo)) {
      // Already have a result.
      processTestResult(testCase, testResult);
      return;
    }
    compileStrategy.maybeAddTestBlockForCurrentTest(testCase, batchingStrategy);

    try {
      if (!runStyleStarted) {
        runStyle.launchModule(currentModule.getName());
      }
    } catch (UnableToCompleteException e) {
      lastLaunchFailed = true;
      testResult.addError(testCase, new JUnitFatalLaunchException(e));
      return;
    }
    runStyleStarted = true;

    boolean mustRetry = mustRetry(numTries);
    // Wait for test to complete
    try {
      // Set a timeout period to automatically fail if the servlet hasn't been
      // contacted; something probably went wrong (the module failed to load?)
      testBeginTime = System.currentTimeMillis();
      testBeginTimeout = testBeginTime + baseTestBeginTimeoutMillis;
      testMethodTimeout = 0; // wait until test execution begins
      while (notDone()) {
        messageQueue.waitForResults(1000);
      }
      if (!mustRetry && pendingException != null) {
        UnableToCompleteException e = pendingException;
        pendingException = null;
        throw e;
      }
    } catch (TimeoutException e) {
      if (!mustRetry) {
        lastLaunchFailed = true;
        testResult.addError(testCase, e);
        return;
      }
    }

    if (mustRetry) {
      if (messageQueue.needsRerunning(currentTestInfo)) {
        // remove the result if it is present and rerun
        messageQueue.removeResults(currentTestInfo);
        getTopLogger().log(TreeLogger.WARN,
            currentTestInfo + " is being retried, retry attempt = " + numTries);
        runTestImpl(testCase, testResult, numTries);
        return;
      }
    }
    assert (messageQueue.hasResults(currentTestInfo));
    processTestResult(testCase, testResult);
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
      Pattern quotedArgsPattern = Pattern.compile("^([\"'])(.*)([\"'])$");

      while (matcher.find()) {
        // Strip leading and trailing quotes from the arg
        String arg = matcher.group();
        Matcher qmatcher = quotedArgsPattern.matcher(arg);
        if (qmatcher.matches()) {
          argList.add(qmatcher.group(2));
        } else {
          argList.add(arg);
        }
      }
    }

    return argList.toArray(new String[argList.size()]);
  }
}
