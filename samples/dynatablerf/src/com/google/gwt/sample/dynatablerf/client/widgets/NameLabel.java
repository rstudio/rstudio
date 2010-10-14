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

import com.google.gwt.editor.client.EditorDelegate;
import com.google.gwt.editor.client.ValueAwareEditor;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.sample.dynatablerf.client.events.EditPersonEvent;
import com.google.gwt.sample.dynatablerf.shared.PersonProxy;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;

/**
 * This NameLabel uses the EditorDelegate to receive notifications on updates to
 * the displayed object.
 */
class NameLabel extends Composite implements ValueAwareEditor<PersonProxy> {
  /**
   * Many of the GWT UI widgets that implement TakesValue also implement
   * IsEditor and are directly usable as sub-Editors.
   */
  final Label nameEditor = new Label();
  private PersonProxy person;
  private HandlerRegistration subscription;

  public NameLabel() {
    this(null);
  }

  public NameLabel(final EventBus eventBus) {
    initWidget(nameEditor);

    if (eventBus != null) {
      nameEditor.addClickHandler(new ClickHandler() {
        public void onClick(ClickEvent event) {
          eventBus.fireEvent(new EditPersonEvent(person));
        }
      });
    }
  }

  public void flush() {
    // No-op
  }

  public void onPropertyChange(String... paths) {
  }

  public void setDelegate(EditorDelegate<PersonProxy> delegate) {
    if (subscription != null) {
      subscription.removeHandler();
    }
    subscription = delegate.subscribe();
  }

  public void setValue(PersonProxy value) {
    person = value;
  }

  /**
   * Unhook event notifications when being permanently disposed of by
   * FavoritesWidget.
   */
  protected void cancelSubscription() {
    if (subscription != null) {
      subscription.removeHandler();
    }
  }
}