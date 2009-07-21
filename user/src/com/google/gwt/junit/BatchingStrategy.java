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

import java.util.Set;

/**
 * An interface that specifies how tests should be batched.
 */
public interface BatchingStrategy {

  /**
   * Returns the list of tests that should be executed along with this test.
   */
  TestInfo[] getTestBlock(TestInfo currentTest);
}

/**
 * 
 * Strategy that does not batch tests.
 */
class NoBatchingStrategy implements BatchingStrategy {
  public TestInfo[] getTestBlock(TestInfo currentTest) {
    return new TestInfo[] {currentTest};
  }
}

/**
 * Strategy that batches all tests belonging to one module.
 */
class ModuleBatchingStrategy implements BatchingStrategy {

  /**
   * Returns the list of all tests belonging to the module of
   * <code>currentTest</code>.
   */
  public TestInfo[] getTestBlock(TestInfo currentTest) {
    String moduleName = currentTest.getTestModule();
    if (moduleName.endsWith(".JUnit")) {
      moduleName = moduleName.substring(0, moduleName.length()
          - ".JUnit".length());
    }
    Set<TestInfo> allTestsInModule = GWTTestCase.ALL_GWT_TESTS.get(moduleName);
    if (allTestsInModule != null) {
      assert allTestsInModule.size() > 0;
      return allTestsInModule.toArray(new TestInfo[allTestsInModule.size()]);
    }
    // No data, default to just this test.
    return new TestInfo[] {currentTest};
  }
}
