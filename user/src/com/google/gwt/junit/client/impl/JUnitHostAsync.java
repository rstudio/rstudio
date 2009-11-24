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

import com.google.gwt.junit.client.impl.JUnitHost.ClientInfo;
import com.google.gwt.junit.client.impl.JUnitHost.InitialResponse;
import com.google.gwt.junit.client.impl.JUnitHost.TestBlock;
import com.google.gwt.junit.client.impl.JUnitHost.TestInfo;
import com.google.gwt.user.client.rpc.AsyncCallback;

import java.util.HashMap;

/**
 * The asynchronous version of {@link JUnitHost}.
 */
public interface JUnitHostAsync {

  /**
   * Gets a specific block of tests to run.
   * 
   * @param blockIndex the index of the test block to retrieve
   * @param clientInfo the info for this client
   * @param callBack the object that will receive the initial response
   */
  void getTestBlock(int blockIndex, ClientInfo clientInfo,
      AsyncCallback<InitialResponse> callBack);

  /**
   * Reports results for the last method run and gets the name of next method to
   * run.
   * 
   * @param results the results of executing the test
   * @param blockIndex the index of the test block to retrieve
   * @param clientInfo the info for this client
   * @param callBack the object that will receive the next test block
   */
  void reportResultsAndGetTestBlock(HashMap<TestInfo, JUnitResult> results,
      int blockIndex, ClientInfo clientInfo, AsyncCallback<TestBlock> callBack);
}
