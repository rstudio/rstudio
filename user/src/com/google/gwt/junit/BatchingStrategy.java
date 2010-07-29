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

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.junit.client.impl.JUnitHost.TestInfo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An interface that specifies how tests should be batched. A single batch
 * should never include tests from more than one module, or the browser will
 * load the new module and lose results from existing tests.
 */
public abstract class BatchingStrategy {

  /**
   * Returns an ordered list of all tests blocks that should be executed for the
   * specified module. Each test block is an array of {@link TestInfo}.
   *
   * @param syntheticModuleName the name of the synthetic module
   * @return an ordered list of test blocks to run
   */
  public abstract List<TestInfo[]> getTestBlocks(String syntheticModuleName);

  /**
   * Check if this batching strategy only supports execution of a single test at
   * a time.
   *
   * If this method returns true, test methods will be executed on the client as
   * they are run by JUnit. If it returns false, test methods will be batched
   * and sent to the clients in groups. If you are using a test runner that
   * shards test methods across multiple clients, you should use a strategy that
   * returns false (such as {@link NoBatchingStrategy}) or all tests will be
   * executed on all clients.
   *
   * @return true if batches never contain more than one test
   */
  public abstract boolean isSingleTestOnly();

  /**
   * Get the set of tests for this module, minus tests that should not be
   * executed.
   *
   * @return the set of tests to execute
   */
  protected final Set<TestInfo> getTestsForModule(String syntheticModuleName) {
    Set<TestInfo> toExecute = GWTTestCase.getTestsForModule(syntheticModuleName).getTests();
    Set<TestInfo> toRemove = new HashSet<TestInfo>();
    for (TestInfo info : toExecute) {
      if (JUnitShell.mustNotExecuteTest(info)) {
        toRemove.add(info);
      }
    }
    toExecute.removeAll(toRemove);
    return toExecute;
  }

  /**
   * Returns the multiplicative factor for adjusting the timeout. Default value
   * of 1 for no batching.
   */
  protected int getTimeoutMultiplier() {
    return 1;
  }

}
