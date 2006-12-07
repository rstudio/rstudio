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
package com.google.gwt.user.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.impl.HTTPRequestImpl;

/**
 * This class allows you to make asynchronous HTTP requests to the originating
 * server.
 */
public class HTTPRequest {

  private static final HTTPRequestImpl httpRequest = (HTTPRequestImpl) GWT.create(HTTPRequestImpl.class);

  /**
   * Makes an asynchronous HTTP GET to a remote server.
   * 
   * @param url the absolute url to GET
   * @param handler the response handler to be notified when either the request
   *          fails, or is completed successfully
   * @return <code>false</code> if the invocation fails to issue
   */
  public static boolean asyncGet(String url, ResponseTextHandler handler) {
    return httpRequest.asyncGet(url, handler);
  }

  /**
   * Makes an asynchronous HTTP GET to a remote server.
   * 
   * @param url the absolute url to GET
   * @param handler the response handler to be notified when either the request
   *          fails, or is completed successfully
   * @return <code>false</code> if the invocation fails to issue
   */
  public static boolean asyncGet(String user, String pwd, String url,
      ResponseTextHandler handler) {
    return httpRequest.asyncGet(user, pwd, url, handler);
  };

  /**
   * Makes an asynchronous HTTP POST to a remote server.
   * 
   * @param url the absolute url to which the POST data is delivered
   * @param postData the data to post
   * @param handler the response handler to be notified when either the request
   *          fails, or is completed successfully
   * @return <code>false</code> if the invocation fails to issue
   */
  public static boolean asyncPost(String url, String postData,
      ResponseTextHandler handler) {
    return httpRequest.asyncPost(url, postData, handler);
  }

  /**
   * Makes an asynchronous HTTP POST to a remote server.
   * 
   * @param url the absolute url to which the POST data is delivered
   * @param postData the data to post
   * @param handler the response handler to be notified when either the request
   *          fails, or is completed successfully
   * @return <code>false</code> if the invocation fails to issue
   */
  public static boolean asyncPost(String user, String pwd, String url,
      String postData, ResponseTextHandler handler) {
    return httpRequest.asyncPost(user, pwd, url, postData, handler);
  };
}
