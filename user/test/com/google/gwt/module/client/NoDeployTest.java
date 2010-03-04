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
package com.google.gwt.module.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Ensure that generated resources in the no-deploy directory aren't in the
 * output. This validates that the
 * {@link com.google.gwt.dev.linker.NoDeployResourcesShim} is being loaded and
 * operating correctly.
 */
public class NoDeployTest extends GWTTestCase {

  /**
   * Used only to trigger the NoDeployGenerator.
   */
  private static class NoDeploy {
  }

  public static final String TEST_TEXT = "Hello world!";

  /**
   * The maximum amount of time to wait for an RPC response in milliseconds. 
   */
  private static final int RESPONSE_DELAY = 5000; 

  @Override
  public String getModuleName() {
    return "com.google.gwt.module.NoDeployTest";
  }

  public void testDeploy() throws RequestException {
    GWT.create(NoDeploy.class);

    // Try fetching a file that should exist
    RequestBuilder builder = new RequestBuilder(RequestBuilder.GET,
        GWT.getHostPageBaseURL() + "publicFile.txt");
    delayTestFinish(RESPONSE_DELAY);
    builder.sendRequest("", new RequestCallback() {

      public void onError(Request request, Throwable exception) {
        fail();
      }

      public void onResponseReceived(Request request, Response response) {
        assertEquals(TEST_TEXT, response.getText());
        finishTest();
      }
    });
  }

  public void testNoDeploy() throws RequestException {
    if (!GWT.isScript()) {
      // Linkers aren't used in hosted-mode
      return;
    }

    GWT.create(NoDeploy.class);

    // Try fetching a file that shouldn't exist
    RequestBuilder builder = new RequestBuilder(RequestBuilder.GET,
        GWT.getHostPageBaseURL() + "privateFile.txt");
    delayTestFinish(RESPONSE_DELAY);
    builder.sendRequest("", new RequestCallback() {

      public void onError(Request request, Throwable exception) {
        fail();
      }

      public void onResponseReceived(Request request, Response response) {
        assertEquals(404, response.getStatusCode());
        finishTest();
      }
    });
  }

  /**
   * Verify that a no-deploy directory in the public path will be deployed.
   */
  public void testNoDeployInPublic() throws RequestException {
    GWT.create(NoDeploy.class);

    // Try fetching a file that shouldn't exist
    RequestBuilder builder = new RequestBuilder(RequestBuilder.GET,
        GWT.getHostPageBaseURL() + "no-deploy/inPublic.txt");
    delayTestFinish(RESPONSE_DELAY);
    builder.sendRequest("", new RequestCallback() {

      public void onError(Request request, Throwable exception) {
        fail();
      }

      public void onResponseReceived(Request request, Response response) {
        assertEquals(TEST_TEXT, response.getText());
        finishTest();
      }
    });
  }
}
