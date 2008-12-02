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
import com.google.gwt.dev.CompileTaskRunner.CompileTask;
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
import com.google.gwt.dev.jjs.JJSOptions;
import com.google.gwt.dev.jjs.JJSOptionsImpl;
import com.google.gwt.dev.jjs.JavaToJavaScriptCompiler;
import com.google.gwt.dev.jjs.JsOutputOption;
import com.google.gwt.dev.jjs.UnifiedAst;
import com.google.gwt.dev.jjs.impl.FragmentLoaderCreator;
import com.google.gwt.dev.shell.StandardRebindOracle;
import com.google.gwt.dev.util.PerfLogger;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.arg.ArgHandlerDisableAggressiveOptimization;
import com.google.gwt.dev.util.arg.ArgHandlerEnableAssertions;
import com.google.gwt.dev.util.arg.ArgHandlerGenDir;
import com.google.gwt.dev.util.arg.ArgHandlerScriptStyle;
import com.google.gwt.dev.util.arg.ArgHandlerValidateOnlyFlag;
import com.google.gwt.dev.util.arg.OptionGenDir;
import com.google.gwt.dev.util.arg.OptionValidateOnly;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

/**
 * Performs the first phase of compilation, generating the set of permutations
 * to compile, and a ready-to-compile AST.
 */
public class Precompile {

  /**
   * The set of options for the precompiler.
   */
  public interface PrecompileOptions extends JJSOptions, CompileTaskOptions,
      OptionGenDir, OptionValidateOnly {
  }

  static class ArgProcessor extends CompileArgProcessor {
    public ArgProcessor(PrecompileOptions options) {
      super(options);
      registerHandler(new ArgHandlerGenDir(options));
      registerHandler(new ArgHandlerScriptStyle(options));
      registerHandler(new ArgHandlerEnableAssertions(options));
      registerHandler(new ArgHandlerDisableAggressiveOptimization(options));
      registerHandler(new ArgHandlerValidateOnlyFlag(options));
    }

    @Override
    protected String getName() {
      return Precompile.class.getName();
    }
  }

  static class PrecompileOptionsImpl extends CompileTaskOptionsImpl implements
      PrecompileOptions {
    private File genDir;
    private final JJSOptionsImpl jjsOptions = new JJSOptionsImpl();
    private boolean validateOnly;

    public PrecompileOptionsImpl() {
    }

    public PrecompileOptionsImpl(PrecompileOptions other) {
      copyFrom(other);
    }

    public void copyFrom(PrecompileOptions other) {
      super.copyFrom(other);

      jjsOptions.copyFrom(other);

      setGenDir(other.getGenDir());
      setValidateOnly(other.isValidateOnly());
    }

    public File getGenDir() {
      return genDir;
    }

    public JsOutputOption getOutput() {
      return jjsOptions.getOutput();
    }

    public boolean isAggressivelyOptimize() {
      return jjsOptions.isAggressivelyOptimize();
    }

    public boolean isEnableAssertions() {
      return jjsOptions.isEnableAssertions();
    }

    public boolean isSoycEnabled() {
      return jjsOptions.isSoycEnabled();
    }

    public boolean isValidateOnly() {
      return validateOnly;
    }

    public void setAggressivelyOptimize(boolean aggressivelyOptimize) {
      jjsOptions.setAggressivelyOptimize(aggressivelyOptimize);
    }

    public void setEnableAssertions(boolean enableAssertions) {
      jjsOptions.setEnableAssertions(enableAssertions);
    }

    public void setGenDir(File genDir) {
      this.genDir = genDir;
    }

    public void setOutput(JsOutputOption output) {
      jjsOptions.setOutput(output);
    }

    public void setSoycEnabled(boolean enabled) {
      jjsOptions.setSoycEnabled(enabled);
    }

    public void setValidateOnly(boolean validateOnly) {
      this.validateOnly = validateOnly;
    }
  }

  private static class DistillerRebindPermutationOracle implements
      RebindPermutationOracle {

    private Permutation[] permutations;
    private StaticPropertyOracle[] propertyOracles;
    private RebindOracle[] rebindOracles;

    public DistillerRebindPermutationOracle(ModuleDef module,
        CompilationState compilationState, ArtifactSet generatorArtifacts,
        PropertyPermutations perms, File genDir, File generatorResourcesDir) {
      permutations = new Permutation[perms.size()];
      propertyOracles = new StaticPropertyOracle[perms.size()];
      rebindOracles = new RebindOracle[perms.size()];
      BindingProperty[] orderedProps = perms.getOrderedProperties();
      SortedSet<ConfigurationProperty> configPropSet = module.getProperties().getConfigurationProperties();
      ConfigurationProperty[] configProps = configPropSet.toArray(new ConfigurationProperty[configPropSet.size()]);
      Rules rules = module.getRules();
      for (int i = 0; i < rebindOracles.length; ++i) {
        String[] orderedPropValues = perms.getOrderedPropertyValues(i);
        propertyOracles[i] = new StaticPropertyOracle(orderedProps,
            orderedPropValues, configProps);
        rebindOracles[i] = new StandardRebindOracle(compilationState,
            propertyOracles[i], module, rules, genDir, generatorResourcesDir,
            generatorArtifacts);
        permutations[i] = new Permutation(i, propertyOracles[i]);
      }
    }

    public String[] getAllPossibleRebindAnswers(TreeLogger logger,
        String requestTypeName) throws UnableToCompleteException {

      String msg = "Computing all possible rebind results for '"
          + requestTypeName + "'";
      logger = logger.branch(TreeLogger.DEBUG, msg, null);

      Set<String> answers = new HashSet<String>();

      for (int i = 0; i < getPermuationCount(); ++i) {
        String resultTypeName = rebindOracles[i].rebind(logger, requestTypeName);
        answers.add(resultTypeName);
        // Record the correct answer into each permutation.
        permutations[i].putRebindAnswer(requestTypeName, resultTypeName);
      }
      return Util.toArray(String.class, answers);
    }

    public int getPermuationCount() {
      return rebindOracles.length;
    }

    public Permutation[] getPermutations() {
      return permutations;
    }

    public StaticPropertyOracle getPropertyOracle(int permNumber) {
      return propertyOracles[permNumber];
    }

    public RebindOracle getRebindOracle(int permNumber) {
      return rebindOracles[permNumber];
    }
  }

  static final String PERM_COUNT_FILENAME = "permCount.txt";

  static final String PRECOMPILATION_FILENAME = "precompilation.ser";

  /**
   * Performs a command-line precompile.
   */
  public static void main(String[] args) {
    /*
     * NOTE: main always exits with a call to System.exit to terminate any
     * non-daemon threads that were started in Generators. Typically, this is to
     * shutdown AWT related threads, since the contract for their termination is
     * still implementation-dependent.
     */
    final PrecompileOptions options = new PrecompileOptionsImpl();
    if (new ArgProcessor(options).processArgs(args)) {
      CompileTask task = new CompileTask() {
        public boolean run(TreeLogger logger) throws UnableToCompleteException {
          return new Precompile(options).run(logger);
        }
      };
      if (CompileTaskRunner.runWithAppropriateLogger(options, task)) {
        // Exit w/ success code.
        System.exit(0);
      }
    }
    // Exit w/ non-success code.
    System.exit(1);
  }

  /**
   * Precompiles the given module.
   * 
   * @param logger a logger to use
   * @param jjsOptions a set of compiler options
   * @param module the module to compile
   * @param genDir optional directory to dump generated source, may be
   *          <code>null</code>
   * @param generatorResourcesDir required directory to dump generator resources
   * @return the precompilation
   */
  public static Precompilation precompile(TreeLogger logger,
      JJSOptions jjsOptions, ModuleDef module, File genDir,
      File generatorResourcesDir) {
    try {
      CompilationState compilationState = module.getCompilationState(logger);

      String[] declEntryPts = module.getEntryPointTypeNames();
      if (declEntryPts.length == 0) {
        logger.log(TreeLogger.ERROR, "Module has no entry points defined", null);
        throw new UnableToCompleteException();
      }

      ArtifactSet generatedArtifacts = new ArtifactSet();
      DistillerRebindPermutationOracle rpo = new DistillerRebindPermutationOracle(
          module, compilationState, generatedArtifacts,
          new PropertyPermutations(module.getProperties()), genDir,
          generatorResourcesDir);
      FragmentLoaderCreator fragmentLoaderCreator = new FragmentLoaderCreator(
          compilationState, module, genDir, generatorResourcesDir,
          generatedArtifacts);
      WebModeCompilerFrontEnd frontEnd = new WebModeCompilerFrontEnd(
          compilationState, rpo, fragmentLoaderCreator);
      PerfLogger.start("Precompile");
      UnifiedAst unifiedAst = JavaToJavaScriptCompiler.precompile(logger,
          frontEnd, declEntryPts, jjsOptions, rpo.getPermuationCount() == 1);
      PerfLogger.end();

      // Merge all identical permutations together.
      Permutation[] permutations = rpo.getPermutations();
      // Sort the permutations by an ordered key to ensure determinism.
      SortedMap<String, Permutation> merged = new TreeMap<String, Permutation>();
      for (Permutation permutation : permutations) {
        permutation.reduceRebindAnswers(unifiedAst.getRebindRequests());
        // Arbitrarily choose as a key the stringified map of rebind answers.
        String rebindResultsString = permutation.getRebindAnswers().toString();
        if (merged.containsKey(rebindResultsString)) {
          Permutation existing = merged.get(rebindResultsString);
          existing.mergeFrom(permutation);
        } else {
          merged.put(rebindResultsString, permutation);
        }
      }
      permutations = merged.values().toArray(new Permutation[merged.size()]);
      return new Precompilation(unifiedAst, permutations, generatedArtifacts);
    } catch (UnableToCompleteException e) {
      // We intentionally don't pass in the exception here since the real
      // cause has been logged.
      return null;
    }
  }

  /**
   * Validates the given module can be compiled.
   * 
   * @param logger a logger to use
   * @param jjsOptions a set of compiler options
   * @param module the module to compile
   * @param genDir optional directory to dump generated source, may be
   *          <code>null</code>
   * @param generatorResourcesDir required directory to dump generator resources
   */
  public static boolean validate(TreeLogger logger, JJSOptions jjsOptions,
      ModuleDef module, File genDir, File generatorResourcesDir) {
    try {
      CompilationState compilationState = module.getCompilationState(logger);

      String[] declEntryPts = module.getEntryPointTypeNames();
      if (declEntryPts.length == 0) {
        // Pretend that every single compilation unit is an entry point.
        Set<CompilationUnit> compilationUnits = compilationState.getCompilationUnits();
        declEntryPts = new String[compilationUnits.size()];
        int i = 0;
        for (CompilationUnit unit : compilationUnits) {
          declEntryPts[i++] = unit.getTypeName();
        }
      }

      ArtifactSet generatorArtifacts = new ArtifactSet();
      DistillerRebindPermutationOracle rpo = new DistillerRebindPermutationOracle(
          module, compilationState, generatorArtifacts,
          new PropertyPermutations(module.getProperties()), genDir,
          generatorResourcesDir);
      FragmentLoaderCreator fragmentLoaderCreator = new FragmentLoaderCreator(
          compilationState, module, genDir, generatorResourcesDir,
          generatorArtifacts);
      WebModeCompilerFrontEnd frontEnd = new WebModeCompilerFrontEnd(
          compilationState, rpo, fragmentLoaderCreator);
      JavaToJavaScriptCompiler.precompile(logger, frontEnd, declEntryPts,
          jjsOptions, true);
      return true;
    } catch (UnableToCompleteException e) {
      // Already logged.
      return false;
    }
  }

  private ModuleDef module;

  private final PrecompileOptionsImpl options;

  public Precompile(PrecompileOptions options) {
    this.options = new PrecompileOptionsImpl(options);
  }

  public boolean run(TreeLogger logger) throws UnableToCompleteException {
    if (options.isValidateOnly()) {
      init(logger);
      TreeLogger branch = logger.branch(TreeLogger.INFO,
          "Validating compilation " + module.getName());
      if (validate(branch, options, module, options.getGenDir(),
          options.getCompilerWorkDir())) {
        branch.log(TreeLogger.INFO, "Validation succeeded");
        return true;
      } else {
        branch.log(TreeLogger.ERROR, "Validation failed");
        return false;
      }
    } else {
      init(logger);
      TreeLogger branch = logger.branch(TreeLogger.INFO, "Precompiling module "
          + module.getName());
      Precompilation precompilation = precompile(branch, options, module,
          options.getGenDir(), options.getCompilerWorkDir());
      if (precompilation != null) {
        Util.writeObjectAsFile(branch, new File(options.getCompilerWorkDir(),
            PRECOMPILATION_FILENAME), precompilation);
        Util.writeStringAsFile(branch, new File(options.getCompilerWorkDir(),
            PERM_COUNT_FILENAME),
            String.valueOf(precompilation.getPermutations().length));
        branch.log(TreeLogger.INFO,
            "Precompilation succeeded, number of permutations: "
                + precompilation.getPermutations().length);
        return true;
      }
      branch.log(TreeLogger.ERROR, "Precompilation failed");
      return false;
    }
  }

  private void init(TreeLogger logger) throws UnableToCompleteException {
    // Clean out the work dir and/or create it.
    File compilerWorkDir = options.getCompilerWorkDir();
    Util.recursiveDelete(compilerWorkDir, true);
    compilerWorkDir.mkdirs();

    this.module = ModuleDefLoader.loadFromClassPath(logger,
        options.getModuleName());

    // TODO: All JDT checks now before even building TypeOracle?
    module.getCompilationState(logger);
  }
}
