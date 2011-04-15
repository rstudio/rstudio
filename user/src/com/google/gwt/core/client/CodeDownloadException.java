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
package com.google.gwt.core.client;

/**
 * Exception indicating an interruption while downloading resources.
 */
public final class CodeDownloadException extends RuntimeException {

  /**
   * Reason codes for the interruption of code down load. These
   * can be due to missing resource or server not responding or user
   * switching to a new page indicating resource doesn't
   * need to be down loaded.
   */
  public enum Reason {
    /**
     * Generic code for terminating the download.
     */
    TERMINATED,
  }

  private final Reason reason;

  public CodeDownloadException(String message) {
    super(message);
    this.reason = Reason.TERMINATED;
  }
  
  public CodeDownloadException(String message, Reason reason) {
    super(message);
    this.reason = reason;
  }
  
  public Reason getReason() {
    return reason;
  }
}
