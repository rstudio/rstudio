/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.user.client.rpc;

import com.google.gwt.user.client.rpc.RecursiveClassTestService.ResultNode;
import com.google.gwt.core.client.GWT;

/**
 * Class used to test generics with wild cards and recursive references.
 */
public class RecursiveClassTest extends RpcTestBase {

  /**
   * This method is used to test generics with wild cards and recursive references.
   */
  public void testRecursiveClass() {
    RecursiveClassTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();
   
    service.greetServer("Hello", new AsyncCallback<ResultNode>() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(ResultNode result) {
        assertNotNull(result);
        assertTrue(TestSetValidator.isValidRecurisveClassObject(result));
        finishTest();
      }
    });
  }
  
  /**
   * Create a remote service proxy to talk to the server-side Greeting service.
   */
  private RecursiveClassTestServiceAsync getServiceAsync() {
    if (recursiveClassTestService == null) {
      recursiveClassTestService = (RecursiveClassTestServiceAsync) GWT.create(RecursiveClassTestService.class);
      ((ServiceDefTarget) recursiveClassTestService).setServiceEntryPoint(GWT.getModuleBaseURL()
          + "recursiveclass");
    }
    return recursiveClassTestService;
  }

  private RecursiveClassTestServiceAsync recursiveClassTestService;

}

