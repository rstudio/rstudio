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
package com.google.gwt.user.client.ui;

import java.util.EventListener;

/**
 * Handler interface for form submit events.
 * 
 * @deprecated Use {@link FormPanel.SubmitCompleteHandler} and/or
 *             {@link FormPanel.SubmitHandler} instead
 */
@Deprecated
public interface FormHandler extends EventListener {

  /**
   * Fired when the form is submitted.
   * 
   * <p>
   * The FormPanel must <em>not</em> be detached (i.e. removed from its parent
   * or otherwise disconnected from a {@link RootPanel}) until the submission is
   * complete. Otherwise, notification of submission will fail.
   * </p>
   * 
   * @param event an event object containing information about the form
   *          submission
   * @deprecated Use {@link FormPanel.SubmitHandler} instead
   */
  @Deprecated
  void onSubmit(FormSubmitEvent event);

  /**
   * Fired when a form has been submitted successfully.
   * 
   * @param event an event object containing information about the form
   *          submission
   * @deprecated Use {@link FormPanel.SubmitCompleteHandler} instead
   */
  @Deprecated
  void onSubmitComplete(FormSubmitCompleteEvent event);
}
