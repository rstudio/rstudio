/*
 * Copyright 2013 Google Inc.
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
package com.google.gwt.junit.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.junit.ExpectedFailure;

import junit.framework.AssertionFailedError;

/**
 * This class tests GwtTestCase uncaught exception catching capabilities.
 *
 * Note: This classes uses naming conventions to configure the test case. Alternative would have
 * required different test cases for the combinations.
 */
public class GWTTestCaseUncaughtExceptionHandlerTest extends GWTTestCaseTestBase {

  private UncaughtExceptionHandler myHandler = new UncaughtExceptionHandler() {
    public void onUncaughtException(Throwable e) { /* NO_OP */}
  };

  private boolean throwsInGwtSetUp;
  private boolean throwsInGwtTearDown;
  private boolean addsCustomHandlerInGwtSetup;
  private boolean optsOut;

  @Override
  public void setName(String name) {
    super.setName(name);
    String[] config = name.split("_");
    for (int i = 1; i < config.length; i++) {
      setConfig(config[i]);
    }
  }

  private void setConfig(String confToken) {
    if ("addsCustomHandlerInGwtSetup".equals(confToken)) {
      addsCustomHandlerInGwtSetup = true;
    } else if ("throwsInGwtSetUp".equals(confToken)) {
      throwsInGwtSetUp = true;
    } else if ("throwsInGwtTearDown".equals(confToken)) {
      throwsInGwtTearDown = true;
    } else if ("optsOut".equals(confToken)) {
      optsOut = true;
    } else {
      throw new RuntimeException("Unexpected token in test name: " + confToken);
    }
  }

  @Override
  protected void gwtSetUp() throws Exception {
    if (addsCustomHandlerInGwtSetup) {
      GWT.setUncaughtExceptionHandler(myHandler);
    }
    if (throwsInGwtSetUp) {
      failViaUncaughtException("fail_uncaught_setUp");
    }
  }

  @Override
  protected void gwtTearDown() throws Exception {
    if (throwsInGwtTearDown) {
      failViaUncaughtException("fail_uncaught_tearDown");
    }
  }

  @Override
  protected void reportUncaughtException(Throwable ex) {
    if (optsOut) {
      return;
    }
    super.reportUncaughtException(ex);
  }

  @ExpectedFailure(withMessage = "fail_uncaught")
  public void testFailViaUncaughtException() {
    failViaUncaughtException("fail_uncaught");
  }

  @ExpectedFailure(withMessage = "fail_uncaught")
  public void testFailViaUncaughtExceptionWithCustomHandler() {
    // Set our own handler
    GWT.setUncaughtExceptionHandler(myHandler);

    // However we can still failViaUncaughtException
    failViaUncaughtException("fail_uncaught");
  }

  @ExpectedFailure(withMessage = "fail_uncaught")
  public void testFailViaUncaughtException_addsCustomHandlerInGwtSetup() {
    // Verify our own handler is still there
    assertSame(myHandler, GWT.getUncaughtExceptionHandler());

    // However we can still failViaUncaughtException
    failViaUncaughtException("fail_uncaught");
  }

  public void testFailViaUncaughtException_optsOut() {
    failViaUncaughtException("should not fail");
  }

  @ExpectedFailure(withMessage = "fail_uncaught")
  public void testFailViaUncaughtExceptionBeforeSyncronousFailure() {
    failViaUncaughtException("fail_uncaught");
    failNow("failNow");
  }

  @ExpectedFailure(withMessage = "fail_uncaught_teardown")
  public void testFailViaUncaughtExceptionBeforeException() {
    failViaUncaughtException("fail_uncaught_teardown");
    throw new RuntimeException();
  }

  @ExpectedFailure(withMessage = "fail_uncaught_setUp")
  public void testFailViaUncaughtException_throwsInGwtSetUp() {
    // gwtSetUp is configured to throw exception
  }

  @ExpectedFailure(withMessage = "fail_uncaught_setUp")
  public void testFailViaUncaughtException_throwsInGwtSetUp_addsCustomHandlerInGwtSetup() {
    // gwtSetUp is configured to throw exception
  }

  // Suppressed due to http://code.google.com/p/google-web-toolkit/issues/detail?id=7888
  @ExpectedFailure(withMessage = "fail_uncaught_tearDown")
  public void _suppressed_testFailViaUncaughtException_throwsInGwtTearDown() {
    // gwtTearDown is configured to throw exception
  }

  @ExpectedFailure(withMessage = "fail_uncaught")
  public void testFailWithGetUncaughtExceptionHandler() {
    // For legacy behavior, this should still fail
    GWT.getUncaughtExceptionHandler()
        .onUncaughtException(new AssertionFailedError("fail_uncaught"));
  }

  public void testFailWithGetUncaughtExceptionHandler_addsCustomHandlerInGwtSetup() {
    // Status quo: Failing in legacy way with custom handler causes exceptions to be lost:
    GWT.getUncaughtExceptionHandler()
        .onUncaughtException(new AssertionFailedError("should not fail"));
  }
}
