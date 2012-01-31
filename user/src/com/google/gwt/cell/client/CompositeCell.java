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

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * A {@link Cell} that is composed of other {@link Cell}s.
 * </p>
 * 
 * <p>
 * When this cell is rendered, it will render each component {@link Cell} inside
 * a span. If the component {@link Cell} uses block level elements (such as a
 * Div), the component cells will stack vertically.
 * </p>
 * 
 * @param <C> the type that this Cell represents
 */
public class CompositeCell<C> extends AbstractCell<C> {

  /**
   * The events consumed by this cell.
   */
  private Set<String> consumedEvents;

  /**
   * Indicates whether or not this cell depends on selection.
   */
  private boolean dependsOnSelection;

  /**
   * Indicates whether or not this cell handles selection.
   */
  private boolean handlesSelection;

  /**
   * The cells that compose this {@link Cell}.
   * 
   * NOTE: Do not add add/insert/remove hasCells methods to the API. This cell
   * assumes that the index of the cellParent corresponds to the index in the
   * hasCells array.
   */
  private final List<HasCell<C, ?>> hasCells;

  /**
   * Construct a new {@link CompositeCell}.
   * 
   * @param hasCells the cells that makeup the composite
   */
  public CompositeCell(List<HasCell<C, ?>> hasCells) {
    // Create a new array so cells cannot be added or removed.
    this.hasCells = new ArrayList<HasCell<C, ?>>(hasCells);

    // Get the consumed events and depends on selection.
    Set<String> theConsumedEvents = null;
    for (HasCell<C, ?> hasCell : hasCells) {
      Cell<?> cell = hasCell.getCell();
      Set<String> events = cell.getConsumedEvents();
      if (events != null) {
        if (theConsumedEvents == null) {
          theConsumedEvents = new HashSet<String>();
        }
        theConsumedEvents.addAll(events);
      }
      if (cell.dependsOnSelection()) {
        dependsOnSelection = true;
      }
      if (cell.handlesSelection()) {
        handlesSelection = true;
      }
    }
    if (theConsumedEvents != null) {
      this.consumedEvents = Collections.unmodifiableSet(theConsumedEvents);
    }
  }

  @Override
  public boolean dependsOnSelection() {
    return dependsOnSelection;
  }

  @Override
  public Set<String> getConsumedEvents() {
    return consumedEvents;
  }

  @Override
  public boolean handlesSelection() {
    return handlesSelection;
  }

  @Override
  public boolean isEditing(Context context, Element parent, C value) {
    Element curChild = getContainerElement(parent).getFirstChildElement();
    for (HasCell<C, ?> hasCell : hasCells) {
      if (isEditingImpl(context, curChild, value, hasCell)) {
        return true;
      }
      curChild = curChild.getNextSiblingElement();
    }
    return false;
  }

  @Override
  public void onBrowserEvent(Context context, Element parent, C value,
      NativeEvent event, ValueUpdater<C> valueUpdater) {
    int index = 0;
    EventTarget eventTarget = event.getEventTarget();
    if (Element.is(eventTarget)) {
      Element target = eventTarget.cast();
      Element container = getContainerElement(parent);
      Element wrapper = container.getFirstChildElement();
      while (wrapper != null) {
        if (wrapper.isOrHasChild(target)) {
          onBrowserEventImpl(context, wrapper, value, event, valueUpdater,
              hasCells.get(index));
        }

        index++;
        wrapper = wrapper.getNextSiblingElement();
      }
    }
  }

  @Override
  public void render(Context context, C value, SafeHtmlBuilder sb) {
    for (HasCell<C, ?> hasCell : hasCells) {
      render(context, value, sb, hasCell);
    }
  }

  @Override
  public boolean resetFocus(Context context, Element parent, C value) {
    Element curChild = getContainerElement(parent).getFirstChildElement();
    for (HasCell<C, ?> hasCell : hasCells) {
      // The first child that takes focus wins. Only one child should ever be in
      // edit mode, so this is safe.
      if (resetFocusImpl(context, curChild, value, hasCell)) {
        return true;
      }
      curChild = curChild.getNextSiblingElement();
    }
    return false;
  }

  @Override
  public void setValue(Context context, Element parent, C object) {
    Element curChild = getContainerElement(parent).getFirstChildElement();
    for (HasCell<C, ?> hasCell : hasCells) {
      setValueImpl(context, curChild, object, hasCell);
      curChild = curChild.getNextSiblingElement();
    }
  }

  /**
   * Get the element that acts as the container for all children. If children
   * are added directly to the parent, the parent is the container. If children
   * are added in a table row, the row is the parent.
   * 
   * @param parent the parent element of the cell
   * @return the container element
   */
  protected Element getContainerElement(Element parent) {
    return parent;
  }

  /**
   * Render the composite cell as HTML into a {@link SafeHtmlBuilder}, suitable
   * for passing to {@link Element#setInnerHTML} on a container element.
   * 
   * <p>
   * Note: If your cell contains natively focusable elements, such as buttons or
   * input elements, be sure to set the tabIndex to -1 so that they do not steal
   * focus away from the containing widget.
   * </p>
   * 
   * @param context the {@link com.google.gwt.cell.client.Cell.Context Context} of the cell
   * @param value the cell value to be rendered
   * @param sb the {@link SafeHtmlBuilder} to be written to
   * @param hasCell a {@link HasCell} instance containing the cells to be
   *          rendered within this cell
   */
  protected <X> void render(Context context, C value,
      SafeHtmlBuilder sb, HasCell<C, X> hasCell) {
    Cell<X> cell = hasCell.getCell();
    sb.appendHtmlConstant("<span>");
    cell.render(context, hasCell.getValue(value), sb);
    sb.appendHtmlConstant("</span>");
  }

  private <X> boolean isEditingImpl(Context context, Element cellParent, C object,
      HasCell<C, X> hasCell) {
    return hasCell.getCell().isEditing(context, cellParent, hasCell.getValue(object));
  }  

  private <X> void onBrowserEventImpl(final Context context, Element parent,
      final C object, NativeEvent event, final ValueUpdater<C> valueUpdater,
      final HasCell<C, X> hasCell) {
    Cell<X> cell = hasCell.getCell();
    String eventType = event.getType();
    Set<String> cellConsumedEvents = cell.getConsumedEvents();
    if (cellConsumedEvents == null || !cellConsumedEvents.contains(eventType)) {
      // If this sub-cell doesn't consume this event.
      return;
    }
    ValueUpdater<X> tempUpdater = null;
    final FieldUpdater<C, X> fieldUpdater = hasCell.getFieldUpdater();
    if (fieldUpdater != null) {
      tempUpdater = new ValueUpdater<X>() {
        @Override
        public void update(X value) {
          fieldUpdater.update(context.getIndex(), object, value);
          if (valueUpdater != null) {
            valueUpdater.update(object);
          }
        }
      };
    }
    cell.onBrowserEvent(context, parent, hasCell.getValue(object), event,
        tempUpdater);
  }

  private <X> boolean resetFocusImpl(Context context, Element cellParent,
      C value, HasCell<C, X> hasCell) {
    X cellValue = hasCell.getValue(value);
    return hasCell.getCell().resetFocus(context, cellParent, cellValue);
  }

  private <X> void setValueImpl(Context context, Element cellParent, C object,
      HasCell<C, X> hasCell) {
    hasCell.getCell().setValue(context, cellParent, hasCell.getValue(object));
  }  
}
