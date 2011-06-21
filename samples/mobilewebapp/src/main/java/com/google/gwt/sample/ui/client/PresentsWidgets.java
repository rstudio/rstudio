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
package com.google.gwt.sample.ui.client;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.web.bindery.event.shared.EventBus;

/**
 * Implemented by specialized widget drivers that can answer
 * {@link com.google.gwt.activity.shared.Activity#mayStop() mayStop()} calls for
 * {@link com.google.gwt.activity.shared.Activity Activities}.
 * <p>
 * Note that this interface extends {@link IsWidget}. This is to make it easier
 * to evolve app code in MVP directions piecemeal, just where it is useful. If
 * code that assembles widgets thinks of them strictly as IsWidget instances, it
 * doesn't need to notice as they get refactored in to Presenter / View pairs.
 * Or as they don't.
 */
public interface PresentsWidgets extends IsWidget {
  /**
   * Called (probably from
   * {@link com.google.gwt.activity.shared.Activity#mayStop Activity.mayStop})
   * to see if it's safe to stop this presenter.
   * 
   * @return null if it's okay to stop, else a message to ask the user if she's
   *         sure
   */
  String mayStop();
  
  void start(EventBus eventBus);

  void stop();
}
