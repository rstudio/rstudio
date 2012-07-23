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

import com.google.gwt.aria.client.Roles;
import com.google.gwt.aria.client.SelectedValue;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.HTMLTable.CellFormatter;
import com.google.gwt.user.datepicker.client.DefaultCalendarView.CellGrid.DateCell;

import java.util.Date;

/**
 * Simple calendar view. Not extensible as we wish to evolve it freely over
 * time.
 */

@SuppressWarnings(/* Date manipulation required */{"deprecation"})
public final class DefaultCalendarView extends CalendarView {

  /**
   * Cell grid.
   */
  // Javac bug requires that date be fully specified here.
  class CellGrid extends CellGridImpl<java.util.Date> {
    /**
     * A cell representing a date.
     */
    class DateCell extends Cell {
      private String cellStyle;
      private String dateStyle;

      DateCell(boolean isWeekend) {
        super(new Date());
        cellStyle = css().day();

        if (isWeekend) {
          cellStyle += " " + css().dayIsWeekend();
        }
        getElement().setTabIndex(isFiller() ? -1 : 0);
        setAriaSelected(false);
      }

      @Override
      public void addStyleName(String styleName) {
        if (dateStyle.indexOf(" " + styleName + " ") == -1) {
          dateStyle += styleName + " ";
        }
        updateStyle();
      }

      public boolean isFiller() {
        return !getModel().isInCurrentMonth(getValue());
      }

      @Override
      public void onHighlighted(boolean highlighted) {
        setHighlightedDate(getValue());
        updateStyle();
      }

      @Override
      public void onSelected(boolean selected) {
        if (selected) {
          getDatePicker().setValue(getValue(), true);
          if (isFiller()) {
            getDatePicker().setCurrentMonth(getValue());
          }
        }
        updateStyle();
      }

      @Override
      public void removeStyleName(String styleName) {
        dateStyle = dateStyle.replace(" " + styleName + " ", " ");
        updateStyle();
      }

      public void setAriaSelected(boolean value) {
        Roles.getGridcellRole().setAriaSelectedState(getElement(), SelectedValue.of(value));
      }

      @Override
      public void updateStyle() {

        String accum = dateStyle;

        if (isHighlighted()) {
          accum += " " + css().dayIsHighlighted();

          if (isHighlighted() && isSelected()) {
            accum += " " + css().dayIsValueAndHighlighted();
          }
        }
        if (!isEnabled()) {
          accum += " " + css().dayIsDisabled();
        }
        setStyleName(accum);
      }

      void update(Date current) {
        setEnabled(true);
        getValue().setTime(current.getTime());
        String value = getModel().formatDayOfMonth(getValue());
        setText(value);
        dateStyle = cellStyle;
        if (isFiller()) {
          getElement().setTabIndex(-1);
          dateStyle += " " + css().dayIsFiller();
        } else {
          getElement().setTabIndex(0);
          String extraStyle = getDatePicker().getStyleOfDate(current);
          if (extraStyle != null) {
            dateStyle += " " + extraStyle;
          }
        }
        // We want to certify that all date styles have " " before and after
        // them for ease of adding to and replacing them.
        dateStyle += " ";
        updateStyle();
      }

      private void setText(String value) {
        DOM.setInnerText(getElement(), value);
      }
    }

    CellGrid() {
      resize(CalendarModel.WEEKS_IN_MONTH + 1, CalendarModel.DAYS_IN_WEEK);
    }

    @Override
    protected void onSelected(Cell lastSelected, Cell cell) {
    }
  }

  private CellGrid grid = new CellGrid();

  private Date firstDisplayed;

  private Date lastDisplayed = new Date();

  private DateCell ariaSelectedCell;

  /**
   * Constructor.
   */
  public DefaultCalendarView() {
  }

  @Override
  public void addStyleToDate(String styleName, Date date) {
    assert getDatePicker().isDateVisible(date) : "You tried to add style " + styleName + " to "
        + date + ". The calendar is currently showing " + getFirstDate()
        + " to " + getLastDate();
    getCell(date).addStyleName(styleName);
  }

  @Override
  public Date getFirstDate() {
    return firstDisplayed;
  }

  @Override
  public Date getLastDate() {
    return lastDisplayed;
  }

  @Override
  public boolean isDateEnabled(Date d) {
    return getCell(d).isEnabled();
  }

  @Override
  public void refresh() {
    firstDisplayed = getModel().getCurrentFirstDayOfFirstWeek();

    if (firstDisplayed.getDate() == 1) {
      // show one empty week if date is Monday is the first in month.
      CalendarUtil.addDaysToDate(firstDisplayed, -7);
    }

    lastDisplayed.setTime(firstDisplayed.getTime());

    for (int i = 0; i < grid.getNumCells(); i++) {
      if (i != 0) {
        CalendarUtil.addDaysToDate(lastDisplayed, 1);
      }
      DateCell cell = (DateCell) grid.getCell(i);
      cell.update(lastDisplayed);
    }
    setAriaSelectedCell(null);
  }

  @Override
  public void removeStyleFromDate(String styleName, Date date) {
    getCell(date).removeStyleName(styleName);
  }

  @Override
  public void setAriaSelectedCell(Date date) {
    if (ariaSelectedCell != null) {
      ariaSelectedCell.setAriaSelected(false);
    }
    DateCell newSelectedCell = date != null ? getCell(date) : null;
    if (newSelectedCell != null) {
      newSelectedCell.setAriaSelected(true);
    }
    ariaSelectedCell = newSelectedCell;
  }

  @Override
  public void setEnabledOnDate(boolean enabled, Date date) {
    getCell(date).setEnabled(enabled);
  }

  @Override
  public void setup() {
    // Preparation
    CellFormatter formatter = grid.getCellFormatter();
    int weekendStartColumn = -1;
    int weekendEndColumn = -1;

    // Set up the day labels.
    for (int i = 0; i < CalendarModel.DAYS_IN_WEEK; i++) {
      int shift = CalendarUtil.getStartingDayOfWeek();
      int dayIdx = i + shift < CalendarModel.DAYS_IN_WEEK ? i + shift : i
          + shift - CalendarModel.DAYS_IN_WEEK;
      grid.setText(0, i, getModel().formatDayOfWeek(dayIdx));

      if (CalendarUtil.isWeekend(dayIdx)) {
        formatter.setStyleName(0, i, css().weekendLabel());
        if (weekendStartColumn == -1) {
          weekendStartColumn = i;
        } else {
          weekendEndColumn = i;
        }
      } else {
        formatter.setStyleName(0, i, css().weekdayLabel());
      }
    }

    // Set up the calendar grid.
    for (int row = 1; row <= CalendarModel.WEEKS_IN_MONTH; row++) {
      for (int column = 0; column < CalendarModel.DAYS_IN_WEEK; column++) {
        DateCell cell = grid.new DateCell(column == weekendStartColumn
            || column == weekendEndColumn);
        grid.setWidget(row, column, cell);
      }
    }
    initWidget(grid);
    grid.setStyleName(css().days());
  }

  private DateCell getCell(Date d) {
    int index = CalendarUtil.getDaysBetween(firstDisplayed, d);
    if (index < 0 || grid.getNumCells() <= index) {
      return null;
    }

    DateCell cell = (DateCell) grid.getCell(index);
    if (cell.getValue().getDate() != d.getDate()) {
      throw new IllegalStateException(d + " cannot be associated with cell "
          + cell + " as it has date " + cell.getValue());
    }
    return cell;
  }
}
