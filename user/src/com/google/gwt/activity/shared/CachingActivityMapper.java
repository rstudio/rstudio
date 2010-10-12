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
package com.google.gwt.activity.shared;

import com.google.gwt.place.shared.Place;

/**
 * Wraps another {@link ActivityMapper} and caches the last activity it
 * returned, to be re-used if we see the same place twice.
 */
public class CachingActivityMapper implements ActivityMapper {

  private final ActivityMapper wrapped;

  private Place lastPlace;
  private Activity lastActivity;

  /**
   * Constructs a CachingActivityMapper object.
   *
   * @param wrapped an ActivityMapper object
   */
  public CachingActivityMapper(ActivityMapper wrapped) {
    this.wrapped = wrapped;
  }

  public Activity getActivity(Place place) {
    if (!place.equals(lastPlace)) {
      lastPlace = place;
      lastActivity = wrapped.getActivity(place);
    }

    return lastActivity;
  }
}
