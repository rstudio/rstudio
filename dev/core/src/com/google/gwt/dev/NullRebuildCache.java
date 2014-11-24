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
import com.google.gwt.dev.javac.GeneratedUnit;
import com.google.gwt.dev.jjs.JsSourceMap;
import com.google.gwt.dev.jjs.ast.JTypeOracle;
import com.google.gwt.dev.jjs.impl.ResolveRuntimeTypeReferences.IntTypeMapper;
import com.google.gwt.dev.js.JsIncrementalNamer.JsIncrementalNamerState;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * A MinimalRebuildCache that ignores all interaction.
 */
public class NullRebuildCache extends MinimalRebuildCache {

  private static String failMessage =
      "The RebuildCache should not be interacted with outside of per-file compiles.";

  @Override
  public void addGeneratedArtifacts(ArtifactSet generatedArtifacts) {
  }

  @Override
  public void addModifiedCompilationUnitNames(TreeLogger logger,
      Set<String> modifiedCompilationUnitNames) {
  }

  @Override
  public void addSourceCompilationUnitName(String sourceCompilationUnitName) {
  }

  @Override
  public void addTypeReference(String fromTypeName, String toTypeName) {
  }

  @Override
  public void associateReboundTypeWithGeneratedCompilationUnitName(String reboundTypeName,
      String generatedTypeName) {
  }

  @Override
  public void associateReboundTypeWithInputResource(String reboundTypeName,
      String inputResourcePath) {
  }

  @Override
  public void clearPerTypeJsCache() {
  }

  @Override
  public void clearRebinderTypeAssociations(String rebinderTypeName) {
  }

  @Override
  public void clearReboundTypeAssociations(String reboundTypeName) {
  }

  @Override
  public Set<String> computeAndClearStaleTypesCache(TreeLogger logger, JTypeOracle typeOracle) {
    throw new UnsupportedOperationException(failMessage);
  }

  @Override
  public Set<String> computeDeletedTypeNames() {
    throw new UnsupportedOperationException(failMessage);
  }

  @Override
  public Set<String> computeModifiedTypeNames() {
    throw new UnsupportedOperationException(failMessage);
  }

  @Override
  public Set<String> computeReachableTypeNames() {
    throw new UnsupportedOperationException(failMessage);
  }

  @Override
  public ArtifactSet getGeneratedArtifacts() {
    throw new UnsupportedOperationException(failMessage);
  }

  @Override
  public IntTypeMapper getTypeMapper() {
    throw new UnsupportedOperationException(failMessage);
  }

  @Override
  public String getJs(String typeName) {
    throw new UnsupportedOperationException(failMessage);
  }

  @Override
  public Set<String> getModifiedCompilationUnitNames() {
    throw new UnsupportedOperationException(failMessage);
  }

  @Override
  public JsIncrementalNamerState getPersistentPrettyNamerState() {
    throw new UnsupportedOperationException(failMessage);
  }

  @Override
  public Set<String> getPreambleTypeNames() {
    throw new UnsupportedOperationException(failMessage);
  }

  @Override
  public JsSourceMap getSourceMap(String typeName) {
    throw new UnsupportedOperationException(failMessage);
  }

  @Override
  public Set<String> getStaleTypeNames() {
    throw new UnsupportedOperationException(failMessage);
  }

  @Override
  public StatementRanges getStatementRanges(String typeName) {
    throw new UnsupportedOperationException(failMessage);
  }

  @Override
  public boolean hasJs(String typeName) {
    throw new UnsupportedOperationException(failMessage);
  }

  @Override
  public boolean hasPreambleTypeNames() {
    throw new UnsupportedOperationException(failMessage);
  }

  @Override
  public boolean isSourceCompilationUnit(String compilationUnitName) {
    return false;
  }

  @Override
  public void recordBuildResources(ModuleDef module) {
  }

  @Override
  public void recordDiskSourceResources(Map<String, Long> currentModifiedByDiskSourcePath) {
  }

  @Override
  public void recordDiskSourceResources(ModuleDef module) {
  }

  @Override
  public void recordGeneratedUnits(Collection<GeneratedUnit> generatedUnits) {
  }

  @Override
  public void recordNestedTypeName(String compilationUnitTypeName, String nestedTypeName) {
  }

  @Override
  public void recordNestedTypeNamesPerType(CompilationUnit compilationUnit) {
  }

  @Override
  public void recordRebinderTypeForReboundType(String reboundTypeName, String rebinderType) {
  }

  @Override
  public void removeReferencesFrom(String fromTypeName) {
  }

  @Override
  public void setJsForType(TreeLogger logger, String typeName, String typeJs) {
  }

  @Override
  public void setJsoTypeNames(Set<String> jsoTypeNames, Set<String> singleJsoImplInterfaceNames,
      Set<String> dualJsoImplInterfaceNames) {
  }

  @Override
  public void setPreambleTypeNames(TreeLogger logger, Set<String> preambleTypeNames) {
  }

  @Override
  public void setRootTypeNames(Collection<String> rootTypeNames) {
  }

  @Override
  public void setSourceMapForType(String typeName, JsSourceMap sourceMap) {
  }

  @Override
  public void setStatementRangesForType(String typeName, StatementRanges statementRanges) {
  }
}
