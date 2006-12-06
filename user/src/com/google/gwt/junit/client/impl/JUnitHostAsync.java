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

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * The asynchronous version of {@link JUnitHost}.
 */
public interface JUnitHostAsync {

  /**
   * Gets the name of next method to run.
   * 
   * @param testClassName The class name of the calling test case.
   * @param callBack The object that will receive the name of the next method to
   *          run.
   */
  void getFirstMethod(String testClassName, AsyncCallback callBack);

  /**
   * Reports results for the last method run and gets the name of next method to
   * run.
   * 
   * @param testClassName The class name of the calling test case.
   * @param ew The wrapped exception thrown by the the last test, or
   *          <code>null</code> if the last test completed successfully.
   * @param callBack The object that will receive the name of the next method to
   *          run.
   */
  void reportResultsAndGetNextMethod(String testClassName, ExceptionWrapper ew,
      AsyncCallback callBack);

}
