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

import java.util.ArrayList;
import java.util.List;

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
 * <p>
 * Note: This class is new and its interface subject to change.
 * </p>
 *
 * @param <C> the type that this Cell represents
 */
public class CompositeCell<C> extends AbstractCell<C> {

  /**
   * The cells that compose this {@link Cell}.
   *
   *  NOTE: Do not add add/insert/remove hasCells methods to the API. This cell
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
  }

  @Override
  public boolean consumesEvents() {
    // TODO(jlabanca): Should we cache this value? Can it change?
    for (HasCell<C, ?> hasCell : hasCells) {
      if (hasCell.getCell().consumesEvents()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean dependsOnSelection() {
    // TODO(jlabanca): Should we cache this value? Can it change?
    for (HasCell<C, ?> hasCell : hasCells) {
      if (hasCell.getCell().dependsOnSelection()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void onBrowserEvent(Element parent, C value, Object key,
      NativeEvent event, ValueUpdater<C> valueUpdater) {
    int index = 0;
    EventTarget eventTarget = event.getEventTarget();
    if (Element.is(eventTarget)) {
      Element target = eventTarget.cast();
      Element container = getContainerElement(parent);
      Element wrapper = container.getFirstChildElement();
      while (wrapper != null) {
        if (wrapper.isOrHasChild(target)) {
          onBrowserEventImpl(
              wrapper, value, key, event, valueUpdater, hasCells.get(index));
        }

        index++;
        wrapper = wrapper.getNextSiblingElement();
      }
    }
  }

  @Override
  public void render(C value, Object key, StringBuilder sb) {
    for (HasCell<C, ?> hasCell : hasCells) {
      render(value, key, sb, hasCell);
    }
  }

  @Override
  public void setValue(Element parent, C object, Object key) {
    Element curChild = parent.getFirstChildElement();
    for (HasCell<C, ?> hasCell : hasCells) {
      setValueImpl(curChild, object, key, hasCell);
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

  protected <X> void render(
      C value, Object key, StringBuilder sb, HasCell<C, X> hasCell) {
    Cell<X> cell = hasCell.getCell();
    sb.append("<span>");
    cell.render(hasCell.getValue(value), key, sb);
    sb.append("</span>");
  }

  private <X> void onBrowserEventImpl(Element parent, final C object,
      Object key, NativeEvent event, final ValueUpdater<C> valueUpdater,
      final HasCell<C, X> hasCell) {
    ValueUpdater<X> tempUpdater = null;
    final FieldUpdater<C, X> fieldUpdater = hasCell.getFieldUpdater();
    if (fieldUpdater != null) {
      tempUpdater = new ValueUpdater<X>() {
        public void update(X value) {
          fieldUpdater.update(-1, object, value);
          if (valueUpdater != null) {
            valueUpdater.update(object);
          }
        }
      };
    }
    Cell<X> cell = hasCell.getCell();
    cell.onBrowserEvent(
        parent, hasCell.getValue(object), key, event, tempUpdater);
  }

  private <X> void setValueImpl(
      Element cellParent, C object, Object key, HasCell<C, X> hasCell) {
    hasCell.getCell().setValue(cellParent, hasCell.getValue(object), key);
  }
}
