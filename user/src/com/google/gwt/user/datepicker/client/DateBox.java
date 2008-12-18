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

import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TextBox;

import java.util.Date;

/**
 * A text box that shows a {@link DatePicker} when the user focuses on it.
 * 
 * <h3>CSS Style Rules</h3>
 * 
 * <ul class="css">
 * 
 * <li>.gwt-DateBox { }</li>
 * 
 * <li>.dateBoxPopup { Applied to the popup around the DatePicker }</li>
 * 
 * </ul>
 * 
 * <p>
 * <h3>Example</h3>
 * {@example com.google.gwt.examples.DateBoxExample}
 * </p>
 */
public class DateBox extends Composite implements HasValue<Date> {

  /**
   * Implemented by a delegate to report errors parsing date values from the
   * user's input.
   * 
   * @deprecated, is going to be replaced with a format interface shortly.
   */
  @Deprecated
  public interface InvalidDateReporter {
    /**
     * Called when a valid date has been parsed, or the datebox has been
     * cleared.
     */
    void clearError();

    /**
     * Given an unparseable string, explain the situation to the user.
     * 
     * @param input what the user typed
     */
    void reportError(String input);
  }

  private class DateBoxHandler implements ValueChangeHandler<Date>,
      FocusHandler, BlurHandler, ClickHandler, KeyDownHandler {

    public void onBlur(BlurEvent event) {
      if (!popup.isVisible()) {
        updateDateFromTextBox();
      }
    }

    public void onClick(ClickEvent event) {
      showDatePicker();
    }

    public void onFocus(FocusEvent event) {
      if (allowDPShow) {
        showDatePicker();
      }
    }

    public void onKeyDown(KeyDownEvent event) {
      switch (event.getNativeKeyCode()) {
        case KeyCodes.KEY_ENTER:
        case KeyCodes.KEY_TAB:
        case KeyCodes.KEY_ESCAPE:
        case KeyCodes.KEY_UP:
          updateDateFromTextBox();
          hideDatePicker();
          break;
        case KeyCodes.KEY_DOWN:
          showDatePicker();
          break;
      }
    }

    public void onValueChange(ValueChangeEvent<Date> event) {
      setValue(event.getValue());
      hideDatePicker();
      preventDatePickerPopup();
      box.setFocus(true);
    }
  }

  /**
   * Default style name.
   */
  public static final String DEFAULT_STYLENAME = "gwt-DateBox";

  public static final InvalidDateReporter DEFAULT_INVALID_DATE_REPORTER = new InvalidDateReporter() {
    public void clearError() {
    }

    public void reportError(String input) {
    }
  };
  private static final DateTimeFormat DEFAULT_FORMATTER = DateTimeFormat.getMediumDateFormat();

  private final PopupPanel popup;
  private final TextBox box = new TextBox();
  private final DatePicker picker;

  private final InvalidDateReporter invalidDateReporter;
  private DateTimeFormat dateFormatter = DEFAULT_FORMATTER;

  private boolean allowDPShow = true;

  /**
   * Create a new date box with a new {@link DatePicker} and the
   * {@link #DEFAULT_INVALID_DATE_REPORTER}, which does nothing.
   */
  public DateBox() {
    this(new DatePicker(), DEFAULT_INVALID_DATE_REPORTER);
  }

  /**
   * Create a new date box.
   * 
   * @param picker the picker to drop down from the date box
   */
  public DateBox(DatePicker picker, InvalidDateReporter invalidDateReporter) {
    this.picker = picker;
    this.invalidDateReporter = invalidDateReporter;
    this.popup = new PopupPanel();
    popup.setAutoHideEnabled(true);
    popup.setAutoHidePartner(box.getElement());
    popup.setWidget(picker);
    popup.setStyleName("dateBoxPopup");
    initWidget(box);
    setStyleName(DEFAULT_STYLENAME);

    DateBoxHandler handler = new DateBoxHandler();
    picker.addValueChangeHandler(handler);
    box.addFocusHandler(handler);
    box.addBlurHandler(handler);
    box.addClickHandler(handler);
    box.addKeyDownHandler(handler);
  }

  public HandlerRegistration addValueChangeHandler(
      ValueChangeHandler<Date> handler) {
    return addHandler(handler, ValueChangeEvent.getType());
  }

  /**
   * Gets the current cursor position in the date box.
   * 
   * @return the cursor position
   * 
   */
  public int getCursorPos() {
    return box.getCursorPos();
  }

  /**
   * Gets the date picker.
   * 
   * @return the date picker
   */
  public DatePicker getDatePicker() {
    return picker;
  }

  /**
   * Gets the date box's position in the tab index.
   * 
   * @return the date box's tab index
   */
  public int getTabIndex() {
    return box.getTabIndex();
  }

  /**
   * Get text box.
   * 
   * @return the text box used to enter the formatted date
   */
  public TextBox getTextBox() {
    return box;
  }

  /**
   * Get the date displayed, or null if the text box is empty, or cannot be
   * interpretted. The {@link InvalidDateReporter} may fire as a side effect of
   * this call.
   * 
   * @return the Date
   */
  public Date getValue() {
    return parseDate(true);
  }

  /**
   * Hide the date picker.
   */
  public void hideDatePicker() {
    popup.hide();
  }

  /**
   * @return true if date picker is currently visible, false if not
   */
  public boolean isDatePickerVisible() {
    return popup.isVisible();
  }

  /**
   * Sets the date box's 'access key'. This key is used (in conjunction with a
   * browser-specific modifier key) to automatically focus the widget.
   * 
   * @param key the date box's access key
   */
  public void setAccessKey(char key) {
    box.setAccessKey(key);
  }

  /**
   * Sets the date format to the given format. If date box is not empty,
   * contents of date box will be replaced with current date in new format. If
   * the date cannot be parsed, the current value will be preserved and the
   * InvalidDateReporter notified as usual.
   * 
   * @param format format.
   */
  public void setDateFormat(DateTimeFormat format) {
    if (format != dateFormatter) {
      Date date = getValue();
      dateFormatter = format;
      setValue(date);
    }
  }

  /**
   * Sets whether the date box is enabled.
   * 
   * @param enabled is the box enabled
   */
  public void setEnabled(boolean enabled) {
    box.setEnabled(enabled);
  }

  /**
   * Explicitly focus/unfocus this widget. Only one widget can have focus at a
   * time, and the widget that does will receive all keyboard events.
   * 
   * @param focused whether this widget should take focus or release it
   */
  public void setFocus(boolean focused) {
    box.setFocus(focused);
  }

  /**
   * Sets the date box's position in the tab index. If more than one widget has
   * the same tab index, each such widget will receive focus in an arbitrary
   * order. Setting the tab index to <code>-1</code> will cause this widget to
   * be removed from the tab order.
   * 
   * @param index the date box's tab index
   */
  public void setTabIndex(int index) {
    box.setTabIndex(index);
  }

  /**
   * Set the date.
   */
  public void setValue(Date date) {
    setValue(date, false);
  }

  public void setValue(Date date, boolean fireEvents) {
    Date oldDate = getValue();

    if (date == null) {
      picker.setValue(null);
      box.setText("");
    } else {
      picker.setValue(date, false);
      picker.setCurrentMonth(date);
      setDate(date);
    }

    invalidDateReporter.clearError();
    if (fireEvents) {
      DateChangeEvent.fireIfNotEqualDates(this, oldDate, date);
    }
  }

  /**
   * Parses the current date box's value and shows that date.
   */
  public void showDatePicker() {
    Date current = parseDate(false);
    if (current == null) {
      current = new Date();
    }
    picker.setCurrentMonth(current);
    popup.showRelativeTo(this);
  }

  private Date parseDate(boolean reportError) {
    Date d = null;
    String text = box.getText().trim();
    if (!text.equals("")) {
      try {
        d = dateFormatter.parse(text);
      } catch (IllegalArgumentException exception) {
        if (reportError) {
          invalidDateReporter.reportError(text);
        }
      }
    }
    return d;
  }

  private void preventDatePickerPopup() {
    allowDPShow = false;
    DeferredCommand.addCommand(new Command() {
      public void execute() {
        allowDPShow = true;
      }
    });
  }

  /**
   * Does the actual work of setting the date. Performs no validation, fires no
   * events.
   */
  private void setDate(Date value) {
    box.setText(dateFormatter.format(value));
  }

  private void updateDateFromTextBox() {
    Date parsedDate = parseDate(true);
    if (parsedDate != null) {
      setValue(parsedDate);
    }
  }
}
