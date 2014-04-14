/*
 * Copyright 2010 Google Inc.
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

package com.google.gwt.core.ext.linker;

import com.google.gwt.core.ext.Linker;
import com.google.gwt.core.linker.SoycReportLinker;

import java.util.List;

/**
 * Captures some metrics from the precompilation step.
 */
@Transferable
public class PrecompilationMetricsArtifact extends Artifact<PrecompilationMetricsArtifact> {

  private long elapsedMilliseconds;
  private final int permutationBase;
  private int[] permutationIds;
  private String[] finalTypeOracleTypes;
  private String[] referencedAstTypes;

  public PrecompilationMetricsArtifact(int permutationId) {
    this(SoycReportLinker.class, permutationId);
  }

  protected PrecompilationMetricsArtifact(Class<? extends Linker> linker, int permutationBase) {
    super(linker);
    this.permutationBase = permutationBase;
  }

  /**
   * @return the number of types referenced by the AST.
   */
  public String[] getAstTypes() {
    return referencedAstTypes;
  }

  /**
   * @return wall clock time elapsed since start of precompilation
   */
  public long getElapsedMilliseconds() {
    return elapsedMilliseconds;
  }

  /**
   * @return types all types referenced by type oracle after compiling the
   *        sources on the source path with JDT.
   */
  public String[] getFinalTypeOracleTypes() {
    return this.finalTypeOracleTypes;
  }

  /**
   * @return the first permutation Id associated with compiling this permutation.
   */
  public int getPermutationBase() {
    return permutationBase;
  }

  /**
   * @return the permutation ids associated with this precompilation.
   */
  public int[] getPermutationIds() {
    return permutationIds;
  }

  @Override
  public int hashCode() {
    return getName().hashCode();
  }

  /**
   * @param astTypes an array of types referenced by the Java AST.
   */
  public PrecompilationMetricsArtifact setAstTypes(String[] astTypes) {
    this.referencedAstTypes = astTypes;
    return this;
  }

  /**
   * @param elapsedMilliseconds wall clock time elapsed since start of
   *        precompilation
   */
  public PrecompilationMetricsArtifact setElapsedMilliseconds(long elapsedMilliseconds) {
    this.elapsedMilliseconds = elapsedMilliseconds;
    return this;
  }

  /**
   * @param types all types referenced by type oracle after compiling the
   *        sources on the source path with JDT.
   */
  public PrecompilationMetricsArtifact setFinalTypeOracleTypes(List<String> types) {
    this.finalTypeOracleTypes = types.toArray(new String[types.size()]);
    return this;
  }

  /**
   * @param ids the permutation ids associated with this precompilation.
   */
  public PrecompilationMetricsArtifact setPermutationIds(int[] ids) {
    this.permutationIds = ids;
    return this;
  }
  @Override
  protected int compareToComparableArtifact(PrecompilationMetricsArtifact o) {
    return getName().compareTo(o.getName());
  }

  @Override
  protected final Class<PrecompilationMetricsArtifact> getComparableArtifactType() {
    return PrecompilationMetricsArtifact.class;
  }

  private String getName() {
    return "PrecompilationMetricsArtifact-" + permutationBase;
  }
}
