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

import com.google.gwt.core.ext.Linker;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.ModuleMetricsArtifact;
import com.google.gwt.core.ext.linker.PrecompilationMetricsArtifact;
import com.google.gwt.core.ext.linker.impl.StandardLinkerContext;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.dev.CompileTaskRunner.CompileTask;
import com.google.gwt.dev.cfg.ConfigurationProperty;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.ModuleDefLoader;
import com.google.gwt.dev.cfg.PropertyPermutations;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.CompilationUnit;
import com.google.gwt.dev.jjs.AbstractCompiler;
import com.google.gwt.dev.jjs.JJSOptions;
import com.google.gwt.dev.jjs.JavaScriptCompiler;
import com.google.gwt.dev.jjs.UnifiedAst;
import com.google.gwt.dev.shell.CheckForUpdates;
import com.google.gwt.dev.shell.CheckForUpdates.UpdateResult;
import com.google.gwt.dev.util.CollapsedPropertyKey;
import com.google.gwt.dev.util.Memory;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.collect.Lists;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.concurrent.FutureTask;

/**
 * Performs the first phase of compilation, generating the set of permutations
 * to compile, and a ready-to-compile AST.
 */
public class Precompile {
  /**
   * The file name for the max number of permutations output as plain text.
   */
  static final String PERM_COUNT_FILENAME = "permCount.txt";

  static final String PRECOMPILE_FILENAME = Precompile.PRECOMPILE_FILENAME_PREFIX
      + Precompile.PRECOMPILE_FILENAME_SUFFIX;

  /**
   * The file name for the serialized AST artifact from the Precompile step.
   * Sometimes this file is overloaded and only contains a PrecompileOptions
   * object to indicate that precompilation should run inside the CompilePerms
   * step.
   */
  static final String PRECOMPILE_FILENAME_PREFIX = "precompilation";

  static final String PRECOMPILE_FILENAME_SUFFIX = ".ser";

  /**
   * Performs a command-line precompile.
   */
  public static void main(String[] args) {
    Memory.initialize();
    SpeedTracerLogger.init();
    Event precompileEvent = SpeedTracerLogger.start(CompilerEventType.PRECOMPILE);
    if (System.getProperty("gwt.jjs.dumpAst") != null) {
      System.out.println("Will dump AST to: " + System.getProperty("gwt.jjs.dumpAst"));
    }

    /*
     * NOTE: main always exits with a call to System.exit to terminate any
     * non-daemon threads that were started in Generators. Typically, this is to
     * shutdown AWT related threads, since the contract for their termination is
     * still implementation-dependent.
     */
    final PrecompileTaskOptions options = new PrecompileTaskOptionsImpl();
    boolean success = false;
    if (new PrecompileTaskArgProcessor(options).processArgs(args)) {
      CompileTask task = new CompileTask() {
        @Override
        public boolean run(TreeLogger logger) throws UnableToCompleteException {
          FutureTask<UpdateResult> updater = null;
          if (!options.isUpdateCheckDisabled()) {
            updater =
                CheckForUpdates.checkForUpdatesInBackgroundThread(logger, CheckForUpdates.ONE_DAY);
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
        success = true;
      }
    }
    precompileEvent.end();
    System.exit(success ? 0 : 1);
  }

  /**
   * Precompiles the given module.
   * 
   * @param logger a logger to use
   * @param jjsOptions a set of compiler options
   * @param module the module to compile
   * @param genDir optional directory to dump generated source, may be
   *          <code>null</code>
   * @return the precompilation
   */
  public static Precompilation precompile(TreeLogger logger, JJSOptions jjsOptions,
      ModuleDef module, File genDir) {
    PropertyPermutations allPermutations =
        new PropertyPermutations(module.getProperties(), module.getActiveLinkerNames());
    return precompile(logger, jjsOptions, module, 0, allPermutations, genDir);
  }

  /**
   * Validates the given module can be compiled.
   * 
   * @param logger a logger to use
   * @param jjsOptions a set of compiler options
   * @param module the module to compile
   * @param genDir optional directory to dump generated source, may be
   *          <code>null</code>
   */
  public static boolean validate(TreeLogger logger, JJSOptions jjsOptions, ModuleDef module,
      File genDir) {
    Event validateEvent = SpeedTracerLogger.start(CompilerEventType.VALIDATE);
    try {
      CompilationState compilationState =
          module.getCompilationState(logger, !jjsOptions.isStrict());
      if (jjsOptions.isStrict() && compilationState.hasErrors()) {
        abortDueToStrictMode(logger);
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
      DistillerRebindPermutationOracle rpo =
          new DistillerRebindPermutationOracle(module, compilationState, generatorArtifacts,
              new PropertyPermutations(module.getProperties(), module.getActiveLinkerNames()),
              genDir);
      // Allow GC later.
      compilationState = null;
      // Never optimize on a validation run.
      jjsOptions.setOptimizePrecompile(false);
      getCompiler(module).precompile(logger, module, rpo, declEntryPts, additionalRootTypes,
          jjsOptions, true, null);
      return true;
    } catch (UnableToCompleteException e) {
      // Already logged.
      return false;
    } finally {
      validateEvent.end();
    }
  }

  /**
   * Create a list of all possible permutations configured for this module after
   * collapsing soft permutations.
   */
  static List<PropertyPermutations> getCollapsedPermutations(ModuleDef module) {
    PropertyPermutations allPermutations =
        new PropertyPermutations(module.getProperties(), module.getActiveLinkerNames());
    List<PropertyPermutations> collapsedPermutations = allPermutations.collapseProperties();
    return collapsedPermutations;
  }

  static AbstractCompiler getCompiler(ModuleDef module) {
    ConfigurationProperty compilerClassProp =
        module.getProperties().createConfiguration("x.compiler.class", false);
    String compilerClassName = compilerClassProp.getValue();
    if (compilerClassName == null || compilerClassName.length() == 0) {
      return new JavaScriptCompiler();
    }
    Throwable caught;
    try {
      Class<?> compilerClass = Class.forName(compilerClassName);
      return (AbstractCompiler) compilerClass.newInstance();
    } catch (ClassNotFoundException e) {
      caught = e;
    } catch (InstantiationException e) {
      caught = e;
    } catch (IllegalAccessException e) {
      caught = e;
    }
    throw new RuntimeException("Unable to instantiate compiler class '" + compilerClassName + "'",
        caught);
  }

  static Precompilation precompile(TreeLogger logger, JJSOptions jjsOptions, ModuleDef module,
      int permutationBase, PropertyPermutations allPermutations, File genDir) {
    return precompile(logger, jjsOptions, module, permutationBase, allPermutations, genDir,
        ManagementFactory.getRuntimeMXBean().getStartTime());
  }

  static Precompilation precompile(TreeLogger logger, JJSOptions jjsOptions, ModuleDef module,
      int permutationBase, PropertyPermutations allPermutations, File genDir,
      long startTimeMilliseconds) {

    Event precompileEvent = SpeedTracerLogger.start(CompilerEventType.PRECOMPILE);

    // This initializes the Java2D library in a thread so that the main program
    // doesn't block when the library is accessed for the first time.
    new GraphicsInitThread().start();

    ArchivePreloader.preloadArchives(logger, module);

    try {
      CompilationState compilationState =
          module.getCompilationState(logger, !jjsOptions.isStrict());
      if (jjsOptions.isStrict() && compilationState.hasErrors()) {
        abortDueToStrictMode(logger);
      }

      List<String> initialTypeOracleTypes = new ArrayList<String>();
      if (jjsOptions.isCompilerMetricsEnabled()) {
        for (JClassType type : compilationState.getTypeOracle().getTypes()) {
          initialTypeOracleTypes.add(type.getPackage().getName() + "." + type.getName());
        }
      }

      // Track information about the module load including initial type
      // oracle build for diagnostic purposes.
      long moduleLoadFinished = System.currentTimeMillis();

      String[] declEntryPts = module.getEntryPointTypeNames();
      if (declEntryPts.length == 0) {
        logger.log(TreeLogger.ERROR, "Module has no entry points defined", null);
        throw new UnableToCompleteException();
      }

      ArtifactSet generatedArtifacts = new ArtifactSet();
      DistillerRebindPermutationOracle rpo =
          new DistillerRebindPermutationOracle(module, compilationState, generatedArtifacts,
              allPermutations, genDir);
      // Allow GC later.
      compilationState = null;
      PrecompilationMetricsArtifact precompilationMetrics =
          jjsOptions.isCompilerMetricsEnabled()
              ? new PrecompilationMetricsArtifact(permutationBase) : null;
      UnifiedAst unifiedAst =
          getCompiler(module).precompile(logger, module, rpo, declEntryPts, null, jjsOptions,
              rpo.getPermuationCount() == 1, precompilationMetrics);

      if (jjsOptions.isCompilerMetricsEnabled()) {
        ModuleMetricsArtifact moduleMetrics = new ModuleMetricsArtifact();
        moduleMetrics.setSourceFiles(module.getAllSourceFiles());
        // The initial type list has to be gathered before the call to
        // precompile().
        moduleMetrics.setInitialTypes(initialTypeOracleTypes);
        // The elapsed time in ModuleMetricsArtifact represents time
        // which could be done once for all permutations.
        moduleMetrics.setElapsedMilliseconds(moduleLoadFinished - startTimeMilliseconds);
        unifiedAst.setModuleMetrics(moduleMetrics);
      }

      // Merge all identical permutations together.
      List<Permutation> permutations =
          new ArrayList<Permutation>(Arrays.asList(rpo.getPermutations()));

      mergeCollapsedPermutations(permutations);

      // Sort the permutations by an ordered key to ensure determinism.
      SortedMap<RebindAnswersPermutationKey, Permutation> merged =
          new TreeMap<RebindAnswersPermutationKey, Permutation>();
      SortedSet<String> liveRebindRequests = unifiedAst.getRebindRequests();
      for (Permutation permutation : permutations) {
        // Construct a key for the live rebind answers.
        RebindAnswersPermutationKey key =
            new RebindAnswersPermutationKey(permutation, liveRebindRequests);
        if (merged.containsKey(key)) {
          Permutation existing = merged.get(key);
          existing.mergeFrom(permutation, liveRebindRequests);
        } else {
          merged.put(key, permutation);
        }
      }
      if (jjsOptions.isCompilerMetricsEnabled()) {
        int[] ids = new int[allPermutations.size()];
        for (int i = 0; i < allPermutations.size(); i++) {
          ids[i] = permutationBase + i;
        }
        precompilationMetrics.setPermuationIds(ids);
        // TODO(zundel): Right now this double counts module load and
        // precompile time. It correctly counts the amount of time spent
        // in this process. The elapsed time in ModuleMetricsArtifact
        // represents time which could be done once for all permutations.
        precompilationMetrics.setElapsedMilliseconds(System.currentTimeMillis()
            - startTimeMilliseconds);
        unifiedAst.setPrecompilationMetrics(precompilationMetrics);
      }
      return new Precompilation(unifiedAst, merged.values(), permutationBase, generatedArtifacts);
    } catch (UnableToCompleteException e) {
      // We intentionally don't pass in the exception here since the real
      // cause has been logged.
      return null;
    } finally {
      precompileEvent.end();
    }
  }

  private static void abortDueToStrictMode(TreeLogger logger) throws UnableToCompleteException {
    logger.log(TreeLogger.ERROR, "Aborting compile due to errors in some input files");
    throw new UnableToCompleteException();
  }

  /**
   * This merges Permutations that can be considered equivalent by considering
   * their collapsed properties. The list passed into this method may have
   * elements removed from it.
   */
  private static void mergeCollapsedPermutations(List<Permutation> permutations) {
    if (permutations.size() < 2) {
      return;
    }

    // See the doc for CollapsedPropertyKey
    SortedMap<CollapsedPropertyKey, List<Permutation>> mergedByCollapsedProperties =
        new TreeMap<CollapsedPropertyKey, List<Permutation>>();

    // This loop creates the equivalence sets
    for (Iterator<Permutation> it = permutations.iterator(); it.hasNext();) {
      Permutation entry = it.next();
      CollapsedPropertyKey key = new CollapsedPropertyKey(entry);

      List<Permutation> equivalenceSet = mergedByCollapsedProperties.get(key);
      if (equivalenceSet == null) {
        equivalenceSet = Lists.create();
      } else {
        // Mutate list
        it.remove();
        equivalenceSet = Lists.add(equivalenceSet, entry);
      }
      mergedByCollapsedProperties.put(key, equivalenceSet);
    }

    // This loop merges the Permutations together
    for (Map.Entry<CollapsedPropertyKey, List<Permutation>> entry : mergedByCollapsedProperties
        .entrySet()) {
      Permutation mergeInto = entry.getKey().getPermutation();

      /*
       * Merge the deferred-binding properties once we no longer need the
       * PropertyOracle data from the extra permutations.
       */
      for (Permutation mergeFrom : entry.getValue()) {
        mergeInto.mergeRebindsFromCollapsed(mergeFrom);
      }
    }

    // Renumber the Permutations
    for (int i = 0, j = permutations.size(); i < j; i++) {
      permutations.set(i, new Permutation(i, permutations.get(i)));
    }
  }

  private final PrecompileTaskOptionsImpl options;

  public Precompile(PrecompileTaskOptions options) {
    this.options = new PrecompileTaskOptionsImpl(options);
  }

  public boolean run(TreeLogger logger) throws UnableToCompleteException {
    // Avoid early optimizations since permutation compiles will run
    // separately.
    options.setOptimizePrecompile(false);

    for (String moduleName : options.getModuleNames()) {
      File compilerWorkDir = options.getCompilerWorkDir(moduleName);
      Util.recursiveDelete(compilerWorkDir, true);
      // No need to check mkdirs result because an IOException will occur
      // anyway.
      compilerWorkDir.mkdirs();

      File precompilationFile = new File(compilerWorkDir, PRECOMPILE_FILENAME);

      ModuleDef module = ModuleDefLoader.loadFromClassPath(logger, moduleName);

      StandardLinkerContext linkerContext =
          new StandardLinkerContext(TreeLogger.NULL, module, options);

      boolean generateOnShards = true;

      if (!options.isEnabledGeneratingOnShards()) {
        logger.log(TreeLogger.INFO, "Precompiling on the start node");
        generateOnShards = false;
      } else if (!linkerContext.allLinkersAreShardable()) {
        TreeLogger legacyLinkersLogger =
            logger.branch(TreeLogger.INFO,
                "Precompiling on the start node, because some linkers are not updated");
        if (legacyLinkersLogger.isLoggable(TreeLogger.INFO)) {
          for (Linker linker : linkerContext.findUnshardableLinkers()) {
            legacyLinkersLogger.log(TreeLogger.INFO, "Linker"
                + linker.getClass().getCanonicalName() + " is not updated");
          }
        }
        generateOnShards = false;
      } else if (options.isValidateOnly()) {
        // Don't bother running on shards for just a validation run
        generateOnShards = false;
      }

      if (generateOnShards) {
        /*
         * Pre-precompile. Count the permutations and plan to do a real
         * precompile in the CompilePerms shards.
         */
        TreeLogger branch =
            logger.branch(TreeLogger.INFO, "Precompiling (minimal) module " + module.getName());
        Util.writeObjectAsFile(logger, precompilationFile, options);
        int numPermutations =
            new PropertyPermutations(module.getProperties(), module.getActiveLinkerNames())
                .collapseProperties().size();
        Util.writeStringAsFile(logger, new File(compilerWorkDir, PERM_COUNT_FILENAME), String
            .valueOf(numPermutations));
        if (branch.isLoggable(TreeLogger.INFO)) {
          branch.log(TreeLogger.INFO,
              "Precompilation (minimal) succeeded, number of permutations: " + numPermutations);
        }
      } else {
        if (options.isValidateOnly()) {
          TreeLogger branch =
              logger.branch(TreeLogger.INFO, "Validating compilation " + module.getName());
          if (!validate(branch, options, module, options.getGenDir())) {
            branch.log(TreeLogger.ERROR, "Validation failed");
            return false;
          }
          branch.log(TreeLogger.INFO, "Validation succeeded");
        } else {
          TreeLogger branch =
              logger.branch(TreeLogger.INFO, "Precompiling module " + module.getName());

          Precompilation precompilation = precompile(branch, options, module, options.getGenDir());
          if (precompilation == null) {
            branch.log(TreeLogger.ERROR, "Precompilation failed");
            return false;
          }
          Util.writeObjectAsFile(logger, precompilationFile, precompilation);

          int permsPrecompiled = precompilation.getPermutations().length;
          Util.writeStringAsFile(logger, new File(compilerWorkDir, PERM_COUNT_FILENAME), String
              .valueOf(permsPrecompiled));
          if (branch.isLoggable(TreeLogger.INFO)) {
            branch.log(TreeLogger.INFO, "Precompilation succeeded, number of permutations: "
                + permsPrecompiled);
          }
        }
      }
    }
    return true;
  }
}
