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
package com.google.gwt.junit.client.impl;

import com.google.gwt.junit.client.impl.JUnitHost.TestInfo;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * The asynchronous version of {@link JUnitHost}.
 */
public interface JUnitHostAsync {

  /**
   * Gets the name of next method to run.
   * 
   * @param callBack the object that will receive the name of the next method to
   *          run
   */
  void getFirstMethod(AsyncCallback<TestInfo> callBack);

  /**
   * Reports results for the last method run and gets the name of next method to
   * run.
   * 
   * @param testInfo the testInfo the result is for
   * @param result the result of the test
   * @param callBack the object that will receive the name of the next method to
   *          run
   */
  void reportResultsAndGetNextMethod(TestInfo testInfo, JUnitResult result,
      AsyncCallback<TestInfo> callBack);
}
