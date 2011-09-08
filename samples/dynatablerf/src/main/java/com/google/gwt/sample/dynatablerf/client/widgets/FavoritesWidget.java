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
import com.google.gwt.editor.client.adapters.EditorSource;
import com.google.gwt.editor.client.adapters.ListEditor;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.sample.dynatablerf.client.FavoritesManager;
import com.google.gwt.sample.dynatablerf.client.events.MarkFavoriteEvent;
import com.google.gwt.sample.dynatablerf.shared.PersonProxy;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.requestfactory.gwt.client.RequestFactoryEditorDriver;
import com.google.web.bindery.requestfactory.shared.EntityProxyId;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.google.web.bindery.requestfactory.shared.RequestFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Displays Person objects that the user has selected as a favorite. This
 * demonstrates a read-only "editor" that receives update notifications.
 */
public class FavoritesWidget extends Composite {

  interface Binder extends UiBinder<Widget, FavoritesWidget> {
  }

  /**
   * A driver that accepts a List of PersonProxy objects, controlled by a
   * ListEditor of PersonProxy instances, displayed using NameLabels.
   */
  interface Driver extends RequestFactoryEditorDriver<List<PersonProxy>, //
      ListEditor<PersonProxy, NameLabel>> {
  }

  interface Style extends CssResource {
    String favorite();
  }

  /**
   * This is used by the ListEditor to control the state of the UI.
   */
  private class NameLabelSource extends EditorSource<NameLabel> {
    @Override
    public NameLabel create(int index) {
      NameLabel label = new NameLabel(eventBus);
      label.setStylePrimaryName(style.favorite());
      container.insert(label, index);
      return label;
    }

    @Override
    public void dispose(NameLabel subEditor) {
      subEditor.removeFromParent();
      subEditor.cancelSubscription();
    }

    @Override
    public void setIndex(NameLabel editor, int index) {
      container.insert(editor, index);
    }
  }

  @UiField
  FlowPanel container;

  @UiField
  Style style;

  /**
   * This list is a facade provided by the ListEditor. Structural modifications
   * to this list (e.g. add(), set(), remove()) will trigger UI update events.
   */
  private final List<PersonProxy> displayedList;
  private final EventBus eventBus;
  private final RequestFactory factory;
  private final FavoritesManager manager;
  private HandlerRegistration subscription;

  public FavoritesWidget(EventBus eventBus, RequestFactory factory,
      FavoritesManager manager) {
    this.eventBus = eventBus;
    this.factory = factory;
    this.manager = manager;

    // Create the UI
    initWidget(GWT.<Binder> create(Binder.class).createAndBindUi(this));

    // Create the driver which manages the data-bound widgets
    Driver driver = GWT.<Driver> create(Driver.class);

    // Use a ListEditor that uses our NameLabelSource
    ListEditor<PersonProxy, NameLabel> editor = ListEditor.of(new NameLabelSource());

    // Configure the driver
    ListEditor<PersonProxy, NameLabel> listEditor = editor;
    driver.initialize(eventBus, factory, listEditor);

    // Notice the backing list is essentially anonymous.
    driver.display(new ArrayList<PersonProxy>());

    // Modifying this list triggers widget creation and destruction
    displayedList = listEditor.getList();
  }

  @Override
  protected void onLoad() {
    // Subscribe to notifications from the FavoritesManager
    subscription = manager.addMarkFavoriteHandler(new MarkFavoriteEvent.Handler() {
      public void onMarkFavorite(MarkFavoriteEvent event) {
        FavoritesWidget.this.onMarkFavorite(event);
      }
    });

    // Initialize the UI with the existing list of favorites
    for (EntityProxyId<PersonProxy> id : manager.getFavoriteIds()) {
      onMarkFavorite(new MarkFavoriteEvent(id, true));
    }
  }

  @Override
  protected void onUnload() {
    subscription.removeHandler();
  }

  private void onMarkFavorite(MarkFavoriteEvent event) {
    EntityProxyId<PersonProxy> id = event.getId();
    if (id == null) {
      return;
    }

    // EntityProxies should be compared by stable id
    PersonProxy found = null;
    for (PersonProxy displayed : displayedList) {
      if (displayed.stableId().equals(id)) {
        found = displayed;
        break;
      }
    }

    if (event.isFavorite() && found == null) {
      factory.find(id).to(new Receiver<PersonProxy>() {
        @Override
        public void onSuccess(PersonProxy response) {
          displayedList.add(response);
          sortDisplayedList();
        }
      }).fire();
    } else if (!event.isFavorite() && found != null) {
      displayedList.remove(found);
      sortDisplayedList();
    }
  }

  private void sortDisplayedList() {
    // Sorting the list of PersonProxies will also change the UI display
    Collections.sort(displayedList, new Comparator<PersonProxy>() {
      public int compare(PersonProxy o1, PersonProxy o2) {
        // Newly-created records may have null names
        String name1 = o1.getName() == null ? "" : o1.getName();
        String name2 = o2.getName() == null ? "" : o2.getName();
        return name1.compareToIgnoreCase(name2);
      }
    });
  }
}
