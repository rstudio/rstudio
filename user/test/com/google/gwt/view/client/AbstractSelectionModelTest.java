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

/**
 * Tests for {@link AbstractSelectionModel}.
 */
public class AbstractSelectionModelTest extends GWTTestCase {

  /**
   * A mock {@link SelectionChangeEvent.Handler} used for testing.
   */
  static class MockSelectionChangeHandler
      implements SelectionChangeEvent.Handler {

    private boolean eventFired;

    /**
     * Assert that a {@link SelectionChangeEvent} was fired and clear the
     * boolean.
     * 
     * @param expected the expected value
     */
    public void assertEventFired(boolean expected) {
      assertEquals(expected, eventFired);
      eventFired = false;
    }

    public void onSelectionChange(SelectionChangeEvent event) {
      assertFalse(eventFired);
      eventFired = true;
    }
  }

  /**
   * A mock {@link SelectionModel} used for testing.
   *
   * @param <T> the data type
   */
  private static class MockSelectionModel<T> extends AbstractSelectionModel<T> {

    public MockSelectionModel(ProvidesKey<T> keyProvider) {
      super(keyProvider);
    }
    
    public boolean isSelected(T object) {
      return false;
    }

    public void setSelected(T object, boolean selected) {
      scheduleSelectionChangeEvent();
    }
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.view.View";
  }

  public void testFireSelectionChangeEvent() {
    AbstractSelectionModel<String> model = createSelectionModel(null);
    MockSelectionChangeHandler handler = new MockSelectionChangeHandler();
    model.addSelectionChangeHandler(handler);

    model.setSelected("test", true);
    model.fireSelectionChangeEvent();
    handler.assertEventFired(true);
  }

  /**
   * Test that resolving changes doesn't prevent an event from firing.
   */
  public void testResolveChanges() {
    AbstractSelectionModel<String> model = createSelectionModel(null);
    final MockSelectionChangeHandler handler = new MockSelectionChangeHandler();
    model.addSelectionChangeHandler(handler);

    model.setSelected("test1", true);
    handler.assertEventFired(false);
    model.isSelected("test1");

    new Timer() {
      @Override
      public void run() {
        handler.assertEventFired(true);
        finishTest();
      }
    }.schedule(1000);
  }

  public void testScheduleSelectionChangeEvent() {
    AbstractSelectionModel<String> model = createSelectionModel(null);
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
    AbstractSelectionModel<String> model = createSelectionModel(null);

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
    model = createSelectionModel(keyProvider);
    assertEquals(keyProvider, model.getKeyProvider());
    assertEquals("TEST", model.getKey("test"));
    assertEquals(null, model.getKey(null));
  }

  protected AbstractSelectionModel<String> createSelectionModel(ProvidesKey<String> keyProvider) {
    return new MockSelectionModel<String>(keyProvider);
  }
}
