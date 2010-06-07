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
  public Object onBrowserEvent(Element parent, C value, Object viewData,
      NativeEvent event, ValueUpdater<C> valueUpdater) {
    int index = 0;
    EventTarget eventTarget = event.getEventTarget();
    if (Element.is(eventTarget)) {
      Element target = eventTarget.cast();
      Element wrapper = parent.getFirstChildElement();
      while (wrapper != null) {
        if (wrapper.isOrHasChild(target)) {
          return onBrowserEventImpl(wrapper, value, viewData, event,
              valueUpdater, hasCells.get(index));
        }

        index++;
        wrapper = wrapper.getNextSiblingElement();
      }
    }
    return viewData;
  }

  @Override
  public void render(C value, Object viewData, StringBuilder sb) {
    for (HasCell<C, ?> hasCell : hasCells) {
      render(value, viewData, sb, hasCell);
    }
  }

  @Override
  public void setValue(Element parent, C object, Object viewData) {
    Element curChild = parent.getFirstChildElement();
    for (HasCell<C, ?> hasCell : hasCells) {
      setValueImpl(curChild, object, viewData, hasCell);
      curChild = curChild.getNextSiblingElement();
    }
  }

  protected <X> void render(C value, Object viewData, StringBuilder sb,
      HasCell<C, X> hasCell) {
    Cell<X> cell = hasCell.getCell();
    sb.append("<span>");
    cell.render(hasCell.getValue(value), viewData, sb);
    sb.append("</span>");
  }

  private <X> Object onBrowserEventImpl(Element parent, final C object,
      Object viewData, NativeEvent event, final ValueUpdater<C> valueUpdater,
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
    return cell.onBrowserEvent(parent, hasCell.getValue(object), viewData,
        event, tempUpdater);
  }

  private <X> void setValueImpl(Element cellParent, C object, Object viewData,
      HasCell<C, X> hasCell) {
    hasCell.getCell().setValue(cellParent, hasCell.getValue(object), viewData);
  }
}
