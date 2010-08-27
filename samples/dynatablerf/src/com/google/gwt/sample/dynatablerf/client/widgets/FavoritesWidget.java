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
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.SyncResult;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.sample.dynatablerf.client.FavoritesManager;
import com.google.gwt.sample.dynatablerf.client.events.MarkFavoriteEvent;
import com.google.gwt.sample.dynatablerf.client.gen.PersonRequestFactoryDriver;
import com.google.gwt.sample.dynatablerf.shared.DynaTableRequestFactory;
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

  interface Style extends CssResource {
    String favorite();
  }

  @UiField
  FlowPanel container;

  @UiField
  Style style;

  private final EventBus eventBus;
  private final DynaTableRequestFactory factory;
  private FavoritesManager manager;
  private final Map<Long, NameLabel> map = new HashMap<Long, NameLabel>();
  private HandlerRegistration subscription;

  public FavoritesWidget(EventBus eventBus, DynaTableRequestFactory factory,
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

    for (Long id : manager.getFavoriteIds()) {
      factory.personRequest().findPerson(id).fire(new Receiver<PersonProxy>() {
        public void onSuccess(PersonProxy response, Set<SyncResult> syncResults) {
          onMarkFavorite(new MarkFavoriteEvent(response, true));
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
      if (!map.containsKey(person.getId())) {
        NameLabel label = new NameLabel(eventBus);
        PersonRequestFactoryDriver delegate = new PersonRequestFactoryDriver();
        delegate.initialize(eventBus, factory, label);
        delegate.edit(person, null);
        label.setStylePrimaryName(style.favorite());

        container.add(label);
        map.put(person.getId(), label);
      }
    } else {
      NameLabel toRemove = map.remove(person.getId());
      if (toRemove != null) {
        container.remove(toRemove);
      }
    }
  }
}
