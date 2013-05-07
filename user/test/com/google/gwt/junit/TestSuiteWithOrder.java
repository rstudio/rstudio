/*
 * Copyright 2012 Google Inc.
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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Add {@link TestSuite} that runs test cases in alphabetical order.
 */
class TestSuiteWithOrder extends TestSuite {

  /*
   * Implementation Note: When batching is active, TestModuleInfo in GWTTestCase#ALL_GWT_TESTS
   * accidentally forces the test execution order that is derived from first time the TestCase is
   * instantiated. The implementation of this class should guarantee the order not only by sorting
   * TestCases but also creating them in order.
   */
  public TestSuiteWithOrder(Class<? extends TestCase> clazz) {
    super(clazz.getName());
    Map<String, Method> testMethodMap = new HashMap<String, Method>();
    for (Class<?> c = clazz; Test.class.isAssignableFrom(c); c = c.getSuperclass()) {
      for (Method method : c.getDeclaredMethods()) {
        if (isTestMethod(method) && !testMethodMap.containsKey(method.getName())) {
          testMethodMap.put(method.getName(), method);
        }
      }
    }
    for (Method m : getSortedTestMethods(testMethodMap)) {
      addTest(createTest(clazz, m.getName()));
    }
    if (countTestCases() == 0) {
      addTest(warning("No tests found in " + clazz.getName()));
    }
  }

  private boolean isTestMethod(Method m) {
    return m.getParameterTypes().length == 0
        && m.getName().startsWith("test")
        && m.getReturnType().equals(Void.TYPE);
  }

  private List<Method> getSortedTestMethods(Map<String, Method> methodNames) {
    List<Method> methods = new ArrayList<Method>(methodNames.values());
    Collections.sort(methods, new Comparator<Method>() {
      @Override
      public int compare(Method m1, Method m2) {
        return m1.getName().toString().compareTo(m2.getName().toString());
      }
    });
    return methods;
  }
}
