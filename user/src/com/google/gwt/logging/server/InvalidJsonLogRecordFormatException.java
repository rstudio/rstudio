/*
 * Copyright 2014 Google Inc.
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

package com.google.gwt.logging.server;

/**
 * Exception thrown when unable to parse JSON strings or the JSON object does not
 * contain the expected properties.
 */
public class InvalidJsonLogRecordFormatException extends Exception {

  /**
   * Constructs an InvalidJSonStringException.
   */
  public InvalidJsonLogRecordFormatException(Throwable cause) {
    super("Error parsing JSON string", cause);
  }

}
