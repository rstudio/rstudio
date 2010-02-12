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
package com.google.gwt.list.client;

import com.google.gwt.cells.client.Cell;
import com.google.gwt.cells.client.Mutator;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;

/**
 * A representation of a column in a table.
 * 
 * @param <T> the row type
 * @param <C> the column type
 */
public abstract class Column<T, C> {
  private final Cell<C> cell;
  private Mutator<T, C> mutator;

  public Column(Cell<C> cell) {
    this.cell = cell;
  }

  public void onBrowserEvent(Element elem, final T object, NativeEvent event) {
    cell.onBrowserEvent(elem, getValue(object), event, new Mutator<C, C>() {
      public void mutate(C unused, C after) {
        mutator.mutate(object, after);
      }
    });
  }

  public void render(T object, StringBuilder sb) {
    cell.render(getValue(object), sb);
  }

  public void setMutator(Mutator<T, C> mutator) {
    this.mutator = mutator;
  }

  protected Cell<C> getCell() {
    return cell;
  }

  protected abstract C getValue(T object);

}