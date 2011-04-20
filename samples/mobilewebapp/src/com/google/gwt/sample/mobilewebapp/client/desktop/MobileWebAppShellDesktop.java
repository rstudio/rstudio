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

import com.google.gwt.canvas.dom.client.CssColor;
import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceChangeEvent;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellList;
import com.google.gwt.user.cellview.client.CellList.Style;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.DeckLayoutPanel;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HasOneWidget;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwt.sample.mobilewebapp.client.ClientFactory;
import com.google.gwt.sample.mobilewebapp.client.MobileWebAppShellBase;
import com.google.gwt.sample.mobilewebapp.client.activity.TaskListActivity;
import com.google.gwt.sample.mobilewebapp.client.activity.TaskListActivity.TaskListUpdateEvent;
import com.google.gwt.sample.mobilewebapp.client.place.TaskEditPlace;
import com.google.gwt.sample.mobilewebapp.client.place.TaskListPlace;
import com.google.gwt.sample.mobilewebapp.shared.TaskProxy;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Desktop version of the UI shell.
 */
public class MobileWebAppShellDesktop extends MobileWebAppShellBase {

  /**
   * CSS override used for the main menu.
   */
  interface MainMenuStyle extends CellList.Style {
  }

  interface MobileWebAppShellDesktopUiBinder extends
      UiBinder<Widget, MobileWebAppShellDesktop> {
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
    Style cellListStyle();
  }

  /**
   * An item in the main menu that maps to a specific place.
   */
  private static class MainMenuItem {
    private final String name;
    private final Place place;

    /**
     * Construct a new {@link MainMenuItem}.
     * 
     * @param name the display name
     * @param place the place to open when selected
     */
    public MainMenuItem(String name, Place place) {
      this.name = name;
      this.place = place;
    }

    public String getName() {
      return name;
    }

    public Place getPlace() {
      return place;
    }
  }

  /**
   * The cell used to render a {@link MainMenuItem}.
   */
  private static class MainMenuItemCell extends AbstractCell<MainMenuItem> {

    @Override
    public void render(com.google.gwt.cell.client.Cell.Context context, MainMenuItem value,
        SafeHtmlBuilder sb) {
      if (value == null) {
        return;
      }
      sb.appendEscaped(value.getName());
    }
  }

  /**
   * The URL attribute that determines whether or not to include the pie chart.
   */
  private static final String CHART_URL_ATTRIBUTE = "chart";

  private static MobileWebAppShellDesktopUiBinder uiBinder = GWT
      .create(MobileWebAppShellDesktopUiBinder.class);

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
   * A pie chart showing a snapshot of the tasks.
   */
  private PieChart pieChart;

  /**
   * Construct a new {@link MobileWebAppShellDesktop}.
   * 
   * @param clientFactory the {@link ClientFactory} of shared resources
   */
  public MobileWebAppShellDesktop(final ClientFactory clientFactory) {
    // Initialize the main menu.
    Resources resources = GWT.create(Resources.class);
    mainMenu = new CellList<MainMenuItem>(new MainMenuItemCell(), resources);
    mainMenu.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);

    // We don't expect to have more than 30 menu items.
    mainMenu.setVisibleRange(0, 30);

    // Add items to the main menu.
    final List<MainMenuItem> menuItems = new ArrayList<MainMenuItem>();
    menuItems.add(new MainMenuItem("Task List", new TaskListPlace(false)));
    menuItems.add(new MainMenuItem("Add Task", TaskEditPlace.getTaskCreatePlace()));
    mainMenu.setRowData(menuItems);

    // Choose a place when a menu item is selected.
    final SingleSelectionModel<MainMenuItem> selectionModel =
        new SingleSelectionModel<MainMenuItem>();
    selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
      public void onSelectionChange(SelectionChangeEvent event) {
        MainMenuItem selected = selectionModel.getSelectedObject();
        if (selected != null) {
          clientFactory.getPlaceController().goTo(selected.getPlace());
        }
      }
    });
    mainMenu.setSelectionModel(selectionModel);

    // Update selection based on the current place.
    clientFactory.getEventBus().addHandler(PlaceChangeEvent.TYPE, new PlaceChangeEvent.Handler() {
      public void onPlaceChange(PlaceChangeEvent event) {
        Place place = event.getNewPlace();
        for (MainMenuItem menuItem : menuItems) {
          if (place == menuItem.getPlace()) {
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
    pieChart = PieChart.createIfSupported();
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
      pieChart.setWidth("90%");
      pieChart.setHeight("90%");
      pieChart.getElement().getStyle().setMarginLeft(5.0, Unit.PCT);
      pieChart.getElement().getStyle().setMarginTop(5.0, Unit.PCT);
      pieChartContainer.setWidget(pieChart);
    }

    // Initialize the add button.
    setAddButtonHandler(null);

    /*
     * Add both views to the DeckLayoutPanel so we can animate between them.
     * Using a DeckLayoutPanel here works because we only have two views, and we
     * always know that the edit view should animate in from the right side of
     * the screen. A more complex app will require more complex logic to figure
     * out which direction to animate.
     */
    contentContainer.add(clientFactory.getTaskListView());
    contentContainer.add(clientFactory.getTaskEditView());
    contentContainer.setAnimationDuration(800);

    // Listen for events from the task list activity.
    clientFactory.getEventBus().addHandler(TaskListUpdateEvent.getType(),
        new TaskListActivity.TaskListUpdateHandler() {
          public void onTaskListUpdated(TaskListUpdateEvent event) {
            updatePieChart(event.getTasks());
          }
        });
  }

  public boolean isTaskListIncluded() {
    return false;
  }

  /**
   * Set the handler to invoke when the add button is pressed. If no handler is
   * specified, the button is hidden.
   * 
   * @param handler the handler to add to the button, or null to hide
   */
  public void setAddButtonHandler(ClickHandler handler) {
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
   * Update the pie chart with the list of tasks.
   * 
   * @param tasks the list of tasks
   */
  @SuppressWarnings("deprecation")
  private void updatePieChart(List<TaskProxy> tasks) {
    if (pieChart == null) {
      return;
    }

    // Calculate the slices based on the due date.
    double pastDue = 0;
    double dueSoon = 0;
    double onTime = 0;
    double noDate = 0;
    final Date now = new Date();
    final Date tomorrow = new Date(now.getYear(), now.getMonth(), now.getDate() + 1, 23, 59, 59);
    for (TaskProxy task : tasks) {
      Date dueDate = task.getDueDate();
      if (dueDate == null) {
        noDate++;
      } else if (dueDate.before(now)) {
        pastDue++;
      } else if (dueDate.before(tomorrow)) {
        dueSoon++;
      } else {
        onTime++;
      }
    }

    // Update the pie chart.
    pieChart.clearSlices();
    pieChart.addSlice(pastDue, CssColor.make(255, 100, 100));
    pieChart.addSlice(dueSoon, CssColor.make(255, 200, 100));
    pieChart.addSlice(onTime, CssColor.make(100, 255, 100));
    pieChart.addSlice(noDate, CssColor.make(200, 200, 200));
    pieChart.redraw();
  }
}
