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
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.impl.StandardLinkerContext;
import com.google.gwt.dev.cfg.BindingProperty;
import com.google.gwt.dev.cfg.ConfigurationProperty;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.ModuleDefLoader;
import com.google.gwt.dev.cfg.PropertyPermutations;
import com.google.gwt.dev.cfg.Rules;
import com.google.gwt.dev.cfg.StaticPropertyOracle;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.CompilationUnit;
import com.google.gwt.dev.jdt.RebindOracle;
import com.google.gwt.dev.jdt.RebindPermutationOracle;
import com.google.gwt.dev.jdt.WebModeCompilerFrontEnd;
import com.google.gwt.dev.jjs.JavaToJavaScriptCompiler;
import com.google.gwt.dev.shell.StandardRebindOracle;
import com.google.gwt.dev.util.PerfLogger;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.arg.ArgHandlerDisableAggressiveOptimization;
import com.google.gwt.dev.util.arg.ArgHandlerEnableAssertions;
import com.google.gwt.dev.util.arg.ArgHandlerGenDir;
import com.google.gwt.dev.util.arg.ArgHandlerLogLevel;
import com.google.gwt.dev.util.arg.ArgHandlerModuleName;
import com.google.gwt.dev.util.arg.ArgHandlerOutDir;
import com.google.gwt.dev.util.arg.ArgHandlerScriptStyle;
import com.google.gwt.dev.util.arg.ArgHandlerTreeLoggerFlag;
import com.google.gwt.dev.util.arg.ArgHandlerValidateOnlyFlag;
import com.google.gwt.dev.util.log.AbstractTreeLogger;
import com.google.gwt.dev.util.log.DetachedTreeLoggerWindow;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.util.tools.ToolBase;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;

/**
 * The main executable entry point for the GWT Java to JavaScript compiler.
 */
public class GWTCompiler extends ToolBase {

  private class DistillerRebindPermutationOracle implements
      RebindPermutationOracle {

    private StaticPropertyOracle[] propertyOracles;
    private RebindOracle[] rebindOracles;

    public DistillerRebindPermutationOracle(ArtifactSet generatorArtifacts,
        PropertyPermutations perms) {
      propertyOracles = new StaticPropertyOracle[perms.size()];
      rebindOracles = new RebindOracle[perms.size()];
      BindingProperty[] orderedProps = perms.getOrderedProperties();
      SortedSet<ConfigurationProperty> configPropSet = module.getProperties().getConfigurationProperties();
      ConfigurationProperty[] configProps = configPropSet.toArray(new ConfigurationProperty[configPropSet.size()]);
      for (int i = 0; i < rebindOracles.length; ++i) {
        String[] orderedPropValues = perms.getOrderedPropertyValues(i);
        propertyOracles[i] = new StaticPropertyOracle(orderedProps,
            orderedPropValues, configProps);
        rebindOracles[i] = new StandardRebindOracle(compilationState,
            propertyOracles[i], module, rules, options.getGenDir(),
            generatorResourcesDir, generatorArtifacts);
      }
    }

    public String[] getAllPossibleRebindAnswers(TreeLogger logger,
        String requestTypeName) throws UnableToCompleteException {

      String msg = "Computing all possible rebind results for '"
          + requestTypeName + "'";
      logger = logger.branch(TreeLogger.DEBUG, msg, null);

      Set<String> answers = new HashSet<String>();

      for (RebindOracle rebindOracle : rebindOracles) {
        String resultTypeName = rebindOracle.rebind(logger, requestTypeName);
        answers.add(resultTypeName);
      }
      return Util.toArray(String.class, answers);
    }

    public int getPermuationCount() {
      return rebindOracles.length;
    }

    public StaticPropertyOracle getPropertyOracle(int permNumber) {
      return propertyOracles[permNumber];
    }

    public RebindOracle getRebindOracle(int permNumber) {
      return rebindOracles[permNumber];
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

  private CompilationState compilationState;

  private File generatorResourcesDir;

  private ModuleDef module;

  private CompilerOptionsImpl options = new CompilerOptionsImpl();

  private Rules rules;

  public GWTCompiler() {
    registerHandler(new ArgHandlerLogLevel(options));
    registerHandler(new ArgHandlerGenDir(options));
    registerHandler(new ArgHandlerOutDir(options));
    registerHandler(new ArgHandlerTreeLoggerFlag(options));

    registerHandler(new ArgHandlerModuleName(options));

    registerHandler(new ArgHandlerScriptStyle(options));
    registerHandler(new ArgHandlerEnableAssertions(options));
    registerHandler(new ArgHandlerDisableAggressiveOptimization(options));

    registerHandler(new ArgHandlerValidateOnlyFlag(options));
  }

  public void distill(TreeLogger logger, ModuleDef moduleDef)
      throws UnableToCompleteException {
    this.module = moduleDef;
    this.compilationState = moduleDef.getCompilationState();

    // Set up all the initial state.
    checkModule(logger);

    // Place generated resources inside the out dir as a sibling to the module
    File outDir = options.getOutDir();
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
    String[] declEntryPts;
    if (options.isValidateOnly()) {
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
    DistillerRebindPermutationOracle rpo = new DistillerRebindPermutationOracle(
        generatorArtifacts, new PropertyPermutations(module.getProperties()));

    WebModeCompilerFrontEnd frontEnd = new WebModeCompilerFrontEnd(
        compilationState, rpo);
    JavaToJavaScriptCompiler jjs = new JavaToJavaScriptCompiler(logger,
        frontEnd, declEntryPts, options, options.isValidateOnly());

    if (options.isValidateOnly()) {
      logger.log(TreeLogger.INFO, "Validation succeeded", null);
      return;
    }

    StandardLinkerContext linkerContext = new StandardLinkerContext(logger,
        module, outDir, generatorResourcesDir, options);
    compilePermutations(logger, jjs, rpo, linkerContext);

    logger.log(TreeLogger.INFO, "Compilation succeeded", null);
    linkerContext.addOrReplaceArtifacts(generatorArtifacts);
    linkerContext.link(logger, linkerContext, null);
  }

  public CompilerOptions getCompilerOptions() {
    return new CompilerOptionsImpl(options);
  }

  public void setCompilerOptions(CompilerOptions options) {
    this.options.copyFrom(options);
  }

  /**
   * Ensure the module has at least one entry point (except in validation mode).
   */
  private void checkModule(TreeLogger logger) throws UnableToCompleteException {
    if (!options.isValidateOnly()
        && module.getEntryPointTypeNames().length == 0) {
      logger.log(TreeLogger.ERROR, "Module has no entry points defined", null);
      throw new UnableToCompleteException();
    }
  }

  private void compilePermutations(TreeLogger logger,
      JavaToJavaScriptCompiler jjs, DistillerRebindPermutationOracle rpo,
      StandardLinkerContext linkerContext) throws UnableToCompleteException {

    int permCount = rpo.getPermuationCount();
    PerfLogger.start("Compiling " + permCount + " permutations");
    Permutation[] perms = new Permutation[permCount];
    for (int i = 0; i < permCount; ++i) {
      perms[i] = new Permutation(i, rpo.getRebindOracle(i),
          rpo.getPropertyOracle(i));
    }
    PermutationCompiler permCompiler = new PermutationCompiler(logger, jjs,
        perms);
    permCompiler.go(linkerContext);
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

    if (options.isUseGuiLogger()) {
      // Initialize a tree logger window.
      DetachedTreeLoggerWindow loggerWindow = DetachedTreeLoggerWindow.getInstance(
          "Build Output for " + options.getModuleName(), 800, 600, true);

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
      logger.setMaxDetail(options.getLogLevel());

      ModuleDef moduleDef = ModuleDefLoader.loadFromClassPath(logger,
          options.getModuleName());
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
