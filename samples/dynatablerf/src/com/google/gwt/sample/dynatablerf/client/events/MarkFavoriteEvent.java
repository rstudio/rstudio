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
package com.google.gwt.sample.dynatablerf.client.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.sample.dynatablerf.shared.PersonProxy;
import com.google.web.bindery.requestfactory.shared.EntityProxyId;

/**
 * Used client-side to add entries to the favorites list.
 */
public class MarkFavoriteEvent extends GwtEvent<MarkFavoriteEvent.Handler> {
  /**
   * Handles {@link MarkFavoriteEvent}.
   */
  public interface Handler extends EventHandler {
    void onMarkFavorite(MarkFavoriteEvent event);
  }

  public static final Type<Handler> TYPE = new Type<Handler>();

  private final EntityProxyId<PersonProxy> id;
  private final boolean isFavorite;

  public MarkFavoriteEvent(EntityProxyId<PersonProxy> id, boolean isFavorite) {
    this.id = id;
    this.isFavorite = isFavorite;
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  public EntityProxyId<PersonProxy> getId() {
    return id;
  }

  public boolean isFavorite() {
    return isFavorite;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onMarkFavorite(this);
  }
}
