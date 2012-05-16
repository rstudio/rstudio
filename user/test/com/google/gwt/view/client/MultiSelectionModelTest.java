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

import java.util.HashSet;
import java.util.Set;

/**
 * Tests for {@link MultiSelectionModel}.
 */
public class MultiSelectionModelTest extends AbstractSelectionModelTest {

  public void testClear() {
    MultiSelectionModel<String> model = createSelectionModel(null);
    MockSelectionChangeHandler handler = new MockSelectionChangeHandler();
    model.addSelectionChangeHandler(handler);

    // Select a few values.
    model.setSelected("test0", true);
    model.setSelected("test1", true);
    model.setSelected("test2", true);
    assertTrue(model.isSelected("test0"));
    assertTrue(model.isSelected("test1"));
    assertTrue(model.isSelected("test2"));
    handler.assertEventFired(true);

    // Clear selection and verify that an event is fired.
    model.clear();
    assertFalse(model.isSelected("test0"));
    assertFalse(model.isSelected("test1"));
    assertFalse(model.isSelected("test2"));
    handler.assertEventFired(true);
  }

  /**
   * Clearing an empty {@link MultiSelectionModel} should not fire an event,
   * even if there are pending changes.
   */
  public void testClearWhenEmpty() {
    MultiSelectionModel<String> model = createSelectionModel(null);
    MockSelectionChangeHandler handler = new MockSelectionChangeHandler();
    model.addSelectionChangeHandler(handler);

    // Add a pending change.
    model.setSelected("test", true);

    // Clear selection and verify that no event is fired.
    model.clear();
    assertFalse(model.isSelected("test"));
    handler.assertEventFired(false);
  }

  /**
   * Pending changes should apply after the list is cleared. An event should not
   * be fired if all selected values are reselected.
   */
  public void testClearAndReselect() {
    MultiSelectionModel<String> model = createSelectionModel(null);
    MockSelectionChangeHandler handler = new MockSelectionChangeHandler();
    model.addSelectionChangeHandler(handler);

    // Select a few values.
    model.setSelected("test0", true);
    model.setSelected("test1", true);
    model.setSelected("test2", true);
    assertTrue(model.isSelected("test0"));
    handler.assertEventFired(true);

    // Clear selection and reselect. Verify that no event is fired.
    model.clear();
    model.setSelected("test0", true);
    model.setSelected("test1", true);
    model.setSelected("test2", true);
    assertTrue(model.isSelected("test0"));
    handler.assertEventFired(false);
  }

  public void testGetSelectedSet() {
    MultiSelectionModel<String> model = createSelectionModel(null);
    Set<String> selected = new HashSet<String>();
    assertEquals(selected, model.getSelectedSet());

    model.setSelected("test0", true);
    selected.add("test0");
    assertEquals(selected, model.getSelectedSet());

    model.setSelected("test1", true);
    selected.add("test1");
    assertEquals(selected, model.getSelectedSet());

    model.setSelected("test0", false);
    selected.remove("test0");
    assertEquals(selected, model.getSelectedSet());
  }

  public void testSelectedChangeEvent() {
    MultiSelectionModel<String> model = createSelectionModel(null);
    SelectionChangeEvent.Handler handler = new SelectionChangeEvent.Handler() {
        @Override
      public void onSelectionChange(SelectionChangeEvent event) {
        finishTest();
      }
    };
    model.addSelectionChangeHandler(handler);

    delayTestFinish(2000);
    model.setSelected("test", true);
  }

  public void testNoDuplicateChangeEvent() {
    MultiSelectionModel<String> model = createSelectionModel(null);
    SelectionChangeEvent.Handler handler = new SelectionChangeEvent.Handler() {
        @Override
      public void onSelectionChange(SelectionChangeEvent event) {
        fail();
      }
    };

    model.setSelected("test", true);
    model.addSelectionChangeHandler(handler);
    model.setSelected("test", true); // Should not fire change event
    model.setSelected("test", true); // Should not fire change event
  }

  public void testNoDuplicateChangeEvent2() {
    MultiSelectionModel<String> model = createSelectionModel(null);
    SelectionChangeEvent.Handler handler = new SelectionChangeEvent.Handler() {
        @Override
      public void onSelectionChange(SelectionChangeEvent event) {
        fail();
      }
    };

    model.setSelected("test", true);
    model.setSelected("test", false);
    model.addSelectionChangeHandler(handler);
    model.setSelected("test", false); // Should not fire change event
    model.setSelected("test", false); // Should not fire change event
  }

  /**
   * Tests that reselecting the same key from a different item does not fire a
   * change event.
   */
  public void testNoDuplicateChangeEventWithKeyProvider() {
    ProvidesKey<String> keyProvider = new ProvidesKey<String>() {
        @Override
      public Object getKey(String item) {
        return item.toUpperCase();
      }
    };
    MultiSelectionModel<String> model = createSelectionModel(keyProvider);
    SelectionChangeEvent.Handler handler = new SelectionChangeEvent.Handler() {
        @Override
      public void onSelectionChange(SelectionChangeEvent event) {
        fail();
      }
    };

    model.setSelected("test1", true);
    assertTrue(model.isSelected("test1"));

    model.addSelectionChangeHandler(handler);
    // Selecting a different item with the same key should not be seen as a
    // selection change
    String replacement = "TEST1";
    model.setSelected(replacement, true);
    assertTrue(model.isSelected(replacement));
    assertEquals(1, model.getSelectedSet().size());
    assertSame(replacement, model.getSelectedSet().iterator().next());
  }

  public void testSetSelected() {
    MultiSelectionModel<String> model = createSelectionModel(null);
    assertFalse(model.isSelected("test0"));

    model.setSelected("test0", true);
    assertTrue(model.isSelected("test0"));

    model.setSelected("test1", true);
    assertTrue(model.isSelected("test1"));
    assertTrue(model.isSelected("test0"));

    model.setSelected("test1", false);
    assertFalse(model.isSelected("test1"));
    assertTrue(model.isSelected("test0"));
  }

  /**
   * Tests that items with the same key share the same selection state.
   */
  public void testSetSelectedSameKey() {
    ProvidesKey<String> keyProvider = new ProvidesKey<String>() {
        @Override
      public Object getKey(String item) {
        return item.toUpperCase();
      }
    };
    MultiSelectionModel<String> model = createSelectionModel(keyProvider);
    assertFalse(model.isSelected("test0"));

    model.setSelected("test0", true);
    assertTrue(model.isSelected("test0"));

    model.setSelected("Test0", false);
    assertFalse(model.isSelected("test0"));

    // Verify that the last change wins if the key is the same.
    model.setSelected("TEST0", true);
    model.setSelected("test0", false);
    assertFalse(model.isSelected("test0"));
  }

  public void testSetSelectedWithKeyProvider() {
    ProvidesKey<String> keyProvider = new ProvidesKey<String>() {
        @Override
      public Object getKey(String item) {
        return item.toUpperCase();
      }
    };
    MultiSelectionModel<String> model = createSelectionModel(keyProvider);
    assertFalse(model.isSelected("test0"));

    model.setSelected("test0", true);
    assertTrue(model.isSelected("test0"));
    assertTrue(model.isSelected("TEST0"));

    model.setSelected("test1", true);
    assertTrue(model.isSelected("test1"));
    assertTrue(model.isSelected("TEST1"));
    assertTrue(model.isSelected("test0"));
    assertTrue(model.isSelected("TEST0"));

    model.setSelected("test1", false);
    assertFalse(model.isSelected("test1"));
    assertFalse(model.isSelected("TEST1"));
    assertTrue(model.isSelected("test0"));
    assertTrue(model.isSelected("TEST0"));
  }

  @Override
  protected MultiSelectionModel<String> createSelectionModel(ProvidesKey<String> keyProvider) {
    return new MultiSelectionModel<String>(keyProvider);
  }
}
