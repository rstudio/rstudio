/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.cell.client;

import static com.google.gwt.dom.client.BrowserEvents.CLICK;
import static com.google.gwt.dom.client.BrowserEvents.KEYDOWN;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import com.google.gwt.text.shared.SimpleSafeHtmlRenderer;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;
import com.google.gwt.user.datepicker.client.DatePicker;

import java.util.Date;

/**
 * A {@link Cell} used to render and edit {@link Date}s. When a cell is selected
 * by clicking on it, a {@link DatePicker} is popped up. When a date is selected
 * using the {@link DatePicker}, the new date is passed to the
 * {@link ValueUpdater#update update} method of the {@link ValueUpdater} that
 * was passed to {@link #onBrowserEvent} for the click event. Note that this
 * means that the call to {@link ValueUpdater#update} will occur after {@link
 * #onBrowserEvent} has returned. Pressing the 'escape' key dismisses the {@link
 * DatePicker} popup without calling {@link ValueUpdater#update}.
 * 
 * <p>
 * Each {@link DatePickerCell} has a unique {@link DatePicker} popup associated
 * with it; thus, if a single {@link DatePickerCell} is used as the cell for a
 * column in a table, only one entry in that column will be editable at a given
 * time.
 * </p>
 */
public class DatePickerCell extends AbstractEditableCell<Date, Date> {

  private static final int ESCAPE = 27;

  private final DatePicker datePicker;
  private final DateTimeFormat format;
  private int offsetX = 10;
  private int offsetY = 10;
  private Object lastKey;
  private Element lastParent;
  private int lastIndex;
  private int lastColumn;
  private Date lastValue;
  private PopupPanel panel;
  private final SafeHtmlRenderer<String> renderer;
  private ValueUpdater<Date> valueUpdater;

  /**
   * Constructs a new DatePickerCell that uses the date/time format given by
   * {@link DateTimeFormat#getFullDateFormat}.
   */
  @SuppressWarnings("deprecation")
  public DatePickerCell() {
    this(DateTimeFormat.getFullDateFormat(),
        SimpleSafeHtmlRenderer.getInstance());
  }

  /**
   * Constructs a new DatePickerCell that uses the given date/time format and a
   * {@link SimpleSafeHtmlRenderer}.
   *
   * @param format a {@link DateTimeFormat} instance
   */
  public DatePickerCell(DateTimeFormat format) {
    this(format, SimpleSafeHtmlRenderer.getInstance());
  }

  /**
   * Constructs a new DatePickerCell that uses the date/time format given by
   * {@link DateTimeFormat#getFullDateFormat} and the given
   * {@link SafeHtmlRenderer}.
   * 
   * @param renderer a {@link SafeHtmlRenderer SafeHtmlRenderer<String>} instance
   */
  public DatePickerCell(SafeHtmlRenderer<String> renderer) {
    this(DateTimeFormat.getFormat(PredefinedFormat.DATE_FULL), renderer);
  }

  /**
   * Constructs a new DatePickerCell that uses the given date/time format and
   * {@link SafeHtmlRenderer}.
   *
   * @param format a {@link DateTimeFormat} instance
   * @param renderer a {@link SafeHtmlRenderer SafeHtmlRenderer<String>} instance
   */
  public DatePickerCell(DateTimeFormat format, SafeHtmlRenderer<String> renderer) {
    super(CLICK, KEYDOWN);
    if (format == null) {
      throw new IllegalArgumentException("format == null");
    }
    if (renderer == null) {
      throw new IllegalArgumentException("renderer == null");
    }
    this.format = format;
    this.renderer = renderer;

    this.datePicker = new DatePicker();
    this.panel = new PopupPanel(true, true) {
      @Override
      protected void onPreviewNativeEvent(NativePreviewEvent event) {
        if (Event.ONKEYUP == event.getTypeInt()) {
          if (event.getNativeEvent().getKeyCode() == ESCAPE) {
            // Dismiss when escape is pressed
            panel.hide();
          }
        }
      }
    };
    panel.addCloseHandler(new CloseHandler<PopupPanel>() {
      public void onClose(CloseEvent<PopupPanel> event) {
        lastKey = null;
        lastValue = null;
        lastIndex = -1;
        lastColumn = -1;
        if (lastParent != null && !event.isAutoClosed()) {
          // Refocus on the containing cell after the user selects a value, but
          // not if the popup is auto closed.
          lastParent.focus();
        }
        lastParent = null;
      }
    });
    panel.add(datePicker);

    // Hide the panel and call valueUpdater.update when a date is selected
    datePicker.addValueChangeHandler(new ValueChangeHandler<Date>() {
      public void onValueChange(ValueChangeEvent<Date> event) {
        // Remember the values before hiding the popup.
        Element cellParent = lastParent;
        Date oldValue = lastValue;
        Object key = lastKey;
        int index = lastIndex;
        int column = lastColumn;
        panel.hide();

        // Update the cell and value updater.
        Date date = event.getValue();
        setViewData(key, date);
        setValue(new Context(index, column, key), cellParent, oldValue);
        if (valueUpdater != null) {
          valueUpdater.update(date);
        }
      }
    });
  }

  @Override
  public boolean isEditing(Context context, Element parent, Date value) {
    return lastKey != null && lastKey.equals(context.getKey());
  }

  @Override
  public void onBrowserEvent(Context context, Element parent, Date value,
      NativeEvent event, ValueUpdater<Date> valueUpdater) {
    super.onBrowserEvent(context, parent, value, event, valueUpdater);
    if (CLICK.equals(event.getType())) {
      onEnterKeyDown(context, parent, value, event, valueUpdater);
    }
  }

  @Override
  public void render(Context context, Date value, SafeHtmlBuilder sb) {
    // Get the view data.
    Object key = context.getKey();
    Date viewData = getViewData(key);
    if (viewData != null && viewData.equals(value)) {
      clearViewData(key);
      viewData = null;
    }

    String s = null;
    if (viewData != null) {
      s = format.format(viewData);
    } else if (value != null) {
      s = format.format(value);
    }
    if (s != null) {
      sb.append(renderer.render(s));
    }
  }

  @Override
  protected void onEnterKeyDown(Context context, Element parent, Date value,
      NativeEvent event, ValueUpdater<Date> valueUpdater) {
    this.lastKey = context.getKey();
    this.lastParent = parent;
    this.lastValue = value;
    this.lastIndex = context.getIndex();
    this.lastColumn = context.getColumn();
    this.valueUpdater = valueUpdater;

    Date viewData = getViewData(lastKey);
    Date date = (viewData == null) ? lastValue : viewData;
    datePicker.setCurrentMonth(date);
    datePicker.setValue(date);
    panel.setPopupPositionAndShow(new PositionCallback() {
      public void setPosition(int offsetWidth, int offsetHeight) {
        panel.setPopupPosition(lastParent.getAbsoluteLeft() + offsetX,
            lastParent.getAbsoluteTop() + offsetY);
      }
    });
  }
}
