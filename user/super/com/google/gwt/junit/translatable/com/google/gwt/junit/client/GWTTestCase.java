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
package com.google.gwt.junit.client;

import com.google.gwt.junit.client.impl.GWTTestCaseImpl;

import junit.framework.TestCase;

/**
 * This class is the translatable version of {@link GWTTestCase}. It uses RPC
 * to communicate test progress back to the GWT environment, where the real test
 * test is running.
 */
public abstract class GWTTestCase extends TestCase {

  /**
   * A reference to my implementation class. All substantive methods simply
   * delegate to the implementation class, to make debugging easier.
   */
  public GWTTestCaseImpl impl;

  public final void addCheckpoint(String msg) {
    impl.addCheckpoint(msg);
  }

  public boolean catchExceptions() {
    return true;
  }

  public final void clearCheckpoints() {
    impl.clearCheckpoints();
  }

  public final String[] getCheckpoints() {
    return impl.getCheckpoints();
  }

  public abstract String getModuleName();

  protected final void delayTestFinish(int timeoutMillis) {
    if (supportsAsync()) {
      impl.delayTestFinish(timeoutMillis);
    } else {
      throw new UnsupportedOperationException(
          "This test case does not support asynchronous mode.");
    }
  }

  protected final void finishTest() {
    if (supportsAsync()) {
      impl.finishTest();
    } else {
      throw new UnsupportedOperationException(
          "This test case does not support asynchronous mode.");
    }
  }

  /**
   * Returns true if this test case supports asynchronous mode. By default, this
   * is set to true. Originally introduced for Benchmarks which don't currently
   * support asynchronous mode.
   * 
   * <p>
   * Note that an overrider of this method may report different answers for the
   * same test case during the same run, so it is not safe to cache the results.
   * </p>
   */
  protected boolean supportsAsync() {
    return true;
  }
}
