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

import com.google.gwt.dev.jjs.impl.RapidTypeAnalyzer.AnalyzableTypeEnvironment;
import com.google.gwt.dev.util.collect.IntHashMultimap;
import com.google.gwt.dev.util.collect.IntMultimap;
import com.google.gwt.thirdparty.guava.common.annotations.VisibleForTesting;
import com.google.gwt.thirdparty.guava.common.base.Objects;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Maps;

import cern.colt.list.IntArrayList;
import cern.colt.map.OpenIntIntHashMap;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * An AnalyzableTypeEnvironment that is built up from inserted method and type name strings.
 */
public class StringAnalyzableTypeEnvironment implements AnalyzableTypeEnvironment, Serializable {

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static void copyCollection(Collection fromCollection, Collection toCollection) {
    toCollection.clear();
    toCollection.addAll(fromCollection);
  }

  private static void copyCollection(IntArrayList fromCollection, IntArrayList toCollection) {
    toCollection.clear();
    toCollection.addAllOf(fromCollection);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static void copyMap(Map fromMap, Map toMap) {
    toMap.clear();
    toMap.putAll(fromMap);
  }

  private static void copyMap(OpenIntIntHashMap fromMap, OpenIntIntHashMap toMap) {
    IntArrayList keys = fromMap.keys();
    for (int i = 0; i < keys.size(); i++) {
      int key = keys.get(i);
      int value = fromMap.get(key);
      toMap.put(key, value);
    }
  }

  private static void copyMultimap(IntMultimap fromMap, IntMultimap toMap) {
    toMap.clear();
    toMap.putAll(fromMap);
  }

  private final IntMultimap calleeMethodIdsByCallerMethodId = new IntMultimap();
  private final OpenIntIntHashMap enclosingTypeIdByMethodId = new OpenIntIntHashMap();
  private final IntArrayList entryMethodIds = new IntArrayList();
  private final IntMultimap exportedMethodIdsByTypeId = new IntMultimap();
  private final IntMultimap instantiatedTypeIdsByMethodId = new IntMultimap();
  private final IntHashMultimap memberMethodIdsByTypeId = new IntHashMultimap();
  private final Map<String, Integer> methodIdsByName = Maps.newHashMap();
  private final List<String> methodNamesById = Lists.newArrayList();
  private final IntMultimap overidingMethodIdsByOverriddenMethodId = new IntMultimap();
  private final IntHashMultimap overriddenMethodIdsByOverridingMethodId = new IntHashMultimap();
  private final IntHashMultimap staticallyReferencedTypeIdsByMethodId = new IntHashMultimap();
  private final Map<String, Integer> typeIdsByName = Maps.newHashMap();
  private final OpenIntIntHashMap typeIdsWithExportedStaticReferences = new OpenIntIntHashMap();
  private final List<String> typeNamesById = Lists.newArrayList();

  StringAnalyzableTypeEnvironment() {
  }

  @Override
  public IntArrayList getMemberMethodIdsIn(int enclosingTypeId) {
    return memberMethodIdsByTypeId.get(enclosingTypeId);
  }

  @Override
  public IntArrayList getMethodIdsCalledBy(int callerMethodId) {
    return calleeMethodIdsByCallerMethodId.get(callerMethodId);
  }

  @Override
  public IntArrayList getOverriddenMethodIds(int overridingMethodId) {
    return overriddenMethodIdsByOverridingMethodId.get(overridingMethodId);
  }

  @Override
  public IntArrayList getOverridingMethodIds(int overriddenMethodId) {
    return overidingMethodIdsByOverriddenMethodId.get(overriddenMethodId);
  }

  @Override
  public IntArrayList getStaticallyReferencedTypeIdsIn(int reachableMethodId) {
    return staticallyReferencedTypeIdsByMethodId.get(reachableMethodId);
  }

  @Override
  public IntArrayList getTypeIdsInstantiatedIn(int inMethodId) {
    return instantiatedTypeIdsByMethodId.get(inMethodId);
  }

  public void recordExportedMethodInType(String methodName, String typeName) {
    int typeId = getTypeIdByName(typeName);
    int methodId = getMethodIdByName(methodName);
    exportedMethodIdsByTypeId.put(typeId, methodId);
  }

  public void recordExportedStaticReferenceInType(String typeName) {
    int typeId = getTypeIdByName(typeName);
    typeIdsWithExportedStaticReferences.put(typeId, typeId);
  }

  public void recordMethodCallsMethod(String callerMethodName, String calleeMethodName) {
    calleeMethodIdsByCallerMethodId.put(getMethodIdByName(callerMethodName),
        getMethodIdByName(calleeMethodName));
  }

  public void recordMethodInstantiatesType(String methodName, String instantiatedTypeName) {
    instantiatedTypeIdsByMethodId.put(getMethodIdByName(methodName),
        getTypeIdByName(instantiatedTypeName));
  }

  public void recordMethodOverridesMethod(String overriderMethodName, String overriddenMethodName) {
    int overriderMethodId = getMethodIdByName(overriderMethodName);
    int overriddenMethodId = getMethodIdByName(overriddenMethodName);
    overriddenMethodIdsByOverridingMethodId.put(overriderMethodId, overriddenMethodId);
    overidingMethodIdsByOverriddenMethodId.put(overriddenMethodId, overriderMethodId);
  }

  public void recordStaticReferenceInMethod(String typeName, String methodName) {
    staticallyReferencedTypeIdsByMethodId.put(getMethodIdByName(methodName),
        getTypeIdByName(typeName));
  }

  public void recordTypeEnclosesMethod(String enclosingTypeName, String nestedMethodName) {
    int enclosingTypeId = getTypeIdByName(enclosingTypeName);
    int nestedMethodId = getMethodIdByName(nestedMethodName);
    memberMethodIdsByTypeId.put(enclosingTypeId, nestedMethodId);
    enclosingTypeIdByMethodId.put(nestedMethodId, enclosingTypeId);
  }

  /**
   * Remove control flow index entries that are created by the processing of the given type.
   */
  public void removeControlFlowIndexesFor(String typeName) {
    int typeId = getTypeIdByName(typeName);
    exportedMethodIdsByTypeId.remove(typeId);
    typeIdsWithExportedStaticReferences.removeKey(typeId);

    IntArrayList memberMethodIds = memberMethodIdsByTypeId.get(typeId);
    if (memberMethodIds == null) {
      return;
    }
    memberMethodIdsByTypeId.remove(typeId);
    for (int i = 0; i < memberMethodIds.size(); i++) {
      int memberMethodId = memberMethodIds.get(i);
      enclosingTypeIdByMethodId.removeKey(memberMethodId);
      calleeMethodIdsByCallerMethodId.remove(memberMethodId);
      instantiatedTypeIdsByMethodId.remove(memberMethodId);

      IntArrayList overriddenMethodIds =
          overriddenMethodIdsByOverridingMethodId.remove(memberMethodId);
      if (overriddenMethodIds != null) {
        for (int j = 0; j < overriddenMethodIds.size(); j++) {
          int overriddenMethodId = overriddenMethodIds.get(j);
          while (overidingMethodIdsByOverriddenMethodId
              .remove(memberMethodId, overriddenMethodId)) {
            // Remove all instances by repeating remove one.
          }
        }
      }
      staticallyReferencedTypeIdsByMethodId.remove(memberMethodId);
    }
  }

  public void setEntryMethodNames(List<String> entryMethodNames) {
    this.entryMethodIds.clear();
    for (String entryMethodName : entryMethodNames) {
      this.entryMethodIds.add(getMethodIdByName(entryMethodName));
    }
  }

  void copyFrom(StringAnalyzableTypeEnvironment that) {
    copyMap(that.typeIdsWithExportedStaticReferences, this.typeIdsWithExportedStaticReferences);
    copyMap(that.enclosingTypeIdByMethodId, this.enclosingTypeIdByMethodId);
    copyMap(that.methodIdsByName, this.methodIdsByName);
    copyMap(that.typeIdsByName, this.typeIdsByName);

    copyMultimap(that.memberMethodIdsByTypeId, this.memberMethodIdsByTypeId);
    copyMultimap(that.calleeMethodIdsByCallerMethodId, this.calleeMethodIdsByCallerMethodId);
    copyMultimap(that.exportedMethodIdsByTypeId, this.exportedMethodIdsByTypeId);
    copyMultimap(that.instantiatedTypeIdsByMethodId, this.instantiatedTypeIdsByMethodId);
    copyMultimap(that.overidingMethodIdsByOverriddenMethodId,
        this.overidingMethodIdsByOverriddenMethodId);
    copyMultimap(that.overriddenMethodIdsByOverridingMethodId,
        this.overriddenMethodIdsByOverridingMethodId);
    copyMultimap(that.staticallyReferencedTypeIdsByMethodId,
        this.staticallyReferencedTypeIdsByMethodId);

    copyCollection(that.entryMethodIds, this.entryMethodIds);
    copyCollection(that.methodNamesById, this.methodNamesById);
    copyCollection(that.typeNamesById, this.typeNamesById);
  }

  int getEnclosingTypeId(int memberMethodId) {
    return enclosingTypeIdByMethodId.get(memberMethodId);
  }

  IntArrayList getEnclosingTypeIdsOfExportedMethods() {
    return exportedMethodIdsByTypeId.keys();
  }

  IntArrayList getEntryMethodIds() {
    return entryMethodIds;
  }

  IntArrayList getExportedMemberMethodIdsIn(int enclosingTypeId) {
    return exportedMethodIdsByTypeId.get(enclosingTypeId);
  }

  int getMethodIdByName(String methodName) {
    if (methodIdsByName.containsKey(methodName)) {
      return methodIdsByName.get(methodName);
    }
    int methodId = methodNamesById.size();
    methodIdsByName.put(methodName, methodId);
    methodNamesById.add(methodName);
    return methodId;
  }

  int getTypeIdByName(String typeName) {
    if (typeIdsByName.containsKey(typeName)) {
      return typeIdsByName.get(typeName);
    }
    int typeId = typeNamesById.size();
    typeIdsByName.put(typeName, typeId);
    typeNamesById.add(typeName);
    return typeId;
  }

  IntArrayList getTypeIdsWithExportedStaticReferences() {
    return typeIdsWithExportedStaticReferences.keys();
  }

  String getTypeNameById(int typeId) {
    return typeNamesById.get(typeId);
  }

  @VisibleForTesting
  boolean hasSameContent(StringAnalyzableTypeEnvironment that) {
    return Objects.equal(this.calleeMethodIdsByCallerMethodId, that.calleeMethodIdsByCallerMethodId)
        && Objects.equal(this.memberMethodIdsByTypeId, that.memberMethodIdsByTypeId)
        && Objects.equal(this.entryMethodIds, that.entryMethodIds)
        && Objects.equal(this.instantiatedTypeIdsByMethodId, that.instantiatedTypeIdsByMethodId)
        && Objects.equal(this.overidingMethodIdsByOverriddenMethodId,
            that.overidingMethodIdsByOverriddenMethodId) && Objects.equal(
            this.overriddenMethodIdsByOverridingMethodId,
            that.overriddenMethodIdsByOverridingMethodId) && Objects.equal(
            this.staticallyReferencedTypeIdsByMethodId, that.staticallyReferencedTypeIdsByMethodId)
        && Objects.equal(this.enclosingTypeIdByMethodId, that.enclosingTypeIdByMethodId) && Objects
            .equal(this.exportedMethodIdsByTypeId, that.exportedMethodIdsByTypeId) && Objects.equal(
            this.typeIdsWithExportedStaticReferences, that.typeIdsWithExportedStaticReferences);
  }
}
