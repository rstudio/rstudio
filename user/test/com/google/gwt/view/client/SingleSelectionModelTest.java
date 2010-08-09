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

/**
 * Tests for {@link SingleSelectionModel}.
 */
public class SingleSelectionModelTest extends AbstractSelectionModelTest {

  public void testGetSelectedObject() {
    SingleSelectionModel<String> model = createSelectionModel();
    assertNull(model.getSelectedObject());

    model.setSelected("test", true);
    assertEquals("test", model.getSelectedObject());

    model.setSelected("test", false);
    assertNull(model.getSelectedObject());
  }

  public void testSelectedChangeEvent() {
    SingleSelectionModel<String> model = createSelectionModel();
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
    SingleSelectionModel<String> model = createSelectionModel();
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
    SingleSelectionModel<String> model = createSelectionModel();
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
    SingleSelectionModel<String> model = createSelectionModel();
    assertFalse(model.isSelected("test0"));

    model.setSelected("test0", true);
    assertTrue(model.isSelected("test0"));

    model.setSelected("test1", true);
    assertTrue(model.isSelected("test1"));
    assertFalse(model.isSelected("test0"));

    model.setSelected("test1", false);
    assertFalse(model.isSelected("test1"));
    assertFalse(model.isSelected("test0"));
  }

  public void testSetSelectedWithKeyProvider() {
    SingleSelectionModel<String> model = createSelectionModel();
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
    assertFalse(model.isSelected("test0"));

    model.setSelected("test1", false);
    assertFalse(model.isSelected("test1"));
    assertFalse(model.isSelected("test0"));
  }

  @Override
  protected SingleSelectionModel<String> createSelectionModel() {
    return new SingleSelectionModel<String>();
  }
}
