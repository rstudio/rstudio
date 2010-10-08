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
package com.google.gwt.user.client.ui;

import java.util.EventObject;

/**
 * Event object containing information about form submission events.
 */
public class FormSubmitEvent extends EventObject {

  private boolean cancel;
  private boolean setCancelledCalled = false;

  /**
   * Creates a new event with information about the source.
   *
   * @param source the object sending the event
   */
  public FormSubmitEvent(FormPanel source) {
    super(source);
  }

  /**
   * Gets whether this form submit will be canceled.
   *
   * @return <code>true</code> if the form submit will be canceled
   */
  public boolean isCancelled() {
    return cancel;
  }

  /**
   * Sets whether the form submit will be canceled.
   *
   * @param cancel <code>true</code> to cancel the submit
   */
  public void setCancelled(boolean cancel) {
    this.cancel = cancel;
    setCancelledCalled = true;
  }

  boolean isSetCancelledCalled() {
    return setCancelledCalled;
  }
}
