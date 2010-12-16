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

package com.google.gwt.core.ext.linker.impl;

import com.google.gwt.core.ext.LinkerContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.Artifact;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.ConfigurationProperty;
import com.google.gwt.core.ext.linker.EmittedArtifact;
import com.google.gwt.core.ext.linker.SelectionProperty;
import com.google.gwt.core.ext.linker.Shardable;
import com.google.gwt.core.ext.linker.StatementRanges;
import com.google.gwt.dev.Permutation;
import com.google.gwt.dev.cfg.BindingProperty;
import com.google.gwt.dev.cfg.StaticPropertyOracle;
import com.google.gwt.dev.jjs.PermutationResult;

import junit.framework.TestCase;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * A regular JUnit test case for {@link SelectionScriptLinker}.
 */
public class SelectionScriptLinkerUnitTest extends TestCase {
  private static class MockLinkerContext implements LinkerContext {

    public SortedSet<ConfigurationProperty> getConfigurationProperties() {
      return new TreeSet<ConfigurationProperty>();
    }

    public String getModuleFunctionName() {
      return MOCK_MODULE_NAME;
    }

    public long getModuleLastModified() {
      return MOCK_MODULE_LAST_MODIFIED;
    }

    public String getModuleName() {
      return MOCK_MODULE_NAME;
    }

    public SortedSet<SelectionProperty> getProperties() {
      return new TreeSet<SelectionProperty>();
    }

    public boolean isOutputCompact() {
      return true;
    }

    public String optimizeJavaScript(TreeLogger logger, String jsProgram) {
      return jsProgram;
    }
  }

  private static class MockPermutationResult implements PermutationResult {
    private ArtifactSet artifacts = new ArtifactSet();

    public void addArtifacts(Collection<? extends Artifact<?>> newArtifacts) {
      artifacts.addAll(newArtifacts);
    }

    public ArtifactSet getArtifacts() {
      return artifacts;
    }

    public byte[][] getJs() {
      return new byte[][] {
          getBytes("code for fragment 0"), getBytes("code for fragment 1")};
    }

    public Permutation getPermutation() {
      return new Permutation(0, new StaticPropertyOracle(
          new BindingProperty[0], new String[0],
          new com.google.gwt.dev.cfg.ConfigurationProperty[0]));
    }

    public byte[] getSerializedSymbolMap() {
      return getBytes("symbol map");
    }

    public StatementRanges[] getStatementRanges() {
      ArrayList<StatementRanges> ranges = new ArrayList<StatementRanges>();
      for (byte[] js : getJs()) {
        ranges.add(new MockStatementRanges(js.length));
      }
      return ranges.toArray(new StatementRanges[0]);
    }
  }

  private static class MockStatementRanges implements StatementRanges {
    private int length;

    MockStatementRanges(int length) {
      this.length = length;
    }

    public int end(int i) {
      return length;
    }

    public int numStatements() {
      return 1;
    }

    public int start(int i) {
      return 0;
    }
  }

  private static class NonShardableSelectionScriptLinker extends
      SelectionScriptLinker {
    @Override
    public String getDescription() {
      return getClass().getName();
    }

    @Override
    protected String getCompilationExtension(TreeLogger logger,
        LinkerContext context) {
      return ".js";
    }

    @Override
    protected String getModulePrefix(TreeLogger logger, LinkerContext context,
        String strongName) {
      return "MODULE_PREFIX";
    }

    @Override
    protected String getModuleSuffix(TreeLogger logger, LinkerContext context) {
      return "MODULE_SUFFIX";
    }

    @Override
    protected String getSelectionScriptTemplate(TreeLogger logger,
        LinkerContext context) {
      return SelectionScriptLinkerUnitTest.class.getPackage().getName().replace(
          '.', '/')
          + "/MockTemplate.js";
    }
  }

  @Shardable
  private static class ShardableSelectionScriptLinker extends
      NonShardableSelectionScriptLinker {
  }

  private static final long MOCK_MODULE_LAST_MODIFIED = 1234;

  private static final String MOCK_MODULE_NAME = "test";

  private static byte[] getBytes(String string) {
    try {
      return string.getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Test that running a non-shardable linker in simulated sharding mode does
   * not lose the permutations.
   */
  public void testNonShardableHasPermutations()
      throws UnableToCompleteException {
    ArtifactSet artifacts = new ArtifactSet();

    StandardCompilationResult result = createCompilationResult();
    artifacts.add(result);

    ArtifactSet updated = new NonShardableSelectionScriptLinker().link(
        TreeLogger.NULL, new MockLinkerContext(), artifacts);

    SortedSet<SelectionInformation> selectionInfos = updated.find(SelectionInformation.class);
    assertEquals(1, selectionInfos.size());
  }

  /**
   * Test timestamps on the selection script. For Development Mode, it should match
   * the module's timestamp. For Production Mode, it should be current.
   */
  public void testTimestampOnSelectionScript() throws UnableToCompleteException {
    // Development Mode
    {
      ArtifactSet artifacts = new ArtifactSet();
      ArtifactSet updated = new ShardableSelectionScriptLinker().link(
          TreeLogger.NULL, new MockLinkerContext(), artifacts, false);
      EmittedArtifact selectionScript = findSelectionScript(updated);
      assertEquals(MOCK_MODULE_LAST_MODIFIED, selectionScript.getLastModified());
    }

    // Production Mode
    {
      ArtifactSet artifacts = new ArtifactSet();
      artifacts.add(createCompilationResult());
      ArtifactSet updated = new ShardableSelectionScriptLinker().link(
          TreeLogger.NULL, new MockLinkerContext(), artifacts, true);
      updated = transferThinning(updated);
      updated = new ShardableSelectionScriptLinker().link(TreeLogger.NULL,
          new MockLinkerContext(), updated, false);
      EmittedArtifact selectionScript = findSelectionScript(updated);
      assertTrue(MOCK_MODULE_LAST_MODIFIED != selectionScript.getLastModified());
    }
  }

  private StandardCompilationResult createCompilationResult() {
    StandardCompilationResult result = new StandardCompilationResult(
        new MockPermutationResult());
    result.addSelectionPermutation(new TreeMap<SelectionProperty, String>());
    result.addSoftPermutation(Collections.<SelectionProperty, String> emptyMap());
    return result;
  }

  private EmittedArtifact findEmittedArtifact(ArtifactSet updated,
      String partialPath) {
    for (EmittedArtifact art : updated.find(EmittedArtifact.class)) {
      if (art.getPartialPath().equals(partialPath)) {
        return art;
      }
    }
    return null;
  }

  private EmittedArtifact findSelectionScript(ArtifactSet updated) {
    return findEmittedArtifact(updated, MOCK_MODULE_NAME + ".nocache.js");
  }

  /**
   * Thin down the artifact set as if it were being transmitted back. The method
   * in Link cannot be used because it wants a StandardLinkerContext, not an
   * arbitrary LinkerContext.
   */
  @SuppressWarnings("unchecked")
  private ArtifactSet transferThinning(ArtifactSet artifacts) {
    ArtifactSet updated = new ArtifactSet();
    // The raw type Artifact is to work around a Java compiler bug:
    // http://bugs.sun.com/view_bug.do?bug_id=6548436
    for (Artifact art : artifacts) {
      if (art.isTransferableFromShards() || (art instanceof EmittedArtifact)) {
        updated.add(art);
      }
    }
    return updated;
  }
}
