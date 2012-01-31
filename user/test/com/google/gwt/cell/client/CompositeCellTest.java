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

import com.google.gwt.cell.client.Cell.Context;
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
   * Test dependsOnSelection and handlesSelection when one inner cell returns
   * true for each of these.
   */
  public void testDependsOnSelectionTrue() {
    // Add one cell that consumes events.
    List<HasCell<String, ?>> cells = createHasCells(3);
    final MockCell<String> mock = new MockCell<String>(true, null);
    addCell(mock, cells);
    CompositeCell<String> cell = new CompositeCell<String>(cells);
    assertNull(cell.getConsumedEvents());
    assertTrue(cell.dependsOnSelection());
  }

  /**
   * Test getConsumedEvents when one inner cell consumes events.
   */
  public void testGetConsumedEventsTrue() {
    // Add one cell that consumes events.
    List<HasCell<String, ?>> cells = createHasCells(3);
    final MockCell<String> mock = new MockCell<String>(false, null, "click");
    addCell(mock, cells);
    CompositeCell<String> cell = new CompositeCell<String>(cells);
    assertEquals(1, cell.getConsumedEvents().size());
    assertTrue(cell.getConsumedEvents().contains("click"));
    assertFalse(cell.dependsOnSelection());
  }

  public void testIsEditingFalse() {
    List<HasCell<String, ?>> cells = createHasCells(3);
    CompositeCell<String> cell = new CompositeCell<String>(cells);
    Element parent = Document.get().createDivElement();
    parent.setInnerHTML(getExpectedInnerHtml());
    assertFalse(cell.isEditing(new Context(0, 0, null), parent, "test"));
  }

  public void testIsEditingTrue() {
    List<HasCell<String, ?>> cells = createHasCells(3);
    // Add a cell that is being edited.
    final MockCell<String> mock = new MockCell<String>(false, null) {
      @Override
      public boolean isEditing(Context context, Element parent, String value) {
        return true;
      }
    };
    addCell(mock, cells);
    CompositeCell<String> cell = new CompositeCell<String>(cells);
    Element parent = Document.get().createDivElement();
    parent.setInnerHTML(getExpectedInnerHtml());
    assertTrue(cell.isEditing(new Context(0, 0, null), parent, "test"));
  }

  /**
   * Fire an event to no cell in particular.
   */
  public void testOnBrowserEventNoCell() {
    NativeEvent event = Document.get().createClickEvent(0, 0, 0, 0, 0, false,
        false, false, false);
    testOnBrowserEvent(getExpectedInnerHtml(), event, "test", null);
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
    List<HasCell<String, ?>> cells = createHasCells(2);
    MockCell<String> innerCell = new MockCell<String>(false, "fromCell2", "click");
    addCell(innerCell, cells);
    final CompositeCell<String> cell = new CompositeCell<String>(cells);

    // Add an event listener.
    EventListener listener = new EventListener() {
      public void onBrowserEvent(Event event) {
        Context context = new Context(3, 4, "key");
        cell.onBrowserEvent(context, parent, "test-x", event, null);
      }
    };
    DOM.sinkEvents(parent, Event.ONCLICK);
    DOM.setEventListener(parent, listener);

    // Fire the event on one of the inner cells.
    NativeEvent event = Document.get().createClickEvent(0, 0, 0, 0, 0, false,
        false, false, false);
    Element.as(parent.getChild(2)).dispatchEvent(event);
    innerCell.assertLastEventValue("test-x");
    innerCell.assertLastParentElement(Element.as(parent.getChild(2)));
    Context innerContext = innerCell.getLastContext();
    assertEquals("key", innerContext.getKey());
    assertEquals(3, innerContext.getIndex());
    assertEquals(4, innerContext.getColumn());

    // Fire the event to another cell that doesn't consume this event. Shouldn't respond
    // to the event
    MockCell<String> innerCell2 = (MockCell<String>) cells.get(1).getCell();
    Element.as(parent.getChild(1)).dispatchEvent(event);
    innerCell2.assertLastEventValue(null);

    // Remove the element and event listener.
    DOM.setEventListener(parent, null);
    Document.get().getBody().removeChild(parent);
  }

  public void testSetValue() {
    Cell<String> cell = createCell();
    Element parent = Document.get().createDivElement();
    parent.setInnerHTML(getExpectedInnerHtml());
    Context context = new Context(0, 0, null);
    cell.setValue(context, parent, "test");

    assertEquals(3, parent.getChildCount());
    assertEquals("test-0", Element.as(parent.getChild(0)).getInnerHTML());
    assertEquals("test-1", Element.as(parent.getChild(1)).getInnerHTML());
    assertEquals("test-2", Element.as(parent.getChild(2)).getInnerHTML());
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
  protected String[] getConsumedEvents() {
    return null;
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
   * Add a cell to a {@link HasCell} list.
   */
  private void addCell(final Cell<String> cell, List<HasCell<String, ?>> cells) {
    cells.add(new HasCell<String, String>() {
      public Cell<String> getCell() {
        return cell;
      }

      public FieldUpdater<String, String> getFieldUpdater() {
        return null;
      }

      public String getValue(String object) {
        return object;
      }
    });
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
      final MockCell<String> inner = new MockCell<String>(false, "fromCell" + i);
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
