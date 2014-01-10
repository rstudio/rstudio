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
import com.google.gwt.dev.cfg.LibraryGroup;
import com.google.gwt.dev.cfg.LibraryWriter;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.ModuleDefLoader;
import com.google.gwt.dev.cfg.ZipLibraryWriter;
import com.google.gwt.dev.javac.LibraryGroupUnitCache;
import com.google.gwt.dev.jjs.PermutationResult;
import com.google.gwt.dev.util.Memory;
import com.google.gwt.dev.util.PersistenceBackedObject;
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
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;
import com.google.gwt.thirdparty.guava.common.base.Preconditions;
import com.google.gwt.thirdparty.guava.common.base.Strings;
import com.google.gwt.thirdparty.guava.common.collect.Sets;
import com.google.gwt.util.tools.Utility;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Executable compiler entry point that constructs a library from an input module and its library
 * dependencies.<br />
 *
 * When compiling the top-level module, you should also pass the -link and -war flags to create the
 * GWT application.<br />
 *
 * This is an alternative to Compiler.java which does a full world compile that neither reads nor
 * writes libraries.<br />
 *
 * EXPERIMENTAL: does not yet work as it depends on changes which have not yet been reviewed and
 * committed.
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
  private List<PersistenceBackedObject<PermutationResult>> permutationResultFiles;
  private Permutation[] permutations;

  public LibraryCompiler(CompilerOptions compilerOptions) {
    this.compilerOptions = new CompilerOptionsImpl(compilerOptions);
    this.compilerContext = compilerContextBuilder.options(compilerOptions).build();
  }

  private void compileModule(TreeLogger logger) throws UnableToCompleteException {
    long beforeCompileMs = System.currentTimeMillis();
    TreeLogger branch =
        logger.branch(TreeLogger.INFO, "Compiling module " + module.getCanonicalName());
    LibraryWriter libraryWriter = compilerContext.getLibraryWriter();

    if (compilerOptions.isValidateOnly()) {
      boolean valid = Precompile.validate(logger, compilerContext);
      if (!valid) {
        // The real cause has been logged.
        throw new UnableToCompleteException();
      }
    }

    Precompilation precompilation = Precompile.precompile(branch, compilerContext);
    // TODO(stalcup): move to precompile() after params are refactored
    if (!compilerOptions.shouldSaveSource()) {
      precompilation.removeSourceArtifacts(logger);
    }

    permutations = new Permutation[] {precompilation.getPermutations()[0]};
    permutationResultFiles = new ArrayList<PersistenceBackedObject<PermutationResult>>();
    permutationResultFiles.add(libraryWriter.getPermutationResultHandle());
    CompilePerms.compile(
        branch, compilerContext, precompilation, permutations, compilerOptions.getLocalWorkers(),
        permutationResultFiles);

    generatedArtifacts = precompilation.getGeneratedArtifacts();
    libraryWriter.addGeneratedArtifacts(generatedArtifacts);

    // Save and close the current library.
    libraryWriter.write();

    long durationMs = System.currentTimeMillis() - beforeCompileMs;
    branch.log(TreeLogger.INFO,
        "Library compilation succeeded -- " + String.format("%.3f", durationMs / 1000d) + "s");
  }

  private void linkLibraries(TreeLogger logger) throws UnableToCompleteException {
    long beforeLinkMs = System.currentTimeMillis();
    Event linkEvent = SpeedTracerLogger.start(CompilerEventType.LINK);

    generatedArtifacts.addAll(libraryGroup.getGeneratedArtifacts());

    Set<PermutationResult> libraryPermutationResults = Sets.newLinkedHashSet();
    List<PersistenceBackedObject<PermutationResult>> permutationResultHandles =
        libraryGroup.getPermutationResultHandlesInLinkOrder();
    for (PersistenceBackedObject<PermutationResult> permutationResultHandle :
        permutationResultHandles) {
      libraryPermutationResults.add(permutationResultHandle.newInstance(logger));
    }

    File absPath = new File(compilerOptions.getWarDir(), module.getName());
    absPath = absPath.getAbsoluteFile();

    String logMessage = "Linking into " + absPath;
    if (compilerOptions.getExtraDir() != null) {
      File absExtrasPath = new File(compilerOptions.getExtraDir(), module.getName());
      absExtrasPath = absExtrasPath.getAbsoluteFile();
      logMessage += "; Writing extras to " + absExtrasPath;
    }
    TreeLogger branch = logger.branch(TreeLogger.TRACE, logMessage);
    try {
      Link.link(branch,
          module, compilerContext.getPublicResourceOracle(), generatedArtifacts, permutations,
          permutationResultFiles, libraryPermutationResults, compilerOptions, compilerOptions);
    } catch (IOException e) {
      // The real cause has been logged.
      throw new UnableToCompleteException();
    }
    linkEvent.end();

    long durationMs = System.currentTimeMillis() - beforeLinkMs;
    branch.log(TreeLogger.INFO,
        "Library link succeeded -- " + String.format("%.3f", durationMs / 1000d) + "s");
  }

  private void loadLibraries(TreeLogger logger) throws UnableToCompleteException {
    try {
      libraryGroup = LibraryGroup.fromZipPaths(compilerOptions.getLibraryPaths());
    } catch (IncompatibleLibraryVersionException e) {
      logger.log(TreeLogger.ERROR, e.getMessage());
      throw new UnableToCompleteException();
    }
    compilerContext = compilerContextBuilder.libraryGroup(libraryGroup)
        .libraryWriter(new ZipLibraryWriter(compilerOptions.getOutputLibraryPath()))
        .unitCache(new LibraryGroupUnitCache(libraryGroup)).build();
  }

  private void loadModule(TreeLogger logger) throws UnableToCompleteException {
    module = ModuleDefLoader.loadFromClassPath(
        logger, compilerContext, compilerOptions.getModuleNames().get(0), false, false);
    compilerContext = compilerContextBuilder.module(module).build();
  }

  private void normalizeOptions(TreeLogger logger) throws UnableToCompleteException {
    Preconditions.checkArgument(compilerOptions.getModuleNames().size() == 1);

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

  private boolean run(TreeLogger logger) {
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
    }
  }
}
