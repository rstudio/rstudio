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
 * Finds the activity to run for a given {@link Place}, used to configure
 * an {@link ActivityManager}.
 */
public interface ActivityMapper {
  /**
   * Returns the activity to run for the given {@link Place}, or null.
   *
   * @param place a Place object
   */
  Activity getActivity(Place place);
}
