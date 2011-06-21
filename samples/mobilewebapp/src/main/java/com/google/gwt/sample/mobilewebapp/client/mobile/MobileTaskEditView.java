/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.sample.mobilewebapp.client.mobile;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.sample.mobilewebapp.client.ui.DateButton;
import com.google.gwt.sample.mobilewebapp.client.ui.EditorDecorator;
import com.google.gwt.sample.mobilewebapp.presenter.task.TaskEditView;
import com.google.gwt.sample.mobilewebapp.shared.TaskProxy;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TextBoxBase;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.widget.client.TextButton;
import com.google.web.bindery.requestfactory.gwt.client.RequestFactoryEditorDriver;

/**
 * View used to edit a task.
 */
public class MobileTaskEditView extends Composite implements TaskEditView {

  /**
   * Editor driver for this view.
   */
  interface Driver extends RequestFactoryEditorDriver<TaskProxy, MobileTaskEditView> {
  }

  /**
   * The UiBinder interface.
   */
  interface MobileTaskEditViewUiBinder extends UiBinder<Widget, MobileTaskEditView> {
  }

  /**
   * The UiBinder used to generate the view.
   */
  private static MobileTaskEditViewUiBinder uiBinder = GWT.create(MobileTaskEditViewUiBinder.class);

  /**
   * The glass panel used to lock the UI.
   */
  private static PopupPanel glassPanel;

  /**
   * Show or hide the glass panel used to lock the UI will the task loads.
   * 
   * @param visible true to show, false to hide
   */
  private static void setGlassPanelVisible(boolean visible) {
    // Initialize the panel.
    if (glassPanel == null) {
      glassPanel = new DecoratedPopupPanel(false, true);
      glassPanel.setWidget(new Label("Loading..."));
    }

    if (visible) {
      // Show the loading panel.
      glassPanel.center();
    } else {
      // Hide the loading panel.
      glassPanel.hide();
    }
  }

  @UiField
  @Ignore
  TextButton deleteButton;
  @UiField
  DateButton dueDateEditor;
  final EditorDecorator<String> nameEditor;
  @UiField
  @Ignore
  TextBoxBase nameField;
  @UiField
  Element nameViolation;
  @UiField
  TextBoxBase notesEditor;
  @UiField
  @Ignore
  TextButton saveButton;

  private final Driver driver = GWT.create(Driver.class);

  /**
   * The {@link TaskEditView.Presenter} for this view.
   */
  private Presenter presenter;

  /**
   * Construct a new {@link MobileTaskEditView}.
   */
  public MobileTaskEditView() {
    initWidget(uiBinder.createAndBindUi(this));
    nameEditor = EditorDecorator.create(nameField.asEditor(), nameViolation);
    driver.initialize(this);

    // Create a new task or modify the current task when done is pressed.
    saveButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        if (presenter != null) {
          presenter.saveTask();
        }
      }
    });

    // Delete the current task or cancel when delete is pressed.
    deleteButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        if (presenter != null) {
          presenter.deleteTask();
        }
      }
    });
  }

  public RequestFactoryEditorDriver<TaskProxy, ?> getEditorDriver() {
    return driver;
  }

  public void setEditing(boolean isEditing) {
    if (isEditing) {
      deleteButton.setText("Delete item");
    } else {
      deleteButton.setText("Cancel");
    }
  }

  public void setLocked(boolean locked) {
    setGlassPanelVisible(locked);
  }

  public void setNameViolation(String message) {
    nameViolation.setInnerText(message);
  }

  public void setPresenter(Presenter presenter) {
    this.presenter = presenter;
  }
}
