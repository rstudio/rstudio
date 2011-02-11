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
import com.google.gwt.dev.javac.CompilationUnitBuilder.GeneratedCompilationUnitBuilder;
import com.google.gwt.dev.javac.CompilationUnitBuilder.ResourceCompilationUnitBuilder;
import com.google.gwt.dev.javac.JdtCompiler.AdditionalTypeProviderDelegate;
import com.google.gwt.dev.javac.JdtCompiler.UnitProcessor;
import com.google.gwt.dev.jjs.CorrelationFactory.DummyCorrelationFactory;
import com.google.gwt.dev.js.ast.JsRootScope;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.util.StringInterner;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.DevModeEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.EventType;

import org.apache.commons.collections.map.AbstractReferenceMap;
import org.apache.commons.collections.map.ReferenceIdentityMap;
import org.apache.commons.collections.map.ReferenceMap;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;

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

/**
 * Manages a centralized cache for compiled units.
 */
public class CompilationStateBuilder {

  /**
   * An opaque class that lets you compile more units later.
   */
  public class CompileMoreLater {

    private final class UnitProcessorImpl implements UnitProcessor {

      public void process(CompilationUnitBuilder builder,
          CompilationUnitDeclaration cud, List<CompiledClass> compiledClasses) {
        Event event = SpeedTracerLogger.start(DevModeEventType.CSB_PROCESS);
        try {
          Map<MethodDeclaration, JsniMethod> jsniMethods = JsniCollector.collectJsniMethods(
              cud, builder.getSource(), JsRootScope.INSTANCE,
              DummyCorrelationFactory.INSTANCE);

          JSORestrictionsChecker.check(jsoState, cud);

          // JSNI check + collect dependencies.
          final Set<String> jsniDeps = new HashSet<String>();
          Map<String, Binding> jsniRefs = new HashMap<String, Binding>();
          JsniChecker.check(cud, jsoState, jsniMethods, jsniRefs,
              new JsniChecker.TypeResolver() {
                public ReferenceBinding resolveType(String typeName) {
                  ReferenceBinding resolveType = compiler.resolveType(typeName);
                  if (resolveType != null) {
                    jsniDeps.add(String.valueOf(resolveType.qualifiedSourceName()));
                  }
                  return resolveType;
                }
              });

          ArtificialRescueChecker.check(cud, builder.isGenerated());
          BinaryTypeReferenceRestrictionsChecker.check(cud);

          MethodArgNamesLookup methodArgs = MethodParamCollector.collect(cud);

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
          Dependencies dependencies = new Dependencies(packageName,
              unresolvedQualified, unresolvedSimple, apiRefs);

          CompilationUnit unit = builder.build(compiledClasses, dependencies,
              jsniMethods.values(), methodArgs,
              cud.compilationResult().getProblems());
          addValidUnit(unit);
          // Cache the valid unit for future compiles.
          ContentId contentId = builder.getContentId();
          unitCache.put(contentId, unit);
          if (builder instanceof ResourceCompilationUnitBuilder) {
            ResourceCompilationUnitBuilder rcub = (ResourceCompilationUnitBuilder) builder;
            ResourceTag resourceTag = new ResourceTag(rcub.getLastModifed(),
                contentId);
            resourceContentCache.put(builder.getLocation(), resourceTag);
            keepAliveLatestVersion.put(resourceTag, unit);
          } else if (builder instanceof GeneratedCompilationUnitBuilder) {
            keepAliveRecentlyGenerated.put(unit.getTypeName(), unit);
          }
          newlyBuiltUnits.add(unit);
        } finally {
          event.end();
        }
      }
    }

    /**
     * A global cache of all currently-valid class files. This is used to
     * validate dependencies when reusing previously cached units, to make sure
     * they can be recompiled if necessary.
     */
    private final Map<String, CompiledClass> allValidClasses = new HashMap<String, CompiledClass>();

    /**
     * The JDT compiler.
     */
    private final JdtCompiler compiler = new JdtCompiler(
        new UnitProcessorImpl());

    /**
     * Continuation state for JSNI checking.
     */
    private final JSORestrictionsChecker.CheckerState jsoState = new JSORestrictionsChecker.CheckerState();

    private transient Collection<CompilationUnit> newlyBuiltUnits;

    public CompileMoreLater(AdditionalTypeProviderDelegate delegate) {
      compiler.setAdditionalTypeProviderDelegate(delegate);
    }

    public Collection<CompilationUnit> addGeneratedTypes(TreeLogger logger,
        Collection<GeneratedUnit> generatedUnits) {
      Event event = 
        SpeedTracerLogger.start(DevModeEventType.CSB_ADD_GENERATED_TYPES);
      try {
        return doBuildGeneratedTypes(logger, generatedUnits, this);
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
        String sourceName = cc.getSourceName();
        allValidClasses.put(sourceName, cc);
      }
    }

    Collection<CompilationUnit> compile(TreeLogger logger,
        Collection<CompilationUnitBuilder> builders,
        Map<CompilationUnitBuilder, CompilationUnit> cachedUnits,
        EventType eventType) {
      // Initialize the set of valid classes to the initially cached units.
      for (CompilationUnit unit : cachedUnits.values()) {
        for (CompiledClass cc : unit.getCompiledClasses()) {
          // Map by source name.
          String sourceName = cc.getSourceName();
          allValidClasses.put(sourceName, cc);
        }
      }
      Map<CompiledClass, CompiledClass> cachedStructurallySame = new IdentityHashMap<CompiledClass, CompiledClass>();

      ArrayList<CompilationUnit> resultUnits = new ArrayList<CompilationUnit>();
      do {
        // Compile anything that needs to be compiled.
        this.newlyBuiltUnits = new ArrayList<CompilationUnit>();
        
        Event jdtCompilerEvent = SpeedTracerLogger.start(eventType);
        try {
          compiler.doCompile(builders);
        } finally {
          jdtCompilerEvent.end();
        }
        
        resultUnits.addAll(this.newlyBuiltUnits);
        builders.clear();

        // Resolve all newly built unit deps against the global classes.
        for (CompilationUnit unit : this.newlyBuiltUnits) {
          unit.getDependencies().resolve(allValidClasses);
        }

        /*
         * Invalidate any cached units with invalid refs.
         */
        Collection<CompilationUnit> invalidatedUnits = new ArrayList<CompilationUnit>();
        for (Iterator<Entry<CompilationUnitBuilder, CompilationUnit>> it = cachedUnits.entrySet().iterator(); it.hasNext();) {
          Entry<CompilationUnitBuilder, CompilationUnit> entry = it.next();
          CompilationUnit unit = entry.getValue();
          boolean isValid = unit.getDependencies().validate(allValidClasses,
              cachedStructurallySame);
          if (!isValid) {
            invalidatedUnits.add(unit);
            builders.add(entry.getKey());
            it.remove();
          }
        }

        // Any units we invalidated must now be removed from the valid classes.
        for (CompilationUnit unit : invalidatedUnits) {
          for (CompiledClass cc : unit.getCompiledClasses()) {
            allValidClasses.remove(cc.getSourceName());
          }
        }
      } while (builders.size() > 0);

      // Any remaining cached units are valid now.
      resultUnits.addAll(cachedUnits.values());

      // Sort, then report all errors (re-report for cached units).
      Collections.sort(resultUnits, CompilationUnit.COMPARATOR);
      logger = logger.branch(TreeLogger.DEBUG,
          "Validating newly compiled units");
      for (CompilationUnit unit : resultUnits) {
        CompilationUnitInvalidator.reportErrors(logger, unit);
      }
      return resultUnits;
    }
  }

  /**
   * A snapshot of a source file at a particular point in time.
   */
  static class ResourceTag {
    /**
     * A Java type name + content hash. E.g.
     * <code>"java.lang.String:1234DEADBEEF"</code>.
     */
    private final ContentId contentId;
    /**
     * The last modification time, for quick freshness checks.
     */
    private final long lastModified;

    public ResourceTag(long lastModified, ContentId contentId) {
      this.lastModified = lastModified;
      this.contentId = contentId;
    }

    public ContentId getContentId() {
      return contentId;
    }

    public long getLastModified() {
      return lastModified;
    }
  }

  private static final CompilationStateBuilder instance = new CompilationStateBuilder();

  public static CompilationState buildFrom(TreeLogger logger,
      Set<Resource> resources) {
    Event event = SpeedTracerLogger.start(DevModeEventType.CSB_BUILD_FROM_ORACLE);
    try {
      return instance.doBuildFrom(logger, resources, null);
    } finally {
      event.end();
    }
  }

  public static CompilationState buildFrom(TreeLogger logger,
      Set<Resource> resources, AdditionalTypeProviderDelegate delegate) {
    Event event = SpeedTracerLogger.start(DevModeEventType.CSB_BUILD_FROM_ORACLE);
    try {
      return instance.doBuildFrom(logger, resources, delegate);
    } finally {
      event.end();
    }
  }

  public static CompilationStateBuilder get() {
    return instance;
  }

  /**
   * This map of weak keys to hard values exists solely to keep the most recent
   * version of any unit from being eagerly garbage collected.
   * 
   * Once a resource gets its tag updated, the {@link #resourceContentCache}
   * will drop the only hard reference to that resourceTag which transitively
   * removes the old value from this cache.
   * 
   * WRITE-ONLY
   */
  @SuppressWarnings("unchecked")
  private final Map<ResourceTag, CompilationUnit> keepAliveLatestVersion = Collections.synchronizedMap(new ReferenceIdentityMap(
      AbstractReferenceMap.WEAK, AbstractReferenceMap.HARD));

  /**
   * This map of hard keys to soft values exists solely to keep the most
   * recently generated version of a type from being eagerly garbage collected.
   * 
   * WRITE-ONLY
   */
  @SuppressWarnings("unchecked")
  private final Map<String, CompilationUnit> keepAliveRecentlyGenerated = Collections.synchronizedMap(new ReferenceMap(
      AbstractReferenceMap.HARD, AbstractReferenceMap.SOFT));

  /**
   * Holds a tag for the last seen content for a each resource location. Should
   * be bounded by the total number of accessible Java source files on the
   * system that we try to compile.
   */
  private final Map<String, ResourceTag> resourceContentCache = Collections.synchronizedMap(new HashMap<String, ResourceTag>());

  /**
   * A map of all previously compiled units by contentId.
   * 
   * TODO: ideally this would be a list of units because the same unit can be
   * compiled multiple ways based on its dependencies.
   */
  @SuppressWarnings("unchecked")
  private final Map<ContentId, CompilationUnit> unitCache = Collections.synchronizedMap(new ReferenceMap(
      AbstractReferenceMap.HARD, AbstractReferenceMap.WEAK));

  /**
   * Build a new compilation state from a source oracle.
   */
  public CompilationState doBuildFrom(TreeLogger logger, Set<Resource> resources) {
    return doBuildFrom(logger, resources, null);
  }

  /**
   * Build a new compilation state from a source oracle. Allow the caller to
   * specify a compiler delegate that will handle undefined names.
   * 
   * TODO: maybe use a finer brush than to synchronize the whole thing.
   */
  public synchronized CompilationState doBuildFrom(TreeLogger logger,
      Set<Resource> resources, AdditionalTypeProviderDelegate compilerDelegate) {
    // Units we definitely want to build.
    List<CompilationUnitBuilder> builders = new ArrayList<CompilationUnitBuilder>();

    // Units we don't want to rebuild unless we have to.
    Map<CompilationUnitBuilder, CompilationUnit> cachedUnits = new IdentityHashMap<CompilationUnitBuilder, CompilationUnit>();

    CompileMoreLater compileMoreLater = new CompileMoreLater(compilerDelegate);

    // For each incoming Java source file...
    for (Resource resource : resources) {
      String typeName = Shared.toTypeName(resource.getPath());
      // Create a builder for all incoming units.
      ResourceCompilationUnitBuilder builder = new ResourceCompilationUnitBuilder(
          typeName, resource);
      // Try to get an existing unit from the cache.
      String location = resource.getLocation();
      ResourceTag tag = resourceContentCache.get(location);
      if (tag != null && tag.getLastModified() == resource.getLastModified()) {
        ContentId contentId = tag.getContentId();
        CompilationUnit existingUnit = unitCache.get(contentId);
        if (existingUnit != null) {
          cachedUnits.put(builder, existingUnit);
          compileMoreLater.addValidUnit(existingUnit);
          continue;
        }
      }
      builders.add(builder);
    }
    Collection<CompilationUnit> resultUnits = compileMoreLater.compile(logger,
        builders, cachedUnits, CompilerEventType.JDT_COMPILER_CSB_FROM_ORACLE);
    return new CompilationState(logger, resultUnits, compileMoreLater);
  }

  /**
   * Compile new generated units into an existing state.
   * 
   * TODO: maybe use a finer brush than to synchronize the whole thing.
   */
  synchronized Collection<CompilationUnit> doBuildGeneratedTypes(
      TreeLogger logger, Collection<GeneratedUnit> generatedUnits,
      CompileMoreLater compileMoreLater) {

    // Units we definitely want to build.
    List<CompilationUnitBuilder> builders = new ArrayList<CompilationUnitBuilder>();

    // Units we don't want to rebuild unless we have to.
    Map<CompilationUnitBuilder, CompilationUnit> cachedUnits = new IdentityHashMap<CompilationUnitBuilder, CompilationUnit>();

    // For each incoming generated Java source file...
    for (GeneratedUnit generatedUnit : generatedUnits) {
      // Create a builder for all incoming units.
      GeneratedCompilationUnitBuilder builder = new GeneratedCompilationUnitBuilder(
          generatedUnit);
      // Try to get an existing unit from the cache.
      ContentId contentId = new ContentId(generatedUnit.getTypeName(),
          generatedUnit.getStrongHash());
      CompilationUnit existingUnit = unitCache.get(contentId);
      if (existingUnit != null) {
        cachedUnits.put(builder, existingUnit);
        compileMoreLater.addValidUnit(existingUnit);
      } else {
        builders.add(builder);
      }
    }
    return compileMoreLater.compile(logger, builders, cachedUnits,
     CompilerEventType.JDT_COMPILER_CSB_GENERATED);
  }
}
