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
package com.google.gwt.sample.mobilewebapp.client.tablet;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.sample.mobilewebapp.client.ClientFactory;
import com.google.gwt.sample.mobilewebapp.client.MobileWebAppShell;
import com.google.gwt.sample.mobilewebapp.client.activity.TaskListView;
import com.google.gwt.sample.mobilewebapp.client.event.AddTaskEvent;
import com.google.gwt.sample.mobilewebapp.client.event.GoHomeEvent;
import com.google.gwt.sample.mobilewebapp.client.ui.OrientationHelper;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DeckLayoutPanel;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RequiresResizeComposite;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Tablet version of the UI shell.
 * 
 * TODO(rjrjr): this thing needs a presenter. Not an activity. A presenter.
 */
public class MobileWebAppShellTablet extends RequiresResizeComposite<DockLayoutPanel> implements MobileWebAppShell {

  interface MobileWebAppShellTabletUiBinder extends UiBinder<DockLayoutPanel, MobileWebAppShellTablet> {
  }

  private static MobileWebAppShellTabletUiBinder uiBinder =
      GWT.create(MobileWebAppShellTabletUiBinder.class);

  /**
   * The width of the task list in landscape mode in PCT.
   */
  private static final double LANDSCAPE_TASK_LIST_WIDTH_PCT = 30.0;

  /**
   * The button used to add items.
   */
  @UiField
  Button addButton;

  /**
   * The container that holds content.
   */
  @UiField
  DeckLayoutPanel contentContainer;

  /**
   * The widget displayed when the user has not selected a task.
   */
  @UiField
  Widget contentEmptyMessage;

  /**
   * The DockLayoutPanel that splits the task list and the task edit views.
   */
  @UiField
  DockLayoutPanel splitPanel;

  /**
   * The container that holds the tast list.
   */
  @UiField
  SimplePanel taskListContainer;

  /**
   * The label containing the application title.
   */
  @UiField
  Label titleLabel;

  /**
   * A reference to the handler for the add button.
   */
  private HandlerRegistration addButtonHandler;

  /**
   * A boolean indicating that we have not yet seen the first content widget.
   */
  private boolean firstContentWidget = true;

  private final EventBus eventBus;

  private final TaskListView taskListView;

  private boolean isShowingTaskList;

  /**
   * Construct a new {@link MobileWebAppShellTablet}.
   * 
   * @param clientFactory the {@link ClientFactory} of shared resources
   */
  public MobileWebAppShellTablet(final EventBus eventBus, OrientationHelper orientationHelper,
      TaskListView taskListView) {
    this.eventBus = eventBus;

    this.taskListView = taskListView;

    // Inject the tablet specific styles.
    TabletResources resources = GWT.create(TabletResources.class);
    resources.tabletStyles().ensureInjected();

    // Initialize this widget.
    initWidget(uiBinder.createAndBindUi(this));

    // Initialize the add button.
    setAddButtonHandler(null);

    orientationHelper.setCommands(this, new Command() {
      @Override
      public void execute() {
        onShiftToPortrait();
      }
    }, new Command() {
      @Override
      public void execute() {
        onShiftToLandscape();
      }
    });

    // Go to the task list place when the title is clicked.
    titleLabel.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        eventBus.fireEvent(new GoHomeEvent());
      }
    });
  }

  /**
   * Set the handler to invoke when the add button is pressed. If no handler is
   * specified, the button is hidden.
   * 
   * @param handler the handler to add to the button, or null to hide
   */
  public void setAddButtonHandler(ClickHandler handler) {
    // Clear the old handler.
    if (addButtonHandler != null) {
      addButtonHandler.removeHandler();
      addButtonHandler = null;
    }

    if (handler == null) {
      // Hide the button.
      addButton.setVisible(false);
    } else {
      // Show the button and add the handler.
      addButton.setVisible(true);
      addButtonHandler = addButton.addClickHandler(handler);
    }
  }

  /**
   * Set the widget to display in content area.
   * 
   * @param content the {@link Widget} to display
   */
  public void setWidget(IsWidget content) {
    contentContainer.setWidget((content == null) ? contentEmptyMessage : content);

    // If the content is null and we are in landscape mode, show the add button.
    if (content == null && isShowingTaskList) {
      setAddButtonHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          eventBus.fireEvent(new AddTaskEvent());
        }
      });
    }

    // Do not animate the first time we show a widget.
    if (firstContentWidget) {
      firstContentWidget = false;
      contentContainer.animate(0);
    }
  }

  private void onShiftToLandscape() {
    // Show the static task list view.
    splitPanel.setWidgetSize(taskListContainer, LANDSCAPE_TASK_LIST_WIDTH_PCT);

    // TODO(rjrjr) View managing activity is an abomination
    if (!isShowingTaskList) {
      taskListView.start();
      taskListContainer.add(taskListView);
      // DeckLayoutPanel sets the display to none, so we need to clear it.
      taskListView.asWidget().getElement().getStyle().clearDisplay();
      isShowingTaskList = true;
    }

    // Do not use animations when the task list is always visible.
    contentContainer.setAnimationDuration(0);

    // Ensure that the task list view is not displayed as content.
    if (contentContainer.getVisibleWidget() == null) {
      contentContainer.setWidget(contentEmptyMessage);
    }
  }

  private void onShiftToPortrait() {
    // Hide the static task list view.
    if (isShowingTaskList) {
      taskListView.stop();
      isShowingTaskList = false;
    }
    splitPanel.setWidgetSize(taskListContainer, 0);

    /*
     * Add both views to the DeckLayoutPanel so we can animate between them.
     * Using a DeckLayoutPanel here works because we only have two views, and we
     * always know that the edit view should animate in from the right side of
     * the screen. A more complex app will require more complex logic to figure
     * out which direction to animate.
     */
    contentContainer.insert(taskListView, 0);
    contentContainer.setAnimationDuration(500);

    // Ensure that something is displayed.
    Widget curWidget = contentContainer.getVisibleWidget();
    if (curWidget == null || curWidget == contentEmptyMessage) {
      eventBus.fireEvent(new GoHomeEvent());
      contentContainer.animate(0);
    }
  }
}
