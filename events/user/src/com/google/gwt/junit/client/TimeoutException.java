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
package com.google.gwt.junit.client;

/**
 * This exception is thrown when a {@link GWTTestCase}-derived class runs a
 * test in asynchronous mode and fails to complete within a specified timeout
 * period.
 * 
 * @see GWTTestCase#delayTestFinish(int)
 */
public final class TimeoutException extends RuntimeException {

  public TimeoutException() {
  }

  /**
   * Constructs a timeout exception for a given number of milliseconds.
   * 
   * @param timeoutMillis the number of milliseconds that elapsed which caused
   *          this exception to be thrown
   */
  public TimeoutException(int timeoutMillis) {
    super("A timeout expired after " + timeoutMillis + "ms elapsed.");
  }

  /**
   * Constructs a timeout exception with the specified detail message.
   * 
   * @param message the detail message
   */
  public TimeoutException(String message) {
    super(message);
  }

  /**
   * Constructs a timeout exception with the specified detail message and cause.
   * 
   * @param message the detail message
   * @param cause the exception that caused this exception
   */
  public TimeoutException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs a timeout exception with the specified cause.
   * 
   * @param cause the exception that caused this exception
   */
  public TimeoutException(Throwable cause) {
    super(cause);
  }

}
