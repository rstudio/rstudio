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
package com.google.gwt.sample.mobilewebapp.client.desktop;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.sample.mobilewebapp.presenter.task.TaskReadView;
import com.google.gwt.sample.mobilewebapp.shared.TaskProxy;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DateLabel;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

/**
 * View used to see the details of a task.
 */
public class DesktopTaskReadView extends Composite implements TaskReadView {

  /**
   * The UiBinder interface.
   */
  interface DesktopTaskReadViewUiBinder extends UiBinder<Widget, DesktopTaskReadView> {
  }

  interface EditorDriver extends SimpleBeanEditorDriver<TaskProxy, DesktopTaskReadView> {
  }

  /**
   * The UiBinder used to generate the view.
   */
  private static DesktopTaskReadViewUiBinder uiBinder = GWT
      .create(DesktopTaskReadViewUiBinder.class);

  @UiField
  DockLayoutPanel dockLayoutPanel;

  /**
   * The panel that contains the edit form.
   */
  @UiField
  HTMLPanel editForm;

  @UiField
  DateLabel dueDateEditor;
  @UiField
  Label nameEditor;
  @UiField
  Label notesEditor;
  @UiField
  Button editButton;

  private final EditorDriver editorDriver = GWT.create(EditorDriver.class);

  /**
   * The {@link TaskReadView.Presenter} for this view.
   */
  private Presenter presenter;

  /**
   * Construct a new {@link DesktopTaskReadView}.
   */
  public DesktopTaskReadView() {
    initWidget(uiBinder.createAndBindUi(this));
    editorDriver.initialize(this);
    // Create a new task or modify the current task when done is pressed.
    editButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        if (presenter != null) {
          presenter.editTask();
        }
      }
    });
  }

  public SimpleBeanEditorDriver<TaskProxy, ?> getEditorDriver() {
    return editorDriver;
  }

  public void setPresenter(Presenter presenter) {
    this.presenter = presenter;
  }
}
