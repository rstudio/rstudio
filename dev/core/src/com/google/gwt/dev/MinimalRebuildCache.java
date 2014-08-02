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
import com.google.gwt.core.ext.linker.StatementRanges;
import com.google.gwt.dev.jjs.ast.JTypeOracle.ImmediateTypeRelations;
import com.google.gwt.thirdparty.guava.common.collect.HashMultimap;
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

  private final ImmediateTypeRelations immediateTypeRelations = new ImmediateTypeRelations();
  private final Set<String> modifiedCompilationUnitNames = Sets.newHashSet();
  private final Map<Integer, PermutationRebuildCache> permutationRebuildCacheById =
      Maps.newHashMap();
  private final Set<String> rootTypeNames = Sets.newHashSet();

  public ImmediateTypeRelations getImmediateTypeRelations() {
    return immediateTypeRelations;
  }

  public PermutationRebuildCache getPermutationRebuildCache(int permutationId) {
    if (!permutationRebuildCacheById.containsKey(permutationId)) {
      PermutationRebuildCache permutationRebuildCache = new PermutationRebuildCache();
      permutationRebuildCacheById.put(permutationId, permutationRebuildCache);
      return permutationRebuildCache;
    }
    return permutationRebuildCacheById.get(permutationId);
  }

  public void setModifiedCompilationUnitNames(TreeLogger logger,
      Set<String> modifiedCompilationUnitNames) {
    logger.log(TreeLogger.DEBUG,
        "caching list of known modified compilation units " + modifiedCompilationUnitNames);
    this.modifiedCompilationUnitNames.clear();
    this.modifiedCompilationUnitNames.addAll(modifiedCompilationUnitNames);
  }

  public void setRootTypeNames(Collection<String> rootTypeNames) {
    this.rootTypeNames.clear();
    this.rootTypeNames.addAll(rootTypeNames);
  }
}
