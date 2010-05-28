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

import com.google.gwt.app.util.IsWidget;

/**
 * Implemented by objects that control a piece of user interface, with a life
 * cycle managed by an {@link ActivityManager}, in response to
 * {@link PlaceChangeEvent} events as the user navigates through the app.
 */
public interface Activity {

  /**
   * Implemented by objects responsible for displaying the widgets that
   * activities drive.
   */
  public interface Display {
    void showActivityWidget(IsWidget widget);
  }

  /**
   * Called when {@link #start} has not yet replied to its callback, but the
   * user has lost interest.
   */
  void onCancel();

  /**
   * Called when the Activity's widget has been removed from view.
   */
  void onStop();

  /**
   * Called when the Activity should prepare its {@link IsWidget} for the user.
   * Once the widget is ready (typically after an RPC response has been
   * received), receiver should present it via
   * {@link Display#showActivityWidget(IsWidget)}.
   * 
   * @param panel the panel to display this activity's widget when it is ready
   */
  void start(Display panel);

  boolean willStop();
}
