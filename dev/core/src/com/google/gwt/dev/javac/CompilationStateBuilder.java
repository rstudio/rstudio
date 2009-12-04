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
import com.google.gwt.dev.javac.JdtCompiler.UnitProcessor;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.resource.Resource;

import org.apache.commons.collections.map.AbstractReferenceMap;
import org.apache.commons.collections.map.ReferenceIdentityMap;
import org.apache.commons.collections.map.ReferenceMap;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

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

        Map<AbstractMethodDeclaration, JsniMethod> jsniMethods = JsniCollector.collectJsniMethods(
            cud, builder.getSource(), jsProgram);

        // JSNI check + collect dependencies.
        final Set<String> jsniDeps = new HashSet<String>();
        JsniChecker.check(cud, jsniMethods, new JsniChecker.TypeResolver() {
          public ReferenceBinding resolveType(String typeName) {
            ReferenceBinding resolveType = compiler.resolveType(typeName);
            if (resolveType != null) {
              String fileName = String.valueOf(resolveType.getFileName());
              jsniDeps.add(fileName);
            }
            return resolveType;
          }
        });

        JSORestrictionsChecker.check(jsoState, cud);
        ArtificialRescueChecker.check(cud, builder.isGenerated());
        BinaryTypeReferenceRestrictionsChecker.check(cud);

        // TODO: Collect parameter names?

        CompilationUnitInvalidator.reportErrors(logger, cud,
            builder.getSource());

        Set<ContentId> dependencies = compiler.computeDependencies(cud,
            jsniDeps);
        CompilationUnit unit = builder.build(compiledClasses, dependencies,
            jsniMethods.values(), cud.compilationResult().getProblems());
        if (cud.compilationResult().hasErrors()) {
          unit = new ErrorCompilationUnit(unit);
        } else {
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
        }
        resultUnits.put(unit.getTypeName(), unit);
      }
    }

    /**
     * The JDT compiler.
     */
    private final JdtCompiler compiler = new JdtCompiler(
        new UnitProcessorImpl());

    /**
     * Continuation state for JSNI checking.
     */
    private final JSORestrictionsChecker.CheckerState jsoState = new JSORestrictionsChecker.CheckerState();

    private transient TreeLogger logger;

    private transient Map<String, CompilationUnit> resultUnits;
    private final Set<ContentId> validDependencies = new HashSet<ContentId>();

    public Collection<CompilationUnit> addGeneratedTypes(TreeLogger logger,
        Collection<GeneratedUnit> generatedUnits) {
      return doBuildGeneratedTypes(logger, generatedUnits, this);
    }

    void addValidUnit(CompilationUnit unit) {
      assert unit.isCompiled();
      compiler.addCompiledUnit(unit);
      validDependencies.add(unit.getContentId());
    }

    void compile(TreeLogger logger,
        Collection<CompilationUnitBuilder> builders,
        Map<String, CompilationUnit> resultUnits) {
      this.logger = logger.branch(TreeLogger.DEBUG,
          "Validating newly compiled units");
      this.resultUnits = resultUnits;
      compiler.doCompile(builders);
    }

    Set<ContentId> getValidDependencies() {
      return validDependencies;
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
    return instance.doBuildFrom(logger, resources);
  }

  public static CompilationStateBuilder get() {
    return instance;
  }

  private static void invalidateUnitsWithInvalidRefs(TreeLogger logger,
      Map<String, CompilationUnit> resultUnits, Set<ContentId> set) {
    Set<CompilationUnit> validResultUnits = new HashSet<CompilationUnit>(
        resultUnits.values());
    CompilationUnitInvalidator.retainValidUnits(logger, validResultUnits, set);
    for (Entry<String, CompilationUnit> entry : resultUnits.entrySet()) {
      CompilationUnit unit = entry.getValue();
      if (unit.isCompiled() && !validResultUnits.contains(unit)) {
        entry.setValue(new InvalidCompilationUnit(unit));
      }
    }
  }

  /**
   * JsProgram for collecting JSNI methods.
   */
  private final JsProgram jsProgram = new JsProgram();

  /**
   * This map of weak keys to hard values exists solely to keep the most recent
   * version of any unit from being eagerly garbage collected.
   */
  @SuppressWarnings("unchecked")
  private final Map<ResourceTag, CompilationUnit> keepAliveLatestVersion = Collections.synchronizedMap(new ReferenceIdentityMap(
      AbstractReferenceMap.WEAK, AbstractReferenceMap.HARD));

  /**
   * This map of hard keys to soft values exists solely to keep the most
   * recently generated version of a type from being eagerly garbage collected.
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
   * 
   * TODO: maybe use a finer brush than to synchronize the whole thing.
   */
  public synchronized CompilationState doBuildFrom(TreeLogger logger,
      Set<Resource> resources) {
    Map<String, CompilationUnit> resultUnits = new HashMap<String, CompilationUnit>();

    // For each incoming Java source file...
    for (Resource resource : resources) {
      // Try to get an existing unit from the cache.
      String location = resource.getLocation();
      ResourceTag tag = resourceContentCache.get(location);
      if (tag != null && tag.getLastModified() == resource.getLastModified()) {
        ContentId contentId = tag.getContentId();
        CompilationUnit existingUnit = unitCache.get(contentId);
        if (existingUnit != null && existingUnit.isCompiled()) {
          resultUnits.put(existingUnit.getTypeName(), existingUnit);
        }
      }
    }

    // Winnow the reusable set of units down to those still valid.
    CompilationUnitInvalidator.retainValidUnits(TreeLogger.NULL,
        resultUnits.values());

    // Compile everything else.
    CompileMoreLater compileMoreLater = new CompileMoreLater();
    List<CompilationUnitBuilder> builders = new ArrayList<CompilationUnitBuilder>();
    for (Resource resource : resources) {
      String typeName = Shared.toTypeName(resource.getPath());
      CompilationUnit validUnit = resultUnits.get(typeName);
      if (validUnit != null) {
        compileMoreLater.addValidUnit(validUnit);
        // Report any existing errors as if the unit were recompiled.
        CompilationUnitInvalidator.reportErrors(logger, validUnit);
      } else {
        builders.add(new ResourceCompilationUnitBuilder(typeName, resource));
      }
    }
    compileMoreLater.compile(logger, builders, resultUnits);

    // Invalidate units with invalid refs.
    invalidateUnitsWithInvalidRefs(logger, resultUnits,
        Collections.<ContentId> emptySet());
    return new CompilationState(logger, resultUnits.values(), compileMoreLater);
  }

  /**
   * Compile new generated units into an existing state.
   * 
   * TODO: maybe use a finer brush than to synchronize the whole thing.
   */
  synchronized Collection<CompilationUnit> doBuildGeneratedTypes(
      TreeLogger logger, Collection<GeneratedUnit> generatedUnits,
      CompileMoreLater compileMoreLater) {
    Map<String, CompilationUnit> resultUnits = new HashMap<String, CompilationUnit>();

    // For each incoming generated Java source file...
    for (GeneratedUnit generatedUnit : generatedUnits) {
      // Try to get an existing unit from the cache.
      ContentId contentId = new ContentId(generatedUnit.getTypeName(),
          generatedUnit.getStrongHash());
      CompilationUnit existingUnit = unitCache.get(contentId);
      if (existingUnit != null && existingUnit.isCompiled()) {
        resultUnits.put(existingUnit.getTypeName(), existingUnit);
      }
    }

    // Winnow the reusable set of units down to those still valid.
    CompilationUnitInvalidator.retainValidUnits(TreeLogger.NULL,
        resultUnits.values(), compileMoreLater.getValidDependencies());
    for (CompilationUnit validUnit : resultUnits.values()) {
      compileMoreLater.addValidUnit(validUnit);
      // Report any existing errors as if the unit were recompiled.
      CompilationUnitInvalidator.reportErrors(logger, validUnit);
    }

    // Compile everything else.
    List<CompilationUnitBuilder> builders = new ArrayList<CompilationUnitBuilder>();
    for (GeneratedUnit generatedUnit : generatedUnits) {
      if (!resultUnits.containsKey(generatedUnit.getTypeName())) {
        builders.add(new GeneratedCompilationUnitBuilder(generatedUnit));
      }
    }

    compileMoreLater.compile(logger, builders, resultUnits);
    invalidateUnitsWithInvalidRefs(logger, resultUnits,
        compileMoreLater.getValidDependencies());
    return resultUnits.values();
  }
}
