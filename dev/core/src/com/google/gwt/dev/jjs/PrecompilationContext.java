/*
 * Copyright 2015 Google Inc.
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
package com.google.gwt.dev.jjs;

import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.PrecompilationMetricsArtifact;
import com.google.gwt.dev.Permutation;
import com.google.gwt.dev.jdt.RebindPermutationOracle;
import com.google.gwt.dev.util.Empty;
import com.google.gwt.thirdparty.guava.common.annotations.VisibleForTesting;

/**
 * Context encapsulating necessary data for precompile.
 */
public class PrecompilationContext {
  private final RebindPermutationOracle rebindPermutationOracle;
  private final String[] entryPoints;
  private final String[] additionalRootTypes;
  private final Permutation[] permutations;
  private final PrecompilationMetricsArtifact precompilationMetricsArtifact;
  private final ArtifactSet generatorArtifacts;

  @VisibleForTesting
  PrecompilationContext(RebindPermutationOracle rebindPermutationOracle) {
    this(rebindPermutationOracle, null, null, null, null, null);
  }

  public PrecompilationContext(RebindPermutationOracle rebindPermutationOracle,
      String[] entryPoints, String[] additionalRootTypes, Permutation[] permutations,
      ArtifactSet generatorArtifacts, PrecompilationMetricsArtifact precompilationMetricsArtifact) {
    this.rebindPermutationOracle = rebindPermutationOracle;
    this.entryPoints = entryPoints;
    this.additionalRootTypes = additionalRootTypes == null ? Empty.STRINGS : additionalRootTypes;
    this.permutations = permutations;
    this.precompilationMetricsArtifact = precompilationMetricsArtifact;
    this.generatorArtifacts = generatorArtifacts;
  }

  public String[] getAdditionalRootTypes() {
    return additionalRootTypes;
  }

  public String[] getEntryPoints() {
    return entryPoints;
  }

  public ArtifactSet getGeneratorArtifacts() {
    return generatorArtifacts;
  }

  public Permutation[] getPermutations() {
    return permutations;
  }

  public PrecompilationMetricsArtifact getPrecompilationMetricsArtifact() {
    return precompilationMetricsArtifact;
  }

  public RebindPermutationOracle getRebindPermutationOracle() {
    return rebindPermutationOracle;
  }
}
