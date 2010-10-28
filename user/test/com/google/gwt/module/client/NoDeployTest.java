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
 * Ensure that generated resources are deployed properly according to their
 * visibility.
 */
public class NoDeployTest extends GWTTestCase {

  /**
   * Used only to trigger the NoDeployGenerator.
   */
  private static class NoDeploy {
  }

  /**
   * The maximum amount of time to wait for an RPC response in milliseconds. 
   */
  private static final int RESPONSE_DELAY = 5000;
  private static final String DEPLOY_PREFIX = "deploy?com.google.gwt.module.NoDeployTest.JUnit/"; 

  @Override
  public String getModuleName() {
    return "com.google.gwt.module.NoDeployTest";
  }

  /**
   * Verify that a no-deploy directory in the public path will be deployed.
   */
  public void testPublicNoDeployPath() throws RequestException {
    assertFileIsPublic("no-deploy/", "inPublic.txt");
  }

  public void testVisibilityDeployHttp() throws RequestException {
    assertFileIsNotPublic("", "deployFile.txt");
  }

  public void testVisibilityDeployServer() throws RequestException {
    assertFileIsDeployed("deployFile.txt");
  }

  public void testVisibilityLegacyDeployHttp() throws RequestException {
    assertFileIsNotPublic("", "legacyFile.txt");
  }

  public void testVisibilityLegacyDeployServer() throws RequestException {
    assertFileIsDeployed("legacyFile.txt");
  }

  public void testVisibilityPrivateHttp() throws RequestException {
      assertFileIsNotPublic("", "privateFile.txt");
  }

  public void testVisibilityPrivateServer() throws RequestException {
    assertFileIsNotDeployed("privateFile.txt");
  }

  public void testVisibilityPublicHttp() throws RequestException {
    assertFileIsPublic("", "publicFile.txt");
  }    

  public void testVisibilityPublicServer() throws RequestException {
    assertFileIsNotDeployed("publicFile.txt");
  }    

  /**
   * Fetch a file from a servlet to make sure it is deployed.
   * @param path
   *
   * @throws RequestException
   */
  private void assertFileIsDeployed(final String path) 
      throws RequestException {
    GWT.create(NoDeploy.class);

    // Try fetching a file that should be publicly accessible
    RequestBuilder builder = new RequestBuilder(RequestBuilder.GET,
        GWT.getHostPageBaseURL() + DEPLOY_PREFIX + path);
    delayTestFinish(RESPONSE_DELAY);
    builder.sendRequest("", new RequestCallback() {

      public void onError(Request request, Throwable exception) {
        fail();
      }

      public void onResponseReceived(Request request, Response response) {
        assertEquals(200, response.getStatusCode());
        assertEquals(path, response.getText());
        finishTest();
      }
    });
  }

  /**
   * Fetch a file from a servlet to make sure it is not deployed.
   * @param path
   *
   * @throws RequestException
   */
  private void assertFileIsNotDeployed(final String path) 
      throws RequestException {
    GWT.create(NoDeploy.class);

    // Try fetching a file that should be publicly accessible
    RequestBuilder builder = new RequestBuilder(RequestBuilder.GET,
        GWT.getHostPageBaseURL() + DEPLOY_PREFIX + path);
    delayTestFinish(RESPONSE_DELAY);
    builder.sendRequest("", new RequestCallback() {

      public void onError(Request request, Throwable exception) {
        throw new RuntimeException(exception);
      }

      public void onResponseReceived(Request request, Response response) {
        assertEquals(404, response.getStatusCode());
        finishTest();
      }
    });
  }

  /**
   * Fetch a file from the HTTP server that should not be publicly accessible.
   *
   * @param prefix
   * @param path
   * @throws RequestException
   */
  private void assertFileIsNotPublic(final String prefix, final String path) 
      throws RequestException {
    GWT.create(NoDeploy.class);

    // Try fetching a file that should be publicly accessible
    RequestBuilder builder = new RequestBuilder(RequestBuilder.GET,
        GWT.getHostPageBaseURL() + prefix + path);
    delayTestFinish(RESPONSE_DELAY);
    builder.sendRequest("", new RequestCallback() {

      public void onError(Request request, Throwable exception) {
        throw new RuntimeException(exception);
      }

      public void onResponseReceived(Request request, Response response) {
        assertEquals(404, response.getStatusCode());
        finishTest();
      }
    });
  }

  /**
   * Fetch a file from the HTTP server that should be publicly accessible.
   *
   * @param prefix
   * @param path
   * @throws RequestException
   */
  private void assertFileIsPublic(String prefix, final String path) 
      throws RequestException {
    GWT.create(NoDeploy.class);

    // Try fetching a file that should be publicly accessible
    RequestBuilder builder = new RequestBuilder(RequestBuilder.GET,
        GWT.getHostPageBaseURL() + prefix + path);
    delayTestFinish(RESPONSE_DELAY);
    builder.sendRequest("", new RequestCallback() {

      public void onError(Request request, Throwable exception) {
        fail();
      }

      public void onResponseReceived(Request request, Response response) {
        assertEquals(200, response.getStatusCode());
        assertEquals(path, response.getText().trim());
        finishTest();
      }
    });
  }
}
