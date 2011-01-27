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
package com.google.gwt.place.testplaces;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

/**
 * Used by tests of {@link com.google.gwt.place.rebind.PlaceHistoryMapperGenerator}.
 */
public class Place6 extends Place {
  public final String content;

  public Place6(String token) {
    this.content = token;
  }
  
  /**
   * Tokenizer.
   */
  @Prefix("")
  public static class Tokenizer implements PlaceTokenizer<Place6> {
    public Place6 getPlace(String token) {
      return new Place6(token);
    }

    public String getToken(Place6 place) {
      return place.content;
    }
  }
}
