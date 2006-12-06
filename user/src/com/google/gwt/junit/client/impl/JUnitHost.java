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
package com.google.gwt.junit.client.impl;

import com.google.gwt.user.client.rpc.RemoteService;

/**
 * An interface for {@link com.google.gwt.junit.client.GWTTestCase} to communicate with the test process
 * through RPC.
 */
public interface JUnitHost extends RemoteService {

  /**
   * Gets the name of next method to run.
   * 
   * @param testClassName The class name of the calling test case.
   * @return the name of the next method to run.
   */
  String getFirstMethod(String testClassName);

  /**
   * Reports results for the last method run and gets the name of next method to
   * run.
   * 
   * @param testClassName The class name of the calling test case.
   * @param ew The wrapped exception thrown by the the last test, or
   *          <code>null</code> if the last test completed successfully.
   * @return the name of the next method to run.
   */
  String reportResultsAndGetNextMethod(String testClassName, ExceptionWrapper ew);
}
