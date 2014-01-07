/*
 * Copyright 2014 Google Inc.
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
package com.google.gwt.core.ext.linker.impl;

import com.google.gwt.core.ext.linker.Artifact;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.StatementRanges;
import com.google.gwt.dev.Permutation;
import com.google.gwt.dev.cfg.BindingProperty;
import com.google.gwt.dev.cfg.StaticPropertyOracle;
import com.google.gwt.dev.jjs.PermutationResult;

import java.util.Collection;

/**
 * A mock {@link PermutationResult} for testing.
 */
public class MockPermutationResult implements PermutationResult {

  private ArtifactSet artifacts = new ArtifactSet();
  private byte[][] js;
  private String jsStrongName;
  private StatementRanges[] statementRanges;
  private byte[] symbolMap;

  public MockPermutationResult(
      byte[][] js, String jsStrongName, StatementRanges[] statementRanges, byte[] symbolMap) {
    this.js = js;
    this.jsStrongName = jsStrongName;
    this.statementRanges = statementRanges;
    this.symbolMap = symbolMap;
  }

  @Override
  public void addArtifacts(Collection<? extends Artifact<?>> newArtifacts) {
    artifacts.addAll(newArtifacts);
  }

  @Override
  public ArtifactSet getArtifacts() {
    return artifacts;
  }

  @Override
  public byte[][] getJs() {
    return js;
  }

  @Override
  public String getJsStrongName() {
    return jsStrongName;
  }

  @Override
  public Permutation getPermutation() {
    return new Permutation(0, new StaticPropertyOracle(new BindingProperty[0], new String[0],
        new com.google.gwt.dev.cfg.ConfigurationProperty[0]));
  }

  @Override
  public byte[] getSerializedSymbolMap() {
    return symbolMap;
  }

  @Override
  public StatementRanges[] getStatementRanges() {
    return statementRanges;
  }
}
