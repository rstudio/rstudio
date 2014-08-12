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
import com.google.gwt.dev.javac.CompilationUnit;
import com.google.gwt.dev.javac.CompiledClass;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JTypeOracle;
import com.google.gwt.dev.jjs.ast.JTypeOracle.ImmediateTypeRelations;
import com.google.gwt.dev.jjs.impl.ResolveRuntimeTypeReferences.IntTypeIdGenerator;
import com.google.gwt.dev.js.JsPersistentPrettyNamer.PersistentPrettyNamerState;
import com.google.gwt.dev.util.Name.InternalName;
import com.google.gwt.thirdparty.guava.common.annotations.VisibleForTesting;
import com.google.gwt.thirdparty.guava.common.collect.HashMultimap;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableList;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Multimap;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

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
    private final Map<String, StatementRanges> statementRangesByTypeName = Maps.newHashMap();
    private final Multimap<String, String> typeNamesByReferencingTypeName = HashMultimap.create();

    public void addTypeReference(String fromTypeName, String toTypeName) {
      referencedTypeNamesByTypeName.put(fromTypeName, toTypeName);
      typeNamesByReferencingTypeName.put(toTypeName, fromTypeName);
    }

    public void clearJsAndStatements(String typeName) {
      jsByTypeName.remove(typeName);
      statementRangesByTypeName.remove(typeName);
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
  private final Set<String> modifiedCompilationUnitNames = Sets.newHashSet();
  private final Multimap<String, String> nestedTypeNamesByUnitTypeName = HashMultimap.create();
  private final Map<Integer, PermutationRebuildCache> permutationRebuildCacheById =
      Maps.newHashMap();
  private final PersistentPrettyNamerState persistentPrettyNamerState =
      new PersistentPrettyNamerState();
  private final Set<String> preambleTypeNames = Sets.newHashSet();
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

  public void clearPerTypeJsCache() {
    rootTypeNames.clear();
    preambleTypeNames.clear();
    modifiedCompilationUnitNames.clear();
    modifiedCompilationUnitNames.addAll(allCompilationUnitNames);
    permutationRebuildCacheById.clear();
  }

  /**
   * Calculates the set of stale types and clears their cached Js and StatementRanges.
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

    // Accumulate the names of stale types resulting from some known type modifications, using
    // various patterns (sub types, referencing types, etc).
    {
      staleTypeNames.addAll(modifiedTypeNames);
      appendSubTypes(staleTypeNames, modifiedTypeNames, typeOracle);
      // Marks stale any type that references either a modified type or any subtype of a modified
      // type. The references of subtypes can be done away with once we revamp Array castmap
      // generation.
      ImmutableList<String> modifiedTypeAndSubTypeNames = ImmutableList.copyOf(staleTypeNames);
      appendReferencingTypes(staleTypeNames, modifiedTypeAndSubTypeNames);
      appendReferencingTypes(staleTypeNames, jsoStatusChangedTypeNames);
      // TODO(stalcup): turn modifications of generator input resources into type staleness.
      staleTypeNames.removeAll(JProgram.SYNTHETIC_TYPE_NAMES);
    }

    logger.log(TreeLogger.DEBUG, "known modified types = " + modifiedTypeNames);
    logger.log(TreeLogger.DEBUG,
        "clearing cached JS for resulting stale types = " + staleTypeNames);

    for (String staleTypeName : staleTypeNames) {
      clearJsAndStatements(staleTypeName);
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

  private void clearJsAndStatements(String staleTypeName) {
    for (PermutationRebuildCache permutationRebuildCache : permutationRebuildCacheById.values()) {
      permutationRebuildCache.clearJsAndStatements(staleTypeName);
    }
  }
}
