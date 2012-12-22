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

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;

import java.lang.reflect.Method;

/**
 * A {@link TestResult} that can interpret {@link ExpectedFailure} on test methods.
 */
class TestResultWithExpectedFailures extends ForwardingTestResult {

  private boolean failed;

  public TestResultWithExpectedFailures(TestResult delegate) {
    super(delegate);
  }

  @Override
  public void startTest(Test test) {
    failed = false;
    super.startTest(test);
  }

  @Override
  public void addFailure(Test test, AssertionFailedError t) {
    failed = true;
    if (isAnExpectedException(test, t)) {
      return; // This is a good kind of failure, do not report it.
    }
    super.addFailure(test, t);
  }

  @Override
  public void addError(Test test, Throwable t) {
    failed = true;
    if (isAnExpectedException(test, t)) {
      return; // This is a good kind of failure, do not report it.
    }
    super.addError(test, t);
  }

  @Override
  public void endTest(Test test) {
    if (!failed && isTestExpectedToFail(test)) {
      // Test should have failed but didn't. Report the failure by calling the super directly to
      // prevent any interference from overridden #addFailure.
      super.addFailure(test, new AssertionFailedError("Expected failure but didn't fail"));
    }
    super.endTest(test);
  }

  private boolean isTestExpectedToFail(Test test) {
    return getExpectedFailureAnnotation(test) != null;
  }

  private boolean isAnExpectedException(Test test, Throwable t) {
    ExpectedFailure annotation = getExpectedFailureAnnotation(test);
    if (annotation != null) {
      t = normalizeGwtTestException(t);
      return t.getClass().isAssignableFrom(annotation.withType())
          && getExceptionMessage(t).contains(annotation.withMessage());
    }
    return false;
  }

  /**
   * Extracts the real exception from the {@code RuntimeException} thrown by GwtTestCase.
   */
  private Throwable normalizeGwtTestException(Throwable t) {
    // GWTTestCase replaces AssertionFailedError with RuntimeException and for all other exceptions
    // it puts them into 'cause' property.
    return t.getCause() == null ? new AssertionFailedError(t.getMessage()) : t.getCause();
  }

  private String getExceptionMessage(Throwable t) {
    return t.getMessage() == null ? "" : t.getMessage();
  }

  private ExpectedFailure getExpectedFailureAnnotation(Test test) {
    if (test instanceof TestCase) {
      TestCase testCase = (TestCase) test;
      try {
        Method testMethod = testCase.getClass().getMethod(testCase.getName());
        return testMethod.getAnnotation(ExpectedFailure.class);
      } catch (NoSuchMethodException e) {
        throw new RuntimeException(e);
      }
    }
    return null;
  }
}