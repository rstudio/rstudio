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

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Helper class for widgets that accept
 * {@link com.google.gwt.user.client.ui.FormHandler FormHandlers}. This
 * subclass of ArrayList assumes that all items added to it will be of type
 * {@link com.google.gwt.user.client.ui.FormHandler}.
 */
public class FormHandlerCollection extends ArrayList {

  /**
   * Fires a {@link FormHandler#onSubmitComplete(FormSubmitCompleteEvent)} on
   * all handlers in the collection.
   * 
   * @param sender the object sending the event
   * @param results the results of the form submission
   */
  public void fireOnComplete(Object sender, String results) {
    FormSubmitCompleteEvent event = new FormSubmitCompleteEvent(sender, results);

    for (Iterator it = iterator(); it.hasNext();) {
      FormHandler handler = (FormHandler) it.next();
      handler.onSubmitComplete(event);
    }
  }

  /**
   * Fires a {@link FormHandler#onSubmit(FormSubmitEvent)} on all handlers in
   * the collection.
   * 
   * @param sender the object sending the event
   * @return <code>true</code> if the event should be cancelled
   */
  public boolean fireOnSubmit(Object sender) {
    FormSubmitEvent event = new FormSubmitEvent(sender);

    for (Iterator it = iterator(); it.hasNext();) {
      FormHandler handler = (FormHandler) it.next();
      handler.onSubmit(event);
    }

    return event.isCancelled();
  }
}
