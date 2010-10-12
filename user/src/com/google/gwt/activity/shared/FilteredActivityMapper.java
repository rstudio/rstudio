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
 * Wraps an activity mapper and applies a filter to the place objects that it
 * sees.
 */
public class FilteredActivityMapper implements ActivityMapper {

  /**
   * Implemented by objects that want to interpret one place as another.
   */
  public interface Filter {
    /**
     * Returns the filtered interpretation of the given {@link Place}.
     *
     * @param place the input {@link Place}.
     * @return the output {@link Place}.
     */
    Place filter(Place place);
  }

  private final Filter filter;
  private final ActivityMapper wrapped;

  /**
   * Constructs a FilteredActivityMapper object.
   *
   * @param filter a Filter object
   * @param wrapped an ActivityMapper object
   */
  public FilteredActivityMapper(Filter filter, ActivityMapper wrapped) {
    this.filter = filter;
    this.wrapped = wrapped;
  }

  public Activity getActivity(Place place) {
    return wrapped.getActivity(filter.filter(place));
  }
}
