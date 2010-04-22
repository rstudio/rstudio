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
package com.google.gwt.bikeshed.cells.client;

import com.google.gwt.bikeshed.list.client.HasCell;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * A {@link Cell} that is composed of other {@link Cell}s.
 * </p>
 * <p>
 * When this cell is rendered, it will render each component {@link Cell} inside
 * a span. If the component {@link Cell} uses block level elements (such as a
 * Div), the component cells will stack vertically.
 * </p>
 * 
 * @param <C> the type that this Cell represents
 * @param <V> the type of view data that this cell consumes
 */
public class CompositeCell<C, V> extends Cell<C, V> {

  /**
   * The cells that compose this {@link Cell}.
   */
  private List<HasCell<C, ?, V>> hasCells = new ArrayList<HasCell<C, ?, V>>();

  /**
   * Add a {@link HasCell} to the composite.
   * 
   * @param hasCell the {@link HasCell} to add
   */
  public void addHasCell(HasCell<C, ?, V> hasCell) {
    hasCells.add(hasCell);
  }

  @Override
  public boolean consumesEvents() {
    // TODO(jlabanca): Should we cache this value? Can it change?
    for (HasCell<C, ?, V> hasCell : hasCells) {
      if (hasCell.getCell().consumesEvents()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean dependsOnSelection() {
    // TODO(jlabanca): Should we cache this value? Can it change?
    for (HasCell<C, ?, V> hasCell : hasCells) {
      if (hasCell.getCell().dependsOnSelection()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Insert a {@link HasCell} into the composite.
   * 
   * @param index the index to insert into
   * @param hasCell the {@link HasCell} to insert
   */
  public void insertHasCell(int index, HasCell<C, ?, V> hasCell) {
    hasCells.add(index, hasCell);
  }

  @Override
  public V onBrowserEvent(Element parent, C value, V viewData,
      NativeEvent event, ValueUpdater<C, V> valueUpdater) {
    int index = 0;
    Element target = event.getEventTarget().cast();
    Element wrapper = parent.getFirstChildElement();
    while (wrapper != null) {
      if (wrapper.isOrHasChild(target)) {
        return onBrowserEventImpl(wrapper, value, viewData, event,
            valueUpdater, hasCells.get(index));
      }

      index++;
      wrapper = wrapper.getNextSiblingElement();
    }
    return viewData;
  }

  /**
   * Remove a {@link HasCell} from the composite.
   * 
   * @param hasCell the {@link HasCell} to remove
   */
  public void removeHasCell(HasCell<C, ?, V> hasCell) {
    hasCells.remove(hasCell);
  }

  @Override
  public void render(C value, V viewData, StringBuilder sb) {
    for (HasCell<C, ?, V> hasCell : hasCells) {
      render(value, viewData, sb, hasCell);
    }
  }

  @Override
  public void setValue(Element parent, C object, V viewData) {
    for (HasCell<C, ?, V> hasCell : hasCells) {
      setValueImpl(parent, object, viewData, hasCell);
    }
  }

  protected <X> void render(C value, V viewData, StringBuilder sb,
      HasCell<C, X, V> hasCell) {
    Cell<X, V> cell = hasCell.getCell();
    sb.append("<span>");
    cell.render(hasCell.getValue(value), viewData, sb);
    sb.append("</span>");
  }

  private <X> V onBrowserEventImpl(Element parent, final C object, V viewData,
      NativeEvent event, final ValueUpdater<C, V> valueUpdater,
      final HasCell<C, X, V> hasCell) {
    ValueUpdater<X, V> tempUpdater = null;
    final FieldUpdater<C, X, V> fieldUpdater = hasCell.getFieldUpdater();
    if (fieldUpdater != null) {
      tempUpdater = new ValueUpdater<X, V>() {
        public void update(X value, V viewData) {
          fieldUpdater.update(-1, object, value, viewData);
          if (valueUpdater != null) {
            valueUpdater.update(object, viewData);
          }
        }
      };
    }
    Cell<X, V> cell = hasCell.getCell();
    return cell.onBrowserEvent(parent, hasCell.getValue(object), viewData,
        event, tempUpdater);
  }

  private <X> void setValueImpl(Element parent, C object, V viewData,
      HasCell<C, X, V> hasCell) {
    hasCell.getCell().setValue(parent, hasCell.getValue(object), viewData);
  }
}
