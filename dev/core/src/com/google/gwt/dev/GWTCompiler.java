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
package com.google.gwt.dev;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.SelectionProperty;
import com.google.gwt.core.ext.linker.impl.StandardCompilationResult;
import com.google.gwt.core.ext.linker.impl.StandardLinkerContext;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.ModuleDefLoader;
import com.google.gwt.dev.cfg.Properties;
import com.google.gwt.dev.cfg.Property;
import com.google.gwt.dev.cfg.PropertyPermutations;
import com.google.gwt.dev.cfg.Rules;
import com.google.gwt.dev.cfg.StaticPropertyOracle;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.CompilationUnit;
import com.google.gwt.dev.jdt.RebindPermutationOracle;
import com.google.gwt.dev.jdt.WebModeCompilerFrontEnd;
import com.google.gwt.dev.jjs.JJSOptions;
import com.google.gwt.dev.jjs.JavaToJavaScriptCompiler;
import com.google.gwt.dev.jjs.JsOutputOption;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.shell.StandardRebindOracle;
import com.google.gwt.dev.util.PerfLogger;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.arg.ArgHandlerGenDir;
import com.google.gwt.dev.util.arg.ArgHandlerLogLevel;
import com.google.gwt.dev.util.arg.ArgHandlerScriptStyle;
import com.google.gwt.dev.util.arg.ArgHandlerTreeLoggerFlag;
import com.google.gwt.dev.util.log.AbstractTreeLogger;
import com.google.gwt.dev.util.log.DetachedTreeLoggerWindow;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.util.tools.ArgHandlerDisableAggressiveOptimization;
import com.google.gwt.util.tools.ArgHandlerEnableAssertions;
import com.google.gwt.util.tools.ArgHandlerExtra;
import com.google.gwt.util.tools.ArgHandlerFlag;
import com.google.gwt.util.tools.ArgHandlerOutDir;
import com.google.gwt.util.tools.ToolBase;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The main executable entry point for the GWT Java to JavaScript compiler.
 */
public class GWTCompiler extends ToolBase {

  private class ArgHandlerModuleName extends ArgHandlerExtra {

    @Override
    public boolean addExtraArg(String arg) {
      setModuleName(arg);
      return true;
    }

    @Override
    public String getPurpose() {
      return "Specifies the name of the module to compile";
    }

    @Override
    public String[] getTagArgs() {
      return new String[] {"module"};
    }

    @Override
    public boolean isRequired() {
      return true;
    }
  }

  /**
   * Argument handler for making the compiler run in "validation" mode.
   */
  private class ArgHandlerValidateOnlyFlag extends ArgHandlerFlag {

    public String getPurpose() {
      return "Validate all source code, but do not compile";
    }

    public String getTag() {
      return "-validateOnly";
    }

    public boolean setFlag() {
      jjsOptions.setValidateOnly(true);
      return true;
    }
  }

  /**
   * Used to smartly deal with rebind across the production of an entire
   * permutation, including cache checking and recording the inputs and outputs
   * into a {@link Compilation}.
   */
  private class CompilationRebindOracle extends StandardRebindOracle {

    private final Map<String, String> cache = new HashMap<String, String>();
    private final StaticPropertyOracle propOracle;

    public CompilationRebindOracle(ArtifactSet generatorArtifacts,
        StaticPropertyOracle propOracle) {
      super(compilationState, propOracle, module, rules, genDir,
          generatorResourcesDir, generatorArtifacts);
      this.propOracle = propOracle;
    }

    public StaticPropertyOracle getPropertyOracle() {
      return propOracle;
    }

    /**
     * Overridden so that we can cache answers.
     */
    @Override
    public String rebind(TreeLogger logger, String in)
        throws UnableToCompleteException {
      String out = cache.get(in);
      if (out == null) {
        // Actually do the work, then cache it.
        //
        out = super.rebind(logger, in);
        cache.put(in, out);
      } else {
        // Was cached.
        //
        String msg = "Rebind answer for '" + in + "' found in cache " + out;
        logger.log(TreeLogger.DEBUG, msg, null);
      }
      return out;
    }
  }

  private class DistillerRebindPermutationOracle implements
      RebindPermutationOracle {

    private CompilationRebindOracle[] rebindOracles;

    public DistillerRebindPermutationOracle(ArtifactSet generatorArtifacts,
        PropertyPermutations perms) {
      rebindOracles = new CompilationRebindOracle[perms.size()];
      Property[] orderedProps = perms.getOrderedProperties();
      for (int i = 0; i < rebindOracles.length; ++i) {
        String[] orderedPropValues = perms.getOrderedPropertyValues(i);
        StaticPropertyOracle propOracle = new StaticPropertyOracle(
            orderedProps, orderedPropValues);
        rebindOracles[i] = new CompilationRebindOracle(generatorArtifacts,
            propOracle);
      }
    }

    public String[] getAllPossibleRebindAnswers(TreeLogger logger,
        String requestTypeName) throws UnableToCompleteException {

      String msg = "Computing all possible rebind results for '"
          + requestTypeName + "'";
      logger = logger.branch(TreeLogger.DEBUG, msg, null);

      Set<String> answers = new HashSet<String>();

      for (CompilationRebindOracle rebindOracle : rebindOracles) {
        String resultTypeName = rebindOracle.rebind(logger, requestTypeName);
        answers.add(resultTypeName);
      }
      return Util.toArray(String.class, answers);
    }

    public int getPermuationCount() {
      return rebindOracles.length;
    }

    public CompilationRebindOracle getRebindOracle(int permNumber) {
      return rebindOracles[permNumber];
    }
  }

  /**
   * Represents the state of a single permutation for compile.
   */
  private static final class Permuation {
    final TreeLogger logger;
    final int number;
    final CompilationRebindOracle rebindOracle;

    public Permuation(TreeLogger logger, int number,
        CompilationRebindOracle rebindOracle) {
      this.logger = logger;
      this.number = number;
      this.rebindOracle = rebindOracle;
    }
  }

  /**
   * Contains all shared state and synchronizes among permutation workers and
   * the main thread.
   * 
   * TODO: clean this up using java.util.concurrent
   */
  private static final class PermutationManager {

    private boolean failed;
    private final StandardLinkerContext linkerContext;
    private final TreeLogger logger;
    private int nextPerm = 0;
    private int numThreads;
    private final CompilationRebindOracle[] rebindOracles;

    public PermutationManager(TreeLogger logger,
        StandardLinkerContext linkerContext,
        DistillerRebindPermutationOracle rpo, int numThreads) {
      this.logger = logger;
      this.linkerContext = linkerContext;
      this.numThreads = numThreads;
      rebindOracles = new CompilationRebindOracle[rpo.getPermuationCount()];
      for (int i = 0; i < rebindOracles.length; ++i) {
        rebindOracles[i] = rpo.getRebindOracle(i);
      }
    }

    public synchronized void finishPermuation(Permuation perm, String js) {
      StandardCompilationResult compilation;
      try {
        compilation = linkerContext.getCompilation(perm.logger, js);
      } catch (UnableToCompleteException e) {
        recordFailure();
        return;
      }
      StaticPropertyOracle propOracle = perm.rebindOracle.getPropertyOracle();
      Property[] orderedProps = propOracle.getOrderedProps();
      String[] orderedPropValues = propOracle.getOrderedPropValues();
      Map<SelectionProperty, String> unboundProperties = new HashMap<SelectionProperty, String>();
      for (int i = 0; i < orderedProps.length; i++) {
        SelectionProperty key = linkerContext.getProperty(orderedProps[i].getName());
        if (key.tryGetValue() != null) {
          /*
           * The view of the Permutation doesn't include properties with defined
           * values.
           */
          continue;
        }
        unboundProperties.put(key, orderedPropValues[i]);
      }
      compilation.addSelectionPermutation(unboundProperties);
    }

    public synchronized int getActiveThreadCount() {
      return numThreads;
    }

    public synchronized Permuation getNextPermuation() {
      if (hasNextPermutation()) {
        // Make sure we have enough memory.
        int permNumber = nextPerm++;
        while (nextPerm < rebindOracles.length
            && rebindOracles[nextPerm] == null) {
          ++nextPerm;
        }
        TreeLogger branch = logger.branch(TreeLogger.TRACE,
            "Analyzing permutation #" + (permNumber + 1), null);
        return new Permuation(branch, permNumber, rebindOracles[permNumber]);
      }
      return null;
    }

    public synchronized boolean isFailed() {
      return failed;
    }

    public synchronized void outOfMemory(Permuation perm, OutOfMemoryError e) {
      if (getActiveThreadCount() > 1) {
        // Recycle and try on a different thread.
        perm.logger.log(TreeLogger.WARN,
            "Out of memory; will retry permutation using fewer concurrent threads");
        rebindOracles[perm.number] = perm.rebindOracle;
        if (nextPerm > perm.number) {
          nextPerm = perm.number;
        }
      } else {
        // Only one thread, we're truly out of memory!
        perm.logger.log(TreeLogger.ERROR, null, e);
      }
    }

    @SuppressWarnings("unused")
    public synchronized void recordFailure() {
      failed = true;
    }

    public synchronized void threadDied() {
      --numThreads;
    }

    private boolean hasNextPermutation() {
      return !isFailed() && nextPerm < rebindOracles.length;
    }
  }

  /**
   * A worker thread for compiling individual permutations.
   */
  private static final class PermutationWorker implements Runnable {
    private static void logProperties(TreeLogger logger,
        StaticPropertyOracle propOracle) {
      Property[] props = propOracle.getOrderedProps();
      String[] values = propOracle.getOrderedPropValues();
      if (logger.isLoggable(TreeLogger.DEBUG)) {
        logger = logger.branch(TreeLogger.DEBUG, "Setting properties", null);
        for (int i = 0; i < props.length; i++) {
          String name = props[i].getName();
          String value = values[i];
          logger.log(TreeLogger.TRACE, name + " = " + value, null);
        }
      }
    }

    private final JavaToJavaScriptCompiler jjs;
    private final PermutationManager manager;

    public PermutationWorker(PermutationManager localState,
        JavaToJavaScriptCompiler jjs) {
      this.manager = localState;
      this.jjs = jjs;
    }

    public void run() {
      try {
        while (hasEnoughMemory()) {
          Permuation perm = manager.getNextPermuation();
          if (perm == null) {
            return;
          }
          PerfLogger.start("Permutation #" + (perm.number + 1));
          try {
            logProperties(perm.logger, perm.rebindOracle.getPropertyOracle());
            String js = jjs.compile(perm.logger, perm.rebindOracle);
            manager.finishPermuation(perm, js);
            // Allow GC.
            js = null;
          } catch (OutOfMemoryError e) {
            manager.outOfMemory(perm, e);
            return;
          } catch (Throwable e) {
            perm.logger.log(TreeLogger.ERROR, "Permutation failed", e);
            manager.recordFailure();
            return;
          } finally {
            PerfLogger.end();
          }
        }
      } finally {
        manager.threadDied();
      }
    }

    private boolean hasEnoughMemory() {
      if (manager.getActiveThreadCount() == 1) {
        // I'm the last thread, so it doesn't matter, we have to try.
        return true;
      }
      if (jjs.getAstMemoryUsage() < getPotentialFreeMemory()) {
        // Best effort memory reclaim.
        System.gc();
      }
      return jjs.getAstMemoryUsage() < getPotentialFreeMemory();
    }
  }

  public static final String GWT_COMPILER_DIR = ".gwt-tmp" + File.separatorChar
      + "compiler";

  public static void main(String[] args) {
    /*
     * NOTE: main always exits with a call to System.exit to terminate any
     * non-daemon threads that were started in Generators. Typically, this is to
     * shutdown AWT related threads, since the contract for their termination is
     * still implementation-dependent.
     */
    GWTCompiler compiler = new GWTCompiler();
    if (compiler.processArgs(args)) {
      if (compiler.run()) {
        // Exit w/ success code.
        System.exit(0);
      }
    }
    // Exit w/ non-success code.
    System.exit(1);
  }

  private static long getPotentialFreeMemory() {
    long used = Runtime.getRuntime().totalMemory()
        - Runtime.getRuntime().freeMemory();
    assert (used > 0);
    long potentialFree = Runtime.getRuntime().maxMemory() - used;
    assert (potentialFree >= 0);
    return potentialFree;
  }

  private CompilationState compilationState;

  private String[] declEntryPts;

  private File genDir;

  private File generatorResourcesDir;

  private JJSOptions jjsOptions = new JJSOptions();

  private Type logLevel;

  private ModuleDef module;

  private String moduleName;

  private File outDir;

  private PropertyPermutations perms;

  private Properties properties;

  private DistillerRebindPermutationOracle rebindPermOracle;

  private Rules rules;

  private boolean useGuiLogger;

  public GWTCompiler() {
    registerHandler(new ArgHandlerLogLevel() {
      @Override
      public void setLogLevel(Type level) {
        logLevel = level;
      }
    });

    registerHandler(new ArgHandlerGenDir() {
      @Override
      public void setDir(File dir) {
        genDir = dir;
      }
    });

    registerHandler(new ArgHandlerOutDir() {
      @Override
      public void setDir(File dir) {
        outDir = dir;
      }
    });

    registerHandler(new ArgHandlerTreeLoggerFlag() {
      @Override
      public boolean setFlag() {
        useGuiLogger = true;
        return true;
      }
    });

    registerHandler(new ArgHandlerModuleName());

    registerHandler(new ArgHandlerScriptStyle(jjsOptions));

    registerHandler(new ArgHandlerEnableAssertions(jjsOptions));

    registerHandler(new ArgHandlerDisableAggressiveOptimization() {
      @Override
      public boolean setFlag() {
        GWTCompiler.this.setAggressivelyOptimize(false);
        return true;
      }
    });

    registerHandler(new ArgHandlerValidateOnlyFlag());
  }

  public void distill(TreeLogger logger, ModuleDef moduleDef)
      throws UnableToCompleteException {
    this.module = moduleDef;
    this.compilationState = moduleDef.getCompilationState();

    // Set up all the initial state.
    checkModule(logger);

    // Place generated resources inside the out dir as a sibling to the module
    generatorResourcesDir = new File(outDir, GWT_COMPILER_DIR + File.separator
        + moduleDef.getName() + File.separator + "generated");

    // Tweak the output directory so that output lives under the module name.
    outDir = new File(outDir, module.getName());

    // Clean the outDir.
    Util.recursiveDelete(outDir, true);

    // Clean out the generated resources directory and/or create it.
    Util.recursiveDelete(generatorResourcesDir, true);
    generatorResourcesDir.mkdirs();

    // TODO: All JDT checks now before even building TypeOracle?
    compilationState.compile(logger);

    rules = module.getRules();
    if (jjsOptions.isValidateOnly()) {
      // TODO: revisit this.. do we even need to run JJS?
      logger.log(TreeLogger.INFO, "Validating compilation " + module.getName(),
          null);
      // Pretend that every single compilation unit is an entry point.
      Set<CompilationUnit> compilationUnits = compilationState.getCompilationUnits();
      declEntryPts = new String[compilationUnits.size()];
      int i = 0;
      for (CompilationUnit unit : compilationUnits) {
        declEntryPts[i++] = unit.getTypeName();
      }
    } else {
      logger.log(TreeLogger.INFO, "Compiling module " + module.getName(), null);
      // Use the real entry points.
      declEntryPts = module.getEntryPointTypeNames();
    }

    ArtifactSet generatorArtifacts = new ArtifactSet();
    properties = module.getProperties();
    perms = new PropertyPermutations(properties);
    rebindPermOracle = new DistillerRebindPermutationOracle(generatorArtifacts,
        perms);

    WebModeCompilerFrontEnd frontEnd = new WebModeCompilerFrontEnd(
        compilationState, rebindPermOracle);
    JavaToJavaScriptCompiler jjs = new JavaToJavaScriptCompiler(logger,
        frontEnd, declEntryPts, jjsOptions);

    if (jjsOptions.isValidateOnly()) {
      logger.log(TreeLogger.INFO, "Validation succeeded", null);
      return;
    }

    StandardLinkerContext linkerContext = new StandardLinkerContext(logger,
        module, outDir, generatorResourcesDir, jjsOptions);
    compilePermutations(logger, jjs, linkerContext);

    logger.log(TreeLogger.INFO, "Compilation succeeded", null);
    linkerContext.addOrReplaceArtifacts(generatorArtifacts);
    linkerContext.link(logger, linkerContext, null);
  }

  public File getGenDir() {
    return genDir;
  }

  public Type getLogLevel() {
    return logLevel;
  }

  public String getModuleName() {
    return moduleName;
  }

  public boolean getUseGuiLogger() {
    return useGuiLogger;
  }

  public void setAggressivelyOptimize(boolean aggressive) {
    jjsOptions.setAggressivelyOptimize(aggressive);
  }

  public void setCompilerOptions(JJSOptions options) {
    jjsOptions.copyFrom(options);
  }

  public void setGenDir(File dir) {
    genDir = dir;
  }

  public void setLogLevel(Type level) {
    this.logLevel = level;
  }

  public void setModuleName(String name) {
    moduleName = name;
  }

  public void setOutDir(File outDir) {
    this.outDir = outDir;
  }

  public void setStyleDetailed() {
    jjsOptions.setOutput(JsOutputOption.DETAILED);
  }

  public void setStyleObfuscated() {
    jjsOptions.setOutput(JsOutputOption.OBFUSCATED);
  }

  public void setStylePretty() {
    jjsOptions.setOutput(JsOutputOption.PRETTY);
  }

  /**
   * Ensure the module has at least one entry point (except in validation mode).
   */
  private void checkModule(TreeLogger logger) throws UnableToCompleteException {
    if (!jjsOptions.isValidateOnly()
        && module.getEntryPointTypeNames().length == 0) {
      logger.log(TreeLogger.ERROR, "Module has no entry points defined", null);
      throw new UnableToCompleteException();
    }
  }

  private void compilePermutations(TreeLogger logger,
      JavaToJavaScriptCompiler jjs, final StandardLinkerContext linkerContext)
      throws UnableToCompleteException {
    PerfLogger.start("Compiling " + perms.size() + " permutations");

    int threadCount = computeThreadCount(logger, jjs);

    PermutationManager manager = new PermutationManager(logger.branch(
        TreeLogger.DEBUG, "Compiling permutations", null), linkerContext,
        rebindPermOracle, threadCount);

    Thread[] threads = new Thread[threadCount];
    for (int i = 0; i < threadCount; ++i) {
      threads[i] = new Thread(new PermutationWorker(manager, jjs));
    }

    for (Thread thread : threads) {
      thread.start();
    }

    for (Thread thread : threads) {
      try {
        thread.join();
      } catch (InterruptedException e) {
        throw new RuntimeException("Unexpected interruption");
      }
    }

    PerfLogger.end();

    if (manager.failed) {
      throw new UnableToCompleteException();
    }
  }

  private int computeThreadCount(TreeLogger logger, JavaToJavaScriptCompiler jjs) {
    /*
     * Don't need more threads than the number of permutations.
     */
    int result = rebindPermOracle.getPermuationCount();

    /*
     * Computation is mostly CPU bound, so don't use more threads than
     * processors.
     */
    result = Math.min(Runtime.getRuntime().availableProcessors(), result);

    /*
     * Allow user-defined override as an escape valve.
     */
    result = Math.min(result, Integer.getInteger("gwt.jjs.maxThreads", result));

    if (result == 1) {
      return 1;
    }

    // More than one thread would definitely be faster at this point.

    if (JProgram.isTracingEnabled()) {
      logger.log(TreeLogger.INFO,
          "Parallel compilation disabled due to gwt.jjs.traceMethods being enabled");
      return 1;
    }

    int desiredThreads = result;

    /*
     * Need to do some memory estimation to figure out how many concurrent
     * threads we can safely run.
     */
    long potentialFreeMemory = getPotentialFreeMemory();
    long astMemoryUsage = jjs.getAstMemoryUsage();
    int memUsageThreads = (int) (potentialFreeMemory / astMemoryUsage) + 1;
    logger.log(TreeLogger.TRACE,
        "Extra threads constrained by estimated memory usage: "
            + memUsageThreads + " = " + potentialFreeMemory + " / "
            + astMemoryUsage);

    if (memUsageThreads < desiredThreads) {
      long currentMaxMemory = Runtime.getRuntime().maxMemory();
      // Convert to megabytes.
      currentMaxMemory /= 1024 * 1024;

      long suggestedMaxMemory = currentMaxMemory * 2;

      logger.log(TreeLogger.WARN, desiredThreads
          + " threads could be run concurrently, but only " + memUsageThreads
          + " threads will be run due to limited memory; "
          + "increasing the amount of memory by using the -Xmx flag "
          + "at startup (java -Xmx" + suggestedMaxMemory
          + "M ...) may result in faster compiles");
    }

    result = Math.min(memUsageThreads, desiredThreads);
    return result;
  }

  /**
   * Runs the compiler. If a gui-based TreeLogger is used, this method will not
   * return until its window is closed by the user.
   * 
   * @return success from the compiler, <code>true</code> if the compile
   *         completed without errors, <code>false</code> otherwise.
   */
  private boolean run() {
    // Set any platform specific system properties.
    BootStrapPlatform.applyPlatformHacks();

    if (useGuiLogger) {
      // Initialize a tree logger window.
      DetachedTreeLoggerWindow loggerWindow = DetachedTreeLoggerWindow.getInstance(
          "Build Output for " + moduleName, 800, 600, true);

      // Eager AWT initialization for OS X to ensure safe coexistence with SWT.
      BootStrapPlatform.maybeInitializeAWT();

      final AbstractTreeLogger logger = loggerWindow.getLogger();
      final boolean[] success = new boolean[1];

      // Compiler will be spawned onto a second thread, UI thread for tree
      // logger will remain on the main.
      Thread compilerThread = new Thread(new Runnable() {
        public void run() {
          success[0] = GWTCompiler.this.run(logger);
        }
      });

      compilerThread.setName("GWTCompiler Thread");
      compilerThread.start();
      loggerWindow.run();

      // Even if the tree logger window is closed, we wait for the compiler
      // to finish.
      waitForThreadToTerminate(compilerThread);

      return success[0];
    } else {
      return run(new PrintWriterTreeLogger());
    }
  }

  private boolean run(AbstractTreeLogger logger) {
    try {
      logger.setMaxDetail(logLevel);

      ModuleDef moduleDef = ModuleDefLoader.loadFromClassPath(logger,
          moduleName);
      distill(logger, moduleDef);
      return true;
    } catch (UnableToCompleteException e) {
      // We intentionally don't pass in the exception here since the real
      // cause has been logged.
      logger.log(TreeLogger.ERROR, "Build failed", null);
      return false;
    }
  }

  /**
   * Waits for a thread to terminate before it returns. This method is a
   * non-cancellable task, in that it will defer thread interruption until it is
   * done.
   * 
   * @param godot the thread that is being waited on.
   */
  private void waitForThreadToTerminate(final Thread godot) {
    // Goetz pattern for non-cancellable tasks.
    // http://www-128.ibm.com/developerworks/java/library/j-jtp05236.html
    boolean isInterrupted = false;
    try {
      while (true) {
        try {
          godot.join();
          return;
        } catch (InterruptedException e) {
          isInterrupted = true;
        }
      }
    } finally {
      if (isInterrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }
}
