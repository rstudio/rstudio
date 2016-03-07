/*
 * Copyright 2016 Google Inc.
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
package com.google.gwt.dev.shell.jetty.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Checks that JSPs are supported in JettyLauncher (through JUnitShell)
 */
public class JspTest extends GWTTestCase {
  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.shell.jetty.Jsp";
  }

  public void testJsp() throws Exception {
    delayTestFinish(5000);
    new RequestBuilder(RequestBuilder.GET, GWT.getModuleBaseForStaticFiles() + "java7.jsp")
        .sendRequest("", new RequestCallback() {
          @Override
          public void onResponseReceived(Request request, Response response) {
            assertEquals(200, response.getStatusCode());
            assertEquals("OK", response.getText().trim());
            finishTest();
          }

          @Override
          public void onError(Request request, Throwable exception) {
            fail();
          }
        });
  }
}
