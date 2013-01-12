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

import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.user.client.rpc.impl.FailingRequestBuilder;

import junit.framework.TestCase;

/**
 * Tests the {@link FailingRequestBuilder} class.
 */
public class FailingRequestBuilderTest extends TestCase {
  public void testBasics() throws RequestException {
    final boolean[] callbackCalled = new boolean[] {false};

    RequestBuilder rb = new FailingRequestBuilder(new SerializationException(),
        new AsyncCallback<Void>() {
          public void onFailure(Throwable caught) {
            assertFalse(callbackCalled[0]);
            callbackCalled[0] = true;
          }

          public void onSuccess(Void result) {
            fail("expected this to fail");
          }
        });

    rb.send();
  }
}
