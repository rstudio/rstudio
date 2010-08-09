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

  public void testGetSelectedSet() {
    MultiSelectionModel<String> model = createSelectionModel();
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
    MultiSelectionModel<String> model = createSelectionModel();
    SelectionChangeEvent.Handler handler = new SelectionChangeEvent.Handler() {
      public void onSelectionChange(SelectionChangeEvent event) {
        finishTest();
      }
    };
    model.addSelectionChangeHandler(handler);

    delayTestFinish(2000);
    model.setSelected("test", true);
  }
  
  public void testNoDuplicateChangeEvent() {
    MultiSelectionModel<String> model = createSelectionModel();
    SelectionChangeEvent.Handler handler = new SelectionChangeEvent.Handler() {
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
    MultiSelectionModel<String> model = createSelectionModel();
    SelectionChangeEvent.Handler handler = new SelectionChangeEvent.Handler() {
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

  public void testSetSelected() {
    MultiSelectionModel<String> model = createSelectionModel();
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

  public void testSetSelectedWithKeyProvider() {
    MultiSelectionModel<String> model = createSelectionModel();
    ProvidesKey<String> keyProvider = new ProvidesKey<String>() {
      public Object getKey(String item) {
        return item.toUpperCase();
      }
    };
    model.setKeyProvider(keyProvider);
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
  protected MultiSelectionModel<String> createSelectionModel() {
    return new MultiSelectionModel<String>();
  }
}
