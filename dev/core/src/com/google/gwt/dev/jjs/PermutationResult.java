/*
 * Copyright 2008 Google Inc.
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

import com.google.gwt.core.ext.linker.Artifact;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.StatementRanges;
import com.google.gwt.dev.Permutation;

import java.io.Serializable;
import java.util.Collection;

/**
 * An extensible return type for the results of compiling a single permutation.
 */
public interface PermutationResult extends Serializable {

  /**
   * Adds additional artifacts to this permutation result.
   */
  void addArtifacts(Collection<? extends Artifact<?>> newArtifacts);

  /**
   * Returns any Artifacts that may have been created as a result of compiling
   * the permutation.
   */
  ArtifactSet getArtifacts();

  /**
   * The compiled JavaScript code as UTF8 bytes.
   */
  byte[][] getJs();

  /**
   * Returns the associated permutation.
   */
  Permutation getPermutation();

  /**
   * The symbol map for the permutation.
   */
  byte[] getSerializedSymbolMap();

  /**
   * The statement ranges for the code returned by {@link #getJs()}.
   */
  StatementRanges[] getStatementRanges();
}
