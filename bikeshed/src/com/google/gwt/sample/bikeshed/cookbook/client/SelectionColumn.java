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
package com.google.gwt.sample.bikeshed.cookbook.client;

import com.google.gwt.bikeshed.cells.client.CheckboxCell;
import com.google.gwt.bikeshed.cells.client.FieldUpdater;
import com.google.gwt.bikeshed.list.client.SimpleColumn;
import com.google.gwt.bikeshed.list.shared.SelectionModel;

/**
 * A column that displays a checkbox that is synchronized with a given
 * selection model.
 * 
 * @param <T> the record data type, used by the row and the selection model
 */
public class SelectionColumn<T> extends SimpleColumn<T, Boolean> {
  
  private final SelectionModel<T> selectionModel;

  public SelectionColumn(final SelectionModel<T> selectionModel) {
    super(new CheckboxCell());
    setFieldUpdater(new FieldUpdater<T, Boolean, Void>() {
      public void update(int index, T object, Boolean value, Void viewData) {
        selectionModel.setSelected(object, value);
      }
    });
    this.selectionModel = selectionModel;
  }

  @Override
  public boolean dependsOnSelection() {
    return true;
  }
  
  @Override
  public Boolean getValue(T object) {
    return selectionModel.isSelected(object);
  }
}
