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
package com.google.gwt.user.client.ui;

/**
 * A widget that implements this interface fires the events defined by the
 * {@link com.google.gwt.user.client.ui.FormHandler} interface.
 * 
 * @deprecated This interface was specific to {@link FormPanel}; it is not
 * generally useful and will be removed in a future version.
 * Use {@link FormPanel#addSubmitHandler(FormPanel.SubmitHandler)} and
 * {@link FormPanel#addSubmitCompleteHandler(FormPanel.SubmitCompleteHandler)}
 * instead
 */
@Deprecated
public interface FiresFormEvents {

  /**
   * Adds a handler interface to receive click events.
   * 
   * @deprecated Use {@link FormPanel#addSubmitCompleteHandler} and
   * {@link FormPanel#addSubmitHandler} instead
   * @param handler the handler interface to add
   */
  @Deprecated
  void addFormHandler(FormHandler handler);

  /**
   * Removes a previously added handler interface.
   * 
   * @param handler the handler interface to remove
   * @deprecated Use the
   * {@link com.google.gwt.event.shared.HandlerRegistration#removeHandler}
   * method on the object returned by an add*Handler method instead
   */
  @Deprecated
  void removeFormHandler(FormHandler handler);
}
