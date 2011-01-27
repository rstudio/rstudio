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
package com.google.gwt.place.impl;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceHistoryMapperWithFactory;
import com.google.gwt.place.shared.PlaceTokenizer;

/**
 * Abstract implementation of {@link PlaceHistoryMapper}.
 *
 * @param <F> factory type
 */
public abstract class AbstractPlaceHistoryMapper<F> implements
    PlaceHistoryMapperWithFactory<F> {

  /**
   * Return value for
   * {@link AbstractPlaceHistoryMapper#getPrefixAndToken(Place)}.
   */
  public static class PrefixAndToken {
    public final String prefix;
    public final String token;

    public PrefixAndToken(String prefix, String token) {
      assert prefix != null && !prefix.contains(":");
      this.prefix = prefix;
      this.token = token;
    }

    @Override
    public String toString() {
      return (prefix.length() == 0) ? token : prefix + ":" + token;
    }
  }

  protected F factory;
  
  public Place getPlace(String token) {
    int colonAt = token.indexOf(':');
    String initial;
    String rest;
    if (colonAt >= 0) {
      initial = token.substring(0, colonAt);
      rest = token.substring(colonAt + 1);
    } else {
      initial = "";
      rest = token;
    }
    PlaceTokenizer<?> tokenizer = getTokenizer(initial);
    if (tokenizer != null) {
      return tokenizer.getPlace(rest);
    }
    return null;
  }
  
  public String getToken(Place place) {
    PrefixAndToken token = getPrefixAndToken(place);
    if (token != null) {
      return token.toString();
    }
    return null;
  }
  
  public void setFactory(F factory) {
    this.factory = factory;
  }

  /**
   * @param newPlace what needs tokenizing
   * @return the token, or null
   */
  protected abstract PrefixAndToken getPrefixAndToken(Place newPlace);

  /**
   * @param prefix the prefix found on the history token
   * @return the PlaceTokenizer registered with that token, or null
   */
  protected abstract PlaceTokenizer<?> getTokenizer(String prefix);
}
