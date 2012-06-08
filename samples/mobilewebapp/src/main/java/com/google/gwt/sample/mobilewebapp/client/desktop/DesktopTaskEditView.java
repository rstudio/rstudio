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

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DataTransfer;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DragDropEventBase;
import com.google.gwt.event.dom.client.DragLeaveEvent;
import com.google.gwt.event.dom.client.DragLeaveHandler;
import com.google.gwt.event.dom.client.DragOverEvent;
import com.google.gwt.event.dom.client.DragOverHandler;
import com.google.gwt.event.dom.client.DragStartEvent;
import com.google.gwt.event.dom.client.DropEvent;
import com.google.gwt.event.dom.client.DropHandler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.sample.mobilewebapp.client.ui.DateButton;
import com.google.gwt.sample.mobilewebapp.client.ui.EditorDecorator;
import com.google.gwt.sample.mobilewebapp.presenter.task.TaskEditView;
import com.google.gwt.sample.mobilewebapp.shared.TaskProxy;
import com.google.gwt.sample.mobilewebapp.shared.TaskProxyImpl;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.uibinder.client.UiRenderer;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.cellview.client.CellList;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TextBoxBase;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.requestfactory.gwt.client.RequestFactoryEditorDriver;

import java.util.ArrayList;
import java.util.List;

/**
 * View used to edit a task.
 */
public class DesktopTaskEditView extends Composite implements TaskEditView {

  /**
   * The UiBinder interface.
   */
  interface DesktopTaskEditViewUiBinder extends UiBinder<Widget, DesktopTaskEditView> {
  }

  /**
   * The cell used to render task templates.
   */
  static class TaskTemplateCell extends AbstractCell<TaskProxy> {

    /**
     * Use a UiBinder template to generate the {@code Cell} contents and process
     * events.
     */
    @UiTemplate("TaskTemplateCell.ui.xml")
    interface Renderer extends UiRenderer {
      void render(SafeHtmlBuilder sb, String name, String notes);
      void onBrowserEvent(TaskTemplateCell o, NativeEvent n, Element e, Context context);
    }

    private Renderer renderer = GWT.create(Renderer.class);

    public TaskTemplateCell() {
      // Register the kinds of event this cell will manage.
      super("dragstart");
    }

    /**
     * Delegates event handling to the generated {@link UiRenderer}.
     */
    @Override
    public void onBrowserEvent(Context context, Element parent, TaskProxy value, NativeEvent event,
        ValueUpdater<TaskProxy> valueUpdater) {
      renderer.onBrowserEvent(this, event, parent, context);
    }

    /**
     * Delegates the cell rendering to the generated {@link UiRenderer}.
     */
    @Override
    public void render(Context context, TaskProxy value, SafeHtmlBuilder sb) {
      if (value == null) {
        return;
      }

      String notes = value.getNotes();
      renderer.render(sb, value.getName(), (notes == null) ? "" : notes);
    }

    /**
     * Handles "drag-start" events inside the element named "root".
    */
    @UiHandler({"root"})
    void onDragStart(DragStartEvent event, Element parent, Context context) {
      // Save the ID of the TaskProxy.
      DataTransfer dataTransfer = event.getDataTransfer();
      dataTransfer.setData("text", String.valueOf(context.getIndex()));

      // Set the image.
      dataTransfer.setDragImage(parent, 25, 15);
    }
  }

  interface Driver extends RequestFactoryEditorDriver<TaskProxy, DesktopTaskEditView> {
  }

  /**
   * The UiBinder used to generate the view.
   */
  private static DesktopTaskEditViewUiBinder uiBinder = GWT
      .create(DesktopTaskEditViewUiBinder.class);

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
  DockLayoutPanel dockLayoutPanel;

  /**
   * The panel that contains the edit form.
   */
  @UiField
  HTMLPanel editForm;

  @UiField
  Button deleteButton;
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
  Button saveButton;

  @UiField(provided = true)
  final CellList<TaskProxy> templateList;

  @UiField
  Widget templateListContainer;

  private final Driver driver = GWT.create(Driver.class);

  /**
   * The {@link TaskEditView.Presenter} for this view.
   */
  private Presenter presenter;

  /**
   * Construct a new {@link DesktopTaskEditView}.
   */
  public DesktopTaskEditView() {
    // Create the template list.
    templateList = createTaskTemplateList();

    initWidget(uiBinder.createAndBindUi(this));
    nameEditor = EditorDecorator.create(nameField.asEditor(), nameViolation);
    driver.initialize(this);

    // Hide the template list if it isn't supported.
    if (!DragDropEventBase.isSupported()) {
      dockLayoutPanel.setWidgetSize(templateListContainer, 0);
    }

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

    // Add the form as a drop target.
    editForm.addDomHandler(new DragOverHandler() {
      public void onDragOver(DragOverEvent event) {
        // Highlight the name and notes box.
        nameField.getElement().getStyle().setBackgroundColor("#ffa");
        notesEditor.getElement().getStyle().setBackgroundColor("#ffa");
      }
    }, DragOverEvent.getType());
    editForm.addDomHandler(new DragLeaveHandler() {
      public void onDragLeave(DragLeaveEvent event) {
        EventTarget eventTarget = event.getNativeEvent().getEventTarget();
        if (!Element.is(eventTarget)) {
          return;
        }
        Element target = Element.as(eventTarget);

        if (target == editForm.getElement()) {
          // Un-highlight the name and notes box.
          nameField.getElement().getStyle().clearBackgroundColor();
          notesEditor.getElement().getStyle().clearBackgroundColor();
        }
      }
    }, DragLeaveEvent.getType());
    editForm.addDomHandler(new DropHandler() {
      public void onDrop(DropEvent event) {
        // Prevent the default text drop.
        event.preventDefault();

        // Un-highlight the name and notes box.
        nameField.getElement().getStyle().clearBackgroundColor();
        notesEditor.getElement().getStyle().clearBackgroundColor();

        // Fill in the form.
        try {
          // Get the template the from the data transfer object.
          DataTransfer dataTransfer = event.getNativeEvent().getDataTransfer();
          int templateIndex = Integer.parseInt(dataTransfer.getData("text"));
          TaskProxy template = templateList.getVisibleItem(templateIndex);
          nameField.setValue(template.getName());
          notesEditor.setValue(template.getNotes());
        } catch (NumberFormatException e) {
          // The user probably dragged something other than a template.
        }
      }
    }, DropEvent.getType());
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

  private CellList<TaskProxy> createTaskTemplateList() {
    CellList<TaskProxy> list =
        new CellList<TaskProxy>(new TaskTemplateCell());
    list.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);

    // Create the templates.
    List<TaskProxy> templates = new ArrayList<TaskProxy>();
    templates.add(new TaskProxyImpl("Call mom", null));
    templates.add(new TaskProxyImpl("Register to vote", "Where is my polling location again?"));
    templates.add(new TaskProxyImpl("Replace air filter", "Size: 24x13x1"));
    templates.add(new TaskProxyImpl("Take out the trash", null));
    list.setRowData(templates);

    return list;
  }
}
