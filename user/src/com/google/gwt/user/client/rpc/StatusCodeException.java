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
package com.google.gwt.user.client.rpc;

/**
 * Indicates that an RPC response was returned with an invalid HTTP status code.
 * This exception will be presented via
 * {@link AsyncCallback#onFailure(Throwable)} if the HTTP response from the
 * server does not have a <code>200</code> status code.
 */
public class StatusCodeException extends InvocationException {
  private final int statusCode;
  private final String statusText;
  private final String encodedResponse;

  /**
   * Construct an exception with the given status code and description.
   * 
   * @param statusCode the HTTP status code to report
   * @param encodedResponse the HTTP response message to report
   */
  public StatusCodeException(int statusCode, String encodedResponse) {
    super(statusCode + " " + encodedResponse);
    this.statusCode = statusCode;
    this.statusText = null;
    this.encodedResponse = encodedResponse;
  }

  /**
   * Construct an exception with the given status code, status text and description.
   *
   * @param statusCode the HTTP status code to report
   * @param statusText the HTTP status text to report
   * @param encodedResponse the HTTP response message to report
   */
  public StatusCodeException(int statusCode, String statusText, String encodedResponse) {
    super(statusCode + " " + statusText + " " + encodedResponse);
    this.statusCode = statusCode;
    this.statusText = statusText;
    this.encodedResponse = encodedResponse;
  }

  /**
   * Returns the response message associated with the failed request.
   */
  public String getEncodedResponse() {
    return encodedResponse;
  }

  /**
   * Returns the status code associated with the failed request.
   * <p>
   * The value will be 0 if the request failed (e.g. network error, or the
   * server <a href="http://www.w3.org/TR/cors">disallowed the request</a>) or
   * has been aborted (this will generally be the case when leaving the page).
   */
  public int getStatusCode() {
    return statusCode;
  }

  /**
   * Returns the status text associated with the failed request.
   */
  public String getStatusText() {
    return statusText;
  }
}
