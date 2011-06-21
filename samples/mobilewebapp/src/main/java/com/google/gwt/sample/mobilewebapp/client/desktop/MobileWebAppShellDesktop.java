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
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.VideoElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.media.client.Video;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceChangeEvent;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.sample.mobilewebapp.client.MobileWebAppShell;
import com.google.gwt.sample.mobilewebapp.presenter.task.TaskEditView;
import com.google.gwt.sample.mobilewebapp.presenter.task.TaskPlace;
import com.google.gwt.sample.mobilewebapp.presenter.task.TaskReadView;
import com.google.gwt.sample.mobilewebapp.presenter.taskchart.TaskChartPresenter;
import com.google.gwt.sample.mobilewebapp.presenter.tasklist.TaskListPlace;
import com.google.gwt.sample.mobilewebapp.presenter.tasklist.TaskListView;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellList;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DeckLayoutPanel;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasOneWidget;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.web.bindery.event.shared.EventBus;

import java.util.ArrayList;
import java.util.List;

/**
 * Desktop version of the UI shell.
 */
public class MobileWebAppShellDesktop extends ResizeComposite implements MobileWebAppShell {

  /**
   * CSS override used for the main menu.
   */
  interface MainMenuStyle extends CellList.Style {
  }

  interface MobileWebAppShellDesktopUiBinder extends UiBinder<Widget, MobileWebAppShellDesktop> {
  }

  /**
   * A ClientBundle that provides images for this widget.
   */
  interface Resources extends CellList.Resources {
    /**
     * The styles used in the main menu. We extend
     * {@link CellList.Style#DEFAULT_CSS} with the styles defined in
     * MainMenuCellList.css.
     */
    @Source({"MainMenuCellList.css", CellList.Style.DEFAULT_CSS})
    MainMenuStyle cellListStyle();
  }

  /**
   * The URL attribute that determines whether or not to include the pie chart.
   */
  private static final String CHART_URL_ATTRIBUTE = "chart";

  /**
   * The external URL of the video tutorial for browsers that do not support
   * video.
   */
  private static final String EXTERNAL_TUTORIAL_URL = "http://www.youtube.com/watch?v=oHg5SJYRHA0";

  private static MobileWebAppShellDesktopUiBinder uiBinder = GWT
      .create(MobileWebAppShellDesktopUiBinder.class);

  @UiField
  Anchor helpLink;

  /**
   * The main menu list.
   */
  @UiField(provided = true)
  CellList<MainMenuItem> mainMenu;

  /**
   * The container that holds content.
   */
  @UiField
  DeckLayoutPanel contentContainer;

  @UiField
  DockLayoutPanel leftNav;

  /**
   * The container that holds the pie chart.
   */
  @UiField
  HasOneWidget pieChartContainer;

  /**
   * A boolean indicating that we have not yet seen the first content widget.
   */
  private boolean firstContentWidget = true;

  /**
   * The {@link DialogBox} used to display the tutorial.
   */
  private PopupPanel tutoralPopup;

  /**
   * The video tutorial.
   */
  private Video tutorialVideo;

  /**
   * Construct a new {@link MobileWebAppShellDesktop}.
   */
  public MobileWebAppShellDesktop(EventBus bus, TaskChartPresenter pieChart,
      final PlaceController placeController, TaskListView taskListView, TaskEditView taskEditView,
      TaskReadView taskReadView) {

    // Initialize the main menu.
    Resources resources = GWT.create(Resources.class);
    mainMenu = new CellList<MainMenuItem>(new MainMenuItem.Cell(), resources);
    mainMenu.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);

    // We don't expect to have more than 30 menu items.
    mainMenu.setVisibleRange(0, 30);

    // Add items to the main menu.
    final List<MainMenuItem> menuItems = new ArrayList<MainMenuItem>();
    menuItems.add(new MainMenuItem("Task List", new TaskListPlace(false)) {
      @Override
      public boolean mapsToPlace(Place p) {
        // Map to all TaskListPlace instances.
        return p instanceof TaskListPlace;
      }
    });
    menuItems.add(new MainMenuItem("Add Task", TaskPlace.getTaskCreatePlace()));
    mainMenu.setRowData(menuItems);

    // Choose a place when a menu item is selected.
    final SingleSelectionModel<MainMenuItem> selectionModel =
        new SingleSelectionModel<MainMenuItem>();
    selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
      public void onSelectionChange(SelectionChangeEvent event) {
        MainMenuItem selected = selectionModel.getSelectedObject();
        if (selected != null && !selected.mapsToPlace(placeController.getWhere())) {
          placeController.goTo(selected.getPlace());
        }
      }
    });
    mainMenu.setSelectionModel(selectionModel);

    // Update selection based on the current place.
    bus.addHandler(PlaceChangeEvent.TYPE, new PlaceChangeEvent.Handler() {
      public void onPlaceChange(PlaceChangeEvent event) {
        Place place = event.getNewPlace();
        for (MainMenuItem menuItem : menuItems) {
          if (menuItem.mapsToPlace(place)) {
            // We found a match in the main menu.
            selectionModel.setSelected(menuItem, true);
            return;
          }
        }

        // We didn't find a match in the main menu.
        selectionModel.setSelected(null, true);
      }
    });

    // Initialize this widget.
    initWidget(uiBinder.createAndBindUi(this));

    // Initialize the pie chart.
    String chartUrlValue = Window.Location.getParameter(CHART_URL_ATTRIBUTE);
    if (chartUrlValue != null && chartUrlValue.startsWith("f")) {
      // Chart manually disabled.
      leftNav.remove(1); // Pie Chart.
      leftNav.remove(0); // Pie chart legend.
    } else if (pieChart == null) {
      // Chart not supported.
      pieChartContainer.setWidget(new Label("Try upgrading to a modern browser to enable charts."));
    } else {
      // Chart supported.
      Widget pieWidget = pieChart.asWidget();
      pieWidget.setWidth("90%");
      pieWidget.setHeight("90%");
      pieWidget.getElement().getStyle().setMarginLeft(5.0, Unit.PCT);
      pieWidget.getElement().getStyle().setMarginTop(5.0, Unit.PCT);
      
      pieChartContainer.setWidget(pieChart);
    }

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
    contentContainer.setAnimationDuration(800);

    // Show a tutorial when the help link is clicked.
    helpLink.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        showTutorial();
      }
    });
  }

  @Override
  public void setAddButtonVisible(boolean isVisible) {
    // No-op: Adding a task is handled in the main menu.
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

  /**
   * Show a tutorial video.
   */
  private void showTutorial() {
    // Reuse the tutorial dialog if it is already created.
    if (tutoralPopup != null) {
      // Reset the video.
      // TODO(jlabanca): Is cache-control=private making the video non-seekable?
      if (tutorialVideo != null) {
        tutorialVideo.setSrc(tutorialVideo.getCurrentSrc());
      }

      tutoralPopup.center();
      return;
    }

    /*
     * Forward the use to YouTube if video is not supported or if none of the
     * source formats are supported.
     */
    tutorialVideo = Video.createIfSupported();
    if (tutorialVideo == null) {
      Label label = new Label("Click the link below to view the tutoral:");
      Anchor anchor = new Anchor(EXTERNAL_TUTORIAL_URL, EXTERNAL_TUTORIAL_URL);
      anchor.setTarget("_blank");
      FlowPanel panel = new FlowPanel();
      panel.add(label);
      panel.add(anchor);

      tutoralPopup = new PopupPanel(true, false);
      tutoralPopup.setWidget(panel);
      tutoralPopup.setGlassEnabled(true);

      // Hide the popup when the user clicks the link.
      anchor.addClickHandler(new ClickHandler() {
        public void onClick(ClickEvent event) {
          tutoralPopup.hide();
        }
      });

      tutoralPopup.center();
      return;
    }

    // Add the video sources.
    tutorialVideo.addSource("video/tutorial.ogv", VideoElement.TYPE_OGG);
    tutorialVideo.addSource("video/tutorial.mp4", VideoElement.TYPE_MP4);

    // Setup the video player.
    tutorialVideo.setControls(true);
    tutorialVideo.setAutoplay(true);

    // Put the video in a dialog.
    final DialogBox popup = new DialogBox(false, false);
    popup.setText("Tutorial");
    VerticalPanel vPanel = new VerticalPanel();
    vPanel.add(tutorialVideo);
    vPanel.add(new Button("Close", new ClickHandler() {
      public void onClick(ClickEvent event) {
        tutorialVideo.pause();
        popup.hide();
      }
    }));
    popup.setWidget(vPanel);
    tutoralPopup = popup;
    popup.center();
  }
}
