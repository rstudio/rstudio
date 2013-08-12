/*
 * Copyright 2008 Google Inc.
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

package com.google.gwt.user.datepicker.client;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;

import java.util.Date;

/**
 * A simple {@link MonthSelector} used for the default date picker. It allows to select months and
 * years but the name is not changed for backward compatibility. Also not extensible as we wish to
 * evolve it freely over time.
 */

public final class DefaultMonthSelector extends MonthSelector {

  private PushButton monthBackwards;
  private PushButton monthForwards;
  private FlexTable grid;
  private PushButton yearBackwards;
  private PushButton yearForwards;
  private ListBox monthSelect;
  private ListBox yearSelect;
  private int monthColumn;

  /**
   * Returns the button for moving to the previous month.
   */
  public Element getBackwardButtonElement() {
    return monthBackwards.getElement();
  }

  /**
   * Returns the button for moving to the next month.
   */
  public Element getForwardButtonElement() {
    return monthForwards.getElement();
  }

  /**
   * Returns the button for moving to the previous year.
   */
  public Element getYearBackwardButtonElement() {
    return yearBackwards.getElement();
  }

  /**
   * Returns the button for moving to the next year.
   */
  public Element getYearForwardButtonElement() {
    return yearForwards.getElement();
  }

  /**
   * Returns the ListBox for selecting the month
   */
  public ListBox getMonthSelectListBox() {
    return monthSelect;
  }

  /**
   * Returns the ListBox for selecting the year
   */
  public ListBox getYearSelectListBox() {
    return yearSelect;
  }

  @Override
  protected void refresh() {
    if (isDatePickerConfigChanged()) {
      // if the config has changed since the last refresh, rebuild the grid
      setupGrid();
    }

    setDate(getModel().getCurrentMonth());
  }

  @Override
  protected void setup() {
    // previous, next buttons
    monthBackwards = createNavigationButton("&lsaquo;", -1, css().previousButton());
    monthForwards = createNavigationButton("&rsaquo;", 1, css().nextButton());
    yearBackwards = createNavigationButton("&laquo;", -12, css().previousYearButton());
    yearForwards = createNavigationButton("&raquo;", 12, css().nextYearButton());

    // month and year selector
    monthSelect = createMonthSelect();
    yearSelect = createYearSelect();

    // Set up grid.
    grid = new FlexTable();
    grid.setStyleName(css().monthSelector());

    setupGrid();

    initWidget(grid);
  }

  private PushButton createNavigationButton(String label, final int noOfMonths, String styleName) {
    PushButton button = new PushButton();

    button.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        addMonths(noOfMonths);
      }
    });

    button.getUpFace().setHTML(label);
    button.setStyleName(styleName);

    return button;
  }

  private ListBox createMonthSelect() {
    final ListBox monthListBox = new ListBox();

    for (int i = 0; i < CalendarModel.MONTHS_IN_YEAR; i++) {
      monthListBox.addItem(getModel().formatMonth(i));
    }

    monthListBox.addChangeHandler(new ChangeHandler() {

      @Override
      public void onChange(ChangeEvent event) {
        int previousMonth = getModel().getCurrentMonth().getMonth();
        int newMonth = monthListBox.getSelectedIndex();
        int delta = newMonth - previousMonth;

        addMonths(delta);
      }
    });

    return monthListBox;
  }

  private ListBox createYearSelect() {
    final ListBox yearListBox = new ListBox();

    yearListBox.addChangeHandler(new ChangeHandler() {

      @Override
      public void onChange(ChangeEvent event) {
        int deltaYears = yearListBox.getSelectedIndex() - getNoOfYearsToDisplayBefore();

        addMonths(deltaYears * CalendarModel.MONTHS_IN_YEAR);
      }
    });

    return yearListBox;
  }


  private boolean isDatePickerConfigChanged() {
    boolean isMonthCurrentlySelectable = monthSelect.getParent() != null;
    boolean isYearNavigationCurrentlyEnabled = yearBackwards.getParent() != null;

    return getDatePicker().isYearAndMonthDropdownVisible() != isMonthCurrentlySelectable ||
        getDatePicker().isYearArrowsVisible() != isYearNavigationCurrentlyEnabled;
  }

  private void setDate(Date date) {
    if (getDatePicker().isYearAndMonthDropdownVisible()) {
      // setup months dropdown
      int month = date.getMonth();
      monthSelect.setSelectedIndex(month);

      // setup years dropdown
      yearSelect.clear();

      int year = date.getYear();
      int startYear = year - getNoOfYearsToDisplayBefore();
      int endYear = year + getNoOfYearsToDisplayAfter();

      Date newDate = new Date();
      for (int i = startYear; i <= endYear; i++) {
        newDate.setYear(i);
        yearSelect.addItem(getModel().getYearFormatter().format(newDate));
      }
      yearSelect.setSelectedIndex(year - startYear);
    } else {
      grid.setText(0, monthColumn, getModel().formatCurrentMonthAndYear());
    }
  }

  private int getNoOfYearsToDisplayBefore() {
    return (getDatePicker().getVisibleYearCount() -  1) / 2;
  }

  private int getNoOfYearsToDisplayAfter() {
    return getDatePicker().getVisibleYearCount() / 2;
  }

  private void setupGrid() {
    grid.removeAllRows();
    grid.insertRow(0);

    // Back arrows
    if (getDatePicker().isYearArrowsVisible()) {
      addCell(yearBackwards, "1");
    }
    addCell(monthBackwards, "1");

    // Month/Year column
    if (getDatePicker().isYearAndMonthDropdownVisible()) {
      // Drop-down
      if (getModel().isMonthBeforeYear()) {
        addCell(monthSelect, "50%", css().month());
        addCell(yearSelect, "50%", css().year());
      } else {
        addCell(yearSelect, "50%", css().year());
        addCell(monthSelect, "50%", css().month());
      }
    } else {
      // Text-only
      monthColumn = addCell(null, "100%", css().month());
    }

    // Forward arrows
    addCell(monthForwards, "1");
    if (getDatePicker().isYearArrowsVisible()) {
      addCell(yearForwards, "1");
    }
  }

  private int addCell(Widget widget, String width) {
    return addCell(widget, width, null);
  }

  private int addCell(Widget widget, String width, String className) {
    int cell = grid.getCellCount(0);
    grid.setWidget(0, cell, widget);
    grid.getCellFormatter().setWidth(0, cell, width);
    if (className != null) {
      grid.getCellFormatter().setStyleName(0, cell, className);
    }
    return cell;
  }
}
