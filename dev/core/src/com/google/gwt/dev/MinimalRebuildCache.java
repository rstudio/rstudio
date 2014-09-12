/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.StatementRanges;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.javac.CompilationUnit;
import com.google.gwt.dev.javac.CompiledClass;
import com.google.gwt.dev.javac.GeneratedUnit;
import com.google.gwt.dev.javac.Shared;
import com.google.gwt.dev.jjs.JsSourceMap;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JTypeOracle;
import com.google.gwt.dev.jjs.ast.JTypeOracle.ImmediateTypeRelations;
import com.google.gwt.dev.jjs.impl.ResolveRuntimeTypeReferences.IntTypeIdGenerator;
import com.google.gwt.dev.js.JsPersistentPrettyNamer.PersistentPrettyNamerState;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.util.Name.InternalName;
import com.google.gwt.thirdparty.guava.common.annotations.VisibleForTesting;
import com.google.gwt.thirdparty.guava.common.base.Objects;
import com.google.gwt.thirdparty.guava.common.collect.HashMultimap;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableList;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Multimap;
import com.google.gwt.thirdparty.guava.common.collect.Multimaps;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * MinimalRebuildCache contains compiler information that can be persisted between compiles to
 * decrease compilation time.
 * <p>
 * Cached information is specific to a single permutation and it is the responsibility of the
 * framework driving the Compiler to supply the right Cache instance for the right Module
 * configuration and to make sure that the Compiler is only processing one permutation at a time.
 * <p>
 * All type names referenced here are assumed to be binary names.
 * <p>
 * A "typeName" here might be a root type or nested type but a "compilationUnitName" here will
 * always be the name of just the root type in a compilation unit.
 */
public class MinimalRebuildCache implements Serializable {

  private static void appendSubTypes(Set<String> accumulatedTypeNames, Set<String> parentTypeNames,
      JTypeOracle typeOracle) {
    for (String parentTypeName : parentTypeNames) {
      Set<String> subTypeNames = typeOracle.getSubTypeNames(parentTypeName);
      if (subTypeNames == null) {
        // It must be a new type, thus there are no subtypes to invalidate.
        continue;
      }
      accumulatedTypeNames.addAll(subTypeNames);
    }
  }

  /**
   * Diffs lastModifiedByResourcePath from the previous compile against currentResources from the
   * current compile. modifiedResourcePaths is wiped and recreated to be a list of just the modified
   * or deleted resources, deletedResourcePaths is wiped and recreated to be a list of just the
   * deleted resources and modifiedResourcePaths is updated in place with new lastModified dates.
   */
  private static void recordModifiedResources(Map<String, Long> currentModifiedByResourcePath,
      Map<String, Long> lastModifiedByResourcePath, Set<String> modifiedResourcePaths,
      Set<String> deletedResourcePaths) {
    deletedResourcePaths.clear();
    modifiedResourcePaths.clear();

    Set<String> currentResourcePaths = Sets.newHashSet();
    for (Entry<String, Long> entry : currentModifiedByResourcePath.entrySet()) {
      String currentResourcePath = entry.getKey();
      Long currentResourceModified = entry.getValue();

      currentResourcePaths.add(currentResourcePath);
      Long lastKnownModified =
          lastModifiedByResourcePath.put(currentResourcePath, currentResourceModified);
      if (!Objects.equal(lastKnownModified, currentResourceModified)) {
        // Added or Modified resource.
        modifiedResourcePaths.add(currentResourcePath);
      }
    }

    // Removed resources.
    {
      // Figure out which resources were removed.
      Set<String> removedResourcePaths = Sets.newHashSet(
          Sets.difference(lastModifiedByResourcePath.keySet(), currentResourcePaths));
      // Log them as "modified".
      deletedResourcePaths.addAll(removedResourcePaths);
      modifiedResourcePaths.addAll(removedResourcePaths);
      // Remove any path to modified date entries for them.
      for (String removedResourcePath : removedResourcePaths) {
        lastModifiedByResourcePath.remove(removedResourcePath);
      }
    }
  }

  private static Set<String> resourcePathsToCompilationUnitNames(Set<String> resourcePaths) {
    Set<String> compilationUnitNames = Sets.newHashSet();
    for (String resourcePath : resourcePaths) {
      compilationUnitNames.add(Shared.toTypeName(resourcePath));
    }
    return compilationUnitNames;
  }

  private static Map<String, Long> resourcesToModifiedByPath(Collection<Resource> resources) {
    Map<String, Long> modifiedByPath = Maps.newHashMap();
    for (Resource resource : resources) {
      modifiedByPath.put(resource.getPath(), resource.getLastModified());
    }
    return modifiedByPath;
  }

  protected final ImmediateTypeRelations immediateTypeRelations = new ImmediateTypeRelations();
  private final Map<String, String> compilationUnitTypeNameByNestedTypeName = Maps.newHashMap();
  private final Map<String, String> contentHashByGeneratedTypeName = Maps.newHashMap();
  private final Set<String> deletedCompilationUnitNames = Sets.newHashSet();
  private final Set<String> deletedDiskSourcePaths = Sets.newHashSet();
  private final Set<String> deletedResourcePaths = Sets.newHashSet();
  private final Set<String> dualJsoImplInterfaceNames = Sets.newHashSet();
  private final ArtifactSet generatedArtifacts = new ArtifactSet();
  private final Multimap<String, String> generatedCompilationUnitNamesByReboundTypeNames =
      HashMultimap.create();
  private final IntTypeIdGenerator intTypeIdGenerator = new IntTypeIdGenerator();
  private final Map<String, String> jsByTypeName = Maps.newHashMap();
  private final Set<String> jsoStatusChangedTypeNames = Sets.newHashSet();
  private final Set<String> jsoTypeNames = Sets.newHashSet();
  private Integer lastLinkedJsBytes;
  private final Map<String, Long> lastModifiedByDiskSourcePath = Maps.newHashMap();
  private final Map<String, Long> lastModifiedByResourcePath = Maps.newHashMap();
  private final Set<String> modifiedCompilationUnitNames = Sets.newHashSet();
  private final Set<String> modifiedDiskSourcePaths = Sets.newHashSet();
  private final Set<String> modifiedResourcePaths = Sets.newHashSet();
  private final Multimap<String, String> nestedTypeNamesByUnitTypeName = HashMultimap.create();
  private final PersistentPrettyNamerState persistentPrettyNamerState =
      new PersistentPrettyNamerState();
  private final Set<String> preambleTypeNames = Sets.newHashSet();
  private final Multimap<String, String> rebinderTypeNamesByReboundTypeName = HashMultimap.create();
  private final Multimap<String, String> reboundTypeNamesByGeneratedCompilationUnitNames =
      HashMultimap.create();
  private final Multimap<String, String> reboundTypeNamesByInputResource = HashMultimap.create();
  private final Multimap<String, String> referencedTypeNamesByTypeName = HashMultimap.create();
  private final Set<String> rootTypeNames = Sets.newHashSet();
  private final Set<String> singleJsoImplInterfaceNames = Sets.newHashSet();
  private final Map<String, JsSourceMap> sourceMapsByTypeName = Maps.newHashMap();
  private final Set<String> staleTypeNames = Sets.newHashSet();
  private final Map<String, StatementRanges> statementRangesByTypeName = Maps.newHashMap();
  private final Multimap<String, String> typeNamesByReferencingTypeName = HashMultimap.create();

  /**
   * Accumulates generated artifacts so that they can be output on recompiles even if no generators
   * are run.
   */
  public void addGeneratedArtifacts(ArtifactSet generatedArtifacts) {
    this.generatedArtifacts.addAll(generatedArtifacts);
  }

  public void addModifiedCompilationUnitNames(TreeLogger logger,
      Set<String> modifiedCompilationUnitNames) {
    logger.log(TreeLogger.DEBUG, "adding to cached list of known modified compilation units "
        + modifiedCompilationUnitNames);
    this.modifiedCompilationUnitNames.addAll(modifiedCompilationUnitNames);
  }

  public void addTypeReference(String fromTypeName, String toTypeName) {
    referencedTypeNamesByTypeName.put(fromTypeName, toTypeName);
    typeNamesByReferencingTypeName.put(toTypeName, fromTypeName);
  }

  public void associateReboundTypeWithGeneratedCompilationUnitName(String reboundTypeName,
      String generatedCompilationUnitName) {
    reboundTypeNamesByGeneratedCompilationUnitNames.put(generatedCompilationUnitName,
        reboundTypeName);
  }

  /**
   * Record that a Generator that was ran as a result of a GWT.create(ReboundType.class) call read a
   * particular resource.
   */
  public void associateReboundTypeWithInputResource(String reboundTypeName,
      String inputResourcePath) {
    reboundTypeNamesByInputResource.put(inputResourcePath, reboundTypeName);
  }

  public void clearPerTypeJsCache() {
    rootTypeNames.clear();
    preambleTypeNames.clear();

    deletedResourcePaths.clear();
    modifiedResourcePaths.clear();
    lastModifiedByResourcePath.clear();

    deletedDiskSourcePaths.clear();
    modifiedDiskSourcePaths.clear();
    lastModifiedByDiskSourcePath.clear();
    contentHashByGeneratedTypeName.clear();

    jsByTypeName.clear();
    referencedTypeNamesByTypeName.clear();
    sourceMapsByTypeName.clear();
    statementRangesByTypeName.clear();
    typeNamesByReferencingTypeName.clear();
  }

  public void clearRebinderTypeAssociations(String rebinderTypeName) {
    rebinderTypeNamesByReboundTypeName.values().remove(rebinderTypeName);
  }

  public void clearReboundTypeAssociations(String reboundTypeName) {
    reboundTypeNamesByInputResource.values().remove(reboundTypeName);
    reboundTypeNamesByGeneratedCompilationUnitNames.values().remove(reboundTypeName);
  }

  /**
   * Calculates the set of stale types and clears their cached Js, StatementRanges and SourceMaps.
   * <p>
   * The calculation of stale types starts with the list of known modified types and expands that
   * set using various patterns intended to find any types whose output JS would be affected by the
   * changes in the modified types. For example if the parent of class Foo was changed then the
   * castmaps in all children of Foo need to be recreated.
   * <p>
   * In some ways this process is similar to that performed by the CompilationUnitInvalidator but it
   * differs both in what type of cached objects are being cleared (JS versus CompilationUnits) and
   * in what invalidation rules must be applied. CompilationUnitInvalidator is concerned only with
   * changes in interface while the invalidation here must also look at JSO promotion/demotion and
   * cast references because of the peculiarities of output JS format.
   */
  public Set<String> clearStaleTypeJsAndStatements(TreeLogger logger, JTypeOracle typeOracle) {
    if (immediateTypeRelations.isEmpty()) {
      return Sets.newHashSet();
    }

    // Cache the ReboundTypeNames -> GeneratedCompilationUnitNames reverse map as it will be needed
    // several times.
    generatedCompilationUnitNamesByReboundTypeNames.clear();
    Multimaps.invertFrom(reboundTypeNamesByGeneratedCompilationUnitNames,
        generatedCompilationUnitNamesByReboundTypeNames);

    // Store stale type names in a persisted field so that tests can inspect behavior.
    staleTypeNames.clear();

    Set<String> modifiedTypeNames = computeModifiedTypeNames();

    // Accumulate the names of stale types resulting from some known type or resource modifications,
    // using various patterns (sub types, referencing types, etc).
    {
      staleTypeNames.addAll(modifiedTypeNames);
      appendSubTypes(staleTypeNames, modifiedTypeNames, typeOracle);
      // Marks stale any type that references either a modified type or any subtype of a modified
      // type. The references of subtypes can be done away with once we revamp Array castmap
      // generation.
      ImmutableList<String> modifiedTypeAndSubTypeNames = ImmutableList.copyOf(staleTypeNames);
      appendReferencingTypes(staleTypeNames, modifiedTypeAndSubTypeNames);
      appendReferencingTypes(staleTypeNames, jsoStatusChangedTypeNames);
      staleTypeNames.addAll(
          computeTypesThatRebindTypes(computeReboundTypesAffectedByModifiedResources()));
      appendTypesToRegenerateStaleGeneratedTypes(staleTypeNames);

      // Generator output is affected by types queried from the TypeOracle but changes in these
      // types are not being directly supported at this time since some of them are already handled
      // because they are referenced by the Generator output and since changes in subtype queries
      // probably make GWTRPC output incompatible with a server anyway (and thus already forces a
      // restart).

      staleTypeNames.removeAll(JProgram.SYNTHETIC_TYPE_NAMES);
    }

    // These log lines can be expensive.
    if (logger.isLoggable(TreeLogger.DEBUG)) {
      logger.log(TreeLogger.DEBUG, "known modified types = " + modifiedTypeNames);
      logger.log(TreeLogger.DEBUG, "known modified resources = " + modifiedResourcePaths);
      logger.log(TreeLogger.DEBUG,
          "clearing cached output for resulting stale types = " + staleTypeNames);
    }

    for (String staleTypeName : staleTypeNames) {
      clearCachedTypeOutput(staleTypeName);
    }

    return Sets.newHashSet(staleTypeNames);
  }

  /**
   * Computes and returns the set of names of deleted types.
   * <p>
   * Deleted types are all those types that are nested within compilation units that are known to
   * have been deleted.
   */
  public Set<String> computeDeletedTypeNames() {
    return computeNestedTypeNames(deletedCompilationUnitNames);
  }

  /**
   * Computes and returns the set of names of modified types.
   * <p>
   * Modified types are all those types that are nested within compilation units that are known to
   * have been modified or were generated by a Generator that is known to have had its output
   * invalidated.
   */
  public Set<String> computeModifiedTypeNames() {
    // Accumulate the names of types that are nested within known modified compilation units.
    Set<String> modifiedTypeNames = Sets.newHashSet();
    modifiedTypeNames.addAll(computeNestedTypeNames(modifiedCompilationUnitNames));

    // Accumulate the names of types that are nested within compilation units that are expected to
    // be modified once invalidated Generators have been run (the Generators were invalidated
    // because they read resources that are known to be modified).
    Set<String> affectedReboundTypeNames = computeReboundTypesAffectedByModifiedResources();
    Set<String> assumedModifiedGeneratedCompilationUnitNames =
        computeCompilationUnitsGeneratedForReboundTypes(affectedReboundTypeNames);
    modifiedTypeNames.addAll(computeNestedTypeNames(assumedModifiedGeneratedCompilationUnitNames));

    return modifiedTypeNames;
  }

  /**
   * Computes and returns the names of the set of types that are transitively referenceable starting
   * from the set of root types.
   */
  public Set<String> computeReachableTypeNames() {
    Set<String> openTypeNames = Sets.newHashSet(rootTypeNames);
    Set<String> reachableTypeNames = Sets.newHashSet();

    while (!openTypeNames.isEmpty()) {
      Iterator<String> iterator = openTypeNames.iterator();
      String toProcessTypeName = iterator.next();
      iterator.remove();

      reachableTypeNames.add(toProcessTypeName);

      Collection<String> referencedTypes = referencedTypeNamesByTypeName.get(toProcessTypeName);
      for (String referencedType : referencedTypes) {
        if (reachableTypeNames.contains(referencedType)) {
          continue;
        }
        openTypeNames.add(referencedType);
      }
    }
    return reachableTypeNames;
  }

  public ArtifactSet getGeneratedArtifacts() {
    return generatedArtifacts;
  }

  public ImmediateTypeRelations getImmediateTypeRelations() {
    return immediateTypeRelations;
  }

  /**
   * Returns true if there is cached data to reuse in the next recompile.
   */
  public boolean isPopulated() {
    return !immediateTypeRelations.isEmpty();
  }

  public IntTypeIdGenerator getIntTypeIdGenerator() {
    return intTypeIdGenerator;
  }

  public String getJs(String typeName) {
    return jsByTypeName.get(typeName);
  }

  public int getLastLinkedJsBytes() {
    return lastLinkedJsBytes;
  }

  @VisibleForTesting
  public Set<String> getModifiedCompilationUnitNames() {
    return modifiedCompilationUnitNames;
  }

  public PersistentPrettyNamerState getPersistentPrettyNamerState() {
    return persistentPrettyNamerState;
  }

  public Set<String> getPreambleTypeNames() {
    return preambleTypeNames;
  }

  public JsSourceMap getSourceMap(String typeName) {
    return sourceMapsByTypeName.get(typeName);
  }

  @VisibleForTesting
  public Set<String> getStaleTypeNames() {
    return staleTypeNames;
  }

  public StatementRanges getStatementRanges(String typeName) {
    return statementRangesByTypeName.get(typeName);
  }

  public boolean hasJs(String typeName) {
    return jsByTypeName.containsKey(typeName);
  }

  public boolean hasPreambleTypeNames() {
    return !preambleTypeNames.isEmpty();
  }

  public boolean knowsLastLinkedJsBytes() {
    return lastLinkedJsBytes != null;
  }

  /**
   * Records the resource paths and modification dates of build resources in the current compile and
   * builds a list of known modified resource paths by comparing the resource paths and modification
   * dates of build resources in the previous compile with those of the current compile.
   */
  public void recordBuildResources(ModuleDef module) {
    Map<String, Long> currentModifiedByResourcePath =
        resourcesToModifiedByPath(module.getBuildResourceOracle().getResources());
    recordModifiedResources(currentModifiedByResourcePath, lastModifiedByResourcePath,
        modifiedResourcePaths, deletedResourcePaths);
  }

  /**
   * Records the paths and modification dates of source resources in the current compile and builds
   * a list of known modified paths by comparing the paths and modification dates of source
   * resources in the previous compile with those of the current compile.
   */
  @VisibleForTesting
  public void recordDiskSourceResources(Map<String, Long> currentModifiedByDiskSourcePath) {
    recordModifiedResources(currentModifiedByDiskSourcePath, lastModifiedByDiskSourcePath,
        modifiedDiskSourcePaths, deletedDiskSourcePaths);

    deletedCompilationUnitNames.clear();
    deletedCompilationUnitNames.addAll(resourcePathsToCompilationUnitNames(deletedDiskSourcePaths));
    modifiedCompilationUnitNames.clear();
    modifiedCompilationUnitNames.addAll(
        resourcePathsToCompilationUnitNames(modifiedDiskSourcePaths));
  }

  /**
   * Records the paths and modification dates of source resources in the current compile and builds
   * a list of known modified paths by comparing the paths and modification dates of source
   * resources in the previous compile with those of the current compile.
   */
  public void recordDiskSourceResources(ModuleDef module) {
    Map<String, Long> currentModifiedByDiskSourcePath =
        resourcesToModifiedByPath(module.getSourceResourceOracle().getResources());
    recordDiskSourceResources(currentModifiedByDiskSourcePath);
  }

  /**
   * Records the paths and content ids of generated source resources in the current compile and
   * updates a list of known modified paths by comparing the paths and content ids of generated
   * source resources in the previous compile with those of the current compile.
   */
  public void recordGeneratedUnits(Collection<GeneratedUnit> generatedUnits) {
    // Not all Generators are run on each compile so it is not possible to compare previous and
    // current generated units to detect deletions. As a result only modifications are tracked.

    for (GeneratedUnit generatedUnit : generatedUnits) {
      String currentStrongHash = generatedUnit.getStrongHash();
      String lastKnownStrongHash =
          contentHashByGeneratedTypeName.put(generatedUnit.getTypeName(), currentStrongHash);
      if (!Objects.equal(lastKnownStrongHash, currentStrongHash)) {
        // Added or Modified resource.
        modifiedCompilationUnitNames.add(generatedUnit.getTypeName());
      }
    }
  }

  @VisibleForTesting
  public void recordNestedTypeName(String compilationUnitTypeName, String nestedTypeName) {
    nestedTypeNamesByUnitTypeName.put(compilationUnitTypeName, nestedTypeName);
    compilationUnitTypeNameByNestedTypeName.put(nestedTypeName, compilationUnitTypeName);
  }

  public void recordNestedTypeNamesPerType(CompilationUnit compilationUnit) {
    // For the root type in the compilation unit the source name and binary name are the same.
    String compilationUnitTypeName = compilationUnit.getTypeName();

    nestedTypeNamesByUnitTypeName.removeAll(compilationUnitTypeName);
    for (CompiledClass compiledClass : compilationUnit.getCompiledClasses()) {
      String nestedTypeName = InternalName.toBinaryName(compiledClass.getInternalName());
      recordNestedTypeName(compilationUnitTypeName, nestedTypeName);
    }
  }

  /**
   * Records that rebinder type Foo contains a GWT.create(ReboundTypeBar.class) call.
   */
  public void recordRebinderTypeForReboundType(String reboundTypeName, String rebinderType) {
    rebinderTypeNamesByReboundTypeName.put(reboundTypeName, rebinderType);
  }

  public void removeReferencesFrom(String fromTypeName) {
    Collection<String> toTypeNames = referencedTypeNamesByTypeName.get(fromTypeName);
    for (String toTypeName : toTypeNames) {
      typeNamesByReferencingTypeName.remove(toTypeName, fromTypeName);
    }
    referencedTypeNamesByTypeName.removeAll(fromTypeName);
  }

  public void setJsForType(TreeLogger logger, String typeName, String typeJs) {
    logger.log(TreeLogger.SPAM, "caching JS for type " + typeName);
    jsByTypeName.put(typeName, typeJs);
  }

  /**
   * Records the names of JSO subtypes, single impl interfaces and dual impl interfaces as well as
   * keeps track of types that entered or left one of the lists in the most recent iteration.
   */
  public void setJsoTypeNames(Set<String> jsoTypeNames, Set<String> singleJsoImplInterfaceNames,
      Set<String> dualJsoImplInterfaceNames) {
    this.jsoStatusChangedTypeNames.clear();
    this.jsoStatusChangedTypeNames.addAll(
        Sets.symmetricDifference(this.jsoTypeNames, jsoTypeNames));
    this.jsoStatusChangedTypeNames.addAll(
        Sets.symmetricDifference(this.singleJsoImplInterfaceNames, singleJsoImplInterfaceNames));
    this.jsoStatusChangedTypeNames.addAll(
        Sets.symmetricDifference(this.dualJsoImplInterfaceNames, dualJsoImplInterfaceNames));

    this.jsoTypeNames.clear();
    this.jsoTypeNames.addAll(jsoTypeNames);
    this.singleJsoImplInterfaceNames.clear();
    this.singleJsoImplInterfaceNames.addAll(singleJsoImplInterfaceNames);
    this.dualJsoImplInterfaceNames.clear();
    this.dualJsoImplInterfaceNames.addAll(dualJsoImplInterfaceNames);
  }

  public void setLastLinkedJsBytes(int lastLinkedJsBytes) {
    this.lastLinkedJsBytes = lastLinkedJsBytes;
  }

  public void setPreambleTypeNames(TreeLogger logger, Set<String> preambleTypeNames) {
    logger.log(TreeLogger.DEBUG, "caching list of known preamble types " + preambleTypeNames);
    this.preambleTypeNames.addAll(preambleTypeNames);
  }

  public void setRootTypeNames(Collection<String> rootTypeNames) {
    this.rootTypeNames.clear();
    this.rootTypeNames.addAll(rootTypeNames);
  }

  public void setSourceMapForType(String typeName, JsSourceMap sourceMap) {
    sourceMapsByTypeName.put(typeName, sourceMap);
  }

  public void setStatementRangesForType(String typeName, StatementRanges statementRanges) {
    statementRangesByTypeName.put(typeName, statementRanges);
  }

  private void appendReferencingTypes(Set<String> accumulatedTypeNames,
      Collection<String> referencedTypeNames) {
    for (String referencedTypeName : referencedTypeNames) {
      Collection<String> referencingTypeNames =
          typeNamesByReferencingTypeName.get(referencedTypeName);
      accumulatedTypeNames.addAll(referencingTypeNames);
    }
  }

  /**
   * If type Foo is a generated type and is stale this pass will append type Bar that triggers
   * Generator Baz that regenerates type Foo.
   * <p>
   * This is necessary since just clearing the cache for type Foo would not be adequate to cause the
   * recreation of its cached JS without also rerunning the Generator that creates type Foo.
   */
  private void appendTypesToRegenerateStaleGeneratedTypes(Set<String> staleTypeNames) {
    Set<String> generatedCompilationUnitNames =
        reboundTypeNamesByGeneratedCompilationUnitNames.keySet();

    // Filter the current stale types list for any compilation units that are known to be generated.
    Set<String> staleGeneratedCompilationUnitNames = Sets.intersection(
        computeCompilationUnitNames(staleTypeNames), generatedCompilationUnitNames);
    boolean discoveredMoreStaleTypes;
    do {
      // Accumulate staleGeneratedCompilationUnits -> generators ->
      // generatorTriggeringCompilationUnits.
      Set<String> reboundTypesThatGenerateTheStaleCompilationUnits =
          computeReboundTypesThatGenerateTypes(staleGeneratedCompilationUnitNames);
      Set<String> generatorTriggeringTypes =
          computeTypesThatRebindTypes(reboundTypesThatGenerateTheStaleCompilationUnits);
      // Mark these generator triggering types stale and keep track of whether any of them were not
      // previously known to be stale.
      discoveredMoreStaleTypes = staleTypeNames.addAll(generatorTriggeringTypes);
      // Output of Generators run for these triggering types, might not be stable, so all the
      // resulting generated types must be assumed stale.
      // TODO(stalcup): this overly broad assumption that generated types are stale might be very
      // costly for individual Generators that have lots of output (like GWTRPC). It would be faster
      // (and more complicated to instead watch Generator output for actually modified types and
      // then recalculate staleness.
      staleTypeNames.addAll(computeNestedTypeNames(computeCompilationUnitsGeneratedForReboundTypes(
          reboundTypesThatGenerateTheStaleCompilationUnits)));

      // It's possible that a generator triggering type was itself also created by a Generator.
      // Repeat the backwards trace process till none of the newly stale types are generated types.
      staleGeneratedCompilationUnitNames = Sets.intersection(
          computeCompilationUnitNames(generatorTriggeringTypes), generatedCompilationUnitNames);
    } while (discoveredMoreStaleTypes);
  }

  private void clearCachedTypeOutput(String staleTypeName) {
    jsByTypeName.remove(staleTypeName);
    statementRangesByTypeName.remove(staleTypeName);
    sourceMapsByTypeName.remove(staleTypeName);
  }

  /**
   * Returns the set of names of CompilationUnits that contain all of the given type names.
   */
  private Set<String> computeCompilationUnitNames(Set<String> typeNames) {
    Set<String> compilationUnitNames = Sets.newHashSet();
    for (String typeName : typeNames) {
      compilationUnitNames.add(compilationUnitTypeNameByNestedTypeName.get(typeName));
    }
    return compilationUnitNames;
  }

  /**
   * Returns the set of names of compilation units that are generated by the given rebound type
   * names.
   */
  private Set<String> computeCompilationUnitsGeneratedForReboundTypes(
      Set<String> reboundTypeNames) {
    Set<String> generatedCompilationUnitNames = Sets.newHashSet();
    for (String reboundTypeName : reboundTypeNames) {
      generatedCompilationUnitNames.addAll(
          generatedCompilationUnitNamesByReboundTypeNames.get(reboundTypeName));
    }
    return generatedCompilationUnitNames;
  }

  /**
   * Returns the set of type names contained within the given compilation units.
   */
  private Set<String> computeNestedTypeNames(Set<String> compilationUnitNames) {
    Set<String> nestedTypeNames = Sets.newHashSet();
    nestedTypeNames.addAll(compilationUnitNames);
    for (String compilationUnitName : compilationUnitNames) {
      nestedTypeNames.addAll(nestedTypeNamesByUnitTypeName.get(compilationUnitName));
    }
    return nestedTypeNames;
  }

  /**
   * Returns the set of names of types that when rebound trigger Generators that access resources
   * which are known to have been modified.
   */
  private Set<String> computeReboundTypesAffectedByModifiedResources() {
    Set<String> affectedRebindTypeNames = Sets.newHashSet();
    for (String modifiedResourcePath : modifiedResourcePaths) {
      affectedRebindTypeNames.addAll(reboundTypeNamesByInputResource.get(modifiedResourcePath));
    }
    return affectedRebindTypeNames;
  }

  private Set<String> computeReboundTypesThatGenerateTypes(
      Set<String> staleGeneratedCompilationUnitNames) {
    Set<String> reboundTypesThatGenerateTypes = Sets.newHashSet();
    for (String staleGeneratedCompilationUnitName : staleGeneratedCompilationUnitNames) {
      reboundTypesThatGenerateTypes.addAll(
          reboundTypeNamesByGeneratedCompilationUnitNames.get(staleGeneratedCompilationUnitName));
    }
    return reboundTypesThatGenerateTypes;
  }

  /**
   * Returns the set of names of types that contain GWT.create(ReboundType.class) calls that rebind
   * the given set of type names.
   */
  private Set<String> computeTypesThatRebindTypes(Set<String> reboundTypeNames) {
    Set<String> typesThatRebindTypes = Sets.newHashSet();
    for (String reboundTypeName : reboundTypeNames) {
      typesThatRebindTypes.addAll(rebinderTypeNamesByReboundTypeName.get(reboundTypeName));
    }
    return typesThatRebindTypes;
  }
}
