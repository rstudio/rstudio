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
package com.google.gwt.sample.mobilewebapp.presenter.taskchart;

import com.google.gwt.canvas.dom.client.CssColor;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.sample.mobilewebapp.client.event.TaskListUpdateEvent;
import com.google.gwt.sample.mobilewebapp.client.ui.PieChart;
import com.google.gwt.sample.mobilewebapp.shared.TaskProxy;
import com.google.gwt.sample.ui.client.PresentsWidgets;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.EventBus;

import java.util.Date;
import java.util.List;

/**
 * Drives a {@link PieChart} to summarize a set of tasks.
 */
public class TaskChartPresenter implements PresentsWidgets {

  /**
   * A pie chart showing a snapshot of the tasks.
   */
  private final PieChart pieChart;

  /**
   * Construct a new {@link TaskChartPresenter}.
   * 
   * @param pieChart the {@link PieChart}
   */
  public TaskChartPresenter(PieChart pieChart) {
    this.pieChart = pieChart;
  }

  public Widget asWidget() {
    return pieChart;
  }

  @Override
  public String mayStop() {
    return null;
  }

  public void start(EventBus eventBus) {
    pieChart.setWidth("90%");
    pieChart.setHeight("90%");
    pieChart.getElement().getStyle().setMarginLeft(5.0, Unit.PCT);
    pieChart.getElement().getStyle().setMarginTop(5.0, Unit.PCT);

    // Listen for events from the task list activity.
    eventBus.addHandler(TaskListUpdateEvent.TYPE, new TaskListUpdateEvent.Handler() {
      public void onTaskListUpdated(TaskListUpdateEvent event) {
        updatePieChart(event.getTasks());
      }
    });
  }

  @Override
  public void stop() {
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
    if (pastDue > 0) {
      pieChart.addSlice(pastDue, CssColor.make(255, 100, 100));
    }
    if (dueSoon > 0) {
      pieChart.addSlice(dueSoon, CssColor.make(255, 200, 100));
    }
    if (onTime > 0) {
      pieChart.addSlice(onTime, CssColor.make(100, 255, 100));
    }
    if (noDate > 0) {
      pieChart.addSlice(noDate, CssColor.make(200, 200, 200));
    }
    pieChart.redraw();
  }
}
