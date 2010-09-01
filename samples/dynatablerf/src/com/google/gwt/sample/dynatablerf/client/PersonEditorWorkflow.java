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

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.requestfactory.client.RequestFactoryEditorDriver;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.RecordRequest;
import com.google.gwt.requestfactory.shared.SyncResult;
import com.google.gwt.sample.dynatablerf.client.events.EditPersonEvent;
import com.google.gwt.sample.dynatablerf.client.widgets.PersonEditor;
import com.google.gwt.sample.dynatablerf.shared.DynaTableRequestFactory;
import com.google.gwt.sample.dynatablerf.shared.PersonProxy;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTMLPanel;

import java.util.Set;

/**
 * This class shows how the UI for editing a person is wired up to the
 * RequestFactoryEditorDelegate. It is also responsible for showing and
 * dismissing the PersonEditor. The use of the FavoriteManager shows integration
 * between a remote service and a local service.
 */
public class PersonEditorWorkflow {
  interface Binder extends UiBinder<DialogBox, PersonEditorWorkflow> {
    Binder BINDER = GWT.create(Binder.class);
  }

  interface Driver extends
      RequestFactoryEditorDriver<PersonProxy, PersonEditor> {
  }

  static void register(EventBus eventBus,
      final DynaTableRequestFactory requestFactory,
      final FavoritesManager manager) {
    eventBus.addHandler(EditPersonEvent.TYPE, new EditPersonEvent.Handler() {
      public void startEdit(PersonProxy person) {
        new PersonEditorWorkflow(requestFactory, manager, person).edit();
      }
    });
  }

  @UiField
  HTMLPanel contents;

  @UiField
  DialogBox dialog;

  @UiField
  CheckBox favorite;

  @UiField
  PersonEditor personEditor;

  private Driver editorDriver;
  private final FavoritesManager manager;
  private final PersonProxy person;
  private final DynaTableRequestFactory requestFactory;

  private PersonEditorWorkflow(DynaTableRequestFactory requestFactory,
      FavoritesManager manager, PersonProxy person) {
    this.requestFactory = requestFactory;
    this.manager = manager;
    this.person = person;
    Binder.BINDER.createAndBindUi(this);
    contents.addDomHandler(new KeyUpHandler() {
      public void onKeyUp(KeyUpEvent event) {
        if (event.getNativeKeyCode() == KeyCodes.KEY_ESCAPE) {
          onCancel(null);
        }
      }
    }, KeyUpEvent.getType());
  }

  @UiHandler("cancel")
  void onCancel(ClickEvent e) {
    dialog.hide();
  }

  @UiHandler("save")
  void onSave(ClickEvent e) {
    // MOVE TO ACTIVITY END
    dialog.hide();
    editorDriver.<Void> flush().fire(new Receiver<Void>() {
      public void onSuccess(Void response, Set<SyncResult> syncResults) {
      }
    });
  }

  @UiHandler("favorite")
  void onValueChanged(ValueChangeEvent<Boolean> event) {
    manager.setFavorite(person, favorite.getValue());
  }

  private void edit() {
    // The request is configured arbitrarily
    RecordRequest<PersonProxy> fetchRequest = requestFactory.personRequest().findPerson(
        person.getId());

    editorDriver = GWT.create(Driver.class);
    editorDriver.initialize(null, requestFactory, personEditor);

    // Add the paths that the EditorDelegate computes are necessary
    fetchRequest.with(editorDriver.getPaths());

    // We could do more with the request, but we just fire it
    fetchRequest.fire(new Receiver<PersonProxy>() {
      public void onSuccess(PersonProxy person, Set<SyncResult> syncResults) {
        // Start the edit process
        editorDriver.edit(person,
            requestFactory.personRequest().persist(person));
        personEditor.focus();
      }
    });

    // Set up UI while waiting for data
    favorite.setValue(manager.isFavorite(person), false);
    dialog.center();
  }
}
