/*
 * Copyright 2012 Google Inc.
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
package com.google.gwt.core.client;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Test {@link com.google.gwt.core.server.GwtServletBase}.
 */
public class GwtServletBaseTest extends GWTTestCase {

  private static final int TIMEOUT_IN_MS = 5000;

  private static RequestBuilder getRequest(String urlLocale) {
    StringBuilder url = new StringBuilder();
    url.append(GWT.getModuleBaseURL()).append("servlet");
    if (urlLocale != null) {
      url.append("?locale=").append(urlLocale);
    }
    return new RequestBuilder(RequestBuilder.GET, url.toString());
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.core.GwtServletBaseTest";
  }

  public void testCookieLocaleDa() throws RequestException {
    // there is no localization for da, so check that it falls back to default
    checkCookieLocale("da", "default");
  }

  public void testCookieLocaleDe() throws RequestException {
    checkCookieLocale("de", "de");
  }

  public void testCookieLocaleEnGb() throws RequestException {
    // there is no localization for en_GB, so check that it falls back to en
    checkCookieLocale("en_GB", "en");
  }

  public void testCookieLocaleEnUs() throws RequestException {
    checkCookieLocale("en_US", "en_US");
  }

  public void testUrlLocaleDa() throws RequestException {
    // there is no localization for da, so check that it falls back to default
    checkUrlLocale("da", "default");
  }

  public void testUrlLocaleDe() throws RequestException {
    checkUrlLocale("de", "de");
  }

  public void testUrlLocaleEnGb() throws RequestException {
    // there is no localization for en_GB, so check that it falls back to en
    checkUrlLocale("en_GB", "en");
  }

  public void testUrlLocaleEnUs() throws RequestException {
    checkUrlLocale("en_US", "en_US");
  }

  public void testUrlLocaleOverridesCookie() throws RequestException {
    RequestBuilder req = getRequest("en");
    setCookieAndCheck("fr", "en", req);
  }

  /**
   * @param locale
   * @throws RequestException
   */
  private void checkCookieLocale(String locale, final String expected) throws RequestException {
    RequestBuilder req = getRequest(null);
    setCookieAndCheck(locale, expected, req);
  }

  /**
   * @param expected
   * @param req
   * @throws RequestException
   */
  private void checkServletResponse(final String expected, RequestBuilder req)
      throws RequestException {
    delayTestFinish(TIMEOUT_IN_MS);
    req.sendRequest(null, new RequestCallback() {
      @Override
      public void onError(Request request, Throwable exception) {
        fail(exception.toString());
      }

      @Override
      public void onResponseReceived(Request request, Response response) {
        assertEquals(200, response.getStatusCode());
        assertEquals(expected, response.getText());
        finishTest();
      }
    });
  }

  /**
   * @param locale
   * @throws RequestException
   */
  private void checkUrlLocale(String locale, final String expected) throws RequestException {
    RequestBuilder req = getRequest(locale);
    setCookieAndCheck(null, expected, req);
  }

  private void setCookieAndCheck(String cookieLocale, final String expected, final RequestBuilder req)
      throws RequestException {
    if (cookieLocale == null) {
      cookieLocale = "";
    }
    RequestBuilder cookieReq = new RequestBuilder(RequestBuilder.POST, GWT.getModuleBaseURL()
        + "servlet");
    cookieReq.sendRequest(cookieLocale, new RequestCallback() {
      @Override
      public void onError(Request request, Throwable exception) {
        fail(exception.toString());
      }

      @Override
      public void onResponseReceived(Request request, Response response) {
        assertEquals(200, response.getStatusCode());
        try {
          checkServletResponse(expected, req);
        } catch (RequestException e) {
          fail(e.toString());
        }
      }
    });
  }
}
