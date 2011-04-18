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
package com.google.gwt.sample.dynatablerf.client.widgets;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.sample.dynatablerf.client.events.FilterChangeEvent;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;

/**
 * Used by DayFilterWidget.
 */
class DayCheckBox extends Composite {
  private final CheckBox cb = new CheckBox();
  private int day;
  private final EventBus eventBus;
  private HandlerRegistration filterRegistration;

  public DayCheckBox(EventBus eventBus) {
    this.eventBus = eventBus;
    initWidget(cb);
    cb.setValue(true);
    cb.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
      public void onValueChange(ValueChangeEvent<Boolean> event) {
        DayCheckBox.this.eventBus.fireEvent(new FilterChangeEvent(getDay(),
            getValue()));
      }
    });
  }

  public int getDay() {
    return day;
  }

  public boolean getValue() {
    return cb.getValue();
  }

  public void setCaption(String caption) {
    cb.setText(caption);
  }

  public void setDay(int day) {
    this.day = day;
  }

  public void setValue(boolean value) {
    cb.setValue(value);
  }

  /**
   * Attach to the event bus only when the widget is actually attached to the
   * DOM.
   */
  @Override
  protected void onLoad() {
    filterRegistration = eventBus.addHandler(FilterChangeEvent.TYPE,
        new FilterChangeEvent.Handler() {
          public void onFilterChanged(FilterChangeEvent e) {
            if (e.getDay() == getDay()) {
              setValue(e.isSelected());
            }
          }
        });
  }

  @Override
  protected void onUnload() {
    filterRegistration.removeHandler();
  }
}