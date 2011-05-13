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

package com.google.gwt.sample.mobilewebapp.client.event;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * Fired when the user abandons edits of a task.
 */
public class EditingCanceledEvent extends GwtEvent<EditingCanceledEvent.Handler> {

  /**
   * Implemented by objects that handle {@link EditingCanceledEvent}.
   */
  public interface Handler extends EventHandler {
    void onEditCanceled(EditingCanceledEvent event);
  }

  /**
   * The event type.
   */
  public static final Type<EditingCanceledEvent.Handler> TYPE = new Type<EditingCanceledEvent.Handler>();

  @Override
  public final Type<EditingCanceledEvent.Handler> getAssociatedType() {
    return TYPE;
  }

  @Override
  protected void dispatch(EditingCanceledEvent.Handler handler) {
    handler.onEditCanceled(this);
  }
}
