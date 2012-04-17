/*
 * Copyright 2012 Google Inc.
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

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for {@link OrderedMultiSelectionModel}.
 */
public class OrderedMultiSelectionModelTest extends MultiSelectionModelTest {

  /**
   * Test if elements are returned in the same order they were added.
   */
  public void testGetSelectedList() {
    OrderedMultiSelectionModel<String> model = createSelectionModel(null);
    List<String> selected = new ArrayList<String>();
    assertEquals(selected, model.getSelectedList());

    model.setSelected("test0", true);
    selected.add("test0");
    assertEquals(selected, model.getSelectedList());

    model.setSelected("test1", true);
    selected.add("test1");
    assertEquals(selected, model.getSelectedList());

    model.setSelected("test2", true);
    selected.add("test2");
    assertEquals(selected, model.getSelectedList());

    model.setSelected("test0", false);
    selected.remove("test0");
    assertEquals(selected, model.getSelectedList());

    model.setSelected("test0", true);
    selected.add("test0");
    assertEquals(selected, model.getSelectedList());

    // adding a duplicate shouldn't change the order in the list
    model.setSelected("test1", true);
    // the list is now still [test1 test2 test0]
    assertEquals(selected, model.getSelectedList());
  }

  @Override
  protected OrderedMultiSelectionModel<String> createSelectionModel(ProvidesKey<String> keyProvider) {
    return new OrderedMultiSelectionModel<String>(keyProvider);
  }
}
