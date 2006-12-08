/*
 * Copyright 2006 Google Inc.
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
 * to communicate test progress back to the GWT environemnt, where the real test
 * test is running.
 */
public abstract class GWTTestCase extends TestCase {

  /**
   * A reference to my implementation class. All substantive methods simply
   * delegate to the implementation class, to make debugging easier.
   */
  public final GWTTestCaseImpl impl = new GWTTestCaseImpl(this);

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

  /**
   * Do not override this method, the generated class will override it for you.
   */
  public GWTTestCase getNewTestCase() {
    return null;
  }

  /**
   * Do not override this method, the generated class will override it for you.
   */
  public String getTestName() {
    return null;
  }

  public final void onModuleLoad() {
    impl.onModuleLoad();
  }

  protected final void delayTestFinish(int timeoutMillis) {
    impl.delayTestFinish(timeoutMillis);
  }

  protected final void finishTest() {
    impl.finishTest();
  }

}
