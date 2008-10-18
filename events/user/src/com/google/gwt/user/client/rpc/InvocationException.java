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
package com.google.gwt.user.client.rpc;

/**
 * Occurs when a service invocation did not complete cleanly.
 * <p>
 * A service invocation completes cleanly if
 * <ol>
 * <li>A response is returned from the service, or</li>
 * <li>An exception generated within the service is successfully received and
 * re-thrown in the client.</li>
 * </ol>
 * </p>
 * 
 * <p>
 * A service invocation can fail to complete cleanly for many reasons, including
 * <ol>
 * <li>The network connection to the server is unavailable</li>
 * <li>The host web server is not available</li>
 * <li>The server is not available</li>
 * </ol>
 * </p>
 * 
 * <p>
 * Note that it <em>is</em> possible for this exception to be thrown even if
 * the service was invoked successfully on the server. This could be the case,
 * for example, if a network failure happened after the invocation request was
 * sent but before the response was received.
 * </p>
 */
public class InvocationException extends RuntimeException {

  /**
   * Constructs an exception with the given description.
   * 
   * @param s the exception's description.
   */
  public InvocationException(String s) {
    super(s, null);
  }

  /**
   * Constructs an exception with the given description and cause.
   * 
   * @param s the exception's description.
   * @param cause the exception's cause.
   */
  public InvocationException(String s, Throwable cause) {
    super(s, cause);
  }
}
