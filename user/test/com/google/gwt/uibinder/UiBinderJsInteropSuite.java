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
package com.google.gwt.uibinder;

import com.google.gwt.junit.tools.GWTTestSuite;
import com.google.gwt.uibinder.test.client.UiBinderJsInteropTest;

import junit.framework.Test;

/**
 * All UiBinder tests that requires jsinterop.
 */
public class UiBinderJsInteropSuite {
  public static Test suite() {
    GWTTestSuite suite = new GWTTestSuite("Integration tests for UiBinder JsInterop functionality");

    suite.addTestSuite(UiBinderJsInteropTest.class);

    return suite;
  }

  private UiBinderJsInteropSuite() {
  }
}
