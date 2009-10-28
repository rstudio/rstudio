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

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.DateValueChangeTester;
import com.google.gwt.user.client.ui.HasValue;

import junit.framework.TestCase;

import java.util.Date;

/**
 * Test the DateChangeEvent in isolation from GWT.
 */
public class DateChangeEventTest extends TestCase {

  private static class MockWidget implements HasValue<Date> {
    private final HandlerManager handlers = new HandlerManager(this);
    private Date value;

    public HandlerRegistration addValueChangeHandler(
        ValueChangeHandler<Date> handler) {
      return handlers.addHandler(ValueChangeEvent.getType(), handler);
    }

    public void fireEvent(GwtEvent<?> event) {
      handlers.fireEvent(event);
    }

    public HandlerManager getHandlers() {
      return handlers;
    }

    public Date getValue() {
      return value;
    }

    public void setValue(Date value) {
      setValue(value, false);
    }

    public void setValue(Date value, boolean fireEvents) {
      Date oldValue = this.value;
      this.value = value;
      if (fireEvents) {
        DateChangeEvent.fireIfNotEqualDates(this, oldValue, value);
      }
    }
  }

  public void testValueChangeViaHasValue() {
    new DateValueChangeTester(new MockWidget()).run();
  }
}
