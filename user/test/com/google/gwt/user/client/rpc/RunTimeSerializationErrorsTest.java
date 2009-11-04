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
package com.google.gwt.user.client.rpc;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestException;

/**
 * Tests run-time serialization errors for GWT RPC.
 */
public class RunTimeSerializationErrorsTest extends RpcTestBase {

  public static MixedSerializableEchoServiceAsync getService() {
    MixedSerializableEchoServiceAsync service = GWT.create(MixedSerializableEchoService.class);

    ((ServiceDefTarget) service).setServiceEntryPoint(GWT.getModuleBaseURL()
        + "echo");

    return service;
  }

  public void testBadSerialization1() {
    delayTestFinishForRpc();
    getService().echoVoid(new MixedSerializable.NonSerializableSub(),
        new AsyncCallback<MixedSerializable>() {
          public void onFailure(Throwable caught) {
            finishTest();
          }

          public void onSuccess(MixedSerializable result) {
            fail("RPC request should have failed");
          }
        });
  }

  public void testBadSerialization2() {
    final boolean[] callbackFired = new boolean[] {false};

    Request req = getService().echoRequest(
        new MixedSerializable.NonSerializableSub(),
        new AsyncCallback<MixedSerializable>() {
          public void onFailure(Throwable caught) {
            callbackFired[0] = true;
          }

          public void onSuccess(MixedSerializable result) {
            fail("RPC request should have failed");
          }
        });

    assertTrue(callbackFired[0]); // should have happened synchronously
    assertFalse(req.isPending());
    req.cancel();
  }

  public void testBadSerialization3() throws RequestException {
    final boolean[] callbackFired = new boolean[] {false};

    RequestBuilder rb = getService().echoRequestBuilder(
        new MixedSerializable.NonSerializableSub(),
        new AsyncCallback<MixedSerializable>() {
          public void onFailure(Throwable caught) {
            assertFalse("callback fired twice", callbackFired[0]);
            callbackFired[0] = true;
          }

          public void onSuccess(MixedSerializable result) {
            fail("RPC request should have failed");
          }
        });

    assertFalse(callbackFired[0]); // should fail when send() is called
    rb.send();
    assertTrue(callbackFired[0]); // should have happened now
  }

  public void testGoodSerialization1() {
    delayTestFinishForRpc();
    getService().echoVoid(new MixedSerializable.SerializableSub(),
        new AsyncCallback<MixedSerializable>() {
          public void onFailure(Throwable caught) {
            fail(caught.toString());
          }

          public void onSuccess(MixedSerializable result) {
            finishTest();
          }
        });
  }

  public void testGoodSerialization2() {
    delayTestFinishForRpc();
    getService().echoRequest(new MixedSerializable.SerializableSub(),
        new AsyncCallback<MixedSerializable>() {
          public void onFailure(Throwable caught) {
            fail(caught.toString());
          }

          public void onSuccess(MixedSerializable result) {
            finishTest();
          }
        });
  }

  public void testGoodSerialization3() {
    delayTestFinishForRpc();
    getService().echoVoid(new MixedSerializable.SerializableSub(),
        new AsyncCallback<MixedSerializable>() {
          public void onFailure(Throwable caught) {
            fail(caught.toString());
          }

          public void onSuccess(MixedSerializable result) {
            finishTest();
          }
        });
  }
}