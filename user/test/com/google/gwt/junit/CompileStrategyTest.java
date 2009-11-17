/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.junit;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.junit.JUnitShell.Strategy;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.junit.client.impl.JUnitHost.TestInfo;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests of {@link BatchingStrategy}. This test must run after a
 * {@link GWTTestCase} to ensure that JUnitShell is already initialized.
 */
public class CompileStrategyTest extends TestCase {

  /**
   * A mock {@link CompileStrategy} used for testing.
   */
  private static class MockCompileStrategy extends CompileStrategy {

    private MockJUnitMessageQueue messageQueue = new MockJUnitMessageQueue();

    /**
     * The number of modules to mock.
     */
    private int mockModuleCount;

    /**
     * Construct a new {@link MockCompileStrategy}.
     * 
     * @param mockModuleCount the number of modules
     */
    public MockCompileStrategy(int mockModuleCount) {
      super(null);
      this.mockModuleCount = mockModuleCount;
    }

    @Override
    public ModuleDef maybeCompileModule(String moduleName,
        String syntheticModuleName, Strategy strategy,
        BatchingStrategy batchingStrategy, TreeLogger treeLogger) {
      fail("This method should not be called.");
      return null;
    }

    @Override
    MockJUnitMessageQueue getMessageQueue() {
      return messageQueue;
    }

    @Override
    int getModuleCount() {
      return mockModuleCount;
    }

    @Override
    ModuleDef maybeCompileModuleImpl2(String moduleName,
        String syntheticModuleName, Strategy strategy, TreeLogger treeLogger) {
      return null;
    }
  }

  /**
   * A mock test case used for testing.
   */
  private static class MockGWTTestCase extends GWTTestCase {
    @Override
    public String getModuleName() {
      return "com.google.gwt.junit.JUnit";
    }

    @Override
    public String getName() {
      return "testMethod1";
    }

    public void testMethod0() {
    }

    public void testMethod1() {
    }

    public void testMethod2() {
    }
  }

  /**
   * A mock {@link JUnitMessageQueue} used for testing.
   */
  private static class MockJUnitMessageQueue extends JUnitMessageQueue {

    /**
     * Indicates that this is the last test block.
     */
    private boolean isLastBlock;

    /**
     * The test blocks added to the queue.
     */
    private List<TestInfo[]> testBlocks;

    public MockJUnitMessageQueue() {
      super(1);
    }

    @Override
    void addTestBlocks(List<TestInfo[]> newTestBlocks, boolean isLastBlock) {
      assertNull(testBlocks);
      this.testBlocks = newTestBlocks;
      this.isLastBlock = isLastBlock;
    }

    void assertIsLastBlock(boolean expected) {
      assertEquals(expected, isLastBlock);
    }

    void assertTestBlocks(List<TestInfo[]> expected) {
      if (expected == null || testBlocks == null) {
        assertEquals(expected, testBlocks);
        return;
      }

      assertEquals(expected.size(), testBlocks.size());
      for (int i = 0; i < testBlocks.size(); i++) {
        TestInfo[] actualBlock = testBlocks.get(i);
        TestInfo[] expectedBlock = expected.get(i);
        assertEquals(expectedBlock.length, actualBlock.length);
        for (int j = 0; j < expectedBlock.length; j++) {
          assertEquals(expectedBlock[j], actualBlock[j]);
        }
      }
    }
  }

  public void testMaybeAddTestBlockForCurrentTestWithBatching() {
    BatchingStrategy batchingStrategy = new ModuleBatchingStrategy();
    assertFalse(batchingStrategy.isSingleTestOnly());

    // Maybe add the current test.
    GWTTestCase testCase = new MockGWTTestCase();
    MockCompileStrategy strategy = new MockCompileStrategy(-1);
    strategy.maybeAddTestBlockForCurrentTest(testCase, batchingStrategy);

    // Verify the test is not added to the queue.
    strategy.getMessageQueue().assertTestBlocks(null);
  }

  public void testMaybeAddTestBlockForCurrentTestWithoutBatching() {
    BatchingStrategy batchingStrategy = new NoBatchingStrategy();
    assertTrue(batchingStrategy.isSingleTestOnly());

    // Maybe add the current test.
    GWTTestCase testCase = new MockGWTTestCase();
    MockCompileStrategy strategy = new MockCompileStrategy(-1);
    strategy.maybeAddTestBlockForCurrentTest(testCase, batchingStrategy);

    // Generate the expected blocks.
    TestInfo testInfo = new TestInfo(testCase.getSyntheticModuleName(),
        testCase.getClass().getName(), testCase.getName());
    List<TestInfo[]> testBlocks = new ArrayList<TestInfo[]>();
    testBlocks.add(new TestInfo[] {testInfo});

    // Verify the test is added to the queue.
    strategy.getMessageQueue().assertIsLastBlock(false);
    strategy.getMessageQueue().assertTestBlocks(testBlocks);
  }

  public void testMaybeCompileModuleImplWithBatchingLastModule() {
    BatchingStrategy batchingStrategy = new ModuleBatchingStrategy();
    assertFalse(batchingStrategy.isSingleTestOnly());

    // Maybe add the current test.
    GWTTestCase testCase = new MockGWTTestCase();
    MockCompileStrategy strategy = new MockCompileStrategy(-1);
    try {
      strategy.maybeCompileModuleImpl(testCase.getModuleName(),
          testCase.getSyntheticModuleName(), testCase.getStrategy(),
          batchingStrategy, TreeLogger.NULL);
    } catch (UnableToCompleteException e) {
      fail("Unexpected UnableToCompleteException: " + e.getMessage());
    }

    // Verify the test block is added to the queue.
    strategy.getMessageQueue().assertIsLastBlock(true);
    strategy.getMessageQueue().assertTestBlocks(
        batchingStrategy.getTestBlocks(testCase.getSyntheticModuleName()));
  }

  public void testMaybeCompileModuleImplWithBatchingNotLastModule() {
    BatchingStrategy batchingStrategy = new ClassBatchingStrategy();
    assertFalse(batchingStrategy.isSingleTestOnly());

    // Maybe add the current test.
    GWTTestCase testCase = new MockGWTTestCase();
    MockCompileStrategy strategy = new MockCompileStrategy(1000);
    try {
      strategy.maybeCompileModuleImpl(testCase.getModuleName(),
          testCase.getSyntheticModuleName(), testCase.getStrategy(),
          batchingStrategy, TreeLogger.NULL);
    } catch (UnableToCompleteException e) {
      fail("Unexpected UnableToCompleteException: " + e.getMessage());
    }

    // Verify the test block is added to the queue.
    strategy.getMessageQueue().assertIsLastBlock(false);
    strategy.getMessageQueue().assertTestBlocks(
        batchingStrategy.getTestBlocks(testCase.getSyntheticModuleName()));
  }

  public void testMaybeCompileModuleImplWithoutBatching() {
    BatchingStrategy batchingStrategy = new NoBatchingStrategy();
    assertTrue(batchingStrategy.isSingleTestOnly());

    // Maybe add the current test.
    GWTTestCase testCase = new MockGWTTestCase();
    MockCompileStrategy strategy = new MockCompileStrategy(-1);
    try {
      strategy.maybeCompileModuleImpl(testCase.getModuleName(),
          testCase.getSyntheticModuleName(), testCase.getStrategy(),
          batchingStrategy, TreeLogger.NULL);
    } catch (UnableToCompleteException e) {
      fail("Unexpected UnableToCompleteException: " + e.getMessage());
    }

    // Verify the test block is not added to the queue.
    strategy.getMessageQueue().assertTestBlocks(null);
  }
}
