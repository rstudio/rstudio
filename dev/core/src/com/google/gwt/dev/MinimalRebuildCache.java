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
import com.google.gwt.dev.jjs.impl.RapidTypeAnalyzer;
import com.google.gwt.dev.jjs.impl.ResolveRuntimeTypeReferences.IntTypeMapper;
import com.google.gwt.dev.js.JsIncrementalNamer.JsIncrementalNamerState;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.util.Name.InternalName;
import com.google.gwt.thirdparty.guava.common.annotations.VisibleForTesting;
import com.google.gwt.thirdparty.guava.common.base.Objects;
import com.google.gwt.thirdparty.guava.common.base.Predicates;
import com.google.gwt.thirdparty.guava.common.collect.HashMultimap;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableList;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableSet;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Multimap;
import com.google.gwt.thirdparty.guava.common.collect.Multimaps;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import cern.colt.list.IntArrayList;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
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

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static void copyCollection(Collection fromCollection, Collection toCollection) {
    toCollection.clear();
    toCollection.addAll(fromCollection);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static void copyMap(Map fromMap, Map toMap) {
    toMap.clear();
    toMap.putAll(fromMap);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static void copyMultimap(Multimap fromMap, Multimap toMap) {
    toMap.clear();
    toMap.putAll(fromMap);
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

  /*
   * Update copyFrom() whenever adding more fields.
   */
  protected final ImmediateTypeRelations immediateTypeRelations = new ImmediateTypeRelations();
  private final Map<String, String> compilationUnitTypeNameByNestedTypeName = Maps.newHashMap();
  private final Map<String, String> contentHashByGeneratedTypeName = Maps.newHashMap();
  private final Set<String> deletedCompilationUnitNames = Sets.newHashSet();
  private final Set<String> deletedDiskSourcePaths = Sets.newHashSet();
  private final Set<String> deletedResourcePaths = Sets.newHashSet();
  private final Set<String> dualJsoImplInterfaceNames = Sets.newHashSet();
  private final Set<String> exportedGlobalNames = Sets.newHashSet();
  private final Multimap<String, String> exportedGlobalNamesByTypeName = HashMultimap.create();
  private final ArtifactSet generatedArtifacts = new ArtifactSet();
  private final Multimap<String, String> generatedCompilationUnitNamesByReboundTypeNames =
      HashMultimap.create();
  private final IntTypeMapper intTypeMapper = new IntTypeMapper();
  private final Map<String, String> jsByTypeName = Maps.newHashMap();
  private final JsIncrementalNamerState jsIncrementalNamerState = new JsIncrementalNamerState();
  private final Set<String> jsoStatusChangedTypeNames = Sets.newHashSet();
  private final Set<String> jsoTypeNames = Sets.newHashSet();
  private Integer lastLinkedJsBytes;
  private final Map<String, Long> lastModifiedByDiskSourcePath = Maps.newHashMap();
  private final Map<String, Long> lastModifiedByResourcePath = Maps.newHashMap();
  private final Set<String> lastReachableTypeNames = Sets.newHashSet();
  private final Set<String> modifiedCompilationUnitNames = Sets.newHashSet();
  private final Set<String> modifiedDiskSourcePaths = Sets.newHashSet();
  private final Set<String> modifiedResourcePaths = Sets.newHashSet();
  private final Multimap<String, String> nestedTypeNamesByUnitTypeName = HashMultimap.create();
  private final Set<String> preambleTypeNames = Sets.newHashSet();
  private transient ImmutableSet<String> processedStaleTypeNames = ImmutableSet.<String> of();
  private final Multimap<String, String> rebinderTypeNamesByReboundTypeName = HashMultimap.create();
  private final Multimap<String, String> reboundTypeNamesByGeneratedCompilationUnitNames =
      HashMultimap.create();
  private final Multimap<String, String> reboundTypeNamesByInputResource = HashMultimap.create();
  private final Multimap<String, String> referencedTypeNamesByTypeName = HashMultimap.create();
  private final Set<String> rootTypeNames = Sets.newHashSet();
  private final Set<String> singleJsoImplInterfaceNames = Sets.newHashSet();
  private final Set<String> sourceCompilationUnitNames = Sets.newHashSet();
  private final Map<String, JsSourceMap> sourceMapsByTypeName = Maps.newHashMap();
  private final Set<String> staleTypeNames = Sets.newHashSet();
  private final Map<String, StatementRanges> statementRangesByTypeName = Maps.newHashMap();
  private StringAnalyzableTypeEnvironment typeEnvironment = new StringAnalyzableTypeEnvironment();
  private final Multimap<String, String> typeNamesByReferencingTypeName = HashMultimap.create();

  public boolean addExportedGlobalName(String exportedGlobalName, String inTypeName) {
    exportedGlobalNamesByTypeName.put(inTypeName, exportedGlobalName);
    return exportedGlobalNames.add(exportedGlobalName);
  }

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

  public void addSourceCompilationUnitName(String sourceCompilationUnitName) {
    // sourceCompilationUnitNames contains all compilation unit type names seen so far even
    // across recompiles. There is a need to know whether a unit is available in source form (
    // either compiled from source in this iteration or its AST available from cache) so that
    // we insert the correct type of reference when we see a BinaryTypeReference in the JDT
    // AST. {@see GwtAstBuilder}.
    // NOTE: DO NOT RESET OR CLEAR THIS SET.
    this.sourceCompilationUnitNames.add(sourceCompilationUnitName);
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

    deletedDiskSourcePaths.clear();
    modifiedDiskSourcePaths.clear();
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
  public Set<String> computeAndClearStaleTypesCache(TreeLogger logger, JTypeOracle typeOracle) {
    if (!isPopulated()) {
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

    /*
     * Filter for just those stale types that are actually reachable. Since if they're not reachable
     * we don't want to artificially traverse them and unnecessarily reveal dependency problems. And
     * if they have become reachable, since they're missing JS, they will already be fully traversed
     * when seen in Unify.
     */
    copyCollection(filterUnreachableTypeNames(staleTypeNames), staleTypeNames);

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
   * have been modified.
   */
  public Set<String> computeModifiedTypeNames() {
    // Accumulate the names of types that are nested within known modified compilation units.
    return computeNestedTypeNames(modifiedCompilationUnitNames);
  }

  /**
   * Computes and returns the names of the set of types that are referenceable within the method
   * level control flow starting from the entry methods, immortal codegen types and exported
   * JsInterop methods.
   * <p>
   * Should only be called once per compile, so that the "lastReachableTypeNames" list accurately
   * reflects the reachable types of the immediately previous compile.
   */
  public Set<String> computeReachableTypeNames() {
    RapidTypeAnalyzer rapidTypeAnalyzer = new RapidTypeAnalyzer(typeEnvironment);

    // Artificially reach and traverse immortal codegen types since references to these may have
    // been synthesized in JS generation. These will not be pruned.
    for (String immortalCodegenTypeName : JProgram.IMMORTAL_CODEGEN_TYPES_SET) {
      int immortalCodegenTypeId = typeEnvironment.getTypeIdByName(immortalCodegenTypeName);
      rapidTypeAnalyzer.markTypeIdReachable(immortalCodegenTypeId);
      // Includes the clinit method.
      rapidTypeAnalyzer.markMemberMethodIdsReachable(immortalCodegenTypeId);
    }

    // Artificially reach @JsExport and @JsType methods. Since the enclosing types are
    // not being marked instantiated or reachable there's still a chance that they will be pruned
    // if no instantiations or static references are found.
    IntArrayList enclosingTypeIds = typeEnvironment.getEnclosingTypeIdsOfExportedMethods();
    for (int i = 0; i < enclosingTypeIds.size(); i++) {
      int enclosingTypeId = enclosingTypeIds.get(i);
      IntArrayList exportedMethodIds =
          typeEnvironment.getExportedMemberMethodIdsIn(enclosingTypeId);
      if (exportedMethodIds == null) {
        continue;
      }

      for (int j = 0; j < exportedMethodIds.size(); j++) {
        int exportedMethodId = exportedMethodIds.get(j);
        rapidTypeAnalyzer.markMethodIdReachable(exportedMethodId, false);
      }
    }

    // Artificially reach types with @JsExport'ed or @JsType'ed static fields or methods (including
    // Constructors). These types will not be pruned.
    IntArrayList typeIdsWithExportedStaticReferences =
        typeEnvironment.getTypeIdsWithExportedStaticReferences();
    for (int i = 0; i < typeIdsWithExportedStaticReferences.size(); i++) {
      int typeId = typeIdsWithExportedStaticReferences.get(i);
      rapidTypeAnalyzer.markTypeIdReachable(typeId);
      String typeName = typeEnvironment.getTypeNameById(typeId);
      int typeClinitMethodId = typeEnvironment.getMethodIdByName(typeName + "::$clinit()V");
      rapidTypeAnalyzer.markMethodIdReachable(typeClinitMethodId, false);
    }

    // Reach and traverse entry method types. This is just the EntryMethodHolder
    // class and its init() function. It will not be pruned. This is the conceptual "root" of the
    // application execution.
    IntArrayList entryMethodIds = typeEnvironment.getEntryMethodIds();
    for (int i = 0; i < entryMethodIds.size(); i++) {
      int entryMethodId = entryMethodIds.get(i);
      int typeId = typeEnvironment.getEnclosingTypeId(entryMethodId);
      rapidTypeAnalyzer.markTypeIdReachable(typeId);
      // Includes the clinit method.
      rapidTypeAnalyzer.markMemberMethodIdsReachable(typeId);
    }

    // Perform rapid type analysis.
    IntArrayList reachableTypeIds = rapidTypeAnalyzer.computeReachableTypeIds();

    // Translate ids back to strings and keep the results around for the next compile.
    Set<String> reachableTypeNames = Sets.newHashSet();
    for (int i = 0; i < reachableTypeIds.size(); i++) {
      int reachableTypeId = reachableTypeIds.get(i);
      reachableTypeNames.add(typeEnvironment.getTypeNameById(reachableTypeId));
    }

    copyCollection(reachableTypeNames, lastReachableTypeNames);
    return reachableTypeNames;
  }

  /**
   * Replaces the contents of this cache with the contents of the given cache.
   * <p>
   * This operation should be kept fast as it will be called once per compile. At the moment it
   * takes about 2.5% of the time in an incremental recompile. If wanting to recover this time in
   * the future consider parallelizing the copy or grouping the values from hash tables with similar
   * keys into a single data object and hashtable so that fewer references need to be replicated.
   */
  public void copyFrom(MinimalRebuildCache that) {
    this.lastLinkedJsBytes = that.lastLinkedJsBytes;

    this.intTypeMapper.copyFrom(that.intTypeMapper);
    this.jsIncrementalNamerState.copyFrom(that.jsIncrementalNamerState);
    this.immediateTypeRelations.copyFrom(that.immediateTypeRelations);
    this.typeEnvironment.copyFrom(that.typeEnvironment);

    copyMap(that.compilationUnitTypeNameByNestedTypeName,
        this.compilationUnitTypeNameByNestedTypeName);
    copyMap(that.contentHashByGeneratedTypeName, this.contentHashByGeneratedTypeName);
    copyMap(that.jsByTypeName, this.jsByTypeName);
    copyMap(that.lastModifiedByDiskSourcePath, this.lastModifiedByDiskSourcePath);
    copyMap(that.lastModifiedByResourcePath, this.lastModifiedByResourcePath);
    copyMap(that.sourceMapsByTypeName, this.sourceMapsByTypeName);
    copyMap(that.statementRangesByTypeName, this.statementRangesByTypeName);

    copyMultimap(that.exportedGlobalNamesByTypeName, this.exportedGlobalNamesByTypeName);
    copyMultimap(that.generatedCompilationUnitNamesByReboundTypeNames,
        this.generatedCompilationUnitNamesByReboundTypeNames);
    copyMultimap(that.nestedTypeNamesByUnitTypeName, this.nestedTypeNamesByUnitTypeName);
    copyMultimap(that.rebinderTypeNamesByReboundTypeName, this.rebinderTypeNamesByReboundTypeName);
    copyMultimap(that.reboundTypeNamesByGeneratedCompilationUnitNames,
        this.reboundTypeNamesByGeneratedCompilationUnitNames);
    copyMultimap(that.reboundTypeNamesByInputResource, this.reboundTypeNamesByInputResource);
    copyMultimap(that.referencedTypeNamesByTypeName, this.referencedTypeNamesByTypeName);
    copyMultimap(that.typeNamesByReferencingTypeName, this.typeNamesByReferencingTypeName);

    copyCollection(that.deletedCompilationUnitNames, this.deletedCompilationUnitNames);
    copyCollection(that.deletedDiskSourcePaths, this.deletedDiskSourcePaths);
    copyCollection(that.deletedResourcePaths, this.deletedResourcePaths);
    copyCollection(that.dualJsoImplInterfaceNames, this.dualJsoImplInterfaceNames);
    copyCollection(that.exportedGlobalNames, this.exportedGlobalNames);
    copyCollection(that.generatedArtifacts, this.generatedArtifacts);
    copyCollection(that.jsoStatusChangedTypeNames, this.jsoStatusChangedTypeNames);
    copyCollection(that.jsoTypeNames, this.jsoTypeNames);
    copyCollection(that.lastReachableTypeNames, this.lastReachableTypeNames);
    copyCollection(that.modifiedCompilationUnitNames, this.modifiedCompilationUnitNames);
    copyCollection(that.modifiedDiskSourcePaths, this.modifiedDiskSourcePaths);
    copyCollection(that.modifiedResourcePaths, this.modifiedResourcePaths);
    copyCollection(that.preambleTypeNames, this.preambleTypeNames);
    copyCollection(that.rootTypeNames, this.rootTypeNames);
    copyCollection(that.singleJsoImplInterfaceNames, this.singleJsoImplInterfaceNames);
    copyCollection(that.sourceCompilationUnitNames, this.sourceCompilationUnitNames);
    copyCollection(that.staleTypeNames, this.staleTypeNames);
  }

  /**
   * Return the set of provided typeNames with unreachable types filtered out.
   */
  public Set<String> filterUnreachableTypeNames(Set<String> typeNames) {
    return Sets.newHashSet(Sets.filter(typeNames, Predicates.in(lastReachableTypeNames)));
  }

  public ArtifactSet getGeneratedArtifacts() {
    return generatedArtifacts;
  }

  public ImmediateTypeRelations getImmediateTypeRelations() {
    return immediateTypeRelations;
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

  public JsIncrementalNamerState getPersistentPrettyNamerState() {
    return jsIncrementalNamerState;
  }

  public Set<String> getPreambleTypeNames() {
    return preambleTypeNames;
  }

  /**
   * Returns the set of the names of types that were processed as stale. This list can be larger
   * than the calculated list of stale types for example when a new reference is created to a type
   * that had not been processed in a previous compile.
   */
  public Set<String> getProcessedStaleTypeNames() {
    return processedStaleTypeNames;
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

  public StringAnalyzableTypeEnvironment getTypeEnvironment() {
    return typeEnvironment;
  }

  public IntTypeMapper getTypeMapper() {
    return intTypeMapper;
  }

  public boolean hasJs(String typeName) {
    return jsByTypeName.containsKey(typeName);
  }

  public boolean hasPreambleTypeNames() {
    return !preambleTypeNames.isEmpty();
  }

  /**
   * Returns true if there is cached data to reuse in the next recompile.
   */
  public boolean isPopulated() {
    return !immediateTypeRelations.isEmpty();
  }

  public boolean isSourceCompilationUnit(String compilationUnitName) {
    return sourceCompilationUnitNames.contains(compilationUnitName);
  }

  public boolean knowsLastLinkedJsBytes() {
    return lastLinkedJsBytes != null;
  }

  @VisibleForTesting
  public void markSourceFileStale(String typeName) {
    lastModifiedByDiskSourcePath.remove(typeName);
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

  public void removeJsInteropNames(String inTypeName) {
    Collection<String> exportedGlobalNamesForType =
        exportedGlobalNamesByTypeName.removeAll(inTypeName);
    exportedGlobalNames.removeAll(exportedGlobalNamesForType);
  }

  public void removeReferencesFrom(String fromTypeName) {
    Collection<String> toTypeNames = referencedTypeNamesByTypeName.get(fromTypeName);
    for (String toTypeName : toTypeNames) {
      typeNamesByReferencingTypeName.remove(toTypeName, fromTypeName);
    }
    referencedTypeNamesByTypeName.removeAll(fromTypeName);
  }

  public void setEntryMethodNames(List<String> entryMethodNames) {
    typeEnvironment.setEntryMethodNames(entryMethodNames);
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

  public void setProcessedStaleTypeNames(Set<String> processedStaleTypeNames) {
    // If this is the first compile.
    if (!isPopulated()) {
      // Don't record processed stale types, since the list is huge and not useful for test
      // assertions.
      return;
    }

    this.processedStaleTypeNames = ImmutableSet.copyOf(processedStaleTypeNames);
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

  @VisibleForTesting
  boolean hasSameContent(MinimalRebuildCache that) {
    // Ignoring processedStaleTypeNames since it is transient.
    return this.immediateTypeRelations.hasSameContent(that.immediateTypeRelations) && Objects.equal(
        this.compilationUnitTypeNameByNestedTypeName, that.compilationUnitTypeNameByNestedTypeName)
        && Objects.equal(this.contentHashByGeneratedTypeName, that.contentHashByGeneratedTypeName)
        && Objects.equal(this.deletedCompilationUnitNames, that.deletedCompilationUnitNames)
        && Objects.equal(this.deletedDiskSourcePaths, that.deletedDiskSourcePaths)
        && Objects.equal(this.deletedResourcePaths, that.deletedResourcePaths)
        && Objects.equal(this.dualJsoImplInterfaceNames, that.dualJsoImplInterfaceNames)
        && Objects.equal(this.generatedArtifacts, that.generatedArtifacts)
        && Objects.equal(this.exportedGlobalNames, that.exportedGlobalNames)
        && Objects.equal(this.exportedGlobalNamesByTypeName, that.exportedGlobalNamesByTypeName)
        && Objects.equal(this.generatedCompilationUnitNamesByReboundTypeNames,
            that.generatedCompilationUnitNamesByReboundTypeNames)
        && this.intTypeMapper.hasSameContent(that.intTypeMapper)
        && Objects.equal(this.jsByTypeName, that.jsByTypeName)
        && Objects.equal(this.jsoStatusChangedTypeNames, that.jsoStatusChangedTypeNames)
        && Objects.equal(this.jsoTypeNames, that.jsoTypeNames)
        && Objects.equal(this.lastLinkedJsBytes, that.lastLinkedJsBytes)
        && Objects.equal(this.lastModifiedByDiskSourcePath, that.lastModifiedByDiskSourcePath)
        && Objects.equal(this.lastModifiedByResourcePath, that.lastModifiedByResourcePath)
        && Objects.equal(this.lastReachableTypeNames, that.lastReachableTypeNames)
        && Objects.equal(this.modifiedCompilationUnitNames, that.modifiedCompilationUnitNames)
        && Objects.equal(this.modifiedDiskSourcePaths, that.modifiedDiskSourcePaths)
        && Objects.equal(this.modifiedResourcePaths, that.modifiedResourcePaths)
        && Objects.equal(this.nestedTypeNamesByUnitTypeName, that.nestedTypeNamesByUnitTypeName)
        && this.jsIncrementalNamerState.hasSameContent(that.jsIncrementalNamerState)
        && Objects.equal(this.preambleTypeNames, that.preambleTypeNames) && Objects.equal(
            this.rebinderTypeNamesByReboundTypeName, that.rebinderTypeNamesByReboundTypeName)
        && Objects.equal(this.reboundTypeNamesByGeneratedCompilationUnitNames,
            that.reboundTypeNamesByGeneratedCompilationUnitNames)
        && Objects.equal(this.reboundTypeNamesByInputResource, that.reboundTypeNamesByInputResource)
        && Objects.equal(this.referencedTypeNamesByTypeName, that.referencedTypeNamesByTypeName)
        && Objects.equal(this.rootTypeNames, that.rootTypeNames)
        && Objects.equal(this.singleJsoImplInterfaceNames, that.singleJsoImplInterfaceNames)
        && Objects.equal(this.sourceCompilationUnitNames, that.sourceCompilationUnitNames)
        && Objects.equal(this.sourceMapsByTypeName, that.sourceMapsByTypeName)
        && Objects.equal(this.staleTypeNames, that.staleTypeNames)
        && Objects.equal(this.statementRangesByTypeName, that.statementRangesByTypeName)
        && this.typeEnvironment.hasSameContent(that.typeEnvironment)
        && Objects.equal(this.typeNamesByReferencingTypeName, that.typeNamesByReferencingTypeName);
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
