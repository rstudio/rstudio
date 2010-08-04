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
   */
  public int getStatusCode() {
    return statusCode;
  }
}