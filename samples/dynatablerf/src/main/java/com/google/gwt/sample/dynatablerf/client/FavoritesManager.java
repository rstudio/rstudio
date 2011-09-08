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
package com.google.gwt.sample.dynatablerf.client;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.sample.dynatablerf.client.events.MarkFavoriteEvent;
import com.google.gwt.sample.dynatablerf.shared.PersonProxy;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.ClosingEvent;
import com.google.gwt.user.client.Window.ClosingHandler;
import com.google.web.bindery.requestfactory.shared.EntityProxyId;
import com.google.web.bindery.requestfactory.shared.RequestFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Manages client-side favorites.
 */
public class FavoritesManager {
  private static final String COOKIE_NAME = "Favorites";
  private final EventBus eventBus = new SimpleEventBus();
  private final Set<EntityProxyId<PersonProxy>> favoriteIds = new HashSet<EntityProxyId<PersonProxy>>();

  public FavoritesManager(final RequestFactory requestFactory) {
    String cookie = Cookies.getCookie(COOKIE_NAME);
    if (cookie != null) {
      try {
        for (String fragment : cookie.split(",")) {
          if (fragment.length() == 0) {
            continue;
          }
          EntityProxyId<PersonProxy> id = requestFactory.getProxyId(fragment);
          if (id != null) {
            favoriteIds.add(id);
          }
        }
      } catch (NumberFormatException e) {
        // Not a big deal, start up without favorites
        favoriteIds.clear();
      }
    }

    Window.addWindowClosingHandler(new ClosingHandler() {
      public void onWindowClosing(ClosingEvent event) {
        StringBuilder sb = new StringBuilder();
        for (EntityProxyId<PersonProxy> id : favoriteIds) {
          sb.append(requestFactory.getHistoryToken(id)).append(",");
        }
        Cookies.setCookie(COOKIE_NAME, sb.toString());
      }
    });
  }

  public HandlerRegistration addMarkFavoriteHandler(
      MarkFavoriteEvent.Handler handler) {
    return eventBus.addHandler(MarkFavoriteEvent.TYPE, handler);
  }

  public Set<EntityProxyId<PersonProxy>> getFavoriteIds() {
    return Collections.unmodifiableSet(favoriteIds);
  }

  public boolean isFavorite(PersonProxy person) {
    return favoriteIds.contains(person.stableId());
  }

  public void setFavorite(EntityProxyId<PersonProxy> id, boolean isFavorite) {
    if (isFavorite) {
      favoriteIds.add(id);
    } else {
      favoriteIds.remove(id);
    }

    eventBus.fireEventFromSource(new MarkFavoriteEvent(id, isFavorite), this);
  }
}
