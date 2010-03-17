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
package com.google.gwt.bikeshed.sample.validation.client;

import com.google.gwt.bikeshed.cells.client.Cell;
import com.google.gwt.bikeshed.cells.client.FieldUpdater;
import com.google.gwt.bikeshed.cells.client.ValueUpdater;
import com.google.gwt.bikeshed.list.client.Column;
import com.google.gwt.bikeshed.sample.validation.client.ValidatableField.DefaultValidatableField;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * A column that support validation.
 * 
 * @param <T> the row type
 * @param <C> the column type
 */
// TODO - avoid wrapping cells that are never modified
public abstract class ValidatableColumn<T, C> extends Column<T, ValidatableField<C>> {
  
  Map<T, ValidatableField<C>> fieldMap = new HashMap<T, ValidatableField<C>>();

  public ValidatableColumn(Cell<ValidatableField<C>> cell) {
    super(cell);
  }

  // Override onBrowserEvent to copy the ValueUpdater value into our copy
  @Override
  public void onBrowserEvent(Element elem, final int index, final T object,
      NativeEvent event) {
    final FieldUpdater<T, ValidatableField<C>> fieldUpdater = getFieldUpdater();
    final ValidatableField<C> field = getValue(object);
    getCell().onBrowserEvent(elem, field, event,
        fieldUpdater == null ? null : new ValueUpdater<ValidatableField<C>>() {
      public void update(ValidatableField<C> value) {
        // Copy pending value from value (copy) to field (original)
        field.setPendingValue(value.getPendingValue());
        fieldUpdater.update(index, object, field);
      }
    });
  }

  /**
   * Returns the value of the field with the underlying object that is to be
   * validated.
   *
   * @param object the underlying data transfer object, of type T
   * @return a value of type C
   */
  protected abstract C getValidatableValue(T object);
  
  @Override
  protected ValidatableField<C> getValue(T object) {
    ValidatableField<C> vfield = fieldMap.get(object);
    if (vfield == null) {
      C validatableValue = getValidatableValue(object);
      vfield = new DefaultValidatableField<C>(validatableValue);
      fieldMap.put(object, vfield);
    }
    
    return vfield;
  }
}
