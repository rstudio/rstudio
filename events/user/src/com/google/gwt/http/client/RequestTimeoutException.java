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
package com.google.gwt.http.client;

/**
 * Thrown to indicate that an HTTP request has timed out.
 * 
 * <h3>Required Module</h3>
 * Modules that use this class should inherit
 * <code>com.google.gwt.http.HTTP</code>.
 * 
 * {@gwt.include com/google/gwt/examples/http/InheritsExample.gwt.xml}
 */
public class RequestTimeoutException extends RequestException {
  private static String formatMessage(int timeoutMillis) {
    return "A request timeout has expired after "
        + Integer.toString(timeoutMillis) + " ms";
  }

  /**
   * Time, in milliseconds, of the timeout.
   */
  private final int timeoutMillis;

  /**
   * Request object which experienced the timed out.
   */
  private final Request request;

  /**
   * Constructs a timeout exception for the given {@link Request}.
   * 
   * @param request the request which timed out
   * @param timeoutMillis the number of milliseconds which expired
   */
  public RequestTimeoutException(Request request, int timeoutMillis) {
    super(formatMessage(timeoutMillis));
    this.request = request;
    this.timeoutMillis = timeoutMillis;
  }

  /**
   * Returns the {@link Request} instance which timed out.
   * 
   * @return the {@link Request} instance which timed out
   */
  public Request getRequest() {
    return request;
  }

  /**
   * Returns the request timeout value in milliseconds.
   * 
   * @return the request timeout value in milliseconds
   */
  public int getTimeoutMillis() {
    return timeoutMillis;
  }
}
