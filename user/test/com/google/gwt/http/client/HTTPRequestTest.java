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
package com.google.gwt.http.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.HTTPRequest;
import com.google.gwt.user.client.ResponseTextHandler;

/**
 * Test cases for the {@link HTTPRequest} class.
 * 
 */
@Deprecated
public class HTTPRequestTest extends RequestTestBase {

  private static String getTestBaseURL() {
    return GWT.getModuleBaseURL() + "testRequestBuilder/";
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.http.RequestBuilderTest";
  }

  public void testAsyncGet() {
    delayTestFinishForRequest();
    HTTPRequest.asyncGet(getTestBaseURL() + "send_GET",
        new ResponseTextHandler() {
          public void onCompletion(String responseText) {
            assertEquals(RequestBuilderTest.SERVLET_GET_RESPONSE, responseText);
            finishTest();
          }
        });
  }

  public void testAsyncPost() {
    delayTestFinishForRequest();
    HTTPRequest.asyncPost(getTestBaseURL() + "simplePost",
        "method=test+request", new ResponseTextHandler() {
          public void onCompletion(String responseText) {
            assertEquals(RequestBuilderTest.SERVLET_POST_RESPONSE, responseText);
            finishTest();
          }
        });
  }
}
