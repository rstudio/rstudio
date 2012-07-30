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
import com.google.gwt.dev.javac.JdtCompiler.AdditionalTypeProviderDelegate;
import com.google.gwt.dev.javac.JdtCompiler.UnitProcessor;
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

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
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

      @Override
      public void process(CompilationUnitBuilder builder, CompilationUnitDeclaration cud,
          List<CompiledClass> compiledClasses) {
        Event event = SpeedTracerLogger.start(DevModeEventType.CSB_PROCESS);
        try {
          Map<MethodDeclaration, JsniMethod> jsniMethods =
              JsniCollector.collectJsniMethods(cud, builder.getSourceMapPath(),
                  builder.getSource(), JsRootScope.INSTANCE, DummyCorrelationFactory.INSTANCE);

          JSORestrictionsChecker.check(jsoState, cud);

          // JSNI check + collect dependencies.
          final Set<String> jsniDeps = new HashSet<String>();
          Map<String, Binding> jsniRefs = new HashMap<String, Binding>();
          JsniChecker.check(cud, jsoState, jsniMethods, jsniRefs, new JsniChecker.TypeResolver() {
            @Override
            public ReferenceBinding resolveType(String typeName) {
              ReferenceBinding resolveType = compiler.resolveType(typeName);
              if (resolveType != null) {
                jsniDeps.add(String.valueOf(resolveType.qualifiedSourceName()));
              }
              return resolveType;
            }
          });

          Map<TypeDeclaration, Binding[]> artificialRescues =
              new HashMap<TypeDeclaration, Binding[]>();
          ArtificialRescueChecker.check(cud, builder.isGenerated(), artificialRescues);
          BinaryTypeReferenceRestrictionsChecker.check(cud);

          MethodArgNamesLookup methodArgs = MethodParamCollector.collect(cud,
              builder.getSourceMapPath());

          StringInterner interner = StringInterner.get();
          String packageName = interner.intern(Shared.getPackageName(builder.getTypeName()));
          List<String> unresolvedQualified = new ArrayList<String>();
          List<String> unresolvedSimple = new ArrayList<String>();
          for (char[] simpleRef : cud.compilationResult().simpleNameReferences) {
            unresolvedSimple.add(interner.intern(String.valueOf(simpleRef)));
          }
          for (char[][] qualifiedRef : cud.compilationResult().qualifiedReferences) {
            unresolvedQualified.add(interner.intern(CharOperation.toString(qualifiedRef)));
          }
          for (String jsniDep : jsniDeps) {
            unresolvedQualified.add(interner.intern(jsniDep));
          }
          ArrayList<String> apiRefs = compiler.collectApiRefs(cud);
          for (int i = 0; i < apiRefs.size(); ++i) {
            apiRefs.set(i, interner.intern(apiRefs.get(i)));
          }
          Dependencies dependencies =
              new Dependencies(packageName, unresolvedQualified, unresolvedSimple, apiRefs);

          List<JDeclaredType> types = Collections.emptyList();
          if (!cud.compilationResult().hasErrors()) {
            // Make a GWT AST.
            types = astBuilder.process(cud, builder.getSourceMapPath(), artificialRescues,
                jsniMethods, jsniRefs);
          }

          for (CompiledClass cc : compiledClasses) {
            allValidClasses.put(cc.getSourceName(), cc);
          }

          builder.setClasses(compiledClasses).setTypes(types).setDependencies(dependencies)
              .setJsniMethods(jsniMethods.values()).setMethodArgs(methodArgs).setProblems(
                  cud.compilationResult().getProblems());
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
    private final Map<String, CompiledClass> allValidClasses = new HashMap<String, CompiledClass>();

    private final GwtAstBuilder astBuilder = new GwtAstBuilder();

    private transient LinkedBlockingQueue<CompilationUnitBuilder> buildQueue;

    /**
     * The JDT compiler.
     */
    private final JdtCompiler compiler = new JdtCompiler(new UnitProcessorImpl());

    /**
     * Continuation state for JSNI checking.
     */
    private final JSORestrictionsChecker.CheckerState jsoState =
        new JSORestrictionsChecker.CheckerState();

    private final boolean suppressErrors;

    public CompileMoreLater(AdditionalTypeProviderDelegate delegate, boolean suppressErrors) {
      compiler.setAdditionalTypeProviderDelegate(delegate);
      this.suppressErrors = suppressErrors;
    }

    public Collection<CompilationUnit> addGeneratedTypes(TreeLogger logger,
        Collection<GeneratedUnit> generatedUnits) {
      Event event = SpeedTracerLogger.start(DevModeEventType.CSB_ADD_GENERATED_TYPES);
      try {
        return doBuildGeneratedTypes(logger, generatedUnits, this, suppressErrors);
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

    Collection<CompilationUnit> compile(TreeLogger logger,
        Collection<CompilationUnitBuilder> builders,
        Map<CompilationUnitBuilder, CompilationUnit> cachedUnits, EventType eventType,
        boolean suppressErrors) {
      // Initialize the set of valid classes to the initially cached units.
      for (CompilationUnit unit : cachedUnits.values()) {
        for (CompiledClass cc : unit.getCompiledClasses()) {
          // Map by source name.
          String sourceName = cc.getSourceName();
          allValidClasses.put(sourceName, cc);
        }
      }

      ArrayList<CompilationUnit> resultUnits = new ArrayList<CompilationUnit>();
      do {
        final TreeLogger branch = logger.branch(TreeLogger.TRACE, "Compiling...");
        // Compile anything that needs to be compiled.
        buildQueue = new LinkedBlockingQueue<CompilationUnitBuilder>();
        final ArrayList<CompilationUnit> newlyBuiltUnits = new ArrayList<CompilationUnit>();
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
          compiler.doCompile(builders);
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

        /*
         * Invalidate any cached units with invalid refs.
         */
        Collection<CompilationUnit> invalidatedUnits = new ArrayList<CompilationUnit>();
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
  }

  private static final CompilationStateBuilder instance = new CompilationStateBuilder();

  /**
   * Use previously compiled {@link CompilationUnit}s to pre-populate the unit
   * cache.
   */
  public static void addArchive(CompilationUnitArchive module) {
    UnitCache unitCache = instance.unitCache;
    for (CachedCompilationUnit archivedUnit : module.getUnits().values()) {
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

  public static CompilationState buildFrom(TreeLogger logger, Set<Resource> resources) {
    return buildFrom(logger, resources, null, false);
  }

  public static CompilationState buildFrom(TreeLogger logger, Set<Resource> resources,
      AdditionalTypeProviderDelegate delegate) {
    return buildFrom(logger, resources, delegate, false);
  }

  public static CompilationState buildFrom(TreeLogger logger, Set<Resource> resources,
      AdditionalTypeProviderDelegate delegate, boolean suppressErrors) {
    Event event = SpeedTracerLogger.start(DevModeEventType.CSB_BUILD_FROM_ORACLE);
    try {
      return instance.doBuildFrom(logger, resources, delegate, suppressErrors);
    } finally {
      event.end();
    }
  }

  public static CompilationStateBuilder get() {
    return instance;
  }

  /**
   * Called to setup the directory where the persistent {@link CompilationUnit}
   * cache should be stored. Only the first call to init() will have an effect.
   */
  public static synchronized void init(TreeLogger logger, File cacheDirectory) {
    instance.unitCache = UnitCacheFactory.get(logger, cacheDirectory);
  }

  /**
   * A cache to store compilation units. This value may be overridden with an
   * explicit call to {@link #init(TreeLogger, File)}.
   */
  private UnitCache unitCache = new MemoryUnitCache();

  /**
   * Build a new compilation state from a source oracle. Allow the caller to
   * specify a compiler delegate that will handle undefined names.
   * 
   * TODO: maybe use a finer brush than to synchronize the whole thing.
   */
  public synchronized CompilationState doBuildFrom(TreeLogger logger, Set<Resource> resources,
      AdditionalTypeProviderDelegate compilerDelegate, boolean suppressErrors) {

    // Units we definitely want to build.
    List<CompilationUnitBuilder> builders = new ArrayList<CompilationUnitBuilder>();

    // Units we don't want to rebuild unless we have to.
    Map<CompilationUnitBuilder, CompilationUnit> cachedUnits =
        new IdentityHashMap<CompilationUnitBuilder, CompilationUnit>();

    CompileMoreLater compileMoreLater = new CompileMoreLater(compilerDelegate, suppressErrors);

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
    if (logger.isLoggable(TreeLogger.TRACE)) {
      logger.log(TreeLogger.TRACE, "Found " + cachedUnits.size() + " cached/archived units.  Used "
          + cachedUnits.size() + " / " + resources.size() + " units from cache.");
    }

    Collection<CompilationUnit> resultUnits =
        compileMoreLater.compile(logger, builders, cachedUnits,
            CompilerEventType.JDT_COMPILER_CSB_FROM_ORACLE, suppressErrors);
    return new CompilationState(logger, resultUnits, compileMoreLater);
  }

  public CompilationState doBuildFrom(TreeLogger logger, Set<Resource> resources,
      boolean suppressErrors) {
    return doBuildFrom(logger, resources, null, suppressErrors);
  }

  /**
   * Compile new generated units into an existing state.
   * 
   * TODO: maybe use a finer brush than to synchronize the whole thing.
   */
  synchronized Collection<CompilationUnit> doBuildGeneratedTypes(TreeLogger logger,
      Collection<GeneratedUnit> generatedUnits, CompileMoreLater compileMoreLater,
      boolean suppressErrors) {

    // Units we definitely want to build.
    List<CompilationUnitBuilder> builders = new ArrayList<CompilationUnitBuilder>();

    // Units we don't want to rebuild unless we have to.
    Map<CompilationUnitBuilder, CompilationUnit> cachedUnits =
        new IdentityHashMap<CompilationUnitBuilder, CompilationUnit>();

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
    return compileMoreLater.compile(logger, builders, cachedUnits,
        CompilerEventType.JDT_COMPILER_CSB_GENERATED, suppressErrors);
  }
}
