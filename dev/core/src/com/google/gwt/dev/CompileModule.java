/*
 * Copyright 2011 Google Inc.
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
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.dev.CompileTaskRunner.CompileTask;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.ModuleDefLoader;
import com.google.gwt.dev.javac.CompilationProblemReporter;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.CompilationStateBuilder;
import com.google.gwt.dev.javac.CompilationUnit;
import com.google.gwt.dev.javac.CompilationUnitArchive;
import com.google.gwt.dev.util.Memory;
import com.google.gwt.dev.util.arg.ArgHandlerLogLevel;
import com.google.gwt.dev.util.arg.ArgHandlerModuleName;
import com.google.gwt.dev.util.arg.ArgHandlerOutDir;
import com.google.gwt.dev.util.arg.ArgHandlerStrict;
import com.google.gwt.dev.util.arg.OptionOutDir;
import com.google.gwt.dev.util.arg.OptionStrict;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Compiles a GWT module into a form that can be re-used by subsequent builds.
 * 
 * Takes all compilation units specified on the module source path and write out
 * CachedCompilationUnits for each one into a file named <module>.gwtar (rhymes
 * with the musical instrument). This will reduce compile and dev mode startup
 * time if a .gwtar file is up to date and doesn't need to be re-built.
 * 
 * Most developers using the GWT SDK won't need to invoke this tool to get
 * performance improvements. The built-in PersistentUnitCache already saves
 * compiled modules between builds.
 * 
 * This tool is of use to library authors for bundling up a pre-compiled gwt
 * library for distributions. Projects that include the library will never incur
 * the penalty of recompiling the library.
 * 
 * It can also be useful in a distributed or multi-process build environment, as
 * separate instances of CompileModule could be invoked in parallel.
 * 
 * CompileModule is meant to be used in conjunction with a build tool such as
 * Apache Ant which can do gross level dependency checking of the inputs and
 * compute the staleness of a .gwtar file. If the .gwtar file is up to date, the
 * assumption is that this tool won't be invoked at all.
 * 
 * If there are dependent modules that already have their own .gwtar files, they
 * are assumed good and loaded first. CachedCompilationUnits that already exist
 * will not be re-written into the <module>.gwtar files.
 * 
 * Note: Currently, the order the modules are passed in is the order in which
 * they will be compiled. This means you should be careful to pass in modules
 * that depend on other modules in the same list last.
 * 
 * TODO(zundel): remove the manual ordering of dependencies.
 */
public class CompileModule {

  static class ArgProcessor extends ArgProcessorBase {
    public ArgProcessor(CompileModuleOptions options) {
      registerHandler(new ArgHandlerLogLevel(options));
      registerHandler(new ArgHandlerOutDir(options) {
        @Override
        public String[] getDefaultArgs() {
          return new String[] {getTag(), "bin"};
        }
      });
      registerHandler(new ArgHandlerModuleName(options));
      registerHandler(new ArgHandlerStrict(options));
    }

    @Override
    protected String getName() {
      return CompileModule.class.getName();
    }
  }

  interface CompileModuleOptions extends CompileTaskOptions, OptionOutDir, OptionStrict {
  }

  static class CompileModuleOptionsImpl extends CompileTaskOptionsImpl implements
      CompileModuleOptions {

    private File outDir;
    private boolean strict = false;

    public CompileModuleOptionsImpl() {
    }

    public CompileModuleOptionsImpl(CompileModuleOptions other) {
      copyFrom(other);
    }

    public void copyFrom(CompileModuleOptions other) {
      super.copyFrom(other);
      setOutDir(other.getOutDir());
      setStrict(other.isStrict());
    }

    @Override
    public File getOutDir() {
      return outDir;
    }

    @Override
    public boolean isStrict() {
      return strict;
    }

    @Override
    public void setOutDir(File outDir) {
      this.outDir = outDir;
    }

    @Override
    public void setStrict(boolean strict) {
      this.strict = strict;
    }
  }

  // TODO(zundel): Many classes in this package share a similar main()
  // structure. Refactor to reduce redundancy?
  public static void main(String[] args) {
    Memory.initialize();
    SpeedTracerLogger.init();
    SpeedTracerLogger.start(CompilerEventType.COMPILE_MODULE);
    /*
     * NOTE: main always exits with a call to System.exit to terminate any
     * non-daemon threads that were started in Generators. Typically, this is to
     * shutdown AWT related threads, since the contract for their termination is
     * still implementation-dependent.
     */
    final CompileModuleOptions options = new CompileModuleOptionsImpl();
    if (new ArgProcessor(options).processArgs(args)) {
      CompileTask task = new CompileTask() {
        @Override
        public boolean run(TreeLogger logger) {
          return new CompileModule(options).run(logger);
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

  private final CompileModuleOptionsImpl options;

  public CompileModule(CompileModuleOptions options) {
    this.options = new CompileModuleOptionsImpl(options);
  }

  /**
   * Main loop.
   * 
   * For each module passed on the command line, populates the compilation state
   * with compilation units from other archives, compiles all resources in this
   * module, and writes out all the compilation units that are not already
   * members of another archive into a new {@link CompilationUnitArchive} file.
   */
  public boolean run(final TreeLogger logger) {
    // Remember units already seen so we don't write the same unit into multiple
    // archives. Also used as an optimization to keep from de-serializing the
    // same archive twice. Key is archive URL string. Maps to the set of unit resource paths
    // for the archive.
    Map<String, Set<String>> unitsInArchives = new HashMap<String, Set<String>>();
    // Modules archived by this invocation of CompileModule.  Once a compiled module is
    // written out as an archive file, it may or may not appear on the classpath 
    // and come back with module.getAllCompilationUnitArchiveURLs().  Thus, use a second check
    // so that the tool doesn't redundantly write the same compilation units into
    // multiple archives.
    Map<String, Set<String>> newlyCompiledModules = new HashMap<String, Set<String>>();
    File outputDir = options.getOutDir();
    if (!outputDir.isDirectory() && !outputDir.mkdirs()) {
      logger.log(Type.ERROR, "Error creating directories for ouptut: "
          + outputDir.getAbsolutePath());
      return false;
    }

    // TODO(zundel): There is an optimal order to compile these modules in.
    // Modify ModuleDefLoader to be able to figure that out and sort them for
    // us.

    for (String moduleToCompile : options.getModuleNames()) {
      ModuleDef module;
      // The units in this set already belong to an archive and should not be
      // written out.
      Set<String> currentModuleArchivedUnits = new HashSet<String>();
      try {
        module = ModuleDefLoader.loadFromClassPath(logger, moduleToCompile, false);
      } catch (Throwable e) {
        CompilationProblemReporter.logAndTranslateException(logger, e);
        return false;
      }

      SpeedTracerLogger.Event loadAllArchives =
          SpeedTracerLogger.start(CompilerEventType.LOAD_ARCHIVE, "module", moduleToCompile);
      try {
        Collection<URL> archiveURLs = module.getAllCompilationUnitArchiveURLs();
        if (logger.isLoggable(TreeLogger.TRACE) && archiveURLs != null) {
          for (URL archiveURL : archiveURLs) {
            logger.log(TreeLogger.TRACE, "Found archive: " + archiveURL);
          }
        }

        // Don't re-archive previously compiled units from this invocation of CompileModule.
        for (String compiledModuleName : newlyCompiledModules.keySet()) {
          if (module.isInherited(compiledModuleName)) {
            currentModuleArchivedUnits.addAll(newlyCompiledModules.get(compiledModuleName));
          }
        }

        // Load up previously archived modules
        for (URL archiveURL : archiveURLs) {
          String archiveURLString = archiveURL.toString();
          Set<String> unitPaths = unitsInArchives.get(archiveURLString);
          // Don't bother deserializing archives that have already been read.
          if (unitPaths != null) {
            currentModuleArchivedUnits.addAll(unitPaths);
            continue;
          }

          SpeedTracerLogger.Event loadArchive =
              SpeedTracerLogger.start(CompilerEventType.LOAD_ARCHIVE, "dependentModule", archiveURL
                  .toString());
          try {
            CompilationUnitArchive archive = CompilationUnitArchive.createFromURL(archiveURL);
            // Pre-populate CompilationStateBuilder with .gwtar files
            CompilationStateBuilder.addArchive(archive);

            // Remember already archived units - we don't want to add them back.
            if (!archive.getTopModuleName().equals(moduleToCompile)) {
              Set<String> archivedUnits = new HashSet<String>();
              unitsInArchives.put(archiveURLString, archivedUnits);
              for (CompilationUnit unit : archive.getUnits().values()) {
                archivedUnits.add(unit.getResourcePath());
              }
              currentModuleArchivedUnits.addAll(archivedUnits);
            }
          } catch (IOException ex) {
            logger.log(TreeLogger.WARN, "Unable to read: " + archiveURL + ". Skipping: " + ex);
          } catch (ClassNotFoundException ex) {
            logger
                .log(TreeLogger.WARN, "Incompatible archive: " + archiveURL + ". Skipping: " + ex);
          } finally {
            loadArchive.end();
          }
        }
      } finally {
        loadAllArchives.end();
      }

      CompilationState compilationState;
      try {
        compilationState = module.getCompilationState(logger, !options.isStrict());
      } catch (Throwable e) {
        CompilationProblemReporter.logAndTranslateException(logger, e);
        return false;
      }

      if (options.isStrict() && compilationState.hasErrors()) {
        logger.log(TreeLogger.ERROR, "Failed to compile " + moduleToCompile);
        return false;
      }

      Set<String> compiledUnits = Sets.newHashSet();
      CompilationUnitArchive outputArchive = new CompilationUnitArchive(moduleToCompile);
      for (CompilationUnit unit : compilationState.getCompilationUnits()) {
        if (!currentModuleArchivedUnits.contains(unit.getResourcePath())) {
          outputArchive.addUnit(unit);
          compiledUnits.add(unit.getResourcePath());
        }
      }
      newlyCompiledModules.put(moduleToCompile, compiledUnits);

      String slashedModuleName =
          module.getName().replace('.', '/') + ModuleDefLoader.COMPILATION_UNIT_ARCHIVE_SUFFIX;
      File outputFile = new File(outputDir, slashedModuleName);
      outputFile.getParentFile().mkdirs();
      logger.log(TreeLogger.INFO, "Writing " + outputArchive.getUnits().size() + " units to "
          + outputFile.getAbsolutePath());
      try {
        outputArchive.writeToFile(outputFile);
      } catch (IOException ex) {
        logger.log(Type.ERROR, "Error writing module file: " + outputFile.getAbsolutePath() + ": "
            + ex);
        return false;
      }
    }
    return true;
  }
}
