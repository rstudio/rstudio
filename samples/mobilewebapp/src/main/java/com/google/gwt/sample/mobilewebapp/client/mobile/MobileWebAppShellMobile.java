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
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.sample.mobilewebapp.client.MobileWebAppShell;
import com.google.gwt.sample.mobilewebapp.client.event.ActionEvent;
import com.google.gwt.sample.mobilewebapp.client.event.ActionNames;
import com.google.gwt.sample.mobilewebapp.presenter.task.TaskEditView;
import com.google.gwt.sample.mobilewebapp.presenter.task.TaskReadView;
import com.google.gwt.sample.mobilewebapp.presenter.tasklist.TaskListView;
import com.google.gwt.sample.ui.client.OrientationHelper;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DeckLayoutPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.EventBus;

/**
 * Mobile version of the UI shell.
 */
public class MobileWebAppShellMobile extends ResizeComposite implements MobileWebAppShell {

  interface MobileWebAppShellMobileUiBinder extends UiBinder<Widget, MobileWebAppShellMobile> {
  }

  private static MobileWebAppShellMobileUiBinder uiBinder = GWT
      .create(MobileWebAppShellMobileUiBinder.class);

  /**
   * The width of the menu bar in landscape mode in EX.
   */
  private static final double LANDSCAPE_MENU_WIDTH_EX = 8.0;

  /**
   * The height of the menu bar in portrait mode in PT.
   */
  private static final double PORTRAIT_MENU_HEIGHT_PT = 28.0;

  /**
   * The button used to add items.
   */
  @UiField
  Button addButton;

  /**
   * The widget that wraps the add button.
   */
  @UiField
  Widget addButtonContainer;

  /**
   * The widget that wraps the back button.
   */
  @UiField
  Widget backButtonContainer;

  /**
   * The panel that holds the current content.
   */
  @UiField
  DeckLayoutPanel contentContainer;

  /**
   * The panel used for layout.
   */
  @UiField
  LayoutPanel layoutPanel;

  @UiField
  Widget titleBar;

  @UiField
  Element titleElem;

  /**
   * A boolean indicating that we have not yet seen the first content widget.
   */
  private boolean firstContentWidget = true;

  /**
   * Construct a new {@link MobileWebAppShellMobile}.
   */
  public MobileWebAppShellMobile(final EventBus eventBus, OrientationHelper orientationHelper,
      TaskListView taskListView, TaskEditView taskEditView, TaskReadView taskReadView) {

    initWidget(uiBinder.createAndBindUi(this));

    // Initialize the add button.
    // Initialize the add button.
    addButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        ActionEvent.fire(eventBus, ActionNames.ADD_TASK);
      }
    });
    setAddButtonVisible(false);

    /*
     * Add all views to the DeckLayoutPanel so we can animate between them.
     * Using a DeckLayoutPanel here works because we only have a few views, and
     * we always know that the task views should animate in from the right side
     * of the screen. A more complex app will require more complex logic to
     * figure out which direction to animate.
     */
    contentContainer.add(taskListView);
    contentContainer.add(taskReadView);
    contentContainer.add(taskEditView);
    contentContainer.setAnimationDuration(500);

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

    // Return to the task list when the title is clicked.
    titleBar.addDomHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        ActionEvent.fire(eventBus, ActionNames.GO_HOME);
      }
    }, ClickEvent.getType());
  }

  @Override
  public void setAddButtonVisible(boolean visible) {
    addButton.setVisible(visible);
  }

  /**
   * Set the widget to display in content area.
   * 
   * @param content the {@link Widget} to display
   */
  public void setWidget(IsWidget content) {
    contentContainer.setWidget(content);

    // Do not animate the first time we show a widget.
    if (firstContentWidget) {
      firstContentWidget = false;
      contentContainer.animate(0);
    }
  }

  private void onShiftToLandscape() {

    // Landscape.
    layoutPanel.setWidgetTopBottom(titleBar, 0, Unit.PX, 0, Unit.PX);
    layoutPanel.setWidgetLeftWidth(titleBar, 0, Unit.PX, LANDSCAPE_MENU_WIDTH_EX, Unit.EX);
    titleElem.getStyle().setDisplay(Display.NONE);

    layoutPanel.setWidgetTopBottom(contentContainer, 0, Unit.PX, 0, Unit.PX);
    layoutPanel.setWidgetLeftRight(contentContainer, LANDSCAPE_MENU_WIDTH_EX, Unit.EX, 0, Unit.PX);

    layoutPanel.setWidgetTopHeight(addButtonContainer, 5, Unit.PX, 4, Unit.EX);
    layoutPanel
        .setWidgetLeftWidth(addButtonContainer, 0, Unit.PX, LANDSCAPE_MENU_WIDTH_EX, Unit.EX);

    layoutPanel.setWidgetBottomHeight(backButtonContainer, 5, Unit.PX, 4, Unit.EX);
    layoutPanel.setWidgetLeftWidth(backButtonContainer, 0, Unit.PX, LANDSCAPE_MENU_WIDTH_EX,
        Unit.EX);
  }

  private void onShiftToPortrait() {
    // Portrait.
    layoutPanel.setWidgetTopHeight(titleBar, 0, Unit.PX, PORTRAIT_MENU_HEIGHT_PT, Unit.PT);
    layoutPanel.setWidgetLeftRight(titleBar, 0, Unit.PX, 0, Unit.PX);
    titleElem.getStyle().clearDisplay();

    layoutPanel.setWidgetTopBottom(contentContainer, PORTRAIT_MENU_HEIGHT_PT, Unit.PT, 0, Unit.PX);
    layoutPanel.setWidgetLeftRight(contentContainer, 0, Unit.EX, 0, Unit.PX);

    layoutPanel
        .setWidgetTopHeight(addButtonContainer, 0, Unit.PX, PORTRAIT_MENU_HEIGHT_PT, Unit.PT);
    layoutPanel.setWidgetRightWidth(addButtonContainer, 8, Unit.PX, 3, Unit.EX);

    layoutPanel.setWidgetTopHeight(backButtonContainer, 0, Unit.PX, PORTRAIT_MENU_HEIGHT_PT,
        Unit.PT);
    layoutPanel.setWidgetLeftWidth(backButtonContainer, 8, Unit.PX, 6, Unit.EX);
  }
}
