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
package com.google.gwt.app.place;

import com.google.gwt.event.shared.EventBus;

/**
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
 * Implemented by objects that control a piece of user interface, with a life
 * cycle managed by an {@link ActivityManager}, in response to
 * {@link PlaceChangeEvent} events as the user navigates through the app.
 */
public interface Activity {

  /**
   * Implemented by objects responsible for displaying the widgets that
   * activities drive.
   * 
   * TODO perhaps this is actually com.google.gwt.user.client.ui.HasWidget,
   * and implemented by SimplePanel
   */
  public interface Display {
    void showActivityWidget(IsWidget widget);
  }

  /**
   * Called when the user is trying to navigate away from this activity.
   * 
   * @return A message to display to the user, e.g. to warn of unsaved work, or
   *         null to say nothing
   */
  String mayStop();

  /**
   * Called when {@link #start} has not yet replied to its callback, but the
   * user has lost interest.
   */
  void onCancel();

  /**
   * Called when the Activity's widget has been removed from view. All event
   * handlers it registered will have been removed before this method is called.
   */
  void onStop();

  /**
   * Called when the Activity should prepare its {@link IsWidget} for the user.
   * Once the widget is ready (typically after an RPC response has been
   * received), receiver should present it via
   * {@link Display#showActivityWidget(IsWidget)}.
   * <p>
   * Any handlers attached to the provided event bus will be de-registered when
   * the activity is stopped, so activities will rarely need to hold on to the
   * {@link HandlerRegistration} instances returned by
   * {@link EventBus#addHandler}.
   * 
   * @param panel the panel to display this activity's widget when it is ready
   * @param eventBus the event bus
   */
  void start(Display panel, EventBus eventBus);
}
