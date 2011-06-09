/*
 * Copyright 2011 Google Inc.
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

package com.google.gwt.geolocation.client;

/**
 * Represents an error that occurred while trying to get the user's current
 * position.
 */
public final class PositionError extends Throwable {

  /**
   * An unknown error occurred.
   */
  public static final int UNKNOWN_ERROR = 0;

  /**
   * The user declined access to their position to this application.
   */
  public static final int PERMISSION_DENIED = 1;

  /**
   * The browser was unable to locate the user.
   */
  public static final int POSITION_UNAVAILABLE = 2;

  /**
   * The browser was unable to locate the user in enough time.
   */
  public static final int TIMEOUT = 3;

  private final int code;

  PositionError(int code, String message) {
    super(message);
    this.code = code;
  }

  /**
   * Returns the error code associated with this error.
   */
  public int getCode() {
    return code;
  }
}
