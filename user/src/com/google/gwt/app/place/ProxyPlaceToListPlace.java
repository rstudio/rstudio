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


/**
 * Converts a {@link #ProxyPlace} to a {@link ProxyListPlace}.
 */
public class ProxyPlaceToListPlace implements FilteredActivityMapper.Filter {

  /**
   * Required by {@link FilteredActivityMapper.Filter}, calls
   * {@link #proxyListPlaceFor()}.
   */
  public Place filter(Place place) {
    return proxyListPlaceFor(place);
  }

  /**
   * @param place a place to process
   * @return an appropriate ProxyListPlace, or null if the given place has
   *         nothing to do with proxies
   */
  public ProxyListPlace proxyListPlaceFor(Place place) {
    if (place instanceof ProxyListPlace) {
      return (ProxyListPlace) place;
    }

    if (!(place instanceof ProxyPlace)) {
      return null;
    }

    ProxyPlace proxyPlace = (ProxyPlace) place;
    return new ProxyListPlace(proxyPlace.getProxyId().getProxyClass());
  }
}
