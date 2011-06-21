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
package com.google.gwt.sample.mobilewebapp.client.activity;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.place.shared.Place;
import com.google.gwt.sample.mobilewebapp.presenter.tasklist.TaskListPlace;
import com.google.gwt.sample.ui.client.OrientationHelper;

/**
 * A mapping of places to activities used by the table version of the
 * application.
 */
public class AppActivityMapperTablet implements ActivityMapper {

  private final ActivityMapper wrapped;
  private final OrientationHelper orientationHelper;

  public AppActivityMapperTablet(ActivityMapper wrapped, OrientationHelper orientationHelper) {
    this.wrapped = wrapped;
    this.orientationHelper = orientationHelper;
  }

  public Activity getActivity(Place place) {
    if (place instanceof TaskListPlace && !orientationHelper.isPortrait()) {
      // The task list is always visible in landscape, so don't start one
      return null;
    }

    return wrapped.getActivity(place);
  }
}
