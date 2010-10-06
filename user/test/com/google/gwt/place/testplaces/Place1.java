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
package com.google.gwt.place.testplaces;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

/**
 * Used by tests of {@link com.google.gwt.place.rebind.PlaceHistoryMapperGenerator}.
 */
public class Place1 extends Place {
  public final String content;

  public Place1(String token) {
    this.content = token;
  }
  
  /**
   * Tokenizer.
   */
  @Prefix(Tokenizer.PREFIX)
  public static class Tokenizer implements PlaceTokenizer<Place1> {
    public static final String PREFIX = "T1";

    public Place1 getPlace(String token) {
      return new Place1(token);
    }

    public String getToken(Place1 place) {
      return place.content;
    }
  }
}
