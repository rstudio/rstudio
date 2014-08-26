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
import com.google.gwt.thirdparty.guava.common.collect.Sets;
import com.google.gwt.thirdparty.guava.common.collect.Sets.SetView;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * MinimalRebuildCache contains compiler information that can be persisted between compiles to
 * decrease compilation time.
 * <p>
 * All type names referenced here are assumed to be binary names.
 */
public class MinimalRebuildCache implements Serializable {

  /**
   * The permutation specific portion of persisted information.
   */
  public class PermutationRebuildCache {

    // The implementation of a Type can vary between permutations because of permutation specific
    // GWT.create() rewrites and JSO Devirtualization.
    private final Map<String, String> jsByTypeName = Maps.newHashMap();
    private final Multimap<String, String> referencedTypeNamesByTypeName = HashMultimap.create();
    private final Map<String, JsSourceMap> sourceMapsByTypeName = Maps.newHashMap();
    private final Map<String, StatementRanges> statementRangesByTypeName = Maps.newHashMap();
    private final Multimap<String, String> typeNamesByReferencingTypeName = HashMultimap.create();

    public void addTypeReference(String fromTypeName, String toTypeName) {
      referencedTypeNamesByTypeName.put(fromTypeName, toTypeName);
      typeNamesByReferencingTypeName.put(toTypeName, fromTypeName);
    }

    public void clearCachedTypeOutput(String typeName) {
      jsByTypeName.remove(typeName);
      statementRangesByTypeName.remove(typeName);
      sourceMapsByTypeName.remove(typeName);
    }

    /**
     * Computes and returns the names of the set of types that are transitively referencable
     * starting from the set of root types.
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

    public String getJs(String typeName) {
      return jsByTypeName.get(typeName);
    }

    public JsSourceMap getSourceMap(String typeName) {
      return sourceMapsByTypeName.get(typeName);
    }

    public StatementRanges getStatementRanges(String typeName) {
      return statementRangesByTypeName.get(typeName);
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

    public void setSourceMapForType(String typeName, JsSourceMap sourceMap) {
      sourceMapsByTypeName.put(typeName, sourceMap);
    }

    public void setStatementRangesForType(String typeName, StatementRanges statementRanges) {
      statementRangesByTypeName.put(typeName, statementRanges);
    }
  }

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

  private final Set<String> allCompilationUnitNames = Sets.newHashSet();
  private final Multimap<String, String> compilationUnitTypeNameByNestedTypeName =
      HashMultimap.create();
  private final Set<String> deletedCompilationUnitNames = Sets.newHashSet();
  private final Set<String> dualJsoImplInterfaceNames = Sets.newHashSet();
  private final ArtifactSet generatedArtifacts = new ArtifactSet();
  private final ImmediateTypeRelations immediateTypeRelations = new ImmediateTypeRelations();
  private final IntTypeIdGenerator intTypeIdGenerator = new IntTypeIdGenerator();
  private final Set<String> jsoStatusChangedTypeNames = Sets.newHashSet();
  private final Set<String> jsoTypeNames = Sets.newHashSet();
  private final Map<String, Long> lastModifiedByResourcePath = Maps.newHashMap();
  private final Set<String> modifiedCompilationUnitNames = Sets.newHashSet();
  private final Set<String> modifiedResourcePaths = Sets.newHashSet();
  private final Multimap<String, String> nestedTypeNamesByUnitTypeName = HashMultimap.create();
  private final Map<Integer, PermutationRebuildCache> permutationRebuildCacheById =
      Maps.newHashMap();
  private final PersistentPrettyNamerState persistentPrettyNamerState =
      new PersistentPrettyNamerState();
  private final Set<String> preambleTypeNames = Sets.newHashSet();
  private final Multimap<String, String> rebinderTypeNamesByReboundTypeName = HashMultimap.create();
  private final Multimap<String, String> reboundTypeNamesByGeneratedTypeName =
      HashMultimap.create();
  private final Multimap<String, String> reboundTypeNamesByInputResource = HashMultimap.create();
  private final Set<String> rootTypeNames = Sets.newHashSet();
  private final Set<String> singleJsoImplInterfaceNames = Sets.newHashSet();

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

  public void associateReboundTypeWithGeneratedType(String reboundTypeName,
      String generatedTypeName) {
    reboundTypeNamesByGeneratedTypeName.put(generatedTypeName, reboundTypeName);
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
    modifiedCompilationUnitNames.clear();
    modifiedCompilationUnitNames.addAll(allCompilationUnitNames);
    permutationRebuildCacheById.clear();
  }

  public void clearRebinderTypeAssociations(String rebinderTypeName) {
    rebinderTypeNamesByReboundTypeName.values().remove(rebinderTypeName);
  }

  public void clearReboundTypeAssociations(String reboundTypeName) {
    reboundTypeNamesByInputResource.values().remove(reboundTypeName);
    reboundTypeNamesByGeneratedTypeName.values().remove(reboundTypeName);
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

    Set<String> staleTypeNames = Sets.newHashSet();
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

    return staleTypeNames;
  }

  /**
   * Computes and returns the set of names of deleted types.
   * <p>
   * Deleted types are all those types that are nested within compilation units that are known to
   * have been deleted.
   */
  public Set<String> computeDeletedTypeNames() {
    Set<String> deletedTypeNames = Sets.newHashSet();
    deletedTypeNames.addAll(deletedCompilationUnitNames);
    for (String deletedCompilationUnitName : deletedCompilationUnitNames) {
      deletedTypeNames.addAll(nestedTypeNamesByUnitTypeName.get(deletedCompilationUnitName));
    }
    return deletedTypeNames;
  }

  /**
   * Computes and returns the set of names of modified types.
   * <p>
   * Modified types are all those types that are nested within compilation units that are known to
   * have been modified.
   */
  public Set<String> computeModifiedTypeNames() {
    Set<String> modifiedTypeNames = Sets.newHashSet();
    modifiedTypeNames.addAll(modifiedCompilationUnitNames);
    for (String modifiedCompilationUnitName : modifiedCompilationUnitNames) {
      modifiedTypeNames.addAll(nestedTypeNamesByUnitTypeName.get(modifiedCompilationUnitName));
    }
    return modifiedTypeNames;
  }

  @VisibleForTesting
  public Set<String> getAllCompilationUnitNames() {
    return allCompilationUnitNames;
  }

  public ArtifactSet getGeneratedArtifacts() {
    return generatedArtifacts;
  }

  public ImmediateTypeRelations getImmediateTypeRelations() {
    return immediateTypeRelations;
  }

  public IntTypeIdGenerator getIntTypeIdGenerator() {
    return intTypeIdGenerator;
  }

  @VisibleForTesting
  public Set<String> getModifiedCompilationUnitNames() {
    return modifiedCompilationUnitNames;
  }

  public PermutationRebuildCache getPermutationRebuildCache(int permutationId) {
    if (!permutationRebuildCacheById.containsKey(permutationId)) {
      PermutationRebuildCache permutationRebuildCache = new PermutationRebuildCache();
      permutationRebuildCacheById.put(permutationId, permutationRebuildCache);
      return permutationRebuildCache;
    }
    return permutationRebuildCacheById.get(permutationId);
  }

  public PersistentPrettyNamerState getPersistentPrettyNamerState() {
    return persistentPrettyNamerState;
  }

  public Set<String> getPreambleTypeNames() {
    return preambleTypeNames;
  }

  public boolean hasJs(String typeName) {
    if (permutationRebuildCacheById.isEmpty()) {
      return false;
    }
    for (PermutationRebuildCache permutationRebuildCache : permutationRebuildCacheById.values()) {
      if (permutationRebuildCache.getJs(typeName) == null) {
        return false;
      }
    }
    return true;
  }

  public boolean hasPreambleTypeNames() {
    return !preambleTypeNames.isEmpty();
  }

  /**
   * Records the resource paths and modification dates of build resources in the current compile and
   * builds a list of known modified resource paths by comparing the resource paths and modification
   * dates of build resources in the previous compile with those of the current compile.
   */
  public void recordBuildResources(ModuleDef module) {
    // To start lastModifiedByResourcePath is data about the previous compile and by the end it
    // contains data about the current compile.

    Set<Resource> currentResources = module.getBuildResourceOracle().getResources();

    // Start with a from-scratch idea of which resources are modified.
    modifiedResourcePaths.clear();

    Set<String> currentResourcePaths = Sets.newHashSet();
    for (Resource currentResource : currentResources) {
      currentResourcePaths.add(currentResource.getPath());
      Long lastKnownModified = lastModifiedByResourcePath.put(currentResource.getPath(),
          currentResource.getLastModified());
      if (!Objects.equal(lastKnownModified, currentResource.getLastModified())) {
        // Added or Modified resource.
        modifiedResourcePaths.add(currentResource.getPath());
      }
    }

    // Removed resources.
    {
      // Figure out which resources were removed.
      SetView<String> removedResourcePaths =
          Sets.difference(lastModifiedByResourcePath.keySet(), currentResourcePaths);
      // Log them as "modified".
      modifiedResourcePaths.addAll(removedResourcePaths);
      // Remove any resource path to modified date entries for them.
      for (String removedResourcePath : removedResourcePaths) {
        lastModifiedByResourcePath.remove(removedResourcePath);
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

  public void setAllCompilationUnitNames(TreeLogger logger,
      Set<String> newAllCompilationUnitNames) {
    deletedCompilationUnitNames.clear();
    deletedCompilationUnitNames.addAll(
        Sets.difference(this.allCompilationUnitNames, newAllCompilationUnitNames));
    logger.log(TreeLogger.DEBUG,
        "caching list of known deleted compilation units " + deletedCompilationUnitNames);

    this.allCompilationUnitNames.clear();
    this.allCompilationUnitNames.addAll(newAllCompilationUnitNames);
  }

  /**
   * Records the names of JSO subtypes, single impl interfaces and dual impl interfaces as well as
   * keeps track of types that entered or left one of the lists in the most recent iteration.
   */
  public void setJsoTypeNames(Set<String> jsoTypeNames, Set<String> singleJsoImplInterfaceNames,
      Set<String> dualJsoImplInterfaceNames) {
    jsoStatusChangedTypeNames.clear();

    jsoStatusChangedTypeNames.addAll(Sets.symmetricDifference(this.jsoTypeNames, jsoTypeNames));
    jsoStatusChangedTypeNames.addAll(
        Sets.symmetricDifference(this.singleJsoImplInterfaceNames, singleJsoImplInterfaceNames));
    jsoStatusChangedTypeNames.addAll(
        Sets.symmetricDifference(this.dualJsoImplInterfaceNames, dualJsoImplInterfaceNames));

    this.jsoTypeNames.clear();
    this.jsoTypeNames.addAll(jsoTypeNames);
    this.singleJsoImplInterfaceNames.clear();
    this.singleJsoImplInterfaceNames.addAll(singleJsoImplInterfaceNames);
    this.dualJsoImplInterfaceNames.clear();
    this.dualJsoImplInterfaceNames.addAll(dualJsoImplInterfaceNames);
  }

  public void setModifiedCompilationUnitNames(TreeLogger logger,
      Set<String> modifiedCompilationUnitNames) {
    logger.log(TreeLogger.DEBUG,
        "caching list of known modified compilation units " + modifiedCompilationUnitNames);
    this.modifiedCompilationUnitNames.clear();
    this.modifiedCompilationUnitNames.addAll(modifiedCompilationUnitNames);
  }

  public void setPreambleTypeNames(TreeLogger logger, Set<String> preambleTypeNames) {
    logger.log(TreeLogger.DEBUG, "caching list of known preamble types " + preambleTypeNames);
    this.preambleTypeNames.addAll(preambleTypeNames);
  }

  public void setRootTypeNames(Collection<String> rootTypeNames) {
    this.rootTypeNames.clear();
    this.rootTypeNames.addAll(rootTypeNames);
  }

  private void appendReferencingTypes(Set<String> accumulatedTypeNames,
      Collection<String> referencedTypeNames) {
    for (String referencedTypeName : referencedTypeNames) {
      for (PermutationRebuildCache permutationRebuildCache : permutationRebuildCacheById.values()) {
        Collection<String> referencingTypeNames =
            permutationRebuildCache.typeNamesByReferencingTypeName.get(referencedTypeName);
        accumulatedTypeNames.addAll(referencingTypeNames);
      }
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
    Set<String> generatedTypeNames = reboundTypeNamesByGeneratedTypeName.keySet();

    // Filter the current stale types list for any types that are known to be generated.
    Set<String> staleGeneratedTypeNames = Sets.intersection(staleTypeNames, generatedTypeNames);
    do {
      // Accumulate staleGeneratedTypes -> generators -> generatorTriggeringTypes.
      Set<String> generatorTriggeringTypes = computeTypesThatRebindTypes(
          computeReboundTypesThatGenerateTypes(staleGeneratedTypeNames));
      // Mark these generator triggering types stale.
      staleTypeNames.addAll(generatorTriggeringTypes);

      // It's possible that a generator triggering type was itself also created by a Generator.
      // Repeat the backwards trace process till none of the newly stale types are generated types.
      staleGeneratedTypeNames = Sets.intersection(generatorTriggeringTypes, generatedTypeNames);

      // Ensure that no generated type is processed more than once, otherwise poorly written
      // Generators could trigger an infinite loop.
      staleGeneratedTypeNames.removeAll(staleTypeNames);
    } while (!staleGeneratedTypeNames.isEmpty());
  }

  private void clearCachedTypeOutput(String staleTypeName) {
    for (PermutationRebuildCache permutationRebuildCache : permutationRebuildCacheById.values()) {
      permutationRebuildCache.clearCachedTypeOutput(staleTypeName);
    }
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

  private Set<String> computeReboundTypesThatGenerateTypes(Set<String> staleGeneratedTypeNames) {
    Set<String> reboundTypesThatGenerateTypes = Sets.newHashSet();
    for (String staleGeneratedTypeName : staleGeneratedTypeNames) {
      reboundTypesThatGenerateTypes.addAll(
          reboundTypeNamesByGeneratedTypeName.get(staleGeneratedTypeName));
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
