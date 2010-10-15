/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.requestfactory.server;

/**
 * Exception thrown during by a {@link RequestProcessor} when 
 * an unexpected exception is caught. Includes an appropriate
 * response of T to send to the client.
 */
public class RequestProcessingException extends Exception {
  private final Object response;

  /**
   * Constructs a new {@link RequestProcessingException} with a given message,
   * Throwable cause, and response Object.
   *
   * @param message a message to display
   * @param t the Throwable cause
   * @param response a response Object, may be cast to T by a
   *     {@link RequestProcessor}&lt;T&gt;
   */
  public RequestProcessingException(String message, Throwable t, Object response) {
    super(message, t);
    this.response = response;
  }
  
  Object getResponse() {
    return response;
  }
}
