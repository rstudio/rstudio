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
package com.google.gwt.json.client;

/**
 * An exception that can be thrown when an interaction with a JSON data
 * structure fails.
 */
public class JSONException extends RuntimeException {

  /**
   * Constructs a new JSONException.
   */
  public JSONException() {
    super();
  }

  /**
   * Constructs a new JSONException with the specified message.
   */
  public JSONException(String message) {
    super(message);
  }

  /**
   * Constructs a new JSONException with the specified message and cause.
   */
  public JSONException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs a new JSONException with the specified cause.
   */
  public JSONException(Throwable cause) {
    super(cause);
  }

}
