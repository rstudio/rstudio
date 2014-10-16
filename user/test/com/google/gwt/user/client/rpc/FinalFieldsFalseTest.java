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

package com.google.gwt.user.client.rpc;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.FinalFieldsTestService.FinalFieldsNode;

/**
 * Test serializing final fields rpc.serializeFinalFields=false (default).
 */
public class FinalFieldsFalseTest extends RpcTestBase {
  public void testFinalFields() {
    FinalFieldsTestServiceAsync service = getServiceAsync();
    delayTestFinishForRpc();

    FinalFieldsNode node = new FinalFieldsNode(4, "C", 9);

    service.transferObject(node, new AsyncCallback<FinalFieldsNode>() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(FinalFieldsNode result) {
        assertNotNull(result);
        assertTrue(TestSetValidator.isValidFinalFieldsObjectDefault(result));
      }
    });

    service.returnI(node, new AsyncCallback<Integer>() {
      public void onFailure(Throwable caught) {
        TestSetValidator.rethrowException(caught);
      }

      public void onSuccess(Integer i) {
        // since finalize is false, i should be 5
        assertEquals(new Integer(5), i);
        finishTest();
      }
    });
  }

  private FinalFieldsTestServiceAsync finalFieldsTestService;

  private FinalFieldsTestServiceAsync getServiceAsync() {
    if (finalFieldsTestService == null) {
      finalFieldsTestService = (FinalFieldsTestServiceAsync) GWT.create(FinalFieldsTestService.class);
      ((ServiceDefTarget) finalFieldsTestService).setServiceEntryPoint(GWT.getModuleBaseURL()
          + "finalfields");
    }
    return finalFieldsTestService;
  }
}
