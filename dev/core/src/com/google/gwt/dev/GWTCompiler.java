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
import com.google.gwt.core.ext.linker.SelectionProperty;
import com.google.gwt.core.ext.linker.impl.StandardCompilationResult;
import com.google.gwt.core.ext.linker.impl.StandardLinkerContext;
import com.google.gwt.core.ext.typeinfo.CompilationUnitProvider;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.ModuleDefLoader;
import com.google.gwt.dev.cfg.Properties;
import com.google.gwt.dev.cfg.Property;
import com.google.gwt.dev.cfg.PropertyPermutations;
import com.google.gwt.dev.cfg.Rules;
import com.google.gwt.dev.cfg.StaticPropertyOracle;
import com.google.gwt.dev.jdt.CacheManager;
import com.google.gwt.dev.jdt.RebindPermutationOracle;
import com.google.gwt.dev.jdt.StandardSourceOracle;
import com.google.gwt.dev.jdt.WebModeCompilerFrontEnd;
import com.google.gwt.dev.jjs.JJSOptions;
import com.google.gwt.dev.jjs.JavaToJavaScriptCompiler;
import com.google.gwt.dev.jjs.JsOutputOption;
import com.google.gwt.dev.shell.StandardRebindOracle;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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

    public CompilationRebindOracle() {
      super(typeOracle, propOracle, module, rules, genDir,
          generatorResourcesDir, cacheManager);
    }

    /**
     * Overridden so that we can selectively record inputs and outputs to derive
     * the cache key for a compilation. Note that the cache gets invalidated if
     * the propOracle changes state.
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

      if (recordDecision(in, out)) {
        List<JClassType> genTypes = generatedTypesByResultTypeName.get(out);
        if (genTypes != null) {
          for (JClassType genType : genTypes) {
            String sourceHash = genType.getTypeHash();
            String genTypeName = genType.getQualifiedSourceName();
            recordGeneratedTypeHash(genTypeName, sourceHash);
          }
        }
      }

      return out;
    }

    @SuppressWarnings("unused")
    protected boolean recordDecision(String in, String out) {
      // TODO(bobv): consider caching compilations again?
      return false;
    }

    @SuppressWarnings("unused")
    protected void recordGeneratedTypeHash(String typeName, String sourceHash) {
      // TODO(bobv): consider caching compilations again?
    }
  }

  private class DistillerRebindPermutationOracle implements
      RebindPermutationOracle {

    private final StandardRebindOracle rebindOracle = new StandardRebindOracle(
        typeOracle, propOracle, module, rules, genDir, generatorResourcesDir,
        cacheManager) {

      /**
       * Record generated types.
       */
      @Override
      protected void onGeneratedTypes(String result, JClassType[] genTypes) {
        List<JClassType> list = new ArrayList<JClassType>();
        Util.addAll(list, genTypes);
        Object existing = generatedTypesByResultTypeName.put(result, list);
        assert (existing == null) : "Internal error: redundant notification of generated types";
      }
    };

    public String[] getAllPossibleRebindAnswers(TreeLogger logger,
        String requestTypeName) throws UnableToCompleteException {

      String msg = "Computing all possible rebind results for '"
          + requestTypeName + "'";
      logger = logger.branch(TreeLogger.DEBUG, msg, null);

      Set<String> answers = new HashSet<String>();

      Property[] orderedProps = perms.getOrderedProperties();
      for (Iterator<String[]> iter = perms.iterator(); iter.hasNext();) {
        String[] orderedPropValues = iter.next();

        // Create a snapshot of the property values by setting their values
        // in the property oracle. Because my rebindOracle uses the shared
        // generator context (which in turns uses the propOracle), this
        // has the effect we're after. It isn't reentrant, though, so don't
        // expect to call this recursively.
        propOracle.setPropertyValues(orderedProps, orderedPropValues);

        // Ask the rebind oracle.
        logProperties(logger, orderedProps, orderedPropValues);
        String resultTypeName = rebindOracle.rebind(logger, requestTypeName);
        answers.add(resultTypeName);
      }
      return Util.toArray(String.class, answers);
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

  /**
   * Returns the fully-qualified main type name of a compilation unit.
   */
  private static String makeTypeName(CompilationUnitProvider cup) {
    if (cup.getPackageName().length() > 0) {
      return cup.getPackageName() + "." + cup.getMainTypeName();
    } else {
      return cup.getMainTypeName();
    }
  }

  private final CacheManager cacheManager;

  private File generatorResourcesDir;

  private String[] declEntryPts;

  private File genDir;

  private Map<String, List<JClassType>> generatedTypesByResultTypeName = new HashMap<String, List<JClassType>>();

  private JavaToJavaScriptCompiler jjs;

  private JJSOptions jjsOptions = new JJSOptions();

  private Type logLevel;

  private ModuleDef module;

  private String moduleName;

  private File outDir;

  private PropertyPermutations perms;

  private Properties properties;

  private StaticPropertyOracle propOracle = new StaticPropertyOracle();

  private RebindPermutationOracle rebindPermOracle;

  private Rules rules;

  private StandardSourceOracle sourceOracle;

  private TypeOracle typeOracle;

  private boolean useGuiLogger;

  public GWTCompiler() {
    this(null);
  }

  public GWTCompiler(CacheManager cacheManager) {
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

    this.cacheManager = cacheManager;
  }

  public void distill(TreeLogger logger, ModuleDef moduleDef)
      throws UnableToCompleteException {
    this.module = moduleDef;

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

    rules = module.getRules();
    typeOracle = module.getTypeOracle(logger);
    sourceOracle = new StandardSourceOracle(typeOracle);
    if (jjsOptions.isValidateOnly()) {
      logger.log(TreeLogger.INFO, "Validating compilation " + module.getName(),
          null);
      // Pretend that every single compilation unit is an entry point.
      CompilationUnitProvider[] compilationUnits = module.getCompilationUnits();
      declEntryPts = new String[compilationUnits.length];
      for (int i = 0; i < compilationUnits.length; ++i) {
        CompilationUnitProvider cup = compilationUnits[i];
        declEntryPts[i] = makeTypeName(cup);
      }
    } else {
      logger.log(TreeLogger.INFO, "Compiling module " + module.getName(), null);
      // Use the real entry points.
      declEntryPts = module.getEntryPointTypeNames();
    }
    rebindPermOracle = new DistillerRebindPermutationOracle();
    properties = module.getProperties();
    perms = new PropertyPermutations(properties);
    WebModeCompilerFrontEnd frontEnd = new WebModeCompilerFrontEnd(
        sourceOracle, rebindPermOracle);
    jjs = new JavaToJavaScriptCompiler(logger, frontEnd, declEntryPts,
        jjsOptions);

    StandardLinkerContext linkerContext = new StandardLinkerContext(logger,
        module, outDir, generatorResourcesDir, jjsOptions);
    compilePermutations(logger, linkerContext);

    if (jjsOptions.isValidateOnly()) {
      logger.log(TreeLogger.INFO, "Validation succeeded", null);
      return;
    }

    logger.log(TreeLogger.INFO, "Compilation succeeded", null);

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
      StandardLinkerContext linkerContext) throws UnableToCompleteException {
    logger = logger.branch(TreeLogger.DEBUG, "Compiling permutations", null);
    Property[] orderedProps = perms.getOrderedProperties();
    int permNumber = 1;
    for (Iterator<String[]> iter = perms.iterator(); iter.hasNext(); ++permNumber) {

      String[] orderedPropValues = iter.next();
      String js = realizePermutation(logger, orderedProps, orderedPropValues,
          permNumber);

      // This is the case in validateOnly mode, which doesn't produce output
      if (js == null) {
        continue;
      }

      StandardCompilationResult compilation = linkerContext.getCompilation(
          logger, js);

      Map<SelectionProperty, String> unboundProperties = new HashMap<SelectionProperty, String>();
      for (int i = 0; i < orderedProps.length; i++) {
        SelectionProperty key = linkerContext.getProperty(orderedProps[i].getName());
        if (key.tryGetValue() != null) {
          // The view of the Permutation doesn't include properties with defined
          // values.
          continue;
        }
        unboundProperties.put(key, orderedPropValues[i]);
      }

      compilation.addSelectionPermutation(unboundProperties);
    }
  }

  private void logProperties(TreeLogger logger, Property[] props,
      String[] values) {
    if (logger.isLoggable(TreeLogger.DEBUG)) {
      logger = logger.branch(TreeLogger.DEBUG, "Setting properties", null);
      for (int i = 0; i < props.length; i++) {
        String name = props[i].getName();
        String value = values[i];
        logger.log(TreeLogger.TRACE, name + " = " + value, null);
      }
    }
  }

  /**
   * Attempts to compile with a single permutation of properties. The result can
   * be one of the following:
   * <ul>
   * <li>There is an existing compilation having the same deferred binding
   * results (and thus would create identical output); compilation is skipped
   * <li>No existing compilation unit matches, so the compilation proceeds
   * </ul>
   */
  private String realizePermutation(TreeLogger logger, Property[] currentProps,
      String[] currentValues, int permNumber) throws UnableToCompleteException {
    String msg = "Analyzing permutation #" + permNumber;
    logger = logger.branch(TreeLogger.TRACE, msg, null);

    logProperties(logger, currentProps, currentValues);

    // Create a rebind oracle that will record decisions so that we can cache
    // them and avoid future computations.
    //
    CompilationRebindOracle rebindOracle = new CompilationRebindOracle();

    // Tell the property provider above about the current property values.
    // Note that the rebindOracle is actually sensitive to these values because
    // in its ctor is uses propOracle as its property oracle.
    //
    propOracle.setPropertyValues(currentProps, currentValues);

    // Create JavaScript.
    //
    return jjs.compile(logger, rebindOracle);
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
