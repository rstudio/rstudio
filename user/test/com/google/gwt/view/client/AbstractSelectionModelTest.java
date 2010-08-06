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
package com.google.gwt.view.client;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.Timer;
import com.google.gwt.view.client.SelectionModel.AbstractSelectionModel;
import com.google.gwt.view.client.SelectionModel.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionModel.SelectionChangeHandler;

/**
 * Tests for {@link AbstractSelectionModel}.
 */
public class AbstractSelectionModelTest extends GWTTestCase {

  /**
   * A mock {@link SelectionChangeHandler} used for testing.
   */
  private static class MockSelectionChangeHandler implements
      SelectionChangeHandler {

    private boolean eventFired;

    public void assertEventFired(boolean expected) {
      assertEquals(expected, eventFired);
    }

    public void onSelectionChange(SelectionChangeEvent event) {
      eventFired = true;
    }
  }

  /**
   * A mock {@link SelectionModel} used for testing.
   * 
   * @param <T> the data type
   */
  private static class MockSelectionModel<T> extends AbstractSelectionModel<T> {
    public boolean isSelected(T object) {
      return false;
    }

    public void setSelected(T object, boolean selected) {
    }
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.view.View";
  }

  public void testFireSelectionChangeEvent() {
    AbstractSelectionModel<String> model = createSelectionModel();
    MockSelectionChangeHandler handler = new MockSelectionChangeHandler();
    model.addSelectionChangeHandler(handler);

    model.setSelected("test", true);
    model.fireSelectionChangeEvent();
    handler.assertEventFired(true);
  }

  public void testScheduleSelectionChangeEvent() {
    AbstractSelectionModel<String> model = createSelectionModel();
    final MockSelectionChangeHandler handler = new MockSelectionChangeHandler() {
      @Override
      public void onSelectionChange(SelectionChangeEvent event) {
        // We should only see one event fired.
        assertEventFired(false);
        super.onSelectionChange(event);
      }
    };
    model.addSelectionChangeHandler(handler);

    // Schedule the event multiple times.
    delayTestFinish(2000);
    model.setSelected("test1", true);
    model.scheduleSelectionChangeEvent();
    model.setSelected("test2", true);
    model.scheduleSelectionChangeEvent();
    model.setSelected("test3", true);
    model.scheduleSelectionChangeEvent();
    model.setSelected("test4", true);
    model.scheduleSelectionChangeEvent();
    model.setSelected("test5", true);
    model.scheduleSelectionChangeEvent();
    model.setSelected("test6", true);
    model.scheduleSelectionChangeEvent();
    handler.assertEventFired(false);

    new Timer() {
      @Override
      public void run() {
        handler.assertEventFired(true);
        finishTest();
      }
    }.schedule(1000);
  }

  public void testSetKeyProvider() {
    AbstractSelectionModel<String> model = createSelectionModel();

    // By default, use the object as a key.
    assertNull(model.getKeyProvider());
    assertEquals("test", model.getKey("test"));
    assertEquals(null, model.getKey(null));

    // Defer to the key provider if one is set.
    ProvidesKey<String> keyProvider = new ProvidesKey<String>() {
      public Object getKey(String item) {
        return item == null ? item : item.toUpperCase();
      }
    };
    model.setKeyProvider(keyProvider);
    assertEquals(keyProvider, model.getKeyProvider());
    assertEquals("TEST", model.getKey("test"));
    assertEquals(null, model.getKey(null));
  }

  protected AbstractSelectionModel<String> createSelectionModel() {
    return new MockSelectionModel<String>();
  }
}
