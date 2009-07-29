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
import com.google.gwt.dev.javac.StandardGeneratorContext;
import com.google.gwt.dev.jdt.RebindOracle;
import com.google.gwt.dev.jdt.RebindPermutationOracle;
import com.google.gwt.dev.jjs.JJSOptions;
import com.google.gwt.dev.jjs.JJSOptionsImpl;
import com.google.gwt.dev.jjs.JavaToJavaScriptCompiler;
import com.google.gwt.dev.jjs.JsOutputOption;
import com.google.gwt.dev.jjs.UnifiedAst;
import com.google.gwt.dev.shell.CheckForUpdates;
import com.google.gwt.dev.shell.StandardRebindOracle;
import com.google.gwt.dev.shell.CheckForUpdates.UpdateResult;
import com.google.gwt.dev.util.Memory;
import com.google.gwt.dev.util.PerfLogger;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.arg.ArgHandlerDisableAggressiveOptimization;
import com.google.gwt.dev.util.arg.ArgHandlerDisableCastChecking;
import com.google.gwt.dev.util.arg.ArgHandlerDisableClassMetadata;
import com.google.gwt.dev.util.arg.ArgHandlerDisableRunAsync;
import com.google.gwt.dev.util.arg.ArgHandlerDisableUpdateCheck;
import com.google.gwt.dev.util.arg.ArgHandlerDraftCompile;
import com.google.gwt.dev.util.arg.ArgHandlerDumpSignatures;
import com.google.gwt.dev.util.arg.ArgHandlerEnableAssertions;
import com.google.gwt.dev.util.arg.ArgHandlerGenDir;
import com.google.gwt.dev.util.arg.ArgHandlerMaxPermsPerPrecompile;
import com.google.gwt.dev.util.arg.ArgHandlerScriptStyle;
import com.google.gwt.dev.util.arg.ArgHandlerSoyc;
import com.google.gwt.dev.util.arg.ArgHandlerValidateOnlyFlag;
import com.google.gwt.dev.util.arg.OptionDisableUpdateCheck;
import com.google.gwt.dev.util.arg.OptionDumpSignatures;
import com.google.gwt.dev.util.arg.OptionGenDir;
import com.google.gwt.dev.util.arg.OptionMaxPermsPerPrecompile;
import com.google.gwt.dev.util.arg.OptionValidateOnly;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.concurrent.FutureTask;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

/**
 * Performs the first phase of compilation, generating the set of permutations
 * to compile, and a ready-to-compile AST.
 */
public class Precompile {

  /**
   * The set of options for the precompiler.
   */
  public interface PrecompileOptions extends JJSOptions, CompileTaskOptions,
      OptionGenDir, OptionValidateOnly, OptionDisableUpdateCheck,
      OptionDumpSignatures, OptionMaxPermsPerPrecompile {
  }

  static class ArgProcessor extends CompileArgProcessor {
    public ArgProcessor(PrecompileOptions options) {
      super(options);
      registerHandler(new ArgHandlerGenDir(options));
      registerHandler(new ArgHandlerScriptStyle(options));
      registerHandler(new ArgHandlerEnableAssertions(options));
      registerHandler(new ArgHandlerDisableAggressiveOptimization(options));
      registerHandler(new ArgHandlerDisableClassMetadata(options));
      registerHandler(new ArgHandlerDisableCastChecking(options));
      registerHandler(new ArgHandlerValidateOnlyFlag(options));
      registerHandler(new ArgHandlerDisableRunAsync(options));
      registerHandler(new ArgHandlerDraftCompile(options));
      registerHandler(new ArgHandlerDisableUpdateCheck(options));
      registerHandler(new ArgHandlerDumpSignatures(options));
      registerHandler(new ArgHandlerMaxPermsPerPrecompile(options));
      registerHandler(new ArgHandlerSoyc(options));
    }

    @Override
    protected String getName() {
      return Precompile.class.getName();
    }
  }

  static class PrecompileOptionsImpl extends CompileTaskOptionsImpl implements
      PrecompileOptions {
    private boolean disableUpdateCheck;
    private File dumpFile;
    private File genDir;
    private final JJSOptionsImpl jjsOptions = new JJSOptionsImpl();
    private int maxPermsPerPrecompile;
    private boolean validateOnly;

    public PrecompileOptionsImpl() {
    }

    public PrecompileOptionsImpl(PrecompileOptions other) {
      copyFrom(other);
    }

    public void copyFrom(PrecompileOptions other) {
      super.copyFrom(other);

      jjsOptions.copyFrom(other);

      setDisableUpdateCheck(other.isUpdateCheckDisabled());
      setDumpSignatureFile(other.getDumpSignatureFile());
      setGenDir(other.getGenDir());
      setMaxPermsPerPrecompile(other.getMaxPermsPerPrecompile());
      setValidateOnly(other.isValidateOnly());
    }

    public File getDumpSignatureFile() {
      return dumpFile;
    }

    public File getGenDir() {
      return genDir;
    }

    public int getMaxPermsPerPrecompile() {
      return maxPermsPerPrecompile;
    }

    public JsOutputOption getOutput() {
      return jjsOptions.getOutput();
    }

    public boolean isAggressivelyOptimize() {
      return jjsOptions.isAggressivelyOptimize();
    }

    public boolean isCastCheckingDisabled() {
      return jjsOptions.isCastCheckingDisabled();
    }

    public boolean isClassMetadataDisabled() {
      return jjsOptions.isClassMetadataDisabled();
    }

    public boolean isCompilationStateRetained() {
      return jjsOptions.isCompilationStateRetained();
    }

    public boolean isDraftCompile() {
      return jjsOptions.isDraftCompile();
    }

    public boolean isEnableAssertions() {
      return jjsOptions.isEnableAssertions();
    }

    public boolean isOptimizePrecompile() {
      return jjsOptions.isOptimizePrecompile();
    }

    public boolean isRunAsyncEnabled() {
      return jjsOptions.isRunAsyncEnabled();
    }

    public boolean isSoycEnabled() {
      return jjsOptions.isSoycEnabled();
    }

    public boolean isUpdateCheckDisabled() {
      return disableUpdateCheck;
    }

    public boolean isValidateOnly() {
      return validateOnly;
    }

    public void setAggressivelyOptimize(boolean aggressivelyOptimize) {
      jjsOptions.setAggressivelyOptimize(aggressivelyOptimize);
    }

    public void setCastCheckingDisabled(boolean disabled) {
      jjsOptions.setCastCheckingDisabled(disabled);
    }

    public void setClassMetadataDisabled(boolean disabled) {
      jjsOptions.setClassMetadataDisabled(disabled);
    }

    public void setCompilationStateRetained(boolean retained) {
      jjsOptions.setCompilationStateRetained(retained);
    }

    public void setDisableUpdateCheck(boolean disabled) {
      disableUpdateCheck = disabled;
    }

    public void setDraftCompile(boolean draft) {
      jjsOptions.setDraftCompile(draft);
    }

    public void setDumpSignatureFile(File dumpFile) {
      this.dumpFile = dumpFile;
    }

    public void setEnableAssertions(boolean enableAssertions) {
      jjsOptions.setEnableAssertions(enableAssertions);
    }

    public void setGenDir(File genDir) {
      this.genDir = genDir;
    }

    public void setMaxPermsPerPrecompile(int maxPermsPerPrecompile) {
      this.maxPermsPerPrecompile = maxPermsPerPrecompile;
    }

    public void setOptimizePrecompile(boolean optimize) {
      jjsOptions.setOptimizePrecompile(optimize);
    }

    public void setOutput(JsOutputOption output) {
      jjsOptions.setOutput(output);
    }

    public void setRunAsyncEnabled(boolean enabled) {
      jjsOptions.setRunAsyncEnabled(enabled);
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

    private StandardGeneratorContext generatorContext;
    private Permutation[] permutations;
    private StaticPropertyOracle[] propertyOracles;
    private RebindOracle[] rebindOracles;

    public DistillerRebindPermutationOracle(ModuleDef module,
        CompilationState compilationState, ArtifactSet generatorArtifacts,
        PropertyPermutations perms, File genDir, File generatorResourcesDir) {
      permutations = new Permutation[perms.size()];
      propertyOracles = new StaticPropertyOracle[perms.size()];
      rebindOracles = new RebindOracle[perms.size()];
      generatorContext = new StandardGeneratorContext(compilationState, module,
          genDir, generatorResourcesDir, generatorArtifacts);
      BindingProperty[] orderedProps = perms.getOrderedProperties();
      SortedSet<ConfigurationProperty> configPropSet = module.getProperties().getConfigurationProperties();
      ConfigurationProperty[] configProps = configPropSet.toArray(new ConfigurationProperty[configPropSet.size()]);
      Rules rules = module.getRules();
      for (int i = 0; i < rebindOracles.length; ++i) {
        String[] orderedPropValues = perms.getOrderedPropertyValues(i);
        propertyOracles[i] = new StaticPropertyOracle(orderedProps,
            orderedPropValues, configProps);
        rebindOracles[i] = new StandardRebindOracle(propertyOracles[i], rules,
            generatorContext);
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

    public StandardGeneratorContext getGeneratorContext() {
      return generatorContext;
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

  /**
   * The file name for the result of Precompile.
   */
  public static final String PRECOMPILE_FILENAME = "precompilation.ser";

  static final String PERM_COUNT_FILENAME = "permCount.txt";

  /**
   * Performs a command-line precompile.
   */
  public static void main(String[] args) {
    Memory.initialize();
    if (System.getProperty("gwt.jjs.dumpAst") != null) {
      System.out.println("Will dump AST to: "
          + System.getProperty("gwt.jjs.dumpAst"));
    }

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
          FutureTask<UpdateResult> updater = null;
          if (!options.isUpdateCheckDisabled()) {
            updater = CheckForUpdates.checkForUpdatesInBackgroundThread(logger,
                CheckForUpdates.ONE_DAY);
          }
          boolean success = new Precompile(options).run(logger);
          if (success) {
            CheckForUpdates.logUpdateAvailable(logger, updater);
          }
          return success;
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
      File generatorResourcesDir, File dumpSignatureFile) {
    return precompile(logger, jjsOptions, module, 0, 0,
        module.getProperties().numPermutations(), genDir,
        generatorResourcesDir, dumpSignatureFile);
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
      ModuleDef module, File genDir, File generatorResourcesDir,
      File dumpSignatureFile) {
    try {
      CompilationState compilationState = module.getCompilationState(logger);
      if (dumpSignatureFile != null) {
        // Dump early to avoid generated types.
        SignatureDumper.dumpSignatures(logger, module.getTypeOracle(logger),
            dumpSignatureFile);
      }

      String[] declEntryPts = module.getEntryPointTypeNames();
      String[] additionalRootTypes = null;
      if (declEntryPts.length == 0) {
        // No declared entry points, just validate all visible classes.
        Collection<CompilationUnit> compilationUnits = compilationState.getCompilationUnits();
        additionalRootTypes = new String[compilationUnits.size()];
        int i = 0;
        for (CompilationUnit unit : compilationUnits) {
          additionalRootTypes[i++] = unit.getTypeName();
        }
      }

      ArtifactSet generatorArtifacts = new ArtifactSet();
      DistillerRebindPermutationOracle rpo = new DistillerRebindPermutationOracle(
          module, compilationState, generatorArtifacts,
          new PropertyPermutations(module.getProperties()), genDir,
          generatorResourcesDir);
      // Never optimize on a validation run.
      jjsOptions.setOptimizePrecompile(false);
      JavaToJavaScriptCompiler.precompile(logger, module, rpo, declEntryPts,
          additionalRootTypes, jjsOptions, true);
      return true;
    } catch (UnableToCompleteException e) {
      // Already logged.
      return false;
    }
  }

  private static Precompilation precompile(TreeLogger logger,
      JJSOptions jjsOptions, ModuleDef module, int permutationBase,
      int firstPerm, int numPerms, File genDir, File generatorResourcesDir,
      File dumpSignatureFile) {

    try {
      CompilationState compilationState = module.getCompilationState(logger);
      if (dumpSignatureFile != null) {
        // Dump early to avoid generated types.
        SignatureDumper.dumpSignatures(logger, module.getTypeOracle(logger),
            dumpSignatureFile);
      }

      String[] declEntryPts = module.getEntryPointTypeNames();
      if (declEntryPts.length == 0) {
        logger.log(TreeLogger.ERROR, "Module has no entry points defined", null);
        throw new UnableToCompleteException();
      }

      ArtifactSet generatedArtifacts = new ArtifactSet();
      DistillerRebindPermutationOracle rpo = new DistillerRebindPermutationOracle(
          module,
          compilationState,
          generatedArtifacts,
          new PropertyPermutations(module.getProperties(), firstPerm, numPerms),
          genDir, generatorResourcesDir);
      PerfLogger.start("Precompile");
      UnifiedAst unifiedAst = JavaToJavaScriptCompiler.precompile(logger,
          module, rpo, declEntryPts, null, jjsOptions,
          rpo.getPermuationCount() == 1);
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
      return new Precompilation(unifiedAst, merged.values(), permutationBase,
          generatedArtifacts);
    } catch (UnableToCompleteException e) {
      // We intentionally don't pass in the exception here since the real
      // cause has been logged.
      return null;
    }
  }

  private final PrecompileOptionsImpl options;

  public Precompile(PrecompileOptions options) {
    this.options = new PrecompileOptionsImpl(options);
  }

  public boolean run(TreeLogger logger) throws UnableToCompleteException {
    boolean originalCompilationStateRetained = options.isCompilationStateRetained();
    // Avoid early optimizations since permutation compiles will run separately.
    options.setOptimizePrecompile(false);

    for (String moduleName : options.getModuleNames()) {
      File compilerWorkDir = options.getCompilerWorkDir(moduleName);
      Util.recursiveDelete(compilerWorkDir, true);
      // No need to check mkdirs result because an IOException will occur anyway
      compilerWorkDir.mkdirs();

      JarOutputStream precompilationJar;
      try {
        precompilationJar = new JarOutputStream(new FileOutputStream(new File(
            compilerWorkDir, PRECOMPILE_FILENAME)));
      } catch (IOException e) {
        logger.log(TreeLogger.ERROR, "Could not create " + PRECOMPILE_FILENAME,
            e);
        return false;
      }

      ModuleDef module = ModuleDefLoader.loadFromClassPath(logger, moduleName);

      // TODO: All JDT checks now before even building TypeOracle?
      module.getCompilationState(logger);

      if (options.isValidateOnly()) {
        TreeLogger branch = logger.branch(TreeLogger.INFO,
            "Validating compilation " + module.getName());
        if (!validate(branch, options, module, options.getGenDir(),
            compilerWorkDir, options.getDumpSignatureFile())) {
          branch.log(TreeLogger.ERROR, "Validation failed");
          return false;
        }
        branch.log(TreeLogger.INFO, "Validation succeeded");
      } else {
        TreeLogger branch = logger.branch(TreeLogger.INFO,
            "Precompiling module " + module.getName());
        int potentialPermutations = module.getProperties().numPermutations();
        int permutationsPerIteration = options.getMaxPermsPerPrecompile();

        if (permutationsPerIteration <= 0) {
          permutationsPerIteration = potentialPermutations;
        }
        /*
         * The potential number of permutations to precompile >= the actual
         * number of permutations that end up being precompiled, because some of
         * the permutations might collapse due to identical rebind results. So
         * we have to track these two counts and ids separately.
         */
        int actualPermutations = 0;
        for (int potentialFirstPerm = 0; potentialFirstPerm < potentialPermutations; potentialFirstPerm += permutationsPerIteration) {
          int numPermsToPrecompile = Math.min(potentialPermutations
              - potentialFirstPerm, permutationsPerIteration);

          /*
           * After the first precompile, we need to forcibly refresh
           * CompilationState to clear out generated units from previous
           * precompilations.
           */
          if (potentialFirstPerm != 0) {
            module.getCompilationState(branch).refresh(branch);
          }

          if (potentialFirstPerm + numPermsToPrecompile < potentialPermutations) {
            /*
             * On all iterations but the last, force retainCompilationState to
             * be true. Otherwise, state will be discarded that is needed on
             * later iterations.
             */
            options.setCompilationStateRetained(true);
          } else {
            // On the last iteration, use whatever the original setting was
            options.setCompilationStateRetained(originalCompilationStateRetained);
          }

          Precompilation precompilation = precompile(branch, options, module,
              actualPermutations, potentialFirstPerm, numPermsToPrecompile,
              options.getGenDir(), compilerWorkDir,
              options.getDumpSignatureFile());
          if (precompilation == null) {
            branch.log(TreeLogger.ERROR, "Precompilation failed");
            return false;
          }
          int actualNumPermsPrecompiled = precompilation.getPermutations().length;
          String precompilationFilename = PrecompilationFile.fileNameForPermutations(
              actualPermutations, actualNumPermsPrecompiled);
          try {
            precompilationJar.putNextEntry(new ZipEntry(precompilationFilename));
            Util.writeObjectToStream(precompilationJar, precompilation);
          } catch (IOException e) {
            branch.log(TreeLogger.ERROR,
                "Failed to write a precompilation result", e);
            return false;
          }

          actualPermutations += actualNumPermsPrecompiled;
          branch.log(TreeLogger.DEBUG, "Compiled " + actualNumPermsPrecompiled
              + " permutations starting from " + potentialFirstPerm);
        }

        try {
          precompilationJar.close();
        } catch (IOException e) {
          branch.log(TreeLogger.ERROR, "Failed to finalize "
              + PRECOMPILE_FILENAME, e);
          return false;
        }

        Util.writeStringAsFile(branch, new File(compilerWorkDir,
            PERM_COUNT_FILENAME), String.valueOf(actualPermutations));
        branch.log(TreeLogger.INFO,
            "Precompilation succeeded, number of permutations: "
                + actualPermutations);
      }
    }
    return true;
  }
}
