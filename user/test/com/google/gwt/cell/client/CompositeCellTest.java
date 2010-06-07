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

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for {@link CompositeCell}.
 */
public class CompositeCellTest extends CellTestBase<String> {

  /**
   * Test consumesEvent when one inner cell consumes events.
   */
  public void testConsumesEventTrue() {
    // Add one cell that consumes events.
    List<HasCell<String, ?>> cells = createHasCells(3);
    final MockCell<String> mock = new MockCell<String>(true, false, null);
    cells.add(new HasCell<String, String>() {
      public Cell<String> getCell() {
        return mock;
      }

      public FieldUpdater<String, String> getFieldUpdater() {
        return null;
      }

      public String getValue(String object) {
        return object;
      }
    });
    CompositeCell<String> cell = new CompositeCell<String>(cells);
    assertTrue(cell.consumesEvents());
    assertFalse(cell.dependsOnSelection());
  }

  /**
   * Test dependsOnSelection when one inner cell dependsOnSelection.
   */
  public void testDependsOnSelectionTrue() {
    // Add one cell that consumes events.
    List<HasCell<String, ?>> cells = createHasCells(3);
    final MockCell<String> mock = new MockCell<String>(false, true, null);
    cells.add(new HasCell<String, String>() {
      public Cell<String> getCell() {
        return mock;
      }

      public FieldUpdater<String, String> getFieldUpdater() {
        return null;
      }

      public String getValue(String object) {
        return object;
      }
    });
    CompositeCell<String> cell = new CompositeCell<String>(cells);
    assertFalse(cell.consumesEvents());
    assertTrue(cell.dependsOnSelection());
  }

  /**
   * Fire an event to no cell in particular.
   */
  public void testOnBrowserEventNoCell() {
    NativeEvent event = Document.get().createChangeEvent();
    testOnBrowserEvent(getExpectedInnerHtml(), event, null, "test", null);
  }

  /**
   * Fire an event to a specific cell.
   */
  @SuppressWarnings("unchecked")
  public void testOnBrowserEventCell() {
    // Setup the parent element.
    final com.google.gwt.user.client.Element parent = Document.get().createDivElement().cast();
    parent.setInnerHTML(getExpectedInnerHtml());
    Document.get().getBody().appendChild(parent);

    // Create the composite cell and updater.
    List<HasCell<String, ?>> cells = createHasCells(3);
    final CompositeCell<String> cell = new CompositeCell<String>(cells);
    MockCell<String> innerCell = (MockCell<String>) cells.get(1).getCell();

    // Add an event listener.
    EventListener listener = new EventListener() {
      public void onBrowserEvent(Event event) {
        cell.onBrowserEvent(parent, "test", null, event, null);
      }
    };
    DOM.sinkEvents(parent, Event.ONCLICK);
    DOM.setEventListener(parent, listener);

    // Fire the event on one of the inner cells.
    NativeEvent event = Document.get().createClickEvent(0, 0, 0, 0, 0, false,
        false, false, false);
    Element.as(parent.getChild(1)).dispatchEvent(event);
    innerCell.assertLastEventValue("test-1");

    // Remove the element and event listener.
    DOM.setEventListener(parent, null);
    Document.get().getBody().removeChild(parent);
  }

  public void testSetValue() {
    Cell<String> cell = createCell();
    Element parent = Document.get().createDivElement();
    parent.setInnerHTML(getExpectedInnerHtml());
    cell.setValue(parent, "test", null);

    assertEquals(3, parent.getChildCount());
    assertEquals("test-0", Element.as(parent.getChild(0)).getInnerHTML());
    assertEquals("test-1", Element.as(parent.getChild(1)).getInnerHTML());
    assertEquals("test-2", Element.as(parent.getChild(2)).getInnerHTML());
  }

  @Override
  protected boolean consumesEvents() {
    return false;
  }

  @Override
  protected CompositeCell<String> createCell() {
    return new CompositeCell<String>(createHasCells(3));
  }

  @Override
  protected String createCellValue() {
    return "helloworld";
  }

  @Override
  protected boolean dependsOnSelection() {
    return false;
  }

  @Override
  protected String getExpectedInnerHtml() {
    return "<span>helloworld-0</span><span>helloworld-1</span><span>helloworld-2</span>";
  }

  @Override
  protected String getExpectedInnerHtmlNull() {
    return "<span></span><span></span><span></span>";
  }

  /**
   * Create an array of {@link HasCell}.
   * 
   * @param count the number of cells to create
   * @return the list of cells
   */
  private List<HasCell<String, ?>> createHasCells(int count) {
    List<HasCell<String, ?>> cells = new ArrayList<HasCell<String, ?>>();
    for (int i = 0; i < count; i++) {
      final int index = i;
      final MockCell<String> inner = new MockCell<String>(false, false,
          "fromCell" + i);
      cells.add(new HasCell<String, String>() {
        public Cell<String> getCell() {
          return inner;
        }

        public FieldUpdater<String, String> getFieldUpdater() {
          return null;
        }

        public String getValue(String object) {
          return object == null ? null : object + "-" + index;
        }
      });
    }
    return cells;
  }
}
