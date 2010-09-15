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
package com.google.gwt.sample.dynatablerf.client.widgets;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.requestfactory.client.RequestFactoryEditorDriver;
import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.EntityProxyId;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.RequestFactory;
import com.google.gwt.requestfactory.shared.SyncResult;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.sample.dynatablerf.client.FavoritesManager;
import com.google.gwt.sample.dynatablerf.client.events.MarkFavoriteEvent;
import com.google.gwt.sample.dynatablerf.shared.PersonProxy;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Displays Person objects that the user has selected as a favorite. This
 * demonstrates a read-only "editor" that receives update notifications.
 */
public class FavoritesWidget extends Composite {

  interface Binder extends UiBinder<Widget, FavoritesWidget> {
  }

  interface Driver extends RequestFactoryEditorDriver<PersonProxy, NameLabel> {
  }

  interface Style extends CssResource {
    String favorite();
  }

  @UiField
  FlowPanel container;

  @UiField
  Style style;
  private final EventBus eventBus;
  private final RequestFactory factory;
  private FavoritesManager manager;
  private final Map<EntityProxyId, NameLabel> map = new HashMap<EntityProxyId, NameLabel>();
  private HandlerRegistration subscription;

  public FavoritesWidget(EventBus eventBus, RequestFactory factory,
      FavoritesManager manager) {
    this.eventBus = eventBus;
    this.factory = factory;
    this.manager = manager;
    initWidget(GWT.<Binder> create(Binder.class).createAndBindUi(this));
  }

  @Override
  protected void onLoad() {
    subscription = manager.addMarkFavoriteHandler(new MarkFavoriteEvent.Handler() {
      public void onMarkFavorite(MarkFavoriteEvent event) {
        FavoritesWidget.this.onMarkFavorite(event);
      }
    });

    for (EntityProxyId id : manager.getFavoriteIds()) {
      factory.find(id).fire(new Receiver<EntityProxy>() {
        public void onSuccess(EntityProxy response, Set<SyncResult> syncResults) {
          PersonProxy person = (PersonProxy) response;
          onMarkFavorite(new MarkFavoriteEvent(person, true));
        }
      });
    }
  }

  @Override
  protected void onUnload() {
    subscription.removeHandler();
  }

  void onMarkFavorite(MarkFavoriteEvent event) {
    PersonProxy person = event.getPerson();
    if (person == null) {
      return;
    }

    if (event.isFavorite()) {
      if (!map.containsKey(person.stableId())) {
        NameLabel label = new NameLabel(eventBus);
        Driver driver = GWT.create(Driver.class);
        driver.initialize(eventBus, factory, label);
        driver.edit(person, null);
        label.setStylePrimaryName(style.favorite());

        container.add(label);
        map.put(person.stableId(), label);
      }
    } else {
      NameLabel toRemove = map.remove(person.stableId());
      if (toRemove != null) {
        container.remove(toRemove);
      }
    }
  }
}
