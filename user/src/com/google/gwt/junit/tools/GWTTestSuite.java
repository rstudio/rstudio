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
package com.google.gwt.junit.tools;

import com.google.gwt.junit.client.GWTTestCase;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A utility class to optimize the order in which GWTTestCases are run by
 * minimizing the number of times the test framework switches modules.
 */
public class GWTTestSuite extends TestSuite {
  private final Map<String, TestSuite> moduleSuites = new TreeMap<String, TestSuite>();

  private TestSuite nonGWTTestSuite;

  public GWTTestSuite() {
  }

  public GWTTestSuite(String name) {
    super(name);
  }

  @Override
  public void addTest(Test test) {
    if (test instanceof TestSuite) {
      doAddTest((TestSuite) test);
    } else {
      doAddTest(test);
    }
  }

  private void doAddTest(Test test) {
    TestSuite moduleSuite = getModuleSuiteFor(test);
    moduleSuite.addTest(test);
  }

  private void doAddTest(TestSuite suite) {
    Test[] homogenized = homogenize(suite);
    for (Test test : homogenized) {
      doAddTest(test);
    }
  }

  /**
   * Returns the "module" suite this test should be placed in; only accurate for
   * suites that are homogeneous.
   */
  private TestSuite getModuleSuiteFor(Test test) {
    if (test instanceof TestSuite) {
      TestSuite suite = (TestSuite) test;
      if (suite.countTestCases() == 0) {
        return getNonGWTTestSuite();
      } else {
        return getModuleSuiteFor(suite.testAt(0));
      }
    }

    if (test instanceof GWTTestCase) {
      GWTTestCase gwtTest = (GWTTestCase) test;
      String moduleName = gwtTest.getSyntheticModuleName();
      if (moduleName != null) {
        TestSuite suite = moduleSuites.get(moduleName);
        if (suite == null) {
          suite = new TestSuite(moduleName + ".gwt.xml");
          moduleSuites.put(moduleName, suite);
          super.addTest(suite);
        }
        return suite;
      } else {
        // Fall-through to group with non-GWT tests.
      }
    }

    return getNonGWTTestSuite();
  }

  private TestSuite getNonGWTTestSuite() {
    if (nonGWTTestSuite == null) {
      nonGWTTestSuite = new TestSuite("Non-GWT");
      super.addTest(nonGWTTestSuite);
    }
    return nonGWTTestSuite;
  }

  /**
   * Breaks non-homogeneous suites into two or more homogeneous suites. A
   * homogeneous suite is one in which all tests run within a single module.
   */
  private TestSuite[] homogenize(TestSuite suite) {
    String suiteName = suite.getName();

    // Each sub-test is placed into a "bucket" corresponding to its module.
    List<TestSuite> buckets = new ArrayList<TestSuite>();
    Map<TestSuite, TestSuite> moduleToBucket = new HashMap<TestSuite, TestSuite>();

    // Sort the sub-tests into buckets by module.
    for (Enumeration<Test> enumeration = suite.tests(); enumeration.hasMoreElements();) {
      Test test = enumeration.nextElement();
      if (test instanceof TestSuite) {
        // Recursively homogenize any sub-suites.
        TestSuite[] subSuites = homogenize((TestSuite) test);
        for (TestSuite subSuite : subSuites) {
          sortIntoBucket(subSuite, buckets, moduleToBucket, suiteName);
        }
      } else {
        sortIntoBucket(test, buckets, moduleToBucket, suiteName);
      }
    }

    // Return myself if there's only one bucket; otherwise return the buckets.
    int numBuckets = buckets.size();
    if (numBuckets == 1) {
      return new TestSuite[] {suite};
    } else {
      return buckets.toArray(new TestSuite[numBuckets]);
    }
  }

  private void sortIntoBucket(Test test, List<TestSuite> buckets,
      Map<TestSuite, TestSuite> moduleToBucket, String newBucketName) {
    TestSuite module = getModuleSuiteFor(test);
    TestSuite bucket = moduleToBucket.get(module);
    if (bucket == null) {
      bucket = new TestSuite(newBucketName);
      buckets.add(bucket);
      moduleToBucket.put(module, bucket);
    }
    bucket.addTest(test);
  }
}
