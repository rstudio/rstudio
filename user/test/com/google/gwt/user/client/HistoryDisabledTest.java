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
package com.google.gwt.user.client;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests for {@link History} when History is disabled. Most of these tests are
 * just assuring that we don't hit an NPE or JS error.
 */
public class HistoryDisabledTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.HistoryDisabledTest";
  }

  @SuppressWarnings("deprecation")
  public void testAddHistoryListener() {
    HistoryListener listener = new HistoryListener() {
      public void onHistoryChanged(String historyToken) {
      }
    };
    History.addHistoryListener(listener);
    History.removeHistoryListener(listener);
  }

  public void testAddValueChangeHandler() {
    HandlerRegistration reg = History.addValueChangeHandler(new ValueChangeHandler<String>() {
      public void onValueChange(ValueChangeEvent<String> event) {
      }
    });
    assertNull(reg);
  }

  public void testFireCurrentHistoryState() {
    HandlerRegistration reg = History.addValueChangeHandler(new ValueChangeHandler<String>() {
      public void onValueChange(ValueChangeEvent<String> event) {
        fail("Handler should not have been added.");
      }
    });
    assertNull(reg);
    History.fireCurrentHistoryState();
  }

  @SuppressWarnings("deprecation")
  public void testOnHistoryChanged() {
    HandlerRegistration reg = History.addValueChangeHandler(new ValueChangeHandler<String>() {
      public void onValueChange(ValueChangeEvent<String> event) {
        fail("Handler should not have been added.");
      }
    });
    assertNull(reg);
    History.onHistoryChanged("test");
  }

  public void testGetToken() {
    assertEquals("", History.getToken());
  }

  public void testNewItem() {
    HandlerRegistration reg = History.addValueChangeHandler(new ValueChangeHandler<String>() {
      public void onValueChange(ValueChangeEvent<String> event) {
        fail("Handler should not have been added.");
      }
    });
    assertNull(reg);

    History.newItem("test");
    assertEquals("", History.getToken());
    History.newItem("test", true);
    assertEquals("", History.getToken());
  }
}
