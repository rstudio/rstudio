/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.user.client;

/**
 * Exception reported to the current
 * {@link com.google.gwt.core.client.GWT.UncaughtExceptionHandler} when a
 * deferred {@link IncrementalCommand} is canceled as a result of a slow script
 * warning.
 */
public class IncrementalCommandCanceledException extends RuntimeException {
  private IncrementalCommand command;

  public IncrementalCommandCanceledException(IncrementalCommand command) {
    this.command = command;
  }

  /**
   * Returns the {@link IncrementalCommand} which was canceled by the user as a
   * result of a slow script warning.
   * 
   * @return the {@link IncrementalCommand} which was canceled by the user as a
   *         result of a slow script warning
   */
  public IncrementalCommand getCommand() {
    return command;
  }
}
