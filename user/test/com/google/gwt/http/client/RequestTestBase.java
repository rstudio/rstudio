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
package com.google.gwt.http.client;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Base class for tests that send an http request.
 */
public abstract class RequestTestBase extends GWTTestCase {
  /**
   * The timeout for request tests.
   */
  protected static final int REQUEST_TIMEOUT = 15000;

  /**
   * Delay finishing a test while we wait for a response. This method should be
   * used instead of {@link #delayTestFinish(int)} so we can adjust timeouts for
   * all Rpc tests at once.
   * 
   * @see #delayTestFinish(int)
   */
  protected final void delayTestFinishForRequest() {
    delayTestFinish(REQUEST_TIMEOUT);
  }
}
