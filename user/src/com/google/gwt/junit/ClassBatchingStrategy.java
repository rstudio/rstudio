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

import com.google.gwt.junit.client.impl.JUnitHost.TestInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Strategy that batches all tests belonging to one class.
 */
class ClassBatchingStrategy extends BatchingStrategy {
  @Override
  public List<TestInfo[]> getTestBlocks(String syntheticModuleName) {
    Set<TestInfo> allTestsInModule = getTestsForModule(syntheticModuleName);
    List<TestInfo[]> testBlocks = new ArrayList<TestInfo[]>();
    String lastTestClass = null;
    List<TestInfo> lastTestBlock = null;
    for (TestInfo testInfo : allTestsInModule) {
      String testClass = testInfo.getTestClass();
      if (!testClass.equals(lastTestClass)) {
        // Add the last test block to the collection.
        if (lastTestBlock != null) {
          testBlocks.add(lastTestBlock.toArray(new TestInfo[lastTestBlock.size()]));
        }

        // Start a new test block.
        lastTestClass = testClass;
        lastTestBlock = new ArrayList<TestInfo>();
      }
      lastTestBlock.add(testInfo);
    }

    // Add the last test block.
    if (lastTestBlock != null) {
      testBlocks.add(lastTestBlock.toArray(new TestInfo[lastTestBlock.size()]));
    }
    return testBlocks;
  }

  @Override
  public boolean isSingleTestOnly() {
    return false;
  }

  @Override
  protected int getTimeoutMultiplier() {
    return 4;
  }
}
