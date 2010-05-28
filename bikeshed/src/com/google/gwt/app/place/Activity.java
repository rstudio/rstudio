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

import com.google.gwt.user.client.ui.Widget;

/**
 * Implemented by objects that control a piece of user interface,
 * with a life cycle managed by an {@link ActivityManager}, in 
 * response to {@link PlaceChangeEvent} events as the user
 * navigates through the app. 
 */
public interface Activity {

  /**
   * Callback object used for asynchronous {@link Activity#start} requests,
   * provides the widget this activity drives.
   */
  public interface Callback {
    void onStarted(Widget widget);
  }

  /**
   * Called when {@link #start} has not yet replied to its callback, but the
   * user has lost interest.
   */
  public void onCancel();

  /**
   * Called when the Activity's widget has been removed from view.
   */
  public void onStop();

  /**
   * Called when the Activity should prepare its {@link Widget} to the user.
   * 
   * @param callback allows the widget to be presented asynchronously
   */
  public void start(Callback callback);

  public boolean willStop();
}
