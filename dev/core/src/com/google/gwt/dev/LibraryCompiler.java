/*
 * Copyright 2014 Google Inc.
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
import com.google.gwt.dev.cfg.Libraries.IncompatibleLibraryVersionException;
import com.google.gwt.dev.cfg.Library;
import com.google.gwt.dev.cfg.LibraryGroup;
import com.google.gwt.dev.cfg.LibraryWriter;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.ModuleDefLoader;
import com.google.gwt.dev.cfg.ResourceLoader;
import com.google.gwt.dev.cfg.ResourceLoaders;
import com.google.gwt.dev.cfg.ZipLibrary;
import com.google.gwt.dev.cfg.ZipLibraryWriter;
import com.google.gwt.dev.javac.UnitCacheSingleton;
import com.google.gwt.dev.jjs.JsOutputOption;
import com.google.gwt.dev.jjs.PermutationResult;
import com.google.gwt.dev.url.CloseableJarHandlerFactory;
import com.google.gwt.dev.util.Memory;
import com.google.gwt.dev.util.PersistenceBackedObject;
import com.google.gwt.dev.util.TinyCompileSummary;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.arg.ArgHandlerDeployDir;
import com.google.gwt.dev.util.arg.ArgHandlerExtraDir;
import com.google.gwt.dev.util.arg.ArgHandlerLibraries;
import com.google.gwt.dev.util.arg.ArgHandlerLink;
import com.google.gwt.dev.util.arg.ArgHandlerLocalWorkers;
import com.google.gwt.dev.util.arg.ArgHandlerOutputLibrary;
import com.google.gwt.dev.util.arg.ArgHandlerSaveSourceOutput;
import com.google.gwt.dev.util.arg.ArgHandlerWarDir;
import com.google.gwt.dev.util.arg.ArgHandlerWorkDirOptional;
import com.google.gwt.dev.util.arg.OptionOptimize;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;
import com.google.gwt.thirdparty.guava.common.base.Preconditions;
import com.google.gwt.thirdparty.guava.common.base.Strings;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Sets;
import com.google.gwt.util.tools.Utility;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Executable compiler entry point that constructs a library from an input module and its library
 * dependencies.
 * <p>
 * When compiling the top-level module, you should also pass the -link and -war flags to create the
 * GWT application.
 * <p>
 * This is an alternative to Compiler.java which does a full world compile that neither reads nor
 * writes libraries.
 */
public class LibraryCompiler {

  private static class ArgProcessor extends PrecompileTaskArgProcessor {
    public ArgProcessor(CompilerOptions options) {
      super(options);

      registerHandler(new ArgHandlerLocalWorkers(options));

      // Override the ArgHandlerWorkDirRequired in the super class.
      registerHandler(new ArgHandlerWorkDirOptional(options));
      registerHandler(new ArgHandlerWarDir(options));
      registerHandler(new ArgHandlerDeployDir(options));
      registerHandler(new ArgHandlerExtraDir(options));

      registerHandler(new ArgHandlerLink(options));
      registerHandler(new ArgHandlerOutputLibrary(options));
      registerHandler(new ArgHandlerLibraries(options));

      registerHandler(new ArgHandlerSaveSourceOutput(options));
    }

    @Override
    protected String getName() {
      return LibraryCompiler.class.getName();
    }
  }

  public static void main(String[] args) {
    Memory.initialize();
    SpeedTracerLogger.init();
    final CompilerOptions compilerOptions = new CompilerOptionsImpl();
    ArgProcessor argProcessor = new ArgProcessor(compilerOptions);

    if (argProcessor.processArgs(args)) {
      CompileTask compileTask = new CompileTask() {
        @Override
        public boolean run(TreeLogger logger) throws UnableToCompleteException {
          return new LibraryCompiler(compilerOptions).run(logger);
        }
      };
      boolean success = CompileTaskRunner.runWithAppropriateLogger(compilerOptions, compileTask);
      System.exit(success ? 0 : 1);
    }
  }

  private CompilerContext compilerContext;
  private final CompilerContext.Builder compilerContextBuilder = new CompilerContext.Builder();
  private final CompilerOptionsImpl compilerOptions;
  private ArtifactSet generatedArtifacts;
  private LibraryGroup libraryGroup;
  private ModuleDef module;
  private Permutation[] permutations;
  private ResourceLoader resourceLoader = ResourceLoaders.forClassLoader(Thread.currentThread());
  private Library resultLibrary;

  public LibraryCompiler(CompilerOptions compilerOptions) {
    this.compilerOptions = new CompilerOptionsImpl(compilerOptions);
    this.compilerContext =
        compilerContextBuilder.options(this.compilerOptions).compileMonolithic(false).build();
    CloseableJarHandlerFactory.installOverride();
  }

  ModuleDef getModule() {
    return module;
  }

  boolean run(TreeLogger logger) {
    try {
      normalizeOptions(logger);
      loadLibraries(logger);
      loadModule(logger);
      compileModule(logger);
      if (compilerOptions.shouldLink()) {
        linkLibraries(logger);
      }
      return true;
    } catch (UnableToCompleteException e) {
      // The real cause has been logged.
      return false;
    } finally {
      // Close all zip files, otherwise the JVM may crash on linux.
      compilerContext.getLibraryGroup().close();
      if (resultLibrary != null) {
        resultLibrary.close();
      }
    }
  }

  void setResourceLoader(ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
  }

  private void compileModule(TreeLogger logger) throws UnableToCompleteException {
    long beforeCompileMs = System.currentTimeMillis();
    LibraryWriter libraryWriter = compilerContext.getLibraryWriter();

    try {
      if (compilerOptions.isValidateOnly()) {
        boolean valid = Precompile.validate(logger, compilerContext);
        if (!valid) {
          // The real cause has been logged.
          throw new UnableToCompleteException();
        }
      }

      Precompilation precompilation = Precompile.precompile(logger, compilerContext);
      if (precompilation == null) {
        // The real cause has been logged.
        throw new UnableToCompleteException();
      }
      // TODO(stalcup): move to precompile() after params are refactored
      if (!compilerOptions.shouldSaveSource()) {
        precompilation.removeSourceArtifacts(logger);
      }

      Event compilePermutationsEvent =
          SpeedTracerLogger.start(CompilerEventType.COMPILE_PERMUTATIONS);
      permutations = new Permutation[] {precompilation.getPermutations()[0]};
      List<PersistenceBackedObject<PermutationResult>> permutationResultFiles =
          new ArrayList<PersistenceBackedObject<PermutationResult>>();
      permutationResultFiles.add(libraryWriter.getPermutationResultHandle());
      CompilePerms.compile(logger, compilerContext, precompilation, permutations,
          compilerOptions.getLocalWorkers(), permutationResultFiles);
      compilePermutationsEvent.end();

      generatedArtifacts = precompilation.getGeneratedArtifacts();
      libraryWriter.addGeneratedArtifacts(generatedArtifacts);
    } finally {
      // Even if a compile problem occurs, close the library cleanly so that it can be examined.
      libraryWriter.write();
    }

    long durationMs = System.currentTimeMillis() - beforeCompileMs;
    TreeLogger detailBranch = logger.branch(TreeLogger.INFO,
        String.format("%.3fs -- Translating Java to Javascript", durationMs / 1000d));

    TinyCompileSummary tinyCompileSummary = compilerContext.getTinyCompileSummary();
    boolean shouldWarn =
        tinyCompileSummary.getTypesForGeneratorsCount() + tinyCompileSummary.getTypesForAstCount()
        > 1500;
    String recommendation = shouldWarn ? " This module should probably be split into smaller "
        + "modules or should trigger fewer generators since its current size hurts "
        + "incremental compiles." : "";
    detailBranch.log(shouldWarn ? TreeLogger.WARN : TreeLogger.INFO, String.format(
        "There were %s static source files, %s generated source files, %s types loaded for "
        + "generators and %s types loaded for AST construction. %s",
        tinyCompileSummary.getStaticSourceFilesCount(),
        tinyCompileSummary.getGeneratedSourceFilesCount(),
        tinyCompileSummary.getTypesForGeneratorsCount(), tinyCompileSummary.getTypesForAstCount(),
        recommendation));
  }

  private void linkLibraries(TreeLogger logger) throws UnableToCompleteException {
    long beforeLinkMs = System.currentTimeMillis();
    Event linkEvent = SpeedTracerLogger.start(CompilerEventType.LINK);

    // Load up the library that was just created so that it's possible to get a read-reference to
    // its contained PermutationResult.
    try {
      resultLibrary = new ZipLibrary(compilerOptions.getOutputLibraryPath());
    } catch (IncompatibleLibraryVersionException e) {
      logger.log(TreeLogger.ERROR, e.getMessage());
      throw new UnableToCompleteException();
    }
    generatedArtifacts.addAll(libraryGroup.getGeneratedArtifacts());

    Set<PermutationResult> libraryPermutationResults = Sets.newLinkedHashSet();
    List<PersistenceBackedObject<PermutationResult>> resultFiles = Lists.newArrayList();
    resultFiles.add(resultLibrary.getPermutationResultHandle());
    List<PersistenceBackedObject<PermutationResult>> permutationResultHandles =
        libraryGroup.getPermutationResultHandlesInLinkOrder();
    for (PersistenceBackedObject<PermutationResult> permutationResultHandle :
        permutationResultHandles) {
      libraryPermutationResults.add(permutationResultHandle.newInstance(logger));
    }

    try {
      Link.link(TreeLogger.NULL, module, compilerContext.getPublicResourceOracle(),
          generatedArtifacts, permutations, resultFiles, libraryPermutationResults, compilerOptions,
          compilerOptions);
      long durationMs = System.currentTimeMillis() - beforeLinkMs;
      logger.log(TreeLogger.INFO,
          String.format("%.3fs -- Successfully linking application", durationMs / 1000d));
    } catch (IOException e) {
      long durationMs = System.currentTimeMillis() - beforeLinkMs;
      logger.log(TreeLogger.INFO,
          String.format("%.3fs -- Failing to link application", durationMs / 1000d));
      throw new UnableToCompleteException();
    }
    linkEvent.end();
  }

  private void loadLibraries(TreeLogger logger) throws UnableToCompleteException {
    try {
      libraryGroup = LibraryGroup.fromZipPaths(compilerOptions.getLibraryPaths());
    } catch (IncompatibleLibraryVersionException e) {
      logger.log(TreeLogger.ERROR, e.getMessage());
      throw new UnableToCompleteException();
    }
    libraryGroup.verify(logger);

    try {
      CloseableJarHandlerFactory.closeStreams(compilerOptions.getOutputLibraryPath());
    } catch (IOException e) {
      logger.log(TreeLogger.WARN, String.format("Failed to close old connections to %s. "
          + "Repeated incremental compiles in the same JVM process may fail.",
          compilerOptions.getOutputLibraryPath()));
    }

    ZipLibraryWriter zipLibraryWriter =
        new ZipLibraryWriter(compilerOptions.getOutputLibraryPath());
    compilerContext = compilerContextBuilder.libraryGroup(libraryGroup).libraryWriter(
        zipLibraryWriter).unitCache(UnitCacheSingleton.get(logger, compilerOptions.getWorkDir()))
        .build();
  }

  private void loadModule(TreeLogger logger) throws UnableToCompleteException {
    long beforeLoadModuleMs = System.currentTimeMillis();
    module = ModuleDefLoader.loadFromResources(logger, compilerContext,
        compilerOptions.getModuleNames().get(0), resourceLoader, false);
    compilerContext = compilerContextBuilder.module(module).build();
    long durationMs = System.currentTimeMillis() - beforeLoadModuleMs;
    logger.log(TreeLogger.INFO,
        String.format("%.3fs -- Parsing and loading module definition", durationMs / 1000d));
  }

  private void normalizeOptions(TreeLogger logger) throws UnableToCompleteException {
    Preconditions.checkArgument(compilerOptions.getModuleNames().size() == 1);

    // Fail early on errors to avoid confusion later.
    compilerOptions.setStrict(true);
    // Current optimization passes are not safe with only partial data.
    compilerOptions.setOptimizationLevel(OptionOptimize.OPTIMIZE_LEVEL_DRAFT);
    // Protects against rampant overlapping source inclusion.
    compilerOptions.setEnforceStrictSourceResources(true);
    // Ensures that output JS identifiers are named consistently in all modules.
    compilerOptions.setOutput(JsOutputOption.DETAILED);
    // Code splitting isn't possible when you can't trace the entire control flow.
    compilerOptions.setRunAsyncEnabled(false);
    compilerOptions.setClosureCompilerEnabled(false);
    if (compilerOptions.getWorkDir() == null) {
      try {
        compilerOptions.setWorkDir(Utility.makeTemporaryDirectory(null, "gwtc"));
        Runtime.getRuntime().addShutdownHook(new Thread() {
          @Override
          public void run() {
            Util.recursiveDelete(compilerOptions.getWorkDir(), false);
          }
        });
      } catch (IOException e) {
        logger.log(TreeLogger.ERROR, e.getMessage());
        throw new UnableToCompleteException();
      }
    }
    if ((compilerOptions.isSoycEnabled() || compilerOptions.isJsonSoycEnabled())
        && compilerOptions.getExtraDir() == null) {
      compilerOptions.setExtraDir(new File("extras"));
    }
    if (Strings.isNullOrEmpty(compilerOptions.getOutputLibraryPath())) {
      compilerOptions.setOutputLibraryPath(compilerOptions.getWorkDir().getPath() + "/"
          + compilerOptions.getModuleNames().get(0) + ".gwtlib");
    }
    // Optimize early since permutation compiles will run in process.
    compilerOptions.setOptimizePrecompile(true);
  }
}
