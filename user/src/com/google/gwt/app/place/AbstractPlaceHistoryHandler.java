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

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.History;

import java.util.logging.Logger;

/**
 * Abstract implementation of {@link PlaceHistoryHandler}.
 * 
 * @param <F> the factory type
 */
public abstract class AbstractPlaceHistoryHandler<F> implements
    PlaceHistoryHandlerWithFactory<F> {
  private static final Logger log = Logger.getLogger(AbstractPlaceHistoryHandler.class.getName());

  /**
   * Return value for
   * {@link AbstractPlaceHistoryHandler#getPrefixAndToken(Place)}.
   */
  public static class PrefixAndToken {
    public final String prefix;
    public final String token;

    public PrefixAndToken(String prefix, String token) {
      super();
      this.prefix = prefix;
      this.token = token;
    }

    @Override
    public String toString() {
      return prefix + ":" + token;
    }
  }

  /**
   * Isolates us from History, for testing.
   */
  protected interface Historian {
    HandlerRegistration addValueChangeHandler(
        ValueChangeHandler<String> valueChangeHandler);

    String getToken();

    void newItem(String token, boolean issueEvent);
  }

  private final Historian historian;

  protected F factory;

  protected PlaceController placeController;

  private Place defaultPlace = Place.NOWHERE;

  protected AbstractPlaceHistoryHandler() {
    this(new Historian() {
      public HandlerRegistration addValueChangeHandler(
          ValueChangeHandler<String> valueChangeHandler) {
        return History.addValueChangeHandler(valueChangeHandler);
      }

      public String getToken() {
        return History.getToken();
      }

      public void newItem(String token, boolean issueEvent) {
        History.newItem(token, issueEvent);
      }
    });
  }

  protected AbstractPlaceHistoryHandler(Historian historian) {
    this.historian = historian;
  }

  public void handleCurrentHistory() {
    handleHistoryToken(historian.getToken());
  }

  public HandlerRegistration register(PlaceController placeController,
      EventBus eventBus, Place defaultPlace) {
    assert factory != null : "No factory was set";
    this.placeController = placeController;
    this.defaultPlace = defaultPlace;

    final HandlerRegistration placeReg = eventBus.addHandler(
        PlaceChangeEvent.TYPE, new PlaceChangeEvent.Handler() {
          public void onPlaceChange(PlaceChangeEvent event) {
            Place newPlace = event.getNewPlace();
            historian.newItem(tokenForPlace(newPlace), false);
          }
        });

    final HandlerRegistration historyReg = historian.addValueChangeHandler(new ValueChangeHandler<String>() {
      public void onValueChange(ValueChangeEvent<String> event) {
        String token = event.getValue();
        handleHistoryToken(token);
      }
    });

    return new HandlerRegistration() {
      public void removeHandler() {
        AbstractPlaceHistoryHandler.this.defaultPlace = Place.NOWHERE;
        AbstractPlaceHistoryHandler.this.placeController = null;
        AbstractPlaceHistoryHandler.this.factory = null;
        placeReg.removeHandler();
        historyReg.removeHandler();
      }
    };
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

  /**
   * Visible for testing.
   */
  Logger log() {
    return log;
  }

  private void handleHistoryToken(String token) {

    Place newPlace = null;

    if ("".equals(token)) {
      newPlace = defaultPlace;
    }

    if (newPlace == null) {
      int colonAt = token.indexOf(':');
      if (colonAt > -1) {
        String initial = token.substring(0, colonAt);
        String rest = token.substring(colonAt + 1);
        PlaceTokenizer<?> tokenizer = getTokenizer(initial);
        if (tokenizer != null) {
          newPlace = tokenizer.getPlace(rest);
        }
      }
    }

    if (newPlace == null) {
      log().warning("Unrecognized history token: " + token);
      newPlace = defaultPlace;
    }

    placeController.goTo(newPlace);
  }

  private String tokenForPlace(Place newPlace) {
    if (defaultPlace.equals(newPlace)) {
      return "";
    }

    PrefixAndToken token = getPrefixAndToken(newPlace);
    if (token != null) {
      return token.toString();
    }

    log().warning("Unregistered place type : " + newPlace);
    return "";
  }

}
