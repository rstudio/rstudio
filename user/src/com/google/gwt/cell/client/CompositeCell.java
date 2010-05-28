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
   */
  private List<HasCell<C, ?>> hasCells = new ArrayList<HasCell<C, ?>>();

  /**
   * Add a {@link HasCell} to the composite.
   * 
   * @param hasCell the {@link HasCell} to add
   */
  public void addHasCell(HasCell<C, ?> hasCell) {
    hasCells.add(hasCell);
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

  /**
   * Insert a {@link HasCell} into the composite.
   * 
   * @param index the index to insert into
   * @param hasCell the {@link HasCell} to insert
   */
  public void insertHasCell(int index, HasCell<C, ?> hasCell) {
    hasCells.add(index, hasCell);
  }

  @Override
  public Object onBrowserEvent(Element parent, C value, Object viewData,
      NativeEvent event, ValueUpdater<C> valueUpdater) {
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
  public void removeHasCell(HasCell<C, ?> hasCell) {
    hasCells.remove(hasCell);
  }

  @Override
  public void render(C value, Object viewData, StringBuilder sb) {
    for (HasCell<C, ?> hasCell : hasCells) {
      render(value, viewData, sb, hasCell);
    }
  }

  @Override
  public void setValue(Element parent, C object, Object viewData) {
    for (HasCell<C, ?> hasCell : hasCells) {
      setValueImpl(parent, object, viewData, hasCell);
    }
  }

  protected <X> void render(C value, Object viewData, StringBuilder sb,
      HasCell<C, X> hasCell) {
    Cell<X> cell = hasCell.getCell();
    sb.append("<span>");
    cell.render(hasCell.getValue(value), viewData, sb);
    sb.append("</span>");
  }

  private <X> Object onBrowserEventImpl(Element parent, final C object, Object viewData,
      NativeEvent event, final ValueUpdater<C> valueUpdater,
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

  private <X> void setValueImpl(Element parent, C object, Object viewData,
      HasCell<C, X> hasCell) {
    hasCell.getCell().setValue(parent, hasCell.getValue(object), viewData);
  }
}
