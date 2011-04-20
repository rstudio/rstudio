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
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TextBoxBase;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.datepicker.client.DatePicker;
import com.google.gwt.sample.mobilewebapp.client.activity.TaskEditView;

import java.util.Date;

/**
 * View used to edit a task.
 */
public class DesktopTaskEditView extends Composite implements TaskEditView {

  /**
   * The UiBinder interface.
   */
  interface DesktopTaskEditViewUiBinder extends
      UiBinder<Widget, DesktopTaskEditView> {
  }

  /**
   * The UiBinder used to generate the view.
   */
  private static DesktopTaskEditViewUiBinder uiBinder = GWT.create(DesktopTaskEditViewUiBinder.class);

  /**
   * The formatter used to format the date.
   */
  private static DateTimeFormat dateFormat = DateTimeFormat.getFormat("EE, MMM d, yyyy");

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

  /**
   * The checkbox used to specify that the task should be added to the calendar.
   */
  @UiField
  CheckBox calendarCheckbox;

  /**
   * The button used to select the date.
   */
  @UiField
  Button dateButton;

  /**
   * The button used to delete a task or cancel changes.
   */
  @UiField
  Button deleteButton;

  /**
   * The text box used to enter the task name.
   */
  @UiField
  TextBoxBase nameBox;

  /**
   * The text box used to enter task notes.
   */
  @UiField
  TextBoxBase notesBox;

  /**
   * The text box used to save changes or create a new task.
   */
  @UiField
  Button saveButton;

  /**
   * The current date value.
   */
  private Date currentDate;

  /**
   * The popup panel that contains a date picker for selecting the date.
   */
  private final PopupPanel datePickerPopup;

  /**
   * The {@link Presenter} for this view.
   */
  private Presenter presenter;

  /**
   * Construct a new {@link DesktopTaskEditView}.
   * 
   * @param presenter the {@link Presenter} that handles interactions
   */
  public DesktopTaskEditView() {
    initWidget(uiBinder.createAndBindUi(this));

    // Create the datePickerPopup.
    final DatePicker datePicker = new DatePicker();
    datePickerPopup = new PopupPanel(true, true);
    datePickerPopup.setWidget(datePicker);
    datePickerPopup.setGlassEnabled(true);

    /*
     * When the user clicks on the date button, open a date picker so they can
     * select a date.
     */
    dateButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        datePicker.setValue(currentDate, false);
        datePickerPopup.center();
      }
    });
    datePicker.addValueChangeHandler(new ValueChangeHandler<Date>() {
      public void onValueChange(ValueChangeEvent<Date> event) {
        setDueDate(event.getValue());
        datePickerPopup.hide();
      }
    });

    // Create a new task or modify the current task when done is pressed.
    saveButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        if (presenter != null) {
          presenter.saveTask(calendarCheckbox.getValue());
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

  public Date getDueDate() {
    return currentDate;
  }

  public String getName() {
    return nameBox.getValue();
  }

  public String getNotes() {
    return notesBox.getValue();
  }

  public void setDueDate(Date date) {
    boolean wasNull = (currentDate == null);
    currentDate = date;
    if (date == null) {
      dateButton.setText("Set due date");
      calendarCheckbox.setValue(false);
      calendarCheckbox.setEnabled(false);
    } else {
      dateButton.setText(dateFormat.format(date));
      calendarCheckbox.setEnabled(true);
      if (wasNull) {
        calendarCheckbox.setValue(true);
      }
    }
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

  public void setName(String name) {
    nameBox.setText(name);
  }

  public void setNotes(String notes) {
    notesBox.setText(notes);
  }

  public void setPresenter(Presenter presenter) {
    this.presenter = presenter;
  }
}
