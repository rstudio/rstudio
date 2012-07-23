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

import com.google.gwt.editor.client.IsEditor;
import com.google.gwt.editor.client.LeafValueEditor;
import com.google.gwt.editor.client.adapters.TakesValueEditor;
import com.google.gwt.event.logical.shared.HasHighlightHandlers;
import com.google.gwt.event.logical.shared.HasShowRangeHandlers;
import com.google.gwt.event.logical.shared.HighlightEvent;
import com.google.gwt.event.logical.shared.HighlightHandler;
import com.google.gwt.event.logical.shared.ShowRangeEvent;
import com.google.gwt.event.logical.shared.ShowRangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.VerticalPanel;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Standard GWT date picker.
 * 
 * <h3>CSS Style Rules</h3>
 * 
 * <ul class="css">
 * 
 * <li>.gwt-DatePicker { }</li>
 * 
 * <li>.datePickerMonthSelector { the month selector widget }</li>
 * 
 * <li>.datePickerMonth { the month in the month selector widget } <li>
 * 
 * <li>.datePickerPreviousButton { the previous month button } <li>
 * 
 * <li>.datePickerNextButton { the next month button } <li>
 * 
 * <li>.datePickerDays { the portion of the picker that shows the days }</li>
 * 
 * <li>.datePickerWeekdayLabel { the label over weekdays }</li>
 * 
 * <li>.datePickerWeekendLabel { the label over weekends }</li>
 * 
 * <li>.datePickerDay { a single day }</li>
 * 
 * <li>.datePickerDayIsToday { today's date }</li>
 * 
 * <li>.datePickerDayIsWeekend { a weekend day }</li>
 * 
 * <li>.datePickerDayIsFiller { a day in another month }</li>
 * 
 * <li>.datePickerDayIsValue { the selected day }</li>
 * 
 * <li>.datePickerDayIsDisabled { a disabled day }</li>
 * 
 * <li>.datePickerDayIsHighlighted { the currently highlighted day }</li>
 * 
 * <li>.datePickerDayIsValueAndHighlighted { the highlighted day if it is also
 * selected }</li>
 * 
 * </ul>
 * 
 * <p>
 * <h3>Example</h3>
 * {@example com.google.gwt.examples.DatePickerExample}
 * </p>
 */
public class DatePicker extends Composite implements
    HasHighlightHandlers<Date>, HasShowRangeHandlers<Date>, HasValue<Date>,
    IsEditor<LeafValueEditor<Date>> {

  /**
   * Convenience class to group css style names.
   */
  static class StandardCss {

    static StandardCss DEFAULT = new StandardCss("gwt-DatePicker", "datePicker");

    private String baseName;
    private String widgetName;

    public StandardCss(String widgetName, String baseName) {
      this.widgetName = widgetName;
      this.baseName = baseName;
    }

    public String datePicker() {
      return getWidgetStyleName();
    }

    public String day() {
      return wrap("Day");
    }

    public String day(String dayModifier) {
      return day() + "Is" + dayModifier;
    }

    public String dayIsDisabled() {
      return day("Disabled");
    }

    public String dayIsFiller() {
      return day("Filler");
    }

    public String dayIsHighlighted() {
      return day("Highlighted");
    }

    public String dayIsToday() {
      return day("Today");
    }

    public String dayIsValue() {
      return day("Value");
    }

    public String dayIsValueAndHighlighted() {
      return dayIsValue() + "AndHighlighted";
    }

    public String dayIsWeekend() {
      return day("Weekend");
    }

    public String days() {
      return wrap("Days");
    }

    public String daysLabel() {
      return wrap("DaysLabel");
    }

    public String getBaseStyleName() {
      return baseName;
    }

    public String getWidgetStyleName() {
      return widgetName;
    }

    public String month() {
      return wrap("Month");
    }

    public String monthSelector() {
      return wrap("MonthSelector");
    }

    public String nextButton() {
      return wrap("NextButton");
    }

    public String previousButton() {
      return wrap("PreviousButton");
    }

    public String weekdayLabel() {
      return wrap("WeekdayLabel");
    }

    public String weekendLabel() {
      return wrap("WeekendLabel");
    }

    /**
     * Prepends the base name to the given style.
     * 
     * @param style style name
     * @return style name
     */
    protected String wrap(String style) {
      return baseName + style;
    }
  }

  /**
   * A date highlighted event that copied on read.
   */
  private static class DateHighlightEvent extends HighlightEvent<Date> {
    protected DateHighlightEvent(Date highlighted) {
      super(highlighted);
    }

    @Override
    public Date getHighlighted() {
      return CalendarUtil.copyDate(super.getHighlighted());
    }
  }

  private static class DateStyler {
    private Map<String, String> info = new HashMap<String, String>();

    public String getStyleName(Date d) {
      return info.get(genKey(d));
    }

    public void setStyleName(Date d, String styleName, boolean add) {
      // Code is easier to maintain if surrounded by " ", and on all browsers
      // this is a no-op.
      styleName = " " + styleName + " ";
      String key = genKey(d);
      String current = info.get(key);

      if (add) {
        if (current == null) {
          info.put(key, styleName);
        } else if (current.indexOf(styleName) == -1) {
          info.put(key, current + styleName);
        }
      } else {
        if (current != null) {
          String newValue = current.replaceAll(styleName, "");
          if (newValue.trim().length() == 0) {
            info.remove(key);
          } else {
            info.put(key, newValue);
          }
        }
      }
    }

    @SuppressWarnings("deprecation")
    private String genKey(Date d) {
      return d.getYear() + "/" + d.getMonth() + "/" + d.getDate();
    }
  }

  private final DateStyler styler = new DateStyler();

  private final MonthSelector monthSelector;
  private final CalendarView view;
  private final CalendarModel model;
  private Date value;
  private Date highlighted;
  private StandardCss css = StandardCss.DEFAULT;
  private LeafValueEditor<Date> editor;

  /**
   * Create a new date picker.
   */
  public DatePicker() {
    this(new DefaultMonthSelector(), new DefaultCalendarView(),
        new CalendarModel());
  }

  /**
   * Creates a new date picker.
   * 
   * @param monthSelector the month selector
   * @param view the view
   * @param model the model
   */

  protected DatePicker(MonthSelector monthSelector, CalendarView view,
      CalendarModel model) {

    this.model = model;
    this.monthSelector = monthSelector;
    monthSelector.setDatePicker(this);
    this.view = view;
    view.setDatePicker(this);

    view.setup();
    monthSelector.setup();
    this.setup();

    setCurrentMonth(new Date());
    addStyleToDates(css().dayIsToday(), new Date());
  }

  public HandlerRegistration addHighlightHandler(HighlightHandler<Date> handler) {
    return addHandler(handler, HighlightEvent.getType());
  }

  public HandlerRegistration addShowRangeHandler(ShowRangeHandler<Date> handler) {
    return addHandler(handler, ShowRangeEvent.getType());
  }

  /**
   * Adds a show range handler and immediately activate the handler on the
   * current view.
   * 
   * @param handler the handler
   * @return the handler registration
   */
  public HandlerRegistration addShowRangeHandlerAndFire(
      ShowRangeHandler<Date> handler) {
    ShowRangeEvent<Date> event = new ShowRangeEvent<Date>(
        getView().getFirstDate(), getView().getLastDate()) {
    };
    handler.onShowRange(event);
    return addShowRangeHandler(handler);
  }

  /**
   * Add a style name to the given dates.
   */
  public void addStyleToDates(String styleName, Date date) {
    styler.setStyleName(date, styleName, true);
    if (isDateVisible(date)) {
      getView().addStyleToDate(styleName, date);
    }
  }

  /**
   * Add a style name to the given dates.
   */
  public void addStyleToDates(String styleName, Date date, Date... moreDates) {
    addStyleToDates(styleName, date);
    for (Date d : moreDates) {
      addStyleToDates(styleName, d);
    }
  }

  /**
   * Add a style name to the given dates.
   */
  public void addStyleToDates(String styleName, Iterable<Date> dates) {
    for (Date d : dates) {
      addStyleToDates(styleName, d);
    }
  }

  /**
   * Adds the given style name to the specified dates, which must be visible.
   * This is only set until the next time the DatePicker is refreshed.
   */
  public void addTransientStyleToDates(String styleName, Date date) {
    assert isDateVisible(date) : date + " must be visible";
    getView().addStyleToDate(styleName, date);
  }

  /**
   * Adds the given style name to the specified dates, which must be visible.
   * This is only set until the next time the DatePicker is refreshed.
   */
  public final void addTransientStyleToDates(String styleName, Date date,
      Date... moreDates) {
    addTransientStyleToDates(styleName, date);
    for (Date d : moreDates) {
      addTransientStyleToDates(styleName, d);
    }
  }

  /**
   * Adds the given style name to the specified dates, which must be visible.
   * This is only set until the next time the DatePicker is refreshed.
   */
  public final void addTransientStyleToDates(String styleName,
      Iterable<Date> dates) {
    for (Date d : dates) {
      addTransientStyleToDates(styleName, d);
    }
  }

  public HandlerRegistration addValueChangeHandler(
      ValueChangeHandler<Date> handler) {
    return addHandler(handler, ValueChangeEvent.getType());
  }

  /**
   * Returns a {@link TakesValueEditor} backed by the DatePicker.
   */
  public LeafValueEditor<Date> asEditor() {
    if (editor == null) {
      editor = TakesValueEditor.of(this);
    }
    return editor;
  }

  /**
   * Gets the current month the date picker is showing.
   * 
   * <p>
   * A datepicker <b> may </b> show days not in the current month. It
   * <b>must</b> show all days in the current month.
   * </p>
   * 
   * @return the current month
   * 
   */
  public Date getCurrentMonth() {
    return getModel().getCurrentMonth();
  }

  /**
   * Returns the first shown date.
   * 
   * @return the first date.
   */
  // Final because the view should always control the value of the first date.
  public final Date getFirstDate() {
    return view.getFirstDate();
  }

  /**
   * Gets the highlighted date (the one the mouse is hovering over), if any.
   * 
   * @return the highlighted date
   */
  public final Date getHighlightedDate() {
    return CalendarUtil.copyDate(highlighted);
  }

  /**
   * Returns the last shown date.
   * 
   * @return the last date.
   */
  // Final because the view should always control the value of the last date.
  public final Date getLastDate() {
    return view.getLastDate();
  }

  /**
   * Gets the style associated with a date (does not include styles set via
   * {@link #addTransientStyleToDates}).
   * 
   * @param date the date
   * @return the styles associated with this date
   */
  public String getStyleOfDate(Date date) {
    return styler.getStyleName(date);
  }

  /**
   * Returns the selected date, or null if none is selected.
   * 
   * @return the selected date, or null
   */
  public final Date getValue() {
    return CalendarUtil.copyDate(value);
  }

  /**
   * Is the visible date enabled?
   * 
   * @param date the date, which must be visible
   * @return is the date enabled?
   */
  public boolean isDateEnabled(Date date) {
    assert isDateVisible(date) : date + " is not visible";
    return getView().isDateEnabled(date);
  }

  /**
   * Is the date currently shown in the date picker?
   * 
   * @param date
   * @return is the date currently shown
   */
  public boolean isDateVisible(Date date) {
    CalendarView r = getView();
    Date first = r.getFirstDate();
    Date last = r.getLastDate();
    return (date != null && (CalendarUtil.isSameDate(first, date)
        || CalendarUtil.isSameDate(last, date) || (first.before(date) && last.after(date))));
  }

  @Override
  public void onLoad() {
    ShowRangeEvent.fire(this, getFirstDate(), getLastDate());
  }

  /**
   * Removes the styleName from the given dates (even if it is transient).
   */
  public void removeStyleFromDates(String styleName, Date date) {
    styler.setStyleName(date, styleName, false);
    if (isDateVisible(date)) {
      getView().removeStyleFromDate(styleName, date);
    }
  }

  /**
   * Removes the styleName from the given dates (even if it is transient).
   */
  public void removeStyleFromDates(String styleName, Date date,
      Date... moreDates) {
    removeStyleFromDates(styleName, date);
    for (Date d : moreDates) {
      removeStyleFromDates(styleName, d);
    }
  }

  /**
   * Removes the styleName from the given dates (even if it is transient).
   */
  public void removeStyleFromDates(String styleName, Iterable<Date> dates) {
    for (Date d : dates) {
      removeStyleFromDates(styleName, d);
    }
  }

  /**
   * Sets the date picker to show the given month, use {@link #getFirstDate()}
   * and {@link #getLastDate()} to access the exact date range the date picker
   * chose to display.
   * <p>
   * A datepicker <b> may </b> show days not in the current month. It
   * <b>must</b> show all days in the current month.
   * </p>
   * 
   * @param month the month to show
   */
  public void setCurrentMonth(Date month) {
    getModel().setCurrentMonth(month);
    refreshAll();
  }

  /**
   * Sets the date picker style name.
   */
  @Override
  public void setStyleName(String styleName) {
    css = new StandardCss(styleName, "datePicker");
    super.setStyleName(styleName);
  }

  /**
   * Sets a visible date to be enabled or disabled. This is only set until the
   * next time the DatePicker is refreshed.
   */
  public final void setTransientEnabledOnDates(boolean enabled, Date date) {
    assert isDateVisible(date) : date + " must be visible";
    getView().setEnabledOnDate(enabled, date);
  }

  /**
   * Sets a visible date to be enabled or disabled. This is only set until the
   * next time the DatePicker is refreshed.
   */
  public final void setTransientEnabledOnDates(boolean enabled, Date date,
      Date... moreDates) {
    setTransientEnabledOnDates(enabled, date);
    for (Date d : moreDates) {
      setTransientEnabledOnDates(enabled, d);
    }
  }

  /**
   * Sets a group of visible dates to be enabled or disabled. This is only set
   * until the next time the DatePicker is refreshed.
   */
  public final void setTransientEnabledOnDates(boolean enabled,
      Iterable<Date> dates) {
    for (Date d : dates) {
      setTransientEnabledOnDates(enabled, d);
    }
  }

  /**
   * Sets the {@link DatePicker}'s value.
   * 
   * @param newValue the new value
   */
  public final void setValue(Date newValue) {
    setValue(newValue, false);
  }

  /**
   * Sets the {@link DatePicker}'s value.
   * 
   * @param newValue the new value for this date picker
   * @param fireEvents should events be fired.
   */
  public final void setValue(Date newValue, boolean fireEvents) {
    Date oldValue = value;

    if (oldValue != null) {
      removeStyleFromDates(css().dayIsValue(), oldValue);
    }

    value = CalendarUtil.copyDate(newValue);
    if (value != null) {
      addStyleToDates(css().dayIsValue(), value);
    }
    getView().setAriaSelectedCell(newValue);

    if (fireEvents) {
      DateChangeEvent.fireIfNotEqualDates(this, oldValue, newValue);
    }
  }

  /**
   * Gets the {@link CalendarModel} associated with this date picker.
   * 
   * @return the model
   */
  protected final CalendarModel getModel() {
    return model;
  }

  /**
   * Gets the {@link MonthSelector} associated with this date picker.
   * 
   * @return the month selector
   */
  protected final MonthSelector getMonthSelector() {
    return monthSelector;
  }

  /**
   * Gets the {@link CalendarView} associated with this date picker.
   * 
   * @return the view
   */
  protected final CalendarView getView() {
    return view;
  }

  /**
   * Refreshes all components of this date picker.
   */
  protected final void refreshAll() {
    highlighted = null;
    getModel().refresh();

    getView().refresh();
    getMonthSelector().refresh();
    if (isAttached()) {
      ShowRangeEvent.fire(this, getFirstDate(), getLastDate());
    }
    getView().setAriaSelectedCell(value);
  }

  /**
   * Sets up the date picker.
   */
  protected void setup() {
    /*
     * Use a table (VerticalPanel) to get shrink-to-fit behavior. Divs expand to
     * fill the available width, so we'd need to give it a size.
     */
    VerticalPanel panel = new VerticalPanel();
    initWidget(panel);
    setStyleName(panel.getElement(), css.datePicker());
    setStyleName(css().datePicker());
    panel.add(this.getMonthSelector());
    panel.add(this.getView());
  }

  /**
   * Gets the css associated with this date picker for use by extended month and
   * cell grids.
   * 
   * @return the css.
   */
  final StandardCss css() {
    return css;
  }

  /**
   * Sets the highlighted date.
   * 
   * @param highlighted highlighted date
   */
  void setHighlightedDate(Date highlighted) {
    this.highlighted = highlighted;
    fireEvent(new DateHighlightEvent(highlighted));
  }
}
