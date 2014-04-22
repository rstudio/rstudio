/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.dev.javac;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.CompilerContext;
import com.google.gwt.dev.javac.JdtCompiler.AdditionalTypeProviderDelegate;
import com.google.gwt.dev.javac.JdtCompiler.UnitProcessor;
import com.google.gwt.dev.javac.typemodel.LibraryTypeOracle;
import com.google.gwt.dev.javac.typemodel.TypeOracle;
import com.google.gwt.dev.jjs.CorrelationFactory.DummyCorrelationFactory;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.impl.GwtAstBuilder;
import com.google.gwt.dev.js.ast.JsRootScope;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.util.StringInterner;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.DevModeEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.EventType;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableList;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableMap;
import com.google.gwt.thirdparty.guava.common.collect.Interner;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ImportReference;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Manages a centralized cache for compiled units.
 */
public class CompilationStateBuilder {

  /**
   * An opaque class that lets you compile more units later.
   */
  public class CompileMoreLater {

    private final class UnitProcessorImpl implements UnitProcessor {

      /**
       * A callback after the JDT compiler has compiled a .java file and created a matching
       * CompilationUnitDeclaration. We take this opportunity to create a matching CompilationUnit.
       */
      @Override
      public void process(CompilationUnitBuilder builder, CompilationUnitDeclaration cud,
          List<ImportReference> cudOriginaImports,
          List<CompiledClass> compiledClasses) {
        Event event = SpeedTracerLogger.start(DevModeEventType.CSB_PROCESS);
        try {
          // Collect parameter method names event when the compilation unit has errors.

          List<JDeclaredType> types = ImmutableList.of();
          final Set<String> jsniDeps = Sets.newHashSet();
          final Map<String, Binding> jsniRefs = Maps.newHashMap();
          Map<MethodDeclaration, JsniMethod> jsniMethods = ImmutableMap.of();
          List<String> apiRefs = ImmutableList.of();
          MethodArgNamesLookup methodArgs = new MethodArgNamesLookup();

          if (!cud.compilationResult().hasErrors()) {
            // Only collect jsniMethod, artificial rescues, etc if the compilation unit
            // does not have errors.
            jsniMethods =
                JsniMethodCollector.collectJsniMethods(cud, builder.getSourceMapPath(),
                    builder.getSource(), JsRootScope.INSTANCE, DummyCorrelationFactory.INSTANCE);

            JSORestrictionsChecker.check(jsoState, cud);

            // JSNI check + collect dependencies.
            JsniReferenceResolver
                .resolve(cud, cudOriginaImports, jsoState, jsniMethods, jsniRefs,
                    new JsniReferenceResolver.TypeResolver() {
                      @Override
                      public ReferenceBinding resolveType(String sourceOrBinaryName) {
                        ReferenceBinding resolveType = compiler.resolveType(sourceOrBinaryName);
                        if (resolveType != null) {
                          jsniDeps.add(String.valueOf(resolveType.qualifiedSourceName()));
                        }
                        return resolveType;
                      }
                    });

            final Map<TypeDeclaration, Binding[]> artificialRescues = Maps.newHashMap();
            ArtificialRescueChecker.check(cud, builder.isGenerated(), artificialRescues);
            if (compilerContext.shouldCompileMonolithic()) {
              // GWT drives JDT in a way that allows missing references in the source to be
              // resolved to precompiled bytecode on disk (see INameEnvironment). This is
              // done so that annotations can be supplied in bytecode form only. But since no
              // AST is available for these types it creates the danger that some functional
              // class (not just an annotation) gets filled in but is missing AST. This would
              // cause later compiler stages to fail.
              //
              // Library compilation needs to ignore this check since it is expected behavior
              // for the source being compiled in a library to make references to other types
              // which are only available as bytecode coming out of dependency libraries.
              //
              // But if the referenced bytecode did not come from a dependency library but
              // instead was free floating in the classpath, then there is no guarantee that
              // AST for it was ever seen and translated to JS anywhere in the dependency tree.
              // This would be a mistake.
              //
              // TODO(stalcup): add a more specific check for library compiles such that binary
              // types can be referenced but only if they are an Annotation or if the binary
              // type comes from a dependency library.
              BinaryTypeReferenceRestrictionsChecker.check(cud);
            }

            if (!cud.compilationResult().hasErrors()) {
              // The above checks might have recorded errors; so we need to check here again.
              // So only construct the GWT AST if no JDT errors and no errors from our checks.
              types = astBuilder.process(cud, builder.getSourceMapPath(), artificialRescues,
                  jsniMethods, jsniRefs);
            }

            // Only run this pass if JDT was able to compile the unit with no errors, otherwise
            // the JDT AST traversal might throw exceptions.
            apiRefs = compiler.collectApiRefs(cud);
            methodArgs = MethodParamCollector.collect(cud, builder.getSourceMapPath());
          }

          final Interner<String> interner = StringInterner.get();
          String packageName = interner.intern(Shared.getPackageName(builder.getTypeName()));

          List<String> unresolvedSimple = Lists.newArrayList();
          for (char[] simpleRef : cud.compilationResult().simpleNameReferences) {
            unresolvedSimple.add(interner.intern(String.valueOf(simpleRef)));
          }

          List<String> unresolvedQualified = Lists.newArrayList();
          for (char[][] qualifiedRef : cud.compilationResult().qualifiedReferences) {
            unresolvedQualified.add(interner.intern(CharOperation.toString(qualifiedRef)));
          }
          for (String jsniDep : jsniDeps) {
            unresolvedQualified.add(interner.intern(jsniDep));
          }
          for (int i = 0; i < apiRefs.size(); ++i) {
            apiRefs.set(i, interner.intern(apiRefs.get(i)));
          }

          // Dependencies need to be included even when the {@code cud} has errors as the unit might
          // be saved as a cached unit and its dependencies might be used to further invalidate
          // other units. See {@link
          // CompilationStateBuilder#removeInvalidCachedUnitsAndRescheduleCorrespondingBuilders}.
          Dependencies dependencies =
              new Dependencies(packageName, unresolvedQualified, unresolvedSimple, apiRefs);

          for (CompiledClass cc : compiledClasses) {
            allValidClasses.put(cc.getSourceName(), cc);
          }

          // Even when compilation units have errors, return a consistent builder.
          builder
              .setTypes(types)
              .setDependencies(dependencies)
              .setJsniMethods(jsniMethods.values())
              .setMethodArgs(methodArgs)
              .setClasses(compiledClasses)
              .setProblems(cud.compilationResult().getProblems());

          buildQueue.add(builder);
        } finally {
          event.end();
        }
      }
    }

    /**
     * A global cache of all currently-valid class files keyed by source name.
     * This is used to validate dependencies when reusing previously cached
     * units, to make sure they can be recompiled if necessary.
     */
    private final Map<String, CompiledClass> allValidClasses = Maps.newHashMap();

    private final GwtAstBuilder astBuilder = new GwtAstBuilder();

    private transient LinkedBlockingQueue<CompilationUnitBuilder> buildQueue;

    /**
     * The JDT compiler.
     */
    private final JdtCompiler compiler;

    /**
     * Continuation state for JSNI checking.
     */
    private final JSORestrictionsChecker.CheckerState jsoState =
        new JSORestrictionsChecker.CheckerState();

    private final boolean suppressErrors;

    private CompilerContext compilerContext;

    public CompileMoreLater(
        CompilerContext compilerContext, AdditionalTypeProviderDelegate delegate) {
      this.compilerContext = compilerContext;
      this.compiler = new JdtCompiler(
          compilerContext, new UnitProcessorImpl());
      this.suppressErrors = !compilerContext.getOptions().isStrict();
      compiler.setAdditionalTypeProviderDelegate(delegate);
    }

    /**
     * Compiles generated source files (unless cached) and adds them to the
     * CompilationState. If the compiler aborts, logs an error and throws
     * UnableToCompleteException.
     */
    public Collection<CompilationUnit> addGeneratedTypes(TreeLogger logger,
        Collection<GeneratedUnit> generatedUnits, CompilationState compilationState)
        throws UnableToCompleteException {
      Event event = SpeedTracerLogger.start(DevModeEventType.CSB_ADD_GENERATED_TYPES);
      try {
        return doBuildGeneratedTypes(logger, compilerContext, generatedUnits, compilationState,
            this);
      } finally {
        event.end();
      }
    }

    public Map<String, CompiledClass> getValidClasses() {
      return Collections.unmodifiableMap(allValidClasses);
    }

    void addValidUnit(CompilationUnit unit) {
      compiler.addCompiledUnit(unit);
      for (CompiledClass cc : unit.getCompiledClasses()) {
        allValidClasses.put(cc.getSourceName(), cc);
      }
    }

    /**
     * Compiles the source code in each supplied CompilationUnitBuilder into a CompilationUnit and
     * reports errors.
     *
     * <p>A compilation unit is considered invalid if any of its dependencies (recursively) isn't
     * being compiled and isn't in allValidClasses, or if it has a signature that doesn't match
     * a dependency. Valid compilation units will be added to cachedUnits and the unit cache, and
     * their types will be added to allValidClasses. Invalid compilation units will be removed.</p>
     *
     * <p>I/O: serializes the AST of each Java type to DiskCache. (This happens even if the
     * compilation unit is later dropped.) If we're using the persistent unit cache, each valid
     * unit will also be serialized to the gwt-unitcache file. (As a result, each AST will be
     * copied there from the DiskCache.) A new persistent unit cache file will be created
     * each time compile() is called (if there's at least one valid unit) and the entire cache
     * will be rewritten to disk every {@link PersistentUnitCache#CACHE_FILE_THRESHOLD} files.</p>
     *
     * <p>This function won't report errors in invalid source files unless suppressErrors is false.
     * Instead, a summary giving the number of invalid files will be logged.</p>
     *
     * <p>If the JDT compiler aborts, logs an error and throws UnableToCompleteException. (This
     * doesn't happen for normal compile errors.)</p>
     */
    Collection<CompilationUnit> compile(TreeLogger logger, CompilerContext compilerContext,
        Collection<CompilationUnitBuilder> builders,
        Map<CompilationUnitBuilder, CompilationUnit> cachedUnits, EventType eventType)
        throws UnableToCompleteException {
      UnitCache unitCache = compilerContext.getUnitCache();
      // Initialize the set of valid classes to the initially cached units.
      for (CompilationUnit unit : cachedUnits.values()) {
        for (CompiledClass cc : unit.getCompiledClasses()) {
          // Map by source name.
          String sourceName = cc.getSourceName();
          allValidClasses.put(sourceName, cc);
        }
      }

      List<CompilationUnit> resultUnits = Lists.newArrayList();
      do {
        final TreeLogger branch = logger.branch(TreeLogger.TRACE, "Compiling...");
        // Compile anything that needs to be compiled.
        buildQueue = new LinkedBlockingQueue<CompilationUnitBuilder>();
        final List<CompilationUnit> newlyBuiltUnits = Lists.newArrayList();
        final CompilationUnitBuilder sentinel = CompilationUnitBuilder.create((GeneratedUnit) null);
        final Throwable[] workerException = new Throwable[1];
        final ProgressLogger progressLogger =
            new ProgressLogger(branch, TreeLogger.TRACE, builders.size(), 10);
        Thread buildThread = new Thread() {
          @Override
          public void run() {
            int processedCompilationUnitBuilders = 0;
            try {
              do {
                CompilationUnitBuilder builder = buildQueue.take();
                if (!progressLogger.isTimerStarted()) {
                  // Set start time here, after first job has arrived, since it can take a little
                  // while for the first job to arrive, and this helps with the accuracy of the
                  // estimated times.
                  progressLogger.startTimer();
                }
                if (builder == sentinel) {
                  return;
                }
                // Expensive, must serialize GWT AST types to bytes.
                CompilationUnit unit = builder.build();
                newlyBuiltUnits.add(unit);

                processedCompilationUnitBuilders++;
                progressLogger.updateProgress(processedCompilationUnitBuilders);
              } while (true);
            } catch (Throwable e) {
              workerException[0] = e;
            }
          }
        };
        buildThread.setName("CompilationUnitBuilder");
        buildThread.start();
        Event jdtCompilerEvent = SpeedTracerLogger.start(eventType);
        long compilationStartNanos = System.nanoTime();
        try {
          compiler.doCompile(branch, builders);
        } finally {
          jdtCompilerEvent.end();
        }
        buildQueue.add(sentinel);
        try {
          buildThread.join();
          long compilationNanos = System.nanoTime() - compilationStartNanos;
          // Convert nanos to seconds.
          double compilationSeconds = compilationNanos / (double) TimeUnit.SECONDS.toNanos(1);
          branch.log(TreeLogger.TRACE,
              String.format("Compilation completed in %.02f seconds", compilationSeconds));
          if (workerException[0] != null) {
            throw workerException[0];
          }
        } catch (RuntimeException e) {
          throw e;
        } catch (Throwable e) {
          throw new RuntimeException("Exception processing units", e);
        } finally {
          buildQueue = null;
        }
        resultUnits.addAll(newlyBuiltUnits);
        builders.clear();

        // Resolve all newly built unit deps against the global classes.
        for (CompilationUnit unit : newlyBuiltUnits) {
          unit.getDependencies().resolve(allValidClasses);
        }

        removeInvalidCachedUnitsAndRescheduleCorrespondingBuilders(logger, builders, cachedUnits);
      } while (builders.size() > 0);

      for (CompilationUnit unit : resultUnits) {
        unitCache.add(unit);
      }

      // Any remaining cached units are valid now.
      resultUnits.addAll(cachedUnits.values());

      // Done with a pass of the build - tell the cache its OK to cleanup
      // stale cache files.
      unitCache.cleanup(logger);

      // Sort, then report all errors (re-report for cached units).
      Collections.sort(resultUnits, CompilationUnit.COMPARATOR);
      logger = logger.branch(TreeLogger.DEBUG, "Validating units:");
      int errorCount = 0;
      for (CompilationUnit unit : resultUnits) {
        if (CompilationProblemReporter.reportErrors(logger, unit, suppressErrors)) {
          errorCount++;
        }
      }
      if (suppressErrors && errorCount > 0 && !logger.isLoggable(TreeLogger.TRACE)
          && logger.isLoggable(TreeLogger.INFO)) {
        logger.log(TreeLogger.INFO, "Ignored " + errorCount + " unit" + (errorCount > 1 ? "s" : "")
            + " with compilation errors in first pass.\n"
            + "Compile with -strict or with -logLevel set to TRACE or DEBUG to see all errors.");
      }
      return resultUnits;
    }

    /**
     * Removes cached units that fail validation with the current set of valid classes; also
     * add the builder of the invalidated unit back for retry later.
     */
    private void removeInvalidCachedUnitsAndRescheduleCorrespondingBuilders(TreeLogger logger,
        Collection<CompilationUnitBuilder> builders,
        Map<CompilationUnitBuilder, CompilationUnit> cachedUnits) {

      /*
       * Invalidate any cached units with invalid refs.
       */
      Collection<CompilationUnit> invalidatedUnits = Lists.newArrayList();
      for (Iterator<Entry<CompilationUnitBuilder, CompilationUnit>> it =
          cachedUnits.entrySet().iterator(); it.hasNext();) {
        Entry<CompilationUnitBuilder, CompilationUnit> entry = it.next();
        CompilationUnit unit = entry.getValue();
        boolean isValid = unit.getDependencies().validate(logger, allValidClasses);
        if (isValid && unit.isError()) {
          // See if the unit has classes that can't provide a
          // NameEnvironmentAnswer
          for (CompiledClass cc : unit.getCompiledClasses()) {
            try {
              cc.getNameEnvironmentAnswer();
            } catch (ClassFormatException ex) {
              isValid = false;
              break;
            }
          }
        }
        if (!isValid) {
          if (logger.isLoggable(TreeLogger.TRACE)) {
            logger.log(TreeLogger.TRACE, "Invalid Unit: " + unit.getTypeName());
          }
          invalidatedUnits.add(unit);
          builders.add(entry.getKey());
          it.remove();
        }
      }

      if (invalidatedUnits.size() > 0) {
        if (logger.isLoggable(TreeLogger.TRACE)) {
          logger.log(TreeLogger.TRACE, "Invalid units found: " + invalidatedUnits.size());
        }
      }

      // Any units we invalidated must now be removed from the valid classes.
      for (CompilationUnit unit : invalidatedUnits) {
        for (CompiledClass cc : unit.getCompiledClasses()) {
          allValidClasses.remove(cc.getSourceName());
        }
      }
    }
  }

  private static final CompilationStateBuilder instance = new CompilationStateBuilder();

  /**
   * Use previously compiled {@link CompilationUnit}s to pre-populate the unit cache.
   */
  public static void addArchive(
      CompilerContext compilerContext, CompilationUnitArchive compilationUnitArchive) {
    UnitCache unitCache = compilerContext.getUnitCache();
    for (CachedCompilationUnit archivedUnit : compilationUnitArchive.getUnits().values()) {
      if (archivedUnit.getTypesSerializedVersion() != GwtAstBuilder.getSerializationVersion()) {
        continue;
      }
      CompilationUnit cachedCompilationUnit = unitCache.find(archivedUnit.getResourcePath());
      // A previously cached unit might be from the persistent cache or another
      // archive.
      if (cachedCompilationUnit == null
          || cachedCompilationUnit.getLastModified() < archivedUnit.getLastModified()) {
        unitCache.addArchivedUnit(archivedUnit);
      }
    }
  }

  /**
   * Compiles the given source files and adds them to the CompilationState. See
   * {@link CompileMoreLater#compile} for details.
   *
   * @throws UnableToCompleteException if the compiler aborts (not a normal compile error).
   */
  public static CompilationState buildFrom(
      TreeLogger logger, CompilerContext compilerContext, Set<Resource> resources)
      throws UnableToCompleteException {
    return buildFrom(logger, compilerContext, resources, null);
  }

  /**
   * Compiles the given source files and adds them to the CompilationState. See
   * {@link CompileMoreLater#compile} for details.
   *
   * @throws UnableToCompleteException if the compiler aborts (not a normal compile error).
   */
  public static CompilationState buildFrom(TreeLogger logger, CompilerContext compilerContext,
      Set<Resource> resources, AdditionalTypeProviderDelegate delegate)
      throws UnableToCompleteException {
    Event event = SpeedTracerLogger.start(DevModeEventType.CSB_BUILD_FROM_ORACLE);
    try {
      return instance.doBuildFrom(logger, compilerContext, resources, delegate);
    } finally {
      event.end();
    }
  }

  public static CompilationStateBuilder get() {
    return instance;
  }

  /**
   * Build a new compilation state from a source oracle. Allow the caller to
   * specify a compiler delegate that will handle undefined names.
   *
   * TODO: maybe use a finer brush than to synchronize the whole thing.
   */
  public synchronized CompilationState doBuildFrom(TreeLogger logger,
      CompilerContext compilerContext, Set<Resource> resources,
      AdditionalTypeProviderDelegate compilerDelegate)
    throws UnableToCompleteException {
    UnitCache unitCache = compilerContext.getUnitCache();

    // Units we definitely want to build.
    List<CompilationUnitBuilder> builders = Lists.newArrayList();

    // Units we don't want to rebuild unless we have to.
    Map<CompilationUnitBuilder, CompilationUnit> cachedUnits = Maps.newIdentityHashMap();

    CompileMoreLater compileMoreLater = new CompileMoreLater(compilerContext, compilerDelegate);

    // For each incoming Java source file...
    for (Resource resource : resources) {
      // Create a builder for all incoming units.
      CompilationUnitBuilder builder = CompilationUnitBuilder.create(resource);

      CompilationUnit cachedUnit = unitCache.find(resource.getPathPrefix() + resource.getPath());

      // Try to rescue cached units from previous sessions where a jar has been
      // recompiled.
      if (cachedUnit != null && cachedUnit.getLastModified() != resource.getLastModified()) {
        unitCache.remove(cachedUnit);
        if (cachedUnit instanceof CachedCompilationUnit
            && cachedUnit.getContentId().equals(builder.getContentId())) {
          CachedCompilationUnit updatedUnit =
              new CachedCompilationUnit((CachedCompilationUnit) cachedUnit, resource
                  .getLastModified(), resource.getLocation());
          unitCache.add(updatedUnit);
        } else {
          cachedUnit = null;
        }
      }
      if (cachedUnit != null) {
        cachedUnits.put(builder, cachedUnit);
        compileMoreLater.addValidUnit(cachedUnit);
        continue;
      }
      builders.add(builder);
    }
    int cachedSourceCount = cachedUnits.size();
    int sourceCount = resources.size();
    if (logger.isLoggable(TreeLogger.TRACE)) {
      logger.log(TreeLogger.TRACE, "Found " + cachedSourceCount + " cached/archived units.  Used "
          + cachedSourceCount + " / " + sourceCount + " units from cache.");
    }

    Collection<CompilationUnit> resultUnits = compileMoreLater.compile(
        logger, compilerContext, builders, cachedUnits,
        CompilerEventType.JDT_COMPILER_CSB_FROM_ORACLE);

    boolean compileMonolithic = compilerContext.shouldCompileMonolithic();
    TypeOracle typeOracle = null;
    CompilationUnitTypeOracleUpdater typeOracleUpdater = null;
    if (compileMonolithic) {
      typeOracle = new TypeOracle();
      typeOracleUpdater = new CompilationUnitTypeOracleUpdater(typeOracle);
    } else {
      typeOracle = new LibraryTypeOracle(compilerContext);
      typeOracleUpdater = ((LibraryTypeOracle) typeOracle).getTypeOracleUpdater();
    }

    CompilationState compilationState = new CompilationState(logger, compilerContext, typeOracle,
        typeOracleUpdater, resultUnits, compileMoreLater);
    compilationState.incrementStaticSourceCount(sourceCount);
    compilationState.incrementCachedStaticSourceCount(cachedSourceCount);
    return compilationState;
  }

  public CompilationState doBuildFrom(
      TreeLogger logger, CompilerContext compilerContext, Set<Resource> resources)
      throws UnableToCompleteException {
    return doBuildFrom(logger, compilerContext, resources, null);
  }

  /**
   * Compile new generated units into an existing state.
   *
   * TODO: maybe use a finer brush than to synchronize the whole thing.
   */
  synchronized Collection<CompilationUnit> doBuildGeneratedTypes(TreeLogger logger,
      CompilerContext compilerContext, Collection<GeneratedUnit> generatedUnits,
      CompilationState compilationState, CompileMoreLater compileMoreLater)
      throws UnableToCompleteException {
    UnitCache unitCache = compilerContext.getUnitCache();

    // Units we definitely want to build.
    List<CompilationUnitBuilder> builders = Lists.newArrayList();

    // Units we don't want to rebuild unless we have to.
    Map<CompilationUnitBuilder, CompilationUnit> cachedUnits = Maps.newIdentityHashMap();

    // For each incoming generated Java source file...
    for (GeneratedUnit generatedUnit : generatedUnits) {
      // Create a builder for all incoming units.
      CompilationUnitBuilder builder = CompilationUnitBuilder.create(generatedUnit);

      // Look for units previously compiled
      CompilationUnit cachedUnit = unitCache.find(builder.getContentId());
      if (cachedUnit != null) {
        // Recompile generated units with errors so source can be dumped.
        if (!cachedUnit.isError()) {
          cachedUnits.put(builder, cachedUnit);
          compileMoreLater.addValidUnit(cachedUnit);
          continue;
        }
      }
      builders.add(builder);
    }
    compilationState.incrementGeneratedSourceCount(builders.size() + cachedUnits.size());
    compilationState.incrementCachedGeneratedSourceCount(cachedUnits.size());
    return compileMoreLater.compile(logger, compilerContext, builders,
        cachedUnits, CompilerEventType.JDT_COMPILER_CSB_GENERATED);
  }
}
