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
package com.google.gwt.place.shared;

/**
 * Maps {@link Place}s to/from tokens, used to configure a
 * {@link PlaceHistoryHandler}.
 * <p>
 * You can annotate subinterfaces of PlaceHistoryMapper with
 * {@link WithTokenizers} to have their implementation automatically generated
 * via a call to {@link com.google.gwt.core.client.GWT#create(Class)}.
 */
public interface PlaceHistoryMapper {

  /**
   * Returns the {@link Place} associated with the given token.
   *
   * @param token a String token
   * @return a {@link Place} instance
   */
  Place getPlace(String token);
  
  /**
   * Returns the String token associated with the given {@link Place}.
   *
   * @param place a {@link Place} instance
   * @return a String token
   */
  String getToken(Place place);
}
